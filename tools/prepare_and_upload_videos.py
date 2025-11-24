#!/usr/bin/env python3
import argparse
import concurrent.futures
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

try:
    from google.cloud import storage
    from google.cloud import firestore
    import firebase_admin
    from firebase_admin import credentials
except ImportError:
    print("Missing packages. Run: pip install -r tools/requirements.txt", file=sys.stderr)
    sys.exit(1)


def slugify(text: str) -> str:
    import unicodedata
    text = unicodedata.normalize('NFKD', text)
    text = text.encode('ascii', 'ignore').decode('ascii')
    text = re.sub(r'[^a-zA-Z0-9\s-]', '', text)
    text = re.sub(r'[\s_-]+', '-', text)
    return text.strip('-').lower()


def detect_category(folder_name: str) -> str:
    # Expected: LSM_Abecedario_Web -> abecedario
    m = re.match(r"LSM_(.+?)_Web", folder_name, flags=re.IGNORECASE)
    if m:
        return slugify(m.group(1)).replace('-', '_')
    return slugify(folder_name).replace('-', '_')


def build_title(file_name: str) -> str:
    base = Path(file_name).stem
    base = base.replace('_', ' ').strip()
    return base[:1].upper() + base[1:]


def ensure_gcloud_clients(project_id: str, bucket_name: str | None):
    # Initialize firebase_admin for Firestore
    if not firebase_admin._apps:
        cred_path = os.environ.get('GOOGLE_APPLICATION_CREDENTIALS')
        if not cred_path or not Path(cred_path).exists():
            print("ERROR: Set GOOGLE_APPLICATION_CREDENTIALS to your service account JSON", file=sys.stderr)
            sys.exit(2)
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred, {
            'projectId': project_id,
            **({'storageBucket': bucket_name} if bucket_name else {})
        })

    fs = firestore.Client(project=project_id)

    # Storage client
    st = storage.Client(project=project_id)
    if bucket_name is None:
        # Default Firebase bucket (may be appspot.com in proyectos antiguos)
        bucket_name = f"{project_id}.appspot.com"
    # Usa el bucket tal cual; si no existe, el error será claro y el usuario puede ajustar
    bucket = st.bucket(bucket_name)
    return fs, bucket


def transcode_ffmpeg(src: Path, out_dir: Path, *, crf: int = 23, scale: str = '1280:-2', audio_bitrate: str = '128k') -> Path:
    out = out_dir / (src.stem + '.mp4')
    cmd = [
        'ffmpeg', '-y', '-i', str(src),
        '-vf', f'scale={scale}',
        '-c:v', 'libx264', '-preset', 'medium', '-crf', str(crf), '-pix_fmt', 'yuv420p',
        '-c:a', 'aac', '-b:a', audio_bitrate,
        str(out)
    ]
    subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
    return out


def upload_object(bucket, local_path: Path, remote_path: str, public: bool = False) -> str:
    blob = bucket.blob(remote_path)
    blob.upload_from_filename(str(local_path))
    if public:
        blob.make_public()
        return blob.public_url
    return f"gs://{bucket.name}/{remote_path}"


def create_firestore_video(fs, *, doc_id: str, title: str, description: str, storage_path: str, category: str, level: str):
    fs.collection('videos').document(doc_id).set({
        'id': doc_id,
        'title': title,
        'description': description,
        'storagePath': storage_path,
        'category': category,
        'level': level,
    })


def create_firestore_image(fs, *, doc_id: str, title: str, description: str, storage_path: str, category: str, level: str):
    fs.collection('images').document(doc_id).set({
        'id': doc_id,
        'title': title,
        'description': description,
        'storagePath': storage_path,
        'category': category,
        'level': level,
        'type': 'image'
    })


