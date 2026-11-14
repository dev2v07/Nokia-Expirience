# Nokia Experience Studio

Windows desktop editor for the Nokia Experience 2 visual configuration.

## Run

```powershell
cd C:\projects\Nokia\NokiaExperienceStudio
python -m studio
```

Use **Open Android project** to point the editor at `C:\projects\Nokia\NokiaExperience2`; if no managed file exists, Studio starts from the verified A5/Exp2 defaults. Save/export creates `studio/project.json`.

## Current export boundary

The editor exports a managed JSON asset to `NokiaExperience2/studio/project.json` and a backup beside the previous file. The current Android runtime adapter consumes the exported status-bar icon color and theme accent. Home placement JSON, widget state, package mappings, and functional gesture code are intentionally not rewritten by Studio.

The canvas is a visual model, not an Android gesture emulator. Real behavior must still be checked on Nokia B with the APK.

## Modules

- `studio/models.py` — schema, project persistence, undo/redo snapshots.
- `studio/canvas.py` — phone canvas and movable visual items.
- `studio/inspector.py` — editable properties.
- `studio/android.py` — project export, ADB read-only device probe/screenshot, Gradle build/install commands.
- `studio/main_window.py` — editor shell, screen/layer tree and commands.

## Device profile

The default A5 profile is 1080×1920, 420 dpi override, Android API 26. The device probe can refresh this from Nokia B without modifying the phone.

## What is exportable in this first version

The schema and runtime bridge currently export the theme accent/status icon color and preserve the device/grid metadata. Home placement JSON, widget state, package mappings, functional gesture code, and arbitrary new Android views are deliberately marked as not exportable; Studio does not pretend that a canvas-only change has reached the APK.

For a Windows shortcut, run `run_studio.bat`. An EXE packaging step is separate from the editor source and requires PyInstaller to be installed.

## Live Device / Apply

`LIVE DEVICE` requests fresh ADB frames from Nokia B every 1.2 seconds and reports `OFFLINE` if the frame cannot be obtained. Set `NOKIA_B_SERIAL` when more than one device is connected. The machine currently has no `scrcpy`, so this is an ADB live-preview rather than a scrcpy input stream. `APPLY TO DEVICE` is explicit and runs export, Gradle build, `adb install -r`, then requests a new frame.
