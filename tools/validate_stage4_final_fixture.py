import json, zipfile, hashlib, sys
from pathlib import Path
zip_path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path('/mnt/data/MKS_STAGE4_FINAL_SCHEMA7_MEDIA_FIXTURE.zip')
with zipfile.ZipFile(zip_path) as z:
    names = set(z.namelist())
    manifest = json.loads(z.read('manifest.json'))
    media = json.loads(z.read('data/media_manifest.json'))
    assert manifest['format'] == 'mks.exchange'
    assert manifest['schemaVersion'] == 7
    assert manifest['includesMedia'] is True
    assert media['files'], 'expected media file'
    item = media['files'][0]
    assert item['archivePath'] in names, item['archivePath']
    payload = z.read(item['archivePath'])
    assert hashlib.sha256(payload).hexdigest() == item['sha256']
print('PASS: Stage 4 final fixture has schema-7 manifest, media_manifest, media payload, and matching sha256')
