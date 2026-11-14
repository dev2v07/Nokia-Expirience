# Nokia Experience 2.0 — Android SDK and build report

Дата: 2026-11-11
Проект: `C:\projects\Nokia\NokiaExperience2`
Ветка: `samsung-ux-architecture`
Контрольная точка доступна: `nokia-exp-working-before-exp2`

## 1. Git state before setup

- Branch: `samsung-ux-architecture`.
- No reset, clean, checkout or destructive operation performed.
- Existing prototype changes were preserved.
- Project did not contain `gradle\wrapper\gradle-wrapper.properties`; the Gradle wrapper was not updated or created.

Existing worktree changes before this task were the isolated prototype sources/tests, the prototype debug Activity registration and its JUnit dependency. They were not discarded.

## 2. Android SDK found

SDK обнаружен в нестандартном, но уже настроенном Android Studio пути:

`C:\projects\Nokia`

This path is also present in Android Studio configuration:

`C:\Users\<local-user>\AppData\Roaming\Google\AndroidStudio2026.1.3\options\android.sdk.path.xml`

The following components were verified:

| Component | Result |
|---|---|
| `platform-tools` | Present at `C:\projects\Nokia\platform-tools` |
| `adb.exe` | Present; Android Debug Bridge 1.0.41, version 37.0.1-15733141 |
| Android platform | Present: `platforms\android-37.0`, API 37.0 |
| Android API 26 | Not installed |
| Build Tools | Present: `build-tools\36.0.0` |
| `aapt2.exe` | Present and used for APK Manifest inspection |
| Emulator directory | Present at `C:\projects\Nokia\emulator` |

The project uses `compileSdk 37`, so the installed API 37 platform is compatible with the declared compile SDK. `minSdk 26` is a runtime minimum and does not require the API 26 platform package for this compile configuration.

No SDK component was downloaded or installed.

## 3. Project versions and requirements

- Android Gradle Plugin: `9.3.2` from root `build.gradle`.
- Project `compileSdk`: `37`.
- `minSdk`: `26`.
- `targetSdk`: `28`.
- Java source/target compatibility: `17`.
- Java used for the build: Android Studio JBR OpenJDK `25.0.2`.
- Gradle used: cached Gradle `9.5.0` at `C:\Users\<local-user>\.gradle\wrapper\dists\gradle-9.5.0-bin\...\gradle-9.5.0`.
- Project Gradle wrapper properties: absent; no wrapper files were changed.

The global environment variables `ANDROID_HOME` and `ANDROID_SDK_ROOT` were not changed. `JAVA_HOME` was set only as a process-local command variable to the existing Android Studio JBR so Gradle could run.

## 4. Local SDK configuration

Created only the project-local file:

`C:\projects\Nokia\NokiaExperience2\local.properties`

Contents:

```properties
sdk.dir=C:\\projects\\Nokia
```

`local.properties` is excluded through the local, untracked file `.git/info/exclude`. It does not appear in `git status` and is not part of the repository diff. The tracked `.gitignore` was not changed.

## 5. `test` result

Command run separately:

```powershell
gradle.bat test --no-daemon
```

Result: **FAILED due to dependency/network resolution, not SDK or source compilation.**

Gradle reached Java compilation setup but could not download the cached-missing dependency:

```text
org.jetbrains:annotations:13.0
Could not GET https://repo.maven.apache.org/maven2/org/jetbrains/annotations/13.0/annotations-13.0.jar
Remote host terminated the handshake
```

No dependency was manually downloaded or changed. The SDK itself was accepted and Android resource tasks ran before this dependency failure.

## 6. `assembleDebug` result

Command run separately:

```powershell
gradle.bat assembleDebug --no-daemon
```

Result: **SUCCESS**.

The APK was created at:

`C:\projects\Nokia\NokiaExperience2\app\build\outputs\apk\debug\app-debug.apk`

APK metadata:

- Size: `13,644,366` bytes.
- Last write time: `2026-11-11 22:32` (local build time).
- Package: `com.nokiaexperience.launcher`.

## 7. APK Manifest verification

`aapt2 dump xmltree` confirmed:

- package is `com.nokiaexperience.launcher`;
- main `com.nokiaexperience.launcher.LauncherActivity` remains registered;
- existing `android.intent.category.HOME` and `android.intent.category.LAUNCHER` entries remain on the main Activity;
- `PrototypeActivity` is registered as `com.nokiaexperience.launcher.workspace.engine.prototype.PrototypeActivity`;
- prototype action exists:
  `com.nokiaexperience.launcher.action.OPEN_WORKSPACE_ENGINE_PROTOTYPE`;
- prototype Activity has only `DEFAULT` category and did not receive a HOME or LAUNCHER category.

The APK was not installed on the phone automatically.

## 8. Final Git verification

Tracked diff contains only the expected minimal configuration changes:

- `app/build.gradle` — JUnit test dependency for prototype tests;
- `app/src/main/AndroidManifest.xml` — isolated debug Activity registration.

Untracked additions are the expected prototype package and unit-test package:

- `app/src/main/java/com/nokiaexperience/launcher/workspace/engine/prototype/`;
- `app/src/test/java/com/nokiaexperience/launcher/workspace/engine/prototype/`.

No changes were made to:

- `LauncherActivity` production implementation;
- `HomeWorkspaceModel`;
- `MenuModel`;
- `MenuOrganizer`;
- `VisualProfile`;
- `IconStore`;
- `AssetRepository`;
- profiles or user data;
- JSON, SharedPreferences, icons or user resources.

`local.properties` is ignored locally and is absent from Git status.

## 9. What remains

1. The APK is buildable and ready for a separate manual install when explicitly requested.
2. Android/JUnit `test` still needs the missing Maven dependency to be available locally or through an approved network setup.
3. The isolated pure-Java smoke tests already passed in the previous prototype step; this report does not claim the Gradle `test` task passed.
4. The next safe action is to install the APK only after user confirmation, then launch the prototype with its explicit action and test it without switching the phone's default Home launcher.
