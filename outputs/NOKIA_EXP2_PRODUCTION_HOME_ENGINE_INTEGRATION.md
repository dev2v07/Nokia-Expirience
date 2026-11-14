# Nokia Experience 2.0 — Production Home Engine Integration

Дата: 2026-11-11
Проект: `C:\projects\Nokia\NokiaExperience2`
Ветка: `samsung-ux-architecture`
Целевой профиль: Nokia Exp 2
Устройство: Samsung Galaxy A5 2017, `SM-A520F`, Android 8.0 / API 26

## Итог

Интеграция нового touch-пути в production Home выполнена только для профиля Nokia Exp 2. Старый Android `startDragAndDrop` путь сохранён для N95, Nokia 5530 и остальных режимов. Drawer и AppWidgetHost не подключались к новому движку.

Статус: **BUILD PASS / DEVICE TEST BLOCKED**.

Причина ограничения device-теста: после зависшего `adb install -r` ADB-сервер был безопасно перезапущен, после чего телефон перестал появляться в `adb devices`. Установка не была подтверждена как завершённая, `lastUpdateTime` остался прежним, данные приложения не очищались.

## 1. Baseline старой реализации

До интеграции production Home использовал:

1. `LauncherActivity.rebuildWorkspace()` создаёт страницы `FrameLayout` внутри собственного `WorkspacePager`, наследуемого от `HorizontalScrollView`.
2. Каждая иконка запускала `startAppDrag()` через `View.startDragAndDrop()` после `OnLongClickListener`.
3. `trackWorkspaceDrag()` вычислял hover-cell и edge-hover, а `previewReorder()` только визуально анимировал соседние элементы.
4. `ACTION_DROP` на поверхности или иконке напрямую вызывал `HomeWorkspaceModel.reorder()`, `moveIntoFolder()` или `createFolder()`.
5. `endWorkspaceDrag()` обрабатывал `ACTION_DRAG_ENDED`, отмену и возврат после edge-crossing.
6. Общий `LauncherActivity.dispatchTouchEvent()` параллельно обрабатывал вертикальный жест открытия Drawer и отмену home long press.

Критическая точка старого пути: решение «папка или reorder» принималось непосредственно в `ACTION_DROP` по геометрии зоны иконки. Отдельного подтверждённого Folder Drop transaction не было.

## 2. Причина конфликта Page Navigation и Folder Creation

При старом пути edge-hover переключал `workspacePage`, но drop на новой странице всё ещё проходил через тот же `onIconDrop` код. Если координаты отпускания попадали в центральную зону другой иконки, выполнялся `createFolder()` или `moveIntoFolder()`. Page Navigation и Folder Creation не имели независимых владельцев и общего transaction state.

Дополнительные конкурирующие механизмы:

- `HorizontalScrollView.dispatchTouchEvent()` для page swipe;
- `LauncherActivity.dispatchTouchEvent()` для Drawer swipe;
- Android framework drag lifecycle;
- `OnDragListener` поверхности и каждой иконки;
- `OnLongClickListener` на реальной иконке;
- `emptyHomeTouch()` на пустой поверхности.

## 3. Новая production-архитектура

Создан [Exp2HomeDragEngine.java](C:/projects/Nokia/NokiaExperience2/app/src/main/java/com/nokiaexperience/launcher/Exp2HomeDragEngine.java).

Он:

- принимает один реальный `MotionEvent` pointer sequence от иконки;
- использует `GestureArbitrator` для `POSSIBLE_DRAG`, `SWIPE`, `LONG_PRESS`, `DRAG`, `CANCELLED`;
- использует системные `ViewConfiguration.getLongPressTimeout()` и touch slop;
- использует `DragTransaction` как единственный lifecycle источника;
- не меняет `HomeWorkspaceModel` во время PREVIEW;
- вызывает commit ровно один раз после `ACTION_UP`;
- возвращает страницу и preview при cancel/pause;
- сохраняет hover target отдельно от page edge;
- требует dwell `250 ms` для создания папки на обычной иконке;
- принимает drop на существующую папку напрямую;
- даёт Page Navigation приоритет во время edge-hover;
- оставляет old DnD путь доступным для отката.

Адаптер `Host` внутри `LauncherActivity` связывает движок с:

- `HomeWorkspaceModel` как единственным постоянным источником;
- текущим `workspacePage`;
- `WorkspacePager` и страницами Home;
- существующими `previewReorder()` / `clearDragPreview()`;
- существующим `homeEntry()` и запуском приложения.