def process_one(fs, bucket, src_file: Path, category: str, level: str, dest_prefix: str, tmpdir: Path, args) -> dict:
    title = build_title(src_file.name)
    slug = slugify(title)
    out = transcode_ffmpeg(src_file, tmpdir, crf=args.crf, scale=args.scale, audio_bitrate=args.audio_bitrate)
    storage_path = f"{dest_prefix}/{category}/{slug}.mp4"
    url = upload_object(bucket, out, storage_path, public=args.public)
    if not args.dry_run:
        create_firestore_video(
            fs,
            doc_id=slug,
            title=title,
            description=f"Seña: {title}",
            storage_path=storage_path,
            category=category,
            level=level,
        )
    return {
        'id': slug,
        'title': title,
        'storagePath': storage_path,
        'category': category,
        'level': level,
        'url': url,
    }


def iter_video_files(root: Path):
    for folder in sorted(root.iterdir()):
        if not folder.is_dir():
            continue
        category = detect_category(folder.name)
        for f in sorted(folder.glob('**/*')):
            if f.suffix.lower() in {'.m4v', '.mp4', '.mov'} and f.is_file():
                yield category, f


def iter_image_files(root: Path):
    exts = {'.jpg', '.jpeg', '.png', '.webp'}
    for folder in sorted(root.iterdir()):
        if not folder.is_dir():
            continue
        category = detect_category(folder.name)
        for f in sorted(folder.glob('**/*')):
            if f.suffix.lower() in exts and f.is_file():
                yield category, f


