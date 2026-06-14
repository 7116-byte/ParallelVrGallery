# Parallel VR Gallery

Local Android gallery for generating and caching parallel-eye SBS VR images from normal photos.

## What v1 does

- Reads system photos with `MediaStore`.
- Browses photos in a Compose grid and full-screen pager.
- Generates a local parallel-eye SBS image with the bundled Depth-Anything-V2 TFLite model.
- Keeps a configurable prefetch window around the current image: `3`, `5`, or `10`.
- Caches generated output under the app private external files directory.
- Exports a debug zip for the current READY image with:
  - `source_preview.jpg`
  - `depth.png`
  - `vr_sbs.jpg`
  - `params.json`
  - `job.log`

## Build

Open this folder in Android Studio, or use Gradle after setting `local.properties`:

```properties
sdk.dir=D:\\Android\\Sdk
```

Then run:

```powershell
.\gradlew.bat assembleDebug
```

The current workspace originally had no Android SDK/Gradle on `PATH`, so the project is structured for Android Studio or a D-drive SDK install.

## Model asset

The release APK already bundles `depth_anything_v2.tflite`. The raw model is intentionally not committed to Git because it is a large binary; to build from source, place it at:

```text
app/src/main/assets/models/depth_anything_v2.tflite
```
