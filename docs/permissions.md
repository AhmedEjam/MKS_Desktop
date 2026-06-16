# Permissions rationale

## INTERNET

Used for remote image loading/downloading and other network-backed assets.

## CAMERA

Used by the scanner/OCR flow.

## READ_MEDIA_IMAGES

Kept for now because image-cover selection and legacy image import flows may still rely on direct image-media access on Android 13+. Prefer the system picker/SAF for future flows and remove this permission if direct MediaStore access is no longer needed.

## ACCESS_NOTIFICATION_POLICY

Used by focus mode to request Android Do Not Disturb access. This remains optional; user-facing copy now explains that the permission is for silencing interruptions during study sessions.

## Removed

`READ_EXTERNAL_STORAGE` was removed. Imports/exports should rely on SAF/content URIs.
