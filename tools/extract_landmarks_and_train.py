#!/usr/bin/env python3
"""
Pipeline:
- Conecta a Firebase con service account (JSON proporcionado por el usuario).
- Lista colecciones de Firestore (videos, images) y/o Storage rutas públicas.
- Descarga cada video/imagen, extrae landmarks de mano con MediaPipe (Python).
- Genera dataset (CSV/NPY) de landmarks por clase (slug).
- Si hay suficientes muestras por clase, entrena un clasificador simple (MLP) frame-based.
- Exporta un modelo TFLite (.tflite) y un mapeo de etiquetas.

Notas:
- Con una sola imagen/video por seña, el entrenamiento no será robusto; el script validará mínimos y abortará entrenamiento si no se alcanza el umbral.
- Para señas dinámicas, este prototipo extrae landmarks por frame (video) y usa agregación simple; mejorar a modelos temporales requerirá más datos.
"""

import argparse
import os
import sys
import tempfile
from pathlib import Path
import json
import shutil

import numpy as np

# Dependencias requeridas: mediapipe, opencv-python, google-cloud-storage, google-cloud-firestore, firebase-admin, scikit-learn, tensorflow

def require(module_name: str):
    try:
        __import__(module_name)
    except ImportError:
        print(f"ERROR: Falta el paquete '{module_name}'. Instálalo en tu venv.")
        sys.exit(1)

for m in [
    'cv2',
    'mediapipe',
    'google.cloud.storage',
    'google.cloud.firestore',
    'firebase_admin',
    'sklearn',
    'tensorflow',
]:
    require(m.split('.')[0])

import cv2
import mediapipe as mp
from firebase_admin import credentials as fb_credentials, initialize_app
from google.cloud import storage
from google.cloud import firestore
from google.oauth2 import service_account
from sklearn.model_selection import train_test_split
from sklearn.neural_network import MLPClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
import tensorflow as tf


def init_firebase(service_account_path: str, storage_bucket: str):
    # Firebase Admin for some operations (optional)
    fb_cred = fb_credentials.Certificate(service_account_path)
    app = initialize_app(fb_cred, {'storageBucket': storage_bucket})

    # Google auth credentials for Cloud clients
    gcred = service_account.Credentials.from_service_account_file(service_account_path)
    project_id = json.load(open(service_account_path)).get('project_id')

    fs = firestore.Client(project=project_id, credentials=gcred)
    st = storage.Client(project=project_id, credentials=gcred)
    bucket = st.bucket(storage_bucket)
    return app, fs, bucket


def list_media(fs):
    videos = list(fs.collection('videos').stream())
    images = list(fs.collection('images').stream())
    def to_items(docs, kind):
        items = []
        for d in docs:
            data = d.to_dict() or {}
            slug = data.get('slug') or d.id
            storage_path = data.get('storagePath') or data.get('videoStoragePath')
            category = data.get('category') or 'unknown'
            title = data.get('title') or slug
            if storage_path:
                items.append({'slug': slug, 'storagePath': storage_path, 'category': category, 'title': title, 'type': kind})
        return items
    return to_items(videos, 'video') + to_items(images, 'image')


def download_from_storage(bucket, storage_path: str, out_dir: Path) -> Path:
    out_dir.mkdir(parents=True, exist_ok=True)
    filename = storage_path.split('/')[-1]
    out_path = out_dir / filename
    blob = bucket.blob(storage_path)
    blob.download_to_filename(str(out_path))
    return out_path


def extract_landmarks_from_image(img_path: Path):
    img = cv2.imread(str(img_path))
    if img is None:
        return []
    img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    mp_hands = mp.solutions.hands
    results_out = []
    with mp_hands.Hands(static_image_mode=True, max_num_hands=2) as hands:
        result = hands.process(img_rgb)
        if result.multi_hand_landmarks and result.multi_handedness:
            for lm, handed in zip(result.multi_hand_landmarks, result.multi_handedness):
                points = [(p.x, p.y) for p in lm.landmark]
                label = handed.classification[0].label  # 'Left' or 'Right'
                results_out.append({'landmarks': points, 'handedness': label})
    return results_out


def extract_landmarks_from_video(video_path: Path, max_frames: int = 32):
    cap = cv2.VideoCapture(str(video_path))
    if not cap.isOpened():
        return []
    mp_hands = mp.solutions.hands
    out = []
    with mp_hands.Hands(static_image_mode=False, max_num_hands=2) as hands:
        frame_count = 0
        while True:
            ret, frame = cap.read()
            if not ret:
                break
            frame_count += 1
            if frame_count % 2 == 1:  # submuestreo ligero
                frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                result = hands.process(frame_rgb)
                if result.multi_hand_landmarks and result.multi_handedness:
                    per_frame = []
                    for lm, handed in zip(result.multi_hand_landmarks, result.multi_handedness):
                        points = [(p.x, p.y) for p in lm.landmark]
                        label = handed.classification[0].label
                        per_frame.append({'landmarks': points, 'handedness': label})
                    out.append(per_frame)
            if len(out) >= max_frames:
                break
    cap.release()
    return out


