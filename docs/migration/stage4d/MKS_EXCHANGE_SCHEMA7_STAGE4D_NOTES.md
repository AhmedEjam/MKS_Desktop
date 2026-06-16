# MKS Exchange Schema 7 Stage 4D Notes

## Active contract

Schema-7 archives are standard `.zip` files with JSON entries:

- `manifest.json`
- `workspace.json`
- `data/books.json`
- `data/quizzes.json`
- `data/questions.json`
- `data/question_categories.json`
- `data/asset_references.json`
- `data/question_assets.json`
- `data/source_documents.json`
- `data/annotations.json`
- `data/media_manifest.json`
- `data/soft_deletes.json`
- `media/`

## Stage 4D compatibility rule

Schema-7 exchange ZIPs must not use Android legacy AES ZIP encryption. iOS V04 uses a standard ZIP reader. Android keeps encrypted legacy bundle support separately through older import/export paths.

## Stage 4D fixture

`MKS_STAGE4D_SCHEMA7_MINIMAL_FIXTURE.zip` contains one workspace, one book, one quiz, one question, one category link, and no media bytes.
