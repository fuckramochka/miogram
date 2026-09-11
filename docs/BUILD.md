# Miogram Build & Compilation Guide

> **Miogram / Міограм** · Developer Build Guide  
> Author: **[@dkramochka](https://t.me/dkramochka)**

---

## 1. Environment & Prerequisites

To build Miogram for Android from source, your development workstation requires:

* **Operating System:** Linux (Ubuntu 22.04+ recommended), macOS (Apple Silicon or Intel), or Windows 10/11 with WSL2 / PowerShell.
* **Java Development Kit (JDK):** **JDK 21** (Eclipse Temurin or OpenJDK).
  ```bash
  java -version # Must report openjdk 21.x
  ```
* **Android SDK:**
  * Compile SDK: `37` (Android 15+)
  * Min SDK: `26` (Android 8.0 Oreo)
  * Target SDK: `35` / `37`
  * Build-Tools: `36.0.0`
* **Android NDK:** `27.2.12479018` or `26.1.10909125`
* **Rust Toolchain (for WASM SDK):**
  * `rustup default stable`
  * `rustup target add wasm32-unknown-unknown`
* **Python:** `3.11+` (required by Chaquopy build steps).

---

## 2. Android 15 & 16 KB ELF Page Size Alignment

Starting with Android 15, devices support and enforce **16 KB memory page sizes**. Binaries compiled with legacy 4 KB alignment will crash immediately on newer kernel environments.

Miogram explicitly sets linker flags in `TMessagesProj/jni/CMakeLists.txt`:
```cmake
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")
```

To verify ELF alignment on built `.so` shared libraries:
```bash
llvm-readelf -l /path/to/libtmessages.so | grep -A 1 LOAD
```
Ensure all `LOAD` segment `Align` values are `0x4000` (16384 bytes).

---

## 3. Cloning & Building

### 3.1. Clone Repository (With Submodules)
```bash
git clone --recursive https://github.com/fuckramochka/miogram.git
cd miogram
```

### 3.2. Local Properties Configuration
Create or edit `local.properties` at the project root:
```properties
sdk.dir=/path/to/Android/Sdk
ndk.dir=/path/to/Android/Sdk/ndk/27.2.12479018
```

### 3.3. Assemble APK via Gradle
```bash
# Debug APK:
./gradlew assembleDebug

# Release APK (requires release.keystore configuration):
./gradlew assembleAfatRelease

# Run pure JVM Unit Tests (Core, Vault, AI Router):
./gradlew testReleaseUnitTest
```

---

## 4. Website Build (GitHub Pages)

The project includes a modern Vite + React static landing website located in `website/`:

```bash
cd website
npm install
npm run build
```
Build artifacts are placed in `website/dist` and automatically deployed by `.github/workflows/website.yml`.
