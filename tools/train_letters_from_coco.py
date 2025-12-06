#!/usr/bin/env python3
"""
train_letters_from_coco.py

Entrena un clasificador de letras/gestos estáticos a partir de un dataset en formato COCO JSON
(producido por Roboflow). Asume splits en carpetas: train/, valid/, test/ cada una con
"_annotations.coco.json" y las imágenes referenciadas por "file_name".

Exporta TFLite y labels.json a tools/work/.

Uso:
  python tools/train_letters_from_coco.py --data-dir tools/work/Lengua\ de\ Senas\ Mexicana.v5i.coco --epochs 10 --img-size 160

Dependencias: tensorflow, pillow, numpy
"""
import argparse
import json
import os
from typing import List, Tuple, Optional, Set

import numpy as np
from PIL import Image
import tensorflow as tf
from tensorflow.keras import layers, models
from tensorflow.keras import applications


def load_coco_split(json_path: str, images_base: str, allowed: Optional[Set[str]] = None) -> Tuple[List[Image.Image], List[str]]:
    with open(json_path, 'r') as f:
        coco = json.load(f)
    # categories
    cat_id_to_name = {c['id']: c['name'] for c in coco.get('categories', [])}
    # images
    images = {img['id']: img for img in coco.get('images', [])}
    # annotations
    anns = coco.get('annotations', [])
    # construir etiqueta por imagen: usar la categoría mayoritaria o primera
    img_to_cats: dict[int, List[int]] = {}
    for ann in anns:
        img_id = ann.get('image_id')
        cat_id = ann.get('category_id')
        if img_id is None or cat_id is None:
            continue
        img_to_cats.setdefault(img_id, []).append(cat_id)
    X: List[Image.Image] = []
    y: List[str] = []
    for img_id, meta in images.items():
        file_name = meta.get('file_name')
        if not file_name:
            continue
        path = os.path.join(images_base, file_name)
        if not os.path.exists(path):
            # algunos datasets colocan imágenes junto al json
            alt = os.path.join(os.path.dirname(json_path), file_name)
            if os.path.exists(alt):
                path = alt
            else:
                continue
        # decidir la etiqueta
        cats = img_to_cats.get(img_id, [])
        label = None
        if cats:
            # tomar la primera categoría
            label = cat_id_to_name.get(cats[0])
        # si no hay anotación, intentar deducir del nombre de archivo: "A.jpg" -> "A"
        if not label and file_name:
            base = os.path.basename(file_name)
            stem = os.path.splitext(base)[0]
            if len(stem) >= 1:
                label = stem[0].upper()
        if not label:
            continue
        # Filtrado de clases permitidas
        if allowed is not None and label not in allowed:
            continue
        try:
            pil = Image.open(path).convert('RGB')
        except Exception:
            continue
        X.append(pil)
        y.append(label)
    return X, y


def load_folder_split(split_dir: str, img_size: int, allowed: Optional[Set[str]] = None) -> Tuple[List[Image.Image], List[str]]:
    """Carga dataset con estructura folder/class_name/*.jpg.
    split_dir debe contener subcarpetas por clase.
    """
    X: List[Image.Image] = []
    y: List[str] = []
    if not os.path.isdir(split_dir):
        return X, y
    for class_name in sorted(os.listdir(split_dir)):
        class_path = os.path.join(split_dir, class_name)
        if not os.path.isdir(class_path):
            continue
        label = class_name.strip().upper()
        if allowed is not None and label not in allowed:
            continue
        for fname in os.listdir(class_path):
            fp = os.path.join(class_path, fname)
            if not os.path.isfile(fp):
                continue
            try:
                im = Image.open(fp).convert('RGB')
                X.append(im)
                y.append(label)
            except Exception:
                continue
    return X, y


def build_dataset(images: List[Image.Image], labels: List[str], img_size: int) -> Tuple[np.ndarray, np.ndarray, List[str]]:
    uniq = sorted(set(labels))
    label_to_idx = {c: i for i, c in enumerate(uniq)}
    X = []
    y_idx = []
    for img, lab in zip(images, labels):
        im = img.resize((img_size, img_size), Image.BILINEAR)
        arr = np.asarray(im, dtype=np.float32) / 255.0
        X.append(arr)
        y_idx.append(label_to_idx[lab])
    X = np.stack(X)
    y_arr = np.array(y_idx, dtype=np.int32)
    return X, y_arr, uniq


