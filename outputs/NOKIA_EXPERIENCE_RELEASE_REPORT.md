# Nokia Experience 2 — GitHub Release Report

## Status

**RELEASED** — GitHub Release and APK asset were created and verified through the GitHub API.

## Application metadata

- Application ID: `com.nokiaexperience.launcher`
- Version name: `0.1-mvp`
- Version code: `1`
- minSdk: `26`
- targetSdk: `28`
- compileSdk: `37`
- Launcher activity: `com.nokiaexperience.launcher.LauncherActivity`
- APK type: **Debug**
- APK path: `app/build/outputs/apk/debug/app-debug.apk`
- APK size: `13,750,473` bytes
- SHA-256: `92933C7ECC8AC5BA3358197EE95ACFD97C254A9018C8BD6E280A8B3462EBB46E`
- Native ABI payload: none reported; universal APK.
- Signature: APK Signature Scheme v2, one signer. This is the standard debug signing result, not a production release signature.

## Build results

- `:app:assembleDebug` — APK package completed successfully.
- `:app:assembleRelease` — not published. The project has no configured release signing block, and the release task also failed while resolving external Android lint dependencies because the configured environment could not complete the TLS download from Maven/Google repositories.
- No signing configuration, keystore or password was changed or published.

## Device verification

- ADB target: Nokia B only.
- At verification time the target serial was not connected: `adb install -r` returned `device not found`.
- APK installation: **NOT COMPLETED**.
- Launcher startup on device: **NOT PHYSICALLY VERIFIED**.
- No `pm clear`, uninstall, data reset or Samsung A operation was performed.

## GitHub Release

- Repository: https://github.com/dev2v07/Nokia-Expirience
- Branch containing source: `samsung-ux-architecture`
- Tag: `v0.1-mvp`
- Release: https://github.com/dev2v07/Nokia-Expirience/releases/tag/v0.1-mvp
- APK asset: https://github.com/dev2v07/Nokia-Expirience/releases/download/v0.1-mvp/Nokia-Experience-2-v0.1-mvp.apk
- Asset state: uploaded
- Asset filename: `Nokia-Experience-2-v0.1-mvp.apk`

The release description contains Russian and English sections, installation instructions, Debug labeling, target profile, limitations and the same SHA-256.

## Limitations

This release must not be described as a production Release build. Physical gestures, widget behavior, status-bar scenarios and Launcher startup still require device validation once Nokia B is connected. Studio Live Device currently uses ADB screenshot polling when scrcpy is unavailable.
