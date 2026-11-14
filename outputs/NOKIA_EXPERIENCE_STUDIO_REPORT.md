# Nokia Experience Studio

## Status

Первый рабочий срез редактора создан как отдельное Windows-приложение на Python 3.14 + PySide6. Окно реально запускается и содержит Canvas телефона, дерево экранов/слоёв, инспектор свойств, сохранение JSON, undo/redo, импорт ресурсов, экспорт в Android, Gradle build, ADB install, device probe и screenshot.

## Архитектура

- `studio/models.py` — схема проекта, атомарное сохранение, `.bak`, undo/redo.
- `studio/canvas.py` — `QGraphicsView`, перемещаемые слои, выбор, сетка и масштабирование профиля 1080×1920.
- `studio/inspector.py` — X/Y/Width/Height/Opacity/Radius/Blur, цвета, видимость и блокировка.
- `studio/android.py` — экспорт управляемого JSON, ADB Nokia B, Gradle и установка через `adb install -r`.
- `studio/main_window.py` — рабочая оболочка, экраны, слои и команды.
- Android `StudioRuntimeConfig.java` — безопасный optional bridge: читает `assets/studio/project.json`, применяет цвет статус-бара только для Exp2; при отсутствии/ошибке возвращается штатная логика.

## Реальная интеграция

Экспорт записывает `NokiaExperience2/studio/project.json` и `NokiaExperience2/app/src/main/assets/studio/project.json`. Предыдущие runtime-файлы получают `.bak`. Форматы `home_workspace`, `menu_state`, виджеты, package mapping и функциональные жесты редактор не переписывает.

Экспортируемая граница первой версии: `theme.statusIconColor`, `theme.accent`, профиль устройства и grid metadata. Home placement, arbitrary view creation, widget lifecycle, app package mapping и полная имитация Android-жестов помечены Not Exportable/ограничение.

## Проверки

- `python -m compileall -q studio` — пройдено.
- Окно `Nokia Experience Studio` запущено; accessibility inspection подтвердил toolbar, дерево из 9 экранов, Canvas и инспектор.
- Read-only ADB: Nokia B `NOKIA_B`, `SM-A520F`, Android `8.0.0`, API `26` — подтверждено.
- `:app:testDebugUnitTest` — BUILD SUCCESSFUL.
- `:app:assembleDebug` — BUILD SUCCESSFUL.
- APK `app/build/outputs/apk/debug/app-debug.apk` создан.
- `git diff --check` — без ошибок; предупреждения только о нормализации CRLF существующих файлов.
- Установка на устройство в этом этапе не выполнялась автоматически; кнопка установки в Studio требует явного действия пользователя.

## Запуск

```powershell
cd C:\projects\Nokia\NokiaExperienceStudio
python -m studio
```

Или `run_studio.bat`. В Studio выберите **Открыть Android**, укажите `C:\projects\Nokia\NokiaExperience2`, измените слой, сохраните, нажмите **Экспорт Android**, затем **Собрать APK**. Установка и screenshot вынесены в отдельные явные кнопки.

## Ограничения

Это не декоративный макет: экспортируемый status-bar theme bridge проверяется сборкой Android. При этом Studio ещё не переводит всю Java-архитектуру Launcher в конфигурационный слой, не заменяет живые AppWidgetView и не обещает runtime-поведение жестов из Canvas. Следующий этап — asset-backed preview (wallpaper/SVG/icon layers), Theme/Grid Designer panels и расширение Android adapter после фиксации управляемого контракта.
