# Nokia Experience 2

Android launcher project for the Nokia Experience 2 profile, with a separate Nokia Experience Studio editor.

## Target profile

- Device reference: Samsung Galaxy A5 2017 / SM-A520F
- Android 8.0 / API 26
- Home model: 4 columns × 3 rows, Dock outside the main grid
- Primary development branch: `samsung-ux-architecture`

## Launcher

The Android project contains the Exp2 Home engine, workspace/page navigation, drag and drop, widget integration, status bar assets, Edit Mode, settings and prototype workspace-engine tests. Persistent user data formats (`home_workspace`, `menu_state`, widget storage and package mappings) are kept in the Android implementation and are not replaced by Studio.

## Nokia Experience Studio

Studio source is in [`studio/`](studio/). Run it from the project root:

```powershell
python -m pip install -r studio/requirements.txt
$env:NOKIA_ANDROID_ROOT = (Get-Location).Path
$env:NOKIA_B_SERIAL = "<your Nokia B serial>"
python -m studio
```

Alternatively run [`run_studio.bat`](run_studio.bat). Studio provides the Canvas, layer tree, property inspector, Theme/Grid panels, JSON persistence, ADB device probe, live screenshot mode, Android export, Gradle build and explicit `APPLY TO DEVICE` flow.

`LIVE DEVICE` currently uses fresh ADB screenshots. `scrcpy` input streaming is not claimed unless it is installed and configured locally. The Apply action is explicit and uses export → build → `adb install -r`; it does not clear data or uninstall the launcher.

## Build APK

The repository does not include `local.properties`, Gradle caches or generated APKs. Use Android Studio or an installed Gradle 9.5/JDK 17 environment:

```powershell
$env:JAVA_HOME = "<JDK 17 path>"
$env:GRADLE_USER_HOME = "<Gradle cache path>"
gradle :app:testDebugUnitTest :app:assembleDebug
```

Install explicitly on the intended device with:

```powershell
adb -s <Nokia B serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

## Repository layout

- `app/` — Android launcher source and resources.
- `studio/` — Nokia Experience Studio Python/PySide6 source and managed config.
- `outputs/` — research, audit and implementation Markdown reports.
- `app/src/main/assets/studio/project.json` — exported Studio runtime asset.

## Current status and limitations

The launcher and Studio development state are published together. Studio’s current Android bridge supports the exported visual theme/status-bar configuration and device/grid metadata. Individual app-icon hit testing, direct `home_workspace` editing, live AppWidget editing, arbitrary new Android Views and full scrcpy control remain explicitly limited/NOT EXPORTABLE until a stable Android element registry is implemented.

See [`outputs/NOKIA_EXPERIENCE_STUDIO_LIVE_EDITOR_REPORT.md`](outputs/NOKIA_EXPERIENCE_STUDIO_LIVE_EDITOR_REPORT.md) and [`outputs/NOKIA_EXPERIENCE_STUDIO_REPORT.md`](outputs/NOKIA_EXPERIENCE_STUDIO_REPORT.md) for verification and boundaries.
