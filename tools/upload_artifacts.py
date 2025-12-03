import argparse
from pathlib import Path
from google.cloud import storage
from google.oauth2 import service_account


def upload_file(bucket, local_path: Path, remote_path: str):
    blob = bucket.blob(remote_path)
    blob.upload_from_filename(str(local_path))
    print(f"Subido: {local_path} -> gs://{bucket.name}/{remote_path}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--service_account', required=True)
    parser.add_argument('--storage_bucket', required=True)
    parser.add_argument('--workdir', default='tools/work')
    args = parser.parse_args()

    creds = service_account.Credentials.from_service_account_file(args.service_account)
    client = storage.Client(project=creds.project_id, credentials=creds)
    bucket = client.bucket(args.storage_bucket)

    workdir = Path(args.workdir)
    tflite = workdir / 'gesture_frame_mlp.tflite'
    labels = workdir / 'labels.json'

    if not tflite.exists() or not labels.exists():
        raise FileNotFoundError("Faltan artefactos: gesture_frame_mlp.tflite o labels.json en workdir")

    upload_file(bucket, tflite, 'models/gesture_frame_mlp.tflite')
    upload_file(bucket, labels, 'models/labels.json')


if __name__ == '__main__':
    main()