## 4. Изменённые файлы

- [LauncherActivity.java](C:/projects/Nokia/NokiaExperience2/app/src/main/java/com/nokiaexperience/launcher/LauncherActivity.java) — подключение Exp 2 adapter, feature flag, реальные icon touch listeners, rollback setting, pause cancellation.
- [Exp2HomeDragEngine.java](C:/projects/Nokia/NokiaExperience2/app/src/main/java/com/nokiaexperience/launcher/Exp2HomeDragEngine.java) — production touch adapter.
- Ранее существующие prototype-компоненты сохранены без удаления.

`HomeWorkspaceModel`, JSON-формат `home_workspace`, `menu_state`, профили и AppWidgetHost не переписывались.

## 5. Feature flag и откат

Флаг хранится только в Exp 2 preferences:

```text
nokia_exp_2 / home_engine_v2
```

По умолчанию для Exp 2 используется новый Home Engine. В настройках добавлен пункт:

```text
Home Engine Exp 2: Вкл/Выкл
```

Переключение:

- не меняет `home_workspace`;
- не меняет `menu_state`;
- не выполняет миграцию;
- не очищает данные;
- для N95/5530 не активирует новый путь.

Для отката открыть настройки Nokia Experience и выбрать `Home Engine Exp 2: Вкл/Выкл`, установив `Выкл`. Старый `startDragAndDrop` путь остаётся в исходном коде.

## 6. Folder Drop policy

| Ситуация | Политика |
|---|---|
| Edge-hover | Переключение страницы; само переключение ничего не сохраняет |
| Свободная ячейка | Preview, затем `HomeWorkspaceModel.reorder()` только после UP |
| Обычная иконка | Не папка без dwell-подтверждения |
| Иконка удерживается над обычной иконкой ≥ 250 мс | Подтверждённый folder target |
| Drop на существующую папку | `moveIntoFolder()` |
| ACTION_CANCEL / Activity pause | Cancel, возврат страницы/preview, без model mutation |

Порог `250 ms` — настраиваемая safety policy prototype/production adapter, не измеренный параметр Samsung.

## 7. Drawer и widgets

### App Drawer

Drawer не подключён к `Exp2HomeDragEngine`. Его `MenuOrganizer`, `menu_state`, скрытые приложения, порядок и текущие folder operations сохранены. Старый Drawer DnD и его edge policy требуют отдельного изменения и в этот этап не включались.

### Widgets

`AppWidgetHost`, `AppWidgetManager`, `WidgetFrame` и widget preferences не подключались к новому движку. Существующий widget DnD путь сохранён.

## 8. Тесты и сборка

### Unit tests

Прямой запуск JUnit по собранным классам:

```text
OK (18 tests)
```

Проверены чистые `PageManager`, `ScrollController`, `GestureArbitrator`, `DragTransaction` и `DragController` тесты. Gradle test worker в окружении ранее не запускался из-за отсутствия `GradleWorkerMain`, поэтому результат 18/18 получен прямым JUnit runner.

### Build

```text
assembleDebug: BUILD SUCCESSFUL
```

APK: [app-debug.apk](C:/projects/Nokia/NokiaExperience2/app/build/outputs/apk/debug/app-debug.apk)

### Device

До попытки установки устройство определялось как `SM-A520F`, Android 8.0.0 / API 26. Попытка `adb install -r` зависла на ADB transport. Команда была остановлена без `uninstall`, `pm clear` или factory reset. После перезапуска ADB-сервера `adb devices -l` не показал устройство.

Поэтому не подтверждены:

- запуск нового production Home на телефоне после обновления;
- физический long press реальной иконки;
- drag между реальными Home страницами;
- folder drop на реальную иконку;
- сохранение после перезапуска на устройстве;
- регрессии профилей N95/5530/Exp2.

## 9. Безопасность данных

В этом этапе не выполнялись:

- `adb uninstall`;
- `pm clear`;
- очистка SharedPreferences;
- изменение JSON вручную;
- изменение пользовательских иконок;
- изменение AppWidgetHost;
- смена HOME launcher.

Новый код вызывает постоянную модель только в commit-ветке. Preview и cancel не должны изменять `home_workspace`; это необходимо подтвердить отдельным physical/device прогоном после восстановления ADB.

## 10. Следующая обязательная проверка

После восстановления USB/ADB:

