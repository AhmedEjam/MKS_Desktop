# MKS Migration Concepts: Android to Desktop (KMP)

This document explains the technical shift from an Android-only architecture to a Compose Multiplatform structure, ensuring compatibility between Android and Desktop through a shared core.

## 1. Database: Room → SQLDelight

### The Shift
*   **Room (Android-only):** Tied to the Android SDK. Uses annotations (`@Entity`, `@Dao`) and reflection.
*   **SQLDelight (Multiplatform):** Generates Kotlin code from pure SQL files. Uses platform-specific drivers (Android SQLite driver vs. JVM JDBC driver).

### Why for MKS?
*   **Identical Schema:** The schema (26 tables) remains exactly the same.
*   **Compile-Time Safety:** Errors in SQL are caught during development in the `.sq` files.
*   **Unified Logic:** Both platforms use the exact same Repository code, guaranteeing that data exported from one will always be valid in the other.

---

## 2. File Access: URIs → Direct Paths (FileManager Interface)

### The Shift
*   **Android (Legacy):** Relies on `Uri` and `ContentResolver` (security-scoped access).
*   **Desktop/KMP:** Uses standard absolute file paths (e.g., `/home/user/.local/share/mks/`).

### The Solution: `FileManager` Interface
We use an "Expect/Actual" pattern or a platform-agnostic interface:
*   **Common:** Defines methods like `saveImage(bytes)`.
*   **Android Impl:** Maps to `context.filesDir`.
*   **Desktop Impl:** Maps to standard Linux FHS paths (`~/.local/share/mks`).
*   **Communication:** Bundles use relative paths internally, which the `FileManager` on the receiving platform resolves to its own local storage.

---

## 3. Dependency Injection: Hilt → Koin

### The Shift
*   **Hilt (Android-centric):** Generates Java code and is tied to Android Lifecycles (Activities/Fragments).
*   **Koin (Pure Kotlin):** A lightweight DSL-based DI framework that works anywhere Kotlin runs.

### Why for MKS?
*   **Lazy Resolution:** Koin uses `by inject()`, which solves the circular dependency issues (e.g., BookRepo needing QuizRepo and vice-versa) common in the MKS repository layer.
*   **No Boilerplate:** No need for `@AndroidEntryPoint` or complex Hilt Modules; just simple Kotlin `module { ... }` definitions.

---

## 4. ZIP Handling: Zip4j → java.util.zip

### The Shift
*   **Zip4j (External Library):** A specialized library with its own API surface.
*   **java.util.zip (JVM Standard):** Built into the core of every Java/Android environment.

### Why for Communication?
*   **Standardization:** Using the most basic JVM tool for ZIP creation ensures that a bundle created on Fedora Linux is 100% compatible with the ZIP extractor on an Android phone.
*   **Portability:** It removes an external dependency from the `shared` module, keeping the cross-platform bridge "lean."

---

## Summary: The "Shared Language"
By migrating these four "pipes," the **Shared Module** now acts as the single source of truth. An **Import/Export bundle** is simply a `LibraryBundleDto` (the shared language) packaged into a standard ZIP by the `ExportManager`, which can be opened and understood by the `ImportManager` on any supported platform.
