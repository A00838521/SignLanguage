#!/usr/bin/env python3
"""
train_letters_from_pickle.py

Entrena un clasificador de letras (estáticas) desde un archivo .pickle con imágenes y etiquetas.
Exporta el modelo a TFLite y labels.json en tools/work/.

Requisitos (instalar en tu entorno Python):
- tensorflow>=2.12
- scikit-learn
- numpy
- pillow

Uso:
  python tools/train_letters_from_pickle.py --pickle ABECEDARIOIMAGENES.pickle --epochs 10 --img-size 160

El pickle debe contener una estructura como:
  {
    'images': List[bytes|np.ndarray|PIL.Image],
    'labels': List[str]
  }
Las imágenes se normalizan y se redimensionan a tamaño cuadrado. Las etiquetas se normalizan a una sola letra mayúscula o dígito.
"""
import argparse
import json
import os
import pickle
from typing import List, Tuple
import io

import numpy as np
from PIL import Image
import tensorflow as tf
from tensorflow.keras import layers, models


def normalize_label(raw: str) -> str | None:
    s = raw.strip().lower()
    if s.endswith('-web'):
        s = s[:-4]
    # quitar extensión si la hay
    if '.' in s:
        s = s.split('.', 1)[0]
    if len(s) == 1:
        ch = s[0]
        if ch.isalpha():
            return ch.upper()
        if ch.isdigit():
            return ch
    return None


def load_pickle(path: str) -> Tuple[List[Image.Image], List[str]]:
    with open(path, 'rb') as f:
        data = pickle.load(f)
    images: List[Image.Image] = []
    labels: List[str] = []

    # Soportar múltiples formatos de pickle:
    # 1) dict con keys 'images' y 'labels'
    # 2) lista de tuplas (image, label)
    # 3) dict mapping label -> list(images)
    if isinstance(data, dict):
        if 'images' in data and 'labels' in data:
            raws = data['images']
            labs = data['labels']
            if len(raws) != len(labs):
                raise ValueError('images y labels deben tener la misma longitud')
            pairs = zip(raws, labs)
        else:
            # mapping label -> list(images)
            pairs = []
            for lab, imgs in data.items():
                for img in imgs if isinstance(imgs, list) else [imgs]:
                    pairs.append((img, lab))
    elif isinstance(data, list):
        # lista de rutas, (image,label) o dicts
        pairs = []
        for item in data:
            if isinstance(item, str):
                # ruta a imagen; deducir label del basename
                lab = os.path.basename(item)
                pairs.append((item, lab))
            elif isinstance(item, (tuple, list)) and len(item) >= 2:
                img, lab = item[0], item[1]
                pairs.append((img, lab))
            elif isinstance(item, dict) and 'image' in item and 'label' in item:
                pairs.append((item['image'], item['label']))
            else:
                lbl = getattr(item, 'label', None) or getattr(item, 'name', None) or ''
                pairs.append((item, lbl))
    else:
        raise ValueError('Formato de pickle no soportado: se esperaba dict o list')

    for img, lab in pairs:
        labn = normalize_label(str(lab))
        if labn is None:
            continue
        # convertir a PIL.Image
        pil = None
        if isinstance(img, Image.Image):
            pil = img.convert('RGB')
        elif isinstance(img, (bytes, bytearray)):
            pil = Image.open(io.BytesIO(img)).convert('RGB')
        elif isinstance(img, np.ndarray):
            if img.ndim == 2:
                pil = Image.fromarray(img).convert('RGB')
            elif img.ndim == 3:
                pil = Image.fromarray(img)
                if pil.mode != 'RGB':
                    pil = pil.convert('RGB')
        elif isinstance(img, str):
            try:
                pil = Image.open(img).convert('RGB')
            except Exception:
                pil = None
        if pil is None:
            # intentar casos donde el pickle trae objetos PIL serializados raramente
            try:
                pil = Image.open(io.BytesIO(pickle.dumps(img))).convert('RGB')
            except Exception:
                continue
        images.append(pil)
        labels.append(labn)

    if not images:
        raise ValueError('No se pudieron cargar imágenes válidas del pickle')
    return images, labels