1. Проверить подпись и выполнить `adb install -r`.
2. Запустить `LauncherActivity` обычным способом.
3. Убедиться, что профиль Exp 2 активен и старые иконки отображаются.
4. Рукой проверить tap, long press, свободный drop, occupied drop, edge-hover, cancel и обратный переход.
5. Сохранить `home_workspace` до/после каждого сценария.
6. Переключить `Home Engine Exp 2` в `Выкл` и подтвердить старый путь.
7. Проверить N95 и 5530 после переключения профилей.

До этого device acceptance production integration не считается завершённым.

## 11. Follow-up проверка A5 — 2026-11-12

После восстановления ADB телефон снова определился:

```text
NOKIA_B device product:a5y17ltexx model:SM_A520F device:a5y17lte
SM-A520F / Android 8.0.0 / API 26
```

APK был безопасно обновлён:

```text
adb install -r app-debug.apk
Success
```

Каталог данных сохранил inode `131107` до и после установки. `LauncherActivity` запустилась, в logcat нет `FATAL EXCEPTION`, ANR или падения процесса. Разрешение HOME осталось штатным: приложение Nokia не стало default HOME автоматически.

### Выполненные device-сценарии

Сценарии ниже выполнены через `adb shell input`; это синтетические жесты, не физический палец.

- production Home отображает реальные иконки, Nokia Status Area, Dock и существующий виджет;
- горизонтальный swipe по иконке не создал папку;
- стационарное удержание иконки 800 мс не открыло меню пустого Home и не изменило расположение;
- обычный переход между доступными страницами работает;
- возврат со страницы с виджетом на страницу с иконками работает при свайпе в области рабочего стола (`y=1350`);
- процесс остаётся в `LauncherActivity`;
- `home_workspace` XML до и после тестов полностью идентичен;
- inode пользовательских данных остаётся `131107`.

Скриншоты:

- [production_home_ready.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/production-home-engine/production_home_ready.png)
- [production_home_icon_swipe_fixed.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/production-home-engine/production_home_icon_swipe_fixed.png)
- [production_home_longpress_fixed.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/production-home-engine/production_home_longpress_fixed.png)
- [production_home_reverse_y1350.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/production-home-engine/production_home_reverse_y1350.png)

Лог запуска: [logcat_after_launch.txt](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/production-home-engine/logcat_after_launch.txt)

### Ограничения текущей проверки

Полный физический сценарий «зажать реальную иконку пальцем → переместить → отпустить» через ADB не подтверждается. Также не подтверждены физический edge-hover, folder drop на занятую иконку и ручной cancel во время drag. Для финального acceptance нужен ручной палец пользователя на разблокированном A5 либо внешний UI-канал, который передаёт настоящий непрерывный pointer sequence.

## 12. Исправление Home по сравнению с Samsung Experience — 2026-11-12

Внесены адресные изменения только для Nokia Exp 2 при включённом `home_engine_v2`:

- исправлен сброс `GestureArbitrator` после короткого tap и после отменённой последовательности;
- новый `ACTION_DOWN` безопасно завершает зависшую старую drag-сессию, поэтому повторное перемещение после возврата на страницу не блокируется;
- обработчик Exp2 подключён к визуальным дочерним View и хранит touch offset в screen-space, чтобы нажатие по самой картинке и по подписи использовало одну механику;
- добавлен Exp2-only edit mode поверх существующего Home: превью доступных страниц, выбор страницы, действия обоев/виджетов/настроек, добавление и удаление пустого временного экрана;
- пустой экран не записывается отдельно в `home_workspace`; он становится постоянным только после существующего commit размещения элемента;
- добавлена обработка долгого нажатия на свободной ячейке `WorkspacePager`; профили N95 и 5530 не изменены.

### Проверка после исправления

- `assembleDebug` — `BUILD SUCCESSFUL`;
- `adb install -r` — `Success`, данные приложения не очищались;
- обычное нажатие через ADB по реальной Home-иконке запустило внешнее приложение, без `FATAL EXCEPTION` и ANR;
- после возврата Home остался доступен и продолжил показывать реальные иконки;
- logcat проверен после запуска и касаний, критических падений нет;
- снимок Home: [current_home2.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/production-home-engine/current_home2.png). Попытка синтетического long press свободной области сохранена в [edit_mode_latest2.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/production-home-engine/edit_mode_latest2.png), но вход в edit mode этим способом не засчитан.

Полный ручной acceptance долгого нажатия пустой области, физического drag между страницами, folder drop и edge-hover через ADB не засчитывается: эти сценарии требуют непрерывного физического pointer sequence. Формат `home_workspace`, профили, AppWidgetHost и пользовательские данные в рамках этой проверки не менялись.
