# Device Registry

Дата: 2026-11-12.

| Роль | Serial | Model | Android/API | Launcher | Статус |
|---|---|---|---|---|---|
| A — Samsung reference | `SAMSUNG_A_REFERENCE` | SM-A520F | 8.0.0 / 26 | `com.sec.android.app.launcher/.activities.LauncherActivity` | OBSERVED |
| B — Nokia target | `NOKIA_B` | SM-A520F | 8.0.0 / 26 | `com.nokiaexperience.launcher/.LauncherActivity` | OBSERVED |

На устройстве A активен оригинальный Samsung Launcher, на устройстве B — Nokia Launcher. Оба устройства имеют модель SM-A520F, Android 8.0/API26. Команды адресуются через явный `-s` serial; порядок `adb devices` не используется для выбора цели.

Baseline сохранён: `SAMSUNG_A_REFERENCE_home_baseline.png`, `NOKIA_B_home_baseline.png` и соответствующие UI hierarchy XML.
