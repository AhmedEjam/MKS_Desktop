#!/usr/bin/env python3
import json, zipfile, hashlib, sys
from pathlib import Path
if len(sys.argv) < 2:
    print("usage: validate_schema7_fixture.py <fixture.zip> [signature.json]", file=sys.stderr); sys.exit(2)
fixture=Path(sys.argv[1]); meta_path=Path(sys.argv[2]) if len(sys.argv)>2 else fixture.with_suffix('.json')
required=["manifest.json","workspace.json","data/books.json","data/quizzes.json","data/questions.json","data/question_categories.json","data/asset_references.json","data/question_assets.json","data/source_documents.json","data/annotations.json","data/media_manifest.json","data/soft_deletes.json"]
def canonical(o): return json.dumps(o, sort_keys=True, separators=(',',':'), ensure_ascii=False).encode('utf-8')
with zipfile.ZipFile(fixture) as z:
    names=set(z.namelist())
    missing=[p for p in required if p not in names]
    if missing: raise SystemExit(f"missing entries: {missing}")
    payloads={p: json.loads(z.read(p).decode('utf-8')) for p in required}
manifest=payloads['manifest.json']
if manifest.get('format')!='mks.exchange' or manifest.get('schemaVersion')!=7: raise SystemExit('bad manifest identity')
if manifest.get('entries') != required: raise SystemExit('manifest entries are not canonical Stage 4E order')
blob=b''.join(p.encode()+b'\0'+canonical(payloads[p])+b'\n' for p in required)
sig=hashlib.sha256(blob).hexdigest()
meta=json.loads(meta_path.read_text()) if meta_path.exists() else {}
expected=meta.get('canonicalSignatureSha256')
if expected and expected != sig: raise SystemExit(f'signature mismatch: {sig} != {expected}')
print(json.dumps({'status':'PASS','fixture':str(fixture),'signature':sig,'counts':manifest.get('counts')}, indent=2))
