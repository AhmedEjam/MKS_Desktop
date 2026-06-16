# Legacy root-source review

The project archive contained root-level `data/`, `di/`, and `ui/` Kotlin source trees outside the Gradle `app/src/main/java` source set.

These files use app package names and overlap active app code, but they are not part of the Android module source set. Keeping them at the repository root makes it easy to edit stale code by mistake.

Action taken in this patch:

- moved the root-level `data/`, `di/`, and `ui/` folders to `archive/legacy-root-sources/`;
- left active app code under `app/src/main/java/...` only;
- preserved the legacy files for manual review instead of deleting them outright.

Recommended follow-up: compare the archive files once more after the patched app compiles in a networked Android environment, then delete the archive if no unique logic remains.
