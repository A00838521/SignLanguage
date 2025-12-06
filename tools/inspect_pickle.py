#!/usr/bin/env python3
import pickle, sys
from PIL import Image

path = sys.argv[1]
with open(path, 'rb') as f:
    obj = pickle.load(f)
print('Top-level type:', type(obj))
if isinstance(obj, dict):
    print('Dict keys:', list(obj.keys())[:10])
    for k, v in list(obj.items())[:3]:
        print('Key:', k, 'Value type:', type(v))
elif isinstance(obj, list):
    print('List length:', len(obj))
    for i, v in enumerate(obj[:3]):
        print('Item', i, 'type:', type(v))
        if isinstance(v, (list, tuple)):
            print('  tuple/list lengths:', len(v))
        if isinstance(v, dict):
            print('  dict keys:', list(v.keys()))
else:
    print('Single object:', obj)