def build_model(img_size: int, num_classes: int) -> tf.keras.Model:
    inputs = layers.Input(shape=(img_size, img_size, 3))
    x = layers.Rescaling(1.0/255)(inputs)
    x = layers.RandomFlip("horizontal")(x)
    x = layers.RandomRotation(0.1)(x)
    x = layers.RandomZoom(0.1)(x)
    base = applications.MobileNetV2(include_top=False, weights='imagenet', input_shape=(img_size, img_size, 3), pooling='avg')
    base.trainable = False
    x = base(x)
    x = layers.Dropout(0.3)(x)
    outputs = layers.Dense(num_classes, activation='softmax')(x)
    model = models.Model(inputs, outputs)
    model.compile(optimizer=tf.keras.optimizers.Adam(1e-3), loss='sparse_categorical_crossentropy', metrics=['accuracy'])
    return model


def export_tflite(model: tf.keras.Model, out_path: str):
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()
    with open(out_path, 'wb') as f:
        f.write(tflite_model)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--data-dir', required=True, help='Directorio del dataset COCO (con train/valid/test)')
    ap.add_argument('--extra-folder-dataset', type=str, default=None, help='Dataset adicional en carpetas (train/valid/test con clases en subcarpetas)')
    ap.add_argument('--epochs', type=int, default=12)
    ap.add_argument('--img-size', type=int, default=224)
    ap.add_argument('--fine-tune', action='store_true', help='Descongela la base y hace fine-tuning con LR menor')
    ap.add_argument('--allow-classes', type=str, default='A,B,C,D,E,F,G,H,I,L,M,N,O,P,R,S,T,U,V,W,Y,0,1,2,3,4,5,6,7,8,9', help='Lista de clases permitidas separadas por coma')
    args = ap.parse_args()

    # Cargar split train y valid
    train_json = os.path.join(args.data_dir, 'train', '_annotations.coco.json')
    valid_json = os.path.join(args.data_dir, 'valid', '_annotations.coco.json')
    # imágenes suelen estar también en train/ y valid/
    train_imgs_base = os.path.join(args.data_dir, 'train')
    valid_imgs_base = os.path.join(args.data_dir, 'valid')

    allowed = set([c.strip().upper() for c in args.allow_classes.split(',') if c.strip()])
    X_train_pil, y_train = load_coco_split(train_json, train_imgs_base, allowed)
    X_val_pil, y_val = load_coco_split(valid_json, valid_imgs_base, allowed)

    # Merge con dataset adicional en carpetas si se proporciona
    if args.extra_folder_dataset:
        extra_train = os.path.join(args.extra_folder_dataset, 'train')
        extra_valid = os.path.join(args.extra_folder_dataset, 'valid')
        X_train_extra, y_train_extra = load_folder_split(extra_train, args.img_size, allowed)
        X_val_extra, y_val_extra = load_folder_split(extra_valid, args.img_size, allowed)
        X_train_pil.extend(X_train_extra)
        y_train.extend(y_train_extra)
        X_val_pil.extend(X_val_extra)
        y_val.extend(y_val_extra)

    if not X_train_pil or not X_val_pil:
        raise RuntimeError('No se encontraron imágenes/labels en COCO. Verifica que las imágenes existen junto a los JSON.')

    X_train, y_train_arr, classes = build_dataset(X_train_pil, y_train, args.img_size)
    X_val, y_val_arr, _ = build_dataset(X_val_pil, y_val, args.img_size)

    model = build_model(args.img_size, num_classes=len(classes))
    model.fit(X_train, y_train_arr, validation_data=(X_val, y_val_arr), epochs=args.epochs, batch_size=32, verbose=2)

    if args.fine_tune:
        print('Activando fine-tuning...')
        # localizar capa base dentro del modelo (MobileNetV2)
        for layer in model.layers:
            if isinstance(layer, tf.keras.Model) and layer.name.startswith('mobilenetv2'):
                layer.trainable = True
        model.compile(optimizer=tf.keras.optimizers.Adam(1e-4), loss='sparse_categorical_crossentropy', metrics=['accuracy'])
        model.fit(X_train, y_train_arr, validation_data=(X_val, y_val_arr), epochs=max(4, args.epochs//3), batch_size=32, verbose=2)

    val_loss, val_acc = model.evaluate(X_val, y_val_arr, verbose=0)
    print(f"Validación -> loss: {val_loss:.4f}, acc: {val_acc:.4f}")

    out_dir = os.path.join('tools', 'work')
    os.makedirs(out_dir, exist_ok=True)
    model_path = os.path.join(out_dir, 'gesture_frame_mlp.tflite')
    labels_path = os.path.join(out_dir, 'labels.json')

    export_tflite(model, model_path)
    with open(labels_path, 'w') as f:
        json.dump(classes, f, ensure_ascii=False, indent=2)

    print(f"Modelo TFLite exportado: {model_path}")
    print(f"Labels guardadas: {labels_path}")


if __name__ == '__main__':
    main()
