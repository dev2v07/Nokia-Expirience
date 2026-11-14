# Nokia Experience 2.0 — Drag & Gesture Engine Report

Дата: 2026-11-11
Проект: `C:\projects\Nokia\NokiaExperience2`
Ветка: `samsung-ux-architecture`
Устройство: Samsung Galaxy A5 2017, `SM-A520F`, Android 8.0.0 / API 26

## Итог

**Реализация прототипа завершена; device-проверка — PASS WITH LIMITATIONS.**

В изолированный prototype-пакет добавлены настоящие Android touch-обработчики, карточки как дочерние `TextView`, long-press по системному таймауту, arbitration жестов, drag transaction и in-memory модель перемещения. Производственный Home, профили и пользовательские данные не подключались.

Полный физический сценарий «зажать карточку → перемещать → отпустить» не объявляется пройденным: через доступный `adb shell input swipe` нельзя надёжно разделить фазу удержания и последующего движения в одном pointer sequence. Стационарный long-press и синтетические свайпы проверены отдельно.

## 1. Новая архитектура

### `GestureArbitrator`

Сохраняет единственного владельца pointer sequence:

`IDLE → POSSIBLE_SWIPE/POSSIBLE_DRAG → SWIPE или LONG_PRESS → DRAG → IDLE/CANCELLED`

Карточка получает `POSSIBLE_DRAG`, свободная область — `POSSIBLE_SWIPE`. Движение по горизонтали до long-press выбирает `SWIPE`; уход за touch slop по вертикали отменяет кандидата. После long-press вызывается `requestDisallowInterceptTouchEvent(true)`, и ScrollView не должен забирать последовательность.

### `DragController`

Новый чистый in-memory контроллер:

- хранит тестовые карточки и их committed page/coordinates;
- захватывает объект после подтверждённого long-press;
- рассчитывает preview отдельно от модели;
- передаёт жизненный цикл в единственный `DragTransaction`;
- commit выполняется один раз;
- cancel не меняет committed позицию;
- drop на занятую карточку использует документированную политику swap;
- drop в свободную область меняет page/coordinates только при commit.

### `WorkspaceSurface`

Новый изолированный `ViewGroup`, заменивший только prototype-область старого `HorizontalScrollView`. Причина замены в прототипе — необходимость единого владельца `onInterceptTouchEvent`/`onTouchEvent` и согласования long-press, drag и page swipe. Производственные классы не затронуты.

Поверхность:

- содержит реальные Android `TextView`-карточки;
- обрабатывает `ACTION_DOWN`, `ACTION_MOVE`, `ACTION_UP`, `ACTION_CANCEL`;
- использует `ViewConfiguration.getLongPressTimeout()` и `scaledTouchSlop`;
- поддерживает page swipe;
- отображает owner, transaction, drop result и pointer diagnostics;
- имеет edge-hover policy с настраиваемым prototype timeout 650 мс;
- не пишет SharedPreferences, JSON или пользовательскую модель.

### Изменённые/созданные файлы

- [GestureArbitrator.java](C:/projects/Nokia/NokiaExperience2/app/src/main/java/com/nokiaexperience/launcher/workspace/engine/prototype/GestureArbitrator.java)
- [DragController.java](C:/projects/Nokia/NokiaExperience2/app/src/main/java/com/nokiaexperience/launcher/workspace/engine/prototype/DragController.java)
- [WorkspaceSurface.java](C:/projects/Nokia/NokiaExperience2/app/src/main/java/com/nokiaexperience/launcher/workspace/engine/prototype/WorkspaceSurface.java)
- [PrototypeActivity.java](C:/projects/Nokia/NokiaExperience2/app/src/main/java/com/nokiaexperience/launcher/workspace/engine/prototype/PrototypeActivity.java)
- [DragControllerTest.java](C:/projects/Nokia/NokiaExperience2/app/src/test/java/com/nokiaexperience/launcher/workspace/engine/prototype/DragControllerTest.java)

## 2. Unit-тесты

Чистые тесты были запущены напрямую через JUnit 4.13.2 по собранным классам:

```text
OK (18 tests)
```

Проверены:

- tap/swipe/long-press/drag state transitions;
- widget-touch policy;
- cancellation и возврат в IDLE;
- preview без mutation;
- commit ровно один раз;
- cancel без mutation;
- новая сессия после завершения;
- свободный drop;
- occupied drop со swap;
- page boundaries и scroll one-page-per-gesture.

Команда Gradle `test` дополнительно не прошла из-за локального Gradle worker runner: `worker.org.gradle.process.internal.worker.GradleWorkerMain` отсутствует. Это произошло после успешной Java-компиляции тестов и не связано с assertion. Прямой запуск JUnit дал 18/18.