def build_dataset(images: List[Image.Image], labels: List[str], img_size: int) -> Tuple[np.ndarray, np.ndarray, List[str]]:
    uniq = sorted(set(labels))
    label_to_idx = {c: i for i, c in enumerate(uniq)}
    X = []
    y = []
    for img, lab in zip(images, labels):
        im = img.resize((img_size, img_size), Image.BILINEAR)
        arr = np.asarray(im, dtype=np.float32) / 255.0
        X.append(arr)
        y.append(label_to_idx[lab])
    X = np.stack(X)
    y = np.array(y, dtype=np.int32)
    return X, y, uniq


def build_model(img_size: int, num_classes: int) -> tf.keras.Model:
    # modelo pequeño de CNN para rapidez
    inputs = layers.Input(shape=(img_size, img_size, 3))
    x = layers.Conv2D(32, 3, activation='relu')(inputs)
    x = layers.MaxPooling2D()(x)
    x = layers.Conv2D(64, 3, activation='relu')(x)
    x = layers.MaxPooling2D()(x)
    x = layers.Conv2D(128, 3, activation='relu')(x)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dropout(0.2)(x)
    outputs = layers.Dense(num_classes, activation='softmax')(x)
    model = models.Model(inputs, outputs)
    model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])
    return model


def export_tflite(model: tf.keras.Model, out_path: str):
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()
    with open(out_path, 'wb') as f:
        f.write(tflite_model)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--pickle', required=True, help='Ruta al archivo .pickle con imágenes y labels')
    ap.add_argument('--epochs', type=int, default=10)
    ap.add_argument('--img-size', type=int, default=160)
    ap.add_argument('--images-dir', type=str, default='', help='Directorio base para rutas que vengan sin path en el pickle')
    args = ap.parse_args()

    images, labels = load_pickle(args.pickle)
    # Si las labels provienen de nombres como " 5.png" sin ruta, intentar cargar desde images-dir
    if args.images_dir:
        fixed_images = []
        fixed_labels = []
        for img, lab in zip(images, labels):
            if isinstance(img, str) and not os.path.isabs(img):
                cand = os.path.join(args.images_dir, img.strip())
                if os.path.exists(cand):
                    try:
                        pil = Image.open(cand).convert('RGB')
                        fixed_images.append(pil)
                        fixed_labels.append(lab)
                        continue
                    except Exception:
                        pass
            # si ya es PIL o ruta válida cargada, mantener
            from PIL.Image import Image as PILImage
            if isinstance(img, PILImage):
                fixed_images.append(img)
                fixed_labels.append(lab)
            elif isinstance(img, str) and os.path.exists(img):
                try:
                    pil = Image.open(img).convert('RGB')
                    fixed_images.append(pil)
                    fixed_labels.append(lab)
                except Exception:
                    pass
        if fixed_images:
            images, labels = fixed_images, fixed_labels
    X, y, classes = build_dataset(images, labels, args.img_size)

    # split train/val
    n = len(X)
    idx = np.arange(n)
    np.random.shuffle(idx)
    split = int(0.8 * n)
    train_idx, val_idx = idx[:split], idx[split:]
    X_train, y_train = X[train_idx], y[train_idx]
    X_val, y_val = X[val_idx], y[val_idx]

    model = build_model(args.img_size, num_classes=len(classes))
    model.fit(X_train, y_train, validation_data=(X_val, y_val), epochs=args.epochs, batch_size=32)

    out_dir = os.path.join('tools', 'work')
    os.makedirs(out_dir, exist_ok=True)
    model_path = os.path.join(out_dir, 'gesture_frame_mlp.tflite')
    labels_path = os.path.join(out_dir, 'labels.json')

    export_tflite(model, model_path)
    with open(labels_path, 'w') as f:
        json.dump(classes, f)

    print(f"Modelo TFLite exportado: {model_path}")
    print(f"Labels guardadas: {labels_path}")


if __name__ == '__main__':
    main()