def landmarks_to_features(points):
    # Normaliza por muñeca (0) y escala por distancia muñeca->medio (9)
    if not points or len(points) < 10:
        return None
    wr = np.array(points[0])
    mid = np.array(points[9])
    scale = np.linalg.norm(mid - wr) + 1e-6
    pts = (np.array(points) - wr) / scale
    return pts.flatten()  # 21*2 = 42 features


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--service_account', required=True, help='Ruta al JSON de service account')
    parser.add_argument('--storage_bucket', required=True, help='ID del bucket de Storage, ej. signlanguage-XXXX.appspot.com')
    parser.add_argument('--workdir', default='tools/work', help='Directorio de trabajo')
    parser.add_argument('--min_per_class', type=int, default=5, help='Mínimas muestras por clase para entrenar')
    args = parser.parse_args()

    workdir = Path(args.workdir)
    workdir.mkdir(parents=True, exist_ok=True)

    print('Inicializando Firebase...')
    _, fs, bucket = init_firebase(args.service_account, args.storage_bucket)
    items = list_media(fs)
    print(f'Total media items: {len(items)}')

    dataset_X = []
    dataset_y = []
    class_counts = {}

    tmpdir = workdir / 'downloads'
    tmpdir.mkdir(parents=True, exist_ok=True)

    for it in items:
        slug = it['slug']
        storage_path = it['storagePath']
        typ = it['type']
        local_path = download_from_storage(bucket, storage_path, tmpdir)
        print(f'Descargado: {storage_path} -> {local_path}')
        if typ == 'image':
            hands = extract_landmarks_from_image(local_path)
            for h in hands:
                feats = landmarks_to_features(h['landmarks'])
                if feats is not None:
                    dataset_X.append(feats)
                    dataset_y.append(slug)
        else:
            frames = extract_landmarks_from_video(local_path)
            # Agrega algunos frames muestreados por clase
            for per_frame in frames:
                # Usa la primera mano si hay múltiples
                if per_frame:
                    feats = landmarks_to_features(per_frame[0]['landmarks'])
                    if feats is not None:
                        dataset_X.append(feats)
                        dataset_y.append(slug)

    # Estadísticas por clase
    for c in dataset_y:
        class_counts[c] = class_counts.get(c, 0) + 1
    print('Muestras por clase:', json.dumps(class_counts, indent=2, ensure_ascii=False))

    # Verifica mínimos
    ok_classes = {c for c, n in class_counts.items() if n >= args.min_per_class}
    if not ok_classes:
        print('No hay suficientes muestras por clase para entrenar (min_per_class=%d). Exporto solo el dataset.' % args.min_per_class)
        np.save(workdir / 'X.npy', np.array(dataset_X))
        np.save(workdir / 'y.npy', np.array(dataset_y))
        return

    X = np.array([x for x, y in zip(dataset_X, dataset_y) if y in ok_classes])
    y = np.array([y for y in dataset_y if y in ok_classes])

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

    print('Entrenando MLP (frame-based)...')
    clf = Pipeline([
        ('scaler', StandardScaler()),
        ('mlp', MLPClassifier(hidden_layer_sizes=(128, 64), activation='relu', max_iter=200))
    ])
    clf.fit(X_train, y_train)
    acc = clf.score(X_test, y_test)
    print(f'Accuracy holdout: {acc:.3f}')

    # Exporta como TFLite (simple) mediante un modelo Keras equivalente
    print('Exportando modelo TFLite...')
    classes = sorted(set(y))
    with open(workdir / 'labels.json', 'w') as f:
        json.dump(classes, f, ensure_ascii=False, indent=2)

    # Construye red equivalente en Keras con Normalization adaptada
    from tensorflow import keras
    from tensorflow.keras import layers
    input_dim = X.shape[1]
    num_classes = len(classes)
    norm_layer = layers.Normalization()
    norm_layer.adapt(X_train)
    model = keras.Sequential([
        layers.Input(shape=(input_dim,)),
        norm_layer,
        layers.Dense(128, activation='relu'),
        layers.Dense(64, activation='relu'),
        layers.Dense(num_classes, activation='softmax')
    ])
    model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])
    # Mapea labels a índices
    class_to_idx = {c: i for i, c in enumerate(classes)}
    y_train_idx = np.array([class_to_idx[v] for v in y_train])
    y_test_idx = np.array([class_to_idx[v] for v in y_test])
    model.fit(X_train, y_train_idx, validation_data=(X_test, y_test_idx), epochs=10, batch_size=32, verbose=2)

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()
    out_path = workdir / 'gesture_frame_mlp.tflite'
    with open(out_path, 'wb') as f:
        f.write(tflite_model)
    print(f'Modelo TFLite escrito en {out_path}')

    print('Listo. Sube labels y el .tflite a Storage para integrarlo en la app.')


if __name__ == '__main__':
    main()

