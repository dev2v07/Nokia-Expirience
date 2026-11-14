# Nokia Experience Studio 2.0 — Live Device Designer

## Реализовано в этом этапе

Существующая Studio расширена, новая программа с нуля не создавалась.

- `LIVE DEVICE` — явный режим просмотра свежих кадров Nokia B через ADB.
- `DESIGN MODE` — редакторский режим с Canvas и прозрачными overlay-границами поверх последнего live-кадра.
- OFFLINE-состояние — при ошибке ADB статус не маскируется сохранённым screenshot.
- Получение `wm size`, `wm density`, модели, Android/API и serial.
- `APPLY TO DEVICE` — явная последовательность export → Gradle build → `adb install -r` → новый live-кадр.
- Apply не использует `pm clear`, uninstall, reset или изменение пользовательских данных.
- Gradle fallback добавлен для проекта без `gradlew.bat`.

## Проверенный live-поток

Подключён только serial `NOKIA_B`:

```text
SM-A520F
Android 8.0.0 / API 26
Physical size: 1080x1920
Override density: 420
LIVE DEVICE · 1080×1920 · NOKIA_B
```

Получение кадра выполнено через `adb exec-out screencap -p`; размер PNG подтверждён как 1080×1920. Это не сохранённый screenshot и не заявляется как scrcpy-поток.

## Design Overlay

Существующий `QGraphicsView` сохранён. При наличии live-кадра фоном становится реальный экран, а слои получают прозрачную заливку и редакторские границы. Выбор через дерево и Canvas сохранён, координаты и свойства отображаются в Inspector.

Текущий stable layer contract:

- `wallpaper`
- `status_bar`
- `widget_area`
- `application_grid`
- `dock`

Эти слои являются реальными экспортируемыми областями/визуальными контейнерами. Отдельные app icons и живые AppWidgetView пока не объявляются поддержанными только по факту присутствия на screenshot.

## Android Apply bridge

Studio экспортирует:

- `NokiaExperience2/studio/project.json`;
- `NokiaExperience2/app/src/main/assets/studio/project.json`.

`StudioRuntimeConfig.java` загружает конфигурацию безопасно и применяет `statusIconColor` только к Exp2. При отсутствии asset или ошибке JSON используется штатная логика Launcher. `home_workspace`, `menu_state`, AppWidgetHost storage, package mapping и gesture code не переписываются.

## scrcpy

`scrcpy` не найден в PATH и в проверенных каталогах. Поэтому полноценное интерактивное управление телефоном мышью через scrcpy пока не заявляется. Реализован ADB live-preview; подключение scrcpy можно добавить отдельным адаптером без изменения конфигурационного контракта.

## Проверки

- `python -m compileall -q studio` — успешно.
- LiveCapture через Qt event loop — успешно получил live frame 1080×1920.
- Nokia B read-only probe — успешно.
- `build_apk()` Studio fallback — успешно, `:app:assembleDebug`.
- `:app:testDebugUnitTest` — ранее успешно.
- `:app:assembleDebug` — успешно.
- Установка через Apply в этом этапе не запускалась, чтобы не менять устройство без отдельного действия пользователя.

## NOT EXPORTABLE

Пока явно не поддерживаются для реального применения:

- выбор отдельной иконки Telegram/PlayerPro по пиксельному попаданию на screenshot;
- изменение package-bound icon mapping;
- изменение координат `home_workspace` через Studio;
- живое редактирование AppWidgetView;
- произвольное создание новых Android View;
- full scrcpy input/control;
- автоматическая runtime-перезагрузка Launcher без APK.

Эти ограничения не скрыты: они остаются NOT EXPORTABLE до реализации безопасного Android adapter API.

## Следующий обязательный этап

Сформировать Android-side element registry с ID и bounds, который Launcher экспортирует в Studio в read-only виде. После этого можно безопасно добавить точный hit-test отдельных иконок, перенос координат в существующую модель без изменения формата JSON и проверку результата на Nokia B.
