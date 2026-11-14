# Nokia Experience 2 — P2.5 Startup Animation & SVG Status Bar

Status: **PASS WITH LIMITATIONS**

## Реализовано

### SVG-статус-бар

Предоставленные пользователем SVG встроены без сторонних библиотек как native `VectorDrawable`:

- `p2_5_network_mark.xml` — `сеть.svg`;
- `p2_5_network_indicator.xml` — `Индикатор сети.svg`;
- `p2_5_battery_mark.xml` — `батарея.svg`;
- `p2_5_battery_indicator.xml` — `Индикатор батареи.svg`.

Геометрия и пропорции исходников сохранены. Цвет задаётся через tint, прозрачность отдельных сегментов сохранена.

Уровни сигнала и батареи не стали статичными: полный SVG-индикатор рисуется с вертикальным clipping по текущему уровню. Источники данных остались прежними: `BatteryManager`, `PhoneStateListener`, Wi-Fi и `DeviceStateAdapters`.

Размеры были увеличены после визуальной проверки по референсу: индикаторы теперь рисуются примерно `16×39 dp` и `17×39 dp`, а mark-иконки — примерно `16×19 dp` и `17×22 dp`, с сохранением aspect ratio.

Для профилей N95/5530 оставлена прежняя PNG-графика из `assets/reference`; SVG применяется только к Exp2.

### Общий статус-бар Launcher

`StatusGlyph` теперь является единственным View верхнего слоя `LauncherActivity`, а не дочерним элементом только Home. Поэтому он остаётся видимым:

- на Home;
- в Drawer;
- в Exp2 Edit Mode;
- поверх внутренних overlay Launcher.

Размер геометрии Home сохранён отдельным пустым spacer на прежнем месте. Второй экземпляр статус-бара не создаётся.

Проверено на Nokia B:

- [Home с увеличенными SVG](../work/p2_5-device-test/home-status-svg-large.png);
- [Drawer с общим статус-баром](../work/p2_5-device-test/drawer-status-svg.png);
- [Edit Mode с общим статус-баром и SVG](../work/p2_5-device-test/edit-status-svg-large.png).

### Цвет SVG-иконок

В существующие настройки Launcher добавлен пункт:

`Цвет иконок статус-бара`

Доступные варианты: белый, акцентный цвет темы, голубой, жёлтый. Настройка хранится в текущем profile `SharedPreferences`, без изменения JSON и без затрагивания других профилей.

### Connecting people

Сохранены все 20 кадров. Параметры pacing P2.5:

| Параметр | P2.4 | P2.5 |
|---|---:|---:|
| Интервал кадров | 45–120 ms | 35–100 ms |
| Количество кадров | 20 | 20 |
| Расчётная frame phase | 0.9–2.4 s | 0.7–2.0 s |
| Fade | 220 ms | 220 ms |

`StartupView` теперь удерживает кадр 20 до фактического `MediaPlayer.OnCompletionListener`. После callback выполняется существующий fade и открывается Home. Если аудио недоступно, используется безопасный fallback без зависания заставки.

## Изменённые файлы

- [LauncherActivity.java](../app/src/main/java/com/nokiaexperience/launcher/LauncherActivity.java)
- [StartupView.java](../app/src/main/java/com/nokiaexperience/launcher/StartupView.java)
- [p2_5_network_mark.xml](../app/src/main/res/drawable/p2_5_network_mark.xml)
- [p2_5_network_indicator.xml](../app/src/main/res/drawable/p2_5_network_indicator.xml)
- [p2_5_battery_mark.xml](../app/src/main/res/drawable/p2_5_battery_mark.xml)
- [p2_5_battery_indicator.xml](../app/src/main/res/drawable/p2_5_battery_indicator.xml)

Backups:

- `LauncherActivity.java.p2_5-backup-20261112_033500`
- `StartupView.java.p2_5-backup-20261112_033500`

## Проверки

- `testDebugUnitTest` — PASS.
- `assembleDebug` — PASS.
- `git diff --check` — PASS; остались только стандартные предупреждения Git о LF/CRLF.
- APK установлен на Nokia B через `adb install -r` — `Success`.
- Samsung A не изменялся.
- В logcat нет совпадений `FATAL EXCEPTION`, `ANR in`, `am_anr`.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Проверка через ADB подтверждена синтетически. Физический палец для смены цвета и субъективной оценки синхронизации последнего кадра не использовался.

## Сохранность данных

SHA-256 после установки и проверки:

| Файл | SHA-256 |
|---|---|
| `shared_prefs/nokia_exp_2.xml` | `f960b7ddb0af86233fb3a6fff5eeed48d3f549410b1a62bc0b91f8b579585ad0` |
| `shared_prefs/nokia_experience.xml` | `134cb873820a9294e66e5c1d1b438e5bca1f6bb09112661fcd4175a9ebcd6fdd` |

JSON-форматы Home/Menu, пользовательские иконки, папки, виджеты и их ID не изменялись.

## Ограничения

1. Точное восприятие времени последнего кадра относительно мелодии требует физического cold-start наблюдения или screen recording.
2. Доступность пункта цвета подтверждена сборкой и кодом; отдельная физическая смена цвета в текущем прогоне не выполнялась.
3. Статус `PASS WITH LIMITATIONS` оставлен из-за отсутствия полноценного физического теста пальцем.