## 3. Сборка

```text
assembleDebug: BUILD SUCCESSFUL
```

APK: [app-debug.apk](C:/projects/Nokia/NokiaExperience2/app/build/outputs/apk/debug/app-debug.apk)

Сборка выполнена с уже существующими настройками проекта: API 26 остаётся минимальным SDK, новые API новее Android 8 не использованы.

## 4. Проверка на Samsung Galaxy A5 2017

Установка выполнена безопасным обновлением:

```text
adb install -r app-debug.apk
Success
```

Приложение не удалялось, данные не очищались. inode каталога `/data/user/0/com.nokiaexperience.launcher` до и после установки: `131107`.

Запуск выполнялся только через debug action. Activity Manager подтвердил:

```text
com.nokiaexperience.launcher/.workspace.engine.prototype.PrototypeActivity
```

Проверено:

| Сценарий | Метод | Результат |
|---|---|---|
| Короткий tap по карточке | ADB `input tap` | PASS: drag не запускается |
| Long press без движения | ADB stationary `input swipe` 800 мс | PASS: `Drag: COMMIT` после отпускания |
| Swipe по свободной области | ADB `input swipe` | PASS: Page 1 → Page 2, затем IDLE |
| Движение до long-press timeout | ADB swipe с ранним движением | PASS: `CANCELLED`, ошибочный drag не запускается |
| Drag после реального long-press | Физический палец | НЕ ПРОВЕРЕНО |
| Drop в свободную позицию на устройстве | Физический палец | НЕ ПРОВЕРЕНО |
| Drop на занятую позицию на устройстве | Физический палец | НЕ ПРОВЕРЕНО |
| Edge-hover с сохранением drag | Физический палец | НЕ ПРОВЕРЕНО |
| ACTION_CANCEL через реальный прерванный drag | Физический палец | НЕ ПРОВЕРЕНО |

Все ADB-жесты выше являются синтетическими и не выдаются за физическое взаимодействие пальцем.

## 5. Скриншоты

- [drag_gesture_initial.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/drag-gesture-engine/drag_gesture_initial.png)
- [tap_card_a.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/drag-gesture-engine/tap_card_a.png)
- [long_press_stationary_commit.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/drag-gesture-engine/long_press_stationary_commit.png)
- [swipe_free_area.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/drag-gesture-engine/swipe_free_area.png)
- [drag_free_drop_v2.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/drag-gesture-engine/drag_free_drop_v2.png)
- [drag_occupied_swap.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/drag-gesture-engine/drag_occupied_swap.png)
- [drag_edge_hover_next_page.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/drag-gesture-engine/drag_edge_hover_next_page.png)

Последние два файла сохранены как exploratory ADB runs и не являются подтверждением успешного физического occupied-drop или edge-hover: синтетический swipe начал движение до long-press и был корректно отменён.

## 6. Logcat и стабильность

- [logcat_after_drag_gesture.txt](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/drag-gesture-engine/logcat_after_drag_gesture.txt)
- [logcat_filtered.txt](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/drag-gesture-engine/logcat_filtered.txt)

После очистки logcat и тестовой серии:

- `FATAL EXCEPTION`: 0;
- `ANR in` / `am_anr`: 0;
- процесс приложения оставался жив;
- `PrototypeActivity` оставалась resumed/current focus.

Разрешение HOME не изменилось: `cmd package resolve-activity ... HOME` по-прежнему возвращает `com.nokiaexperience.launcher.LauncherActivity` как HOME Activity. Prototype запускался только отдельным action.

## 7. Сохранность и границы

Не использовались:

- `LauncherActivity` в качестве запускаемого теста;
- `HomeWorkspaceModel`;
- `MenuModel` и `MenuOrganizer`;
- SharedPreferences и JSON;
- реальные иконки и папки;
- `AppWidgetHost` / `AppWidgetManager`;
- Nokia profiles и production Home.

Текущий Git status содержит только ранее существовавшие prototype/build регистрации и новые prototype-файлы; пользовательские данные и производственные модели не изменялись.

## 8. Ограничения и следующий этап

Перед интеграцией в Nokia Home нужно вручную на разблокированном A5 проверить пальцем:

1. удержание карточки до подсветки и перемещение после timeout;
2. drag в свободную область;
3. drag на вторую карточку и swap;
4. удержание у левого/правого края с переходом ровно на одну страницу;
5. отпускание и `ACTION_CANCEL` при уходе из Activity;
6. быстрый новый drag после commit/cancel.

До прохождения этих ручных сценариев нельзя считать физический Drag & Drop Engine полностью принятым. Производственный Home и пользовательские данные к следующему этапу подключать нельзя.