def main():
    parser = argparse.ArgumentParser(description='Transcodifica/sube videos y sube imágenes LSM a Firebase Storage y Firestore.')
    parser.add_argument('--base-path', type=Path, required=True, help='Carpeta raíz con subcarpetas LSM_*_Web')
    parser.add_argument('--project-id', required=True, help='ID de proyecto Firebase')
    parser.add_argument('--bucket', help='Nombre del bucket (por defecto <project-id>.appspot.com)')
    parser.add_argument('--dest-prefix', default='videos', help='Prefijo remoto en Storage (default: videos)')
    parser.add_argument('--images-dest-prefix', default='images', help='Prefijo remoto de imágenes en Storage (default: images)')
    parser.add_argument('--level', default='basico', help='Nivel por defecto (A1/A2/B1 o basico/intermedio/avanzado)')
    parser.add_argument('--public', action='store_true', help='Hacer públicos los objetos (URLs públicas)')
    parser.add_argument('--dry-run', action='store_true', help='No crear documentos en Firestore (solo subir)')
    parser.add_argument('--create-docs-only', action='store_true', help='Solo crear documentos en Firestore para objetos ya existentes en Storage')
    parser.add_argument('--include-images', action='store_true', help='Incluir imágenes encontradas en las carpetas y crear docs en Firestore')
    parser.add_argument('--crf', type=int, default=23, help='Calidad H.264 CRF (menor = más calidad)')
    parser.add_argument('--scale', default='1280:-2', help='Escala de video ffmpeg, p.ej. 1280:-2 (720p) o 960:-2 (540p)')
    parser.add_argument('--audio-bitrate', default='128k', help='Bitrate de audio AAC')
    parser.add_argument('--workers', type=int, default=max(1, os.cpu_count() or 1), help='Paralelismo de transcodificación/subida')
    parser.add_argument('--manifest', type=Path, default=Path('tools/videos_manifest.jsonl'), help='Ruta del manifest generado')
    parser.add_argument('--images-manifest', type=Path, default=Path('tools/images_manifest.jsonl'), help='Ruta del manifest de imágenes generado')
    args = parser.parse_args()

    fs, bucket = ensure_gcloud_clients(args.project_id, args.bucket)

    base = args.base_path
    if not base.exists():
        print(f"Base path not found: {base}", file=sys.stderr)
        sys.exit(3)

    # Modo: solo crear documentos, sin transcodificar ni subir
    if args.create_docs_only:
        created = 0
        processed = 0
        for category, f in iter_video_files(base):
            processed += 1
            title = build_title(f.name)
            slug = slugify(title)
            storage_path = f"{args.dest_prefix}/{category}/{slug}.mp4"
            blob = bucket.blob(storage_path)
            if blob.exists(storage.Client(project=args.project_id)):
                create_firestore_video(
                    fs,
                    doc_id=slug,
                    title=title,
                    description=f"Seña: {title}",
                    storage_path=storage_path,
                    category=category,
                    level=args.level,
                )
                created += 1
                print(f"Doc creado: videos/{slug} -> {storage_path}")
            else:
                print(f"Omitido (no existe en Storage): {storage_path}")
        # Imágenes (opcional)
        if args.include_images:
            img_created = 0
            img_processed = 0
            for category, f in iter_image_files(base):
                img_processed += 1
                title = build_title(f.name)
                slug = slugify(title)
                ext = f.suffix.lower().lstrip('.')
                storage_path = f"{args.images_dest_prefix}/{category}/{slug}.{ext}"
                blob = bucket.blob(storage_path)
                if blob.exists(storage.Client(project=args.project_id)):
                    create_firestore_image(
                        fs,
                        doc_id=slug,
                        title=title,
                        description=f"Imagen: {title}",
                        storage_path=storage_path,
                        category=category,
                        level=args.level,
                    )
                    img_created += 1
                    print(f"Doc creado: images/{slug} -> {storage_path}")
                else:
                    print(f"Omitido (no existe en Storage): {storage_path}")
            print(f"Listo imágenes. Documentos creados: {img_created}/{img_processed}")
        print(f"Listo videos. Documentos creados: {created}/{processed}")
        return

    tmpdir = Path(tempfile.mkdtemp(prefix='lsm_transcode_'))
    try:
        records = []
        img_records = []
        items = list(iter_video_files(base))
        print(f"Encontrados {len(items)} archivos de video…")

        def worker(item):
            category, f = item
            return process_one(fs, bucket, f, category, args.level, args.dest_prefix, tmpdir, args)

        with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as ex:
            for rec in ex.map(worker, items):
                records.append(rec)
                print(f"Subido: {rec['title']} -> {rec['storagePath']}")

        args.manifest.parent.mkdir(parents=True, exist_ok=True)
        with args.manifest.open('w', encoding='utf-8') as w:
            for r in records:
                w.write(json.dumps(r, ensure_ascii=False) + '\n')
        print(f"Manifest escrito en {args.manifest}")

        # Procesar imágenes si procede (sin transcodificación; subida directa)
        if args.include_images:
            img_items = list(iter_image_files(base))
            print(f"Encontrados {len(img_items)} archivos de imagen…")

            def img_worker(item):
                category, f = item
                title = build_title(f.name)
                slug = slugify(title)
                ext = f.suffix.lower().lstrip('.')
                storage_path = f"{args.images_dest_prefix}/{category}/{slug}.{ext}"
                url = upload_object(bucket, f, storage_path, public=args.public)
                if not args.dry_run:
                    create_firestore_image(
                        fs,
                        doc_id=slug,
                        title=title,
                        description=f"Imagen: {title}",
                        storage_path=storage_path,
                        category=category,
                        level=args.level,
                    )
                return {
                    'id': slug,
                    'title': title,
                    'storagePath': storage_path,
                    'category': category,
                    'level': args.level,
                    'url': url,
                    'type': 'image'
                }

            with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as ex:
                for rec in ex.map(img_worker, img_items):
                    img_records.append(rec)
                    print(f"Subida imagen: {rec['title']} -> {rec['storagePath']}")

            args.images_manifest.parent.mkdir(parents=True, exist_ok=True)
            with args.images_manifest.open('w', encoding='utf-8') as w:
                for r in img_records:
                    w.write(json.dumps(r, ensure_ascii=False) + '\n')
            print(f"Manifest de imágenes escrito en {args.images_manifest}")
    finally:
        shutil.rmtree(tmpdir, ignore_errors=True)


if __name__ == '__main__':
    main()
