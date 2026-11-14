# Nokia Experience 2.0 — Device UX Test

Дата проверки: 2026-11-11
Устройство: Samsung Galaxy A5 2017, `SM-A520F`
Android: 8.0.0, API 26
ADB serial: `NOKIA_B`

## Итог

**PASS WITH ISSUES**

Изолированный `PrototypeActivity` запустился и оставался активным. Основные сценарии Page Manager, синтетической прокрутки, отображения состояний Gesture Arbitration и Drag Transaction выполнены без падения приложения. Полное физическое UX-тестирование не подтверждено: свайпы выполнялись через `adb shell input swipe`, а реальный drag по объекту и настоящий AppWidgetHost в прототип не подключены.

## 1. Состояние устройства

ADB обнаружил авторизованное устройство:

```text
NOKIA_B device product:a5y17ltexx model:SM_A520F device:a5y17lte
```

- Экран включён: `Display Power ON`.
- Состояние питания: `mWakefulness=Awake`.
- Keyguard снят: `isStatusBarKeyguard=false`.
- Разрешение: `1080x1920`.
- Плотность: физическая `480`, override `420`.

Исходный экран перед запуском: [before_launch.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/before_launch.png)

Диагностические файлы: [wm_size.txt](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/wm_size.txt), [wm_density.txt](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/wm_density.txt)

## 2. Запуск и визуальная проверка

Запуск выполнялся только через action:

```text
com.nokiaexperience.launcher.action.OPEN_WORKSPACE_ENGINE_PROTOTYPE
```

Activity Manager и Window Manager подтвердили текущую Activity:

```text
com.nokiaexperience.launcher/.workspace.engine.prototype.PrototypeActivity
```

Проверено визуально по скриншоту:

- экран не чёрный;
- Activity занимает доступную область;
- страницы и карточки отображаются;
- индикатор страницы присутствует;
- кнопки видны и читаемы;
- критического наложения или обрезания элементов нет.

Скриншот: [prototype_initial.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/prototype_initial.png)

## 3. Page Manager

| Сценарий | Результат | Доказательство |
|---|---|---|
| Переход вперёд | PASS | [page_next.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/page_next.png) |
| Переход назад | PASS | [page_previous.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/page_previous.png) |
| Ограничение на первой странице | PASS | [page_first_bound.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/page_first_bound.png) |
| Ограничение на последней странице | PASS | [page_last_bound.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/page_last_bound.png) |
| Создание временной страницы | PASS: отображалось `preview page pending`, постоянное число страниц не менялось | [page_preview.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/page_preview.png) |
| Подтверждение создания | PASS: стало `Page 4 / 4` | [page_confirmed.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/page_confirmed.png) |
| Удаление пустой страницы | PASS: после удаления возвращено 3 страницы | [page_deleted_empty.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/page_deleted_empty.png) |
| Отмена создания | Проверена кнопкой, отдельный снимок результата в серии не сохранён | Ограничение фиксации |
| Удаление страницы с содержимым | Не проверено: в тестовом UI нет отдельного заполненного сценария | Не применимо |

## 4. Scroll Controller

Все строки ниже — **синтетические ADB input swipe**, а не физические жесты пальцем.

| Сценарий | Результат | Доказательство |
|---|---|---|
| Медленный свайп влево | PASS: переход к последней странице | [scroll_last_bound_slow_left.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/scroll_last_bound_slow_left.png) |
| Медленный свайп вправо | PASS: возврат с 3-й на 2-ю страницу | [scroll_reverse_slow_right.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/scroll_reverse_slow_right.png) |
| Короткий свайп | Выполнен, визуальный результат сохранён | [scroll_short.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/scroll_short.png) |
| Быстрый свайп вправо | Выполнен | [scroll_fast_right.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/scroll_fast_right.png) |
| Быстрый свайп влево | Выполнен | [scroll_fast_left.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/scroll_fast_left.png) |
| Граница последней страницы | PASS | [scroll_fast_left_bound.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/scroll_fast_left_bound.png) |
| Два быстрых свайпа подряд | Выполнено в серии тестов | [scroll_fast_left_bound.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/scroll_fast_left_bound.png) |

Реакция на физический палец, конфликт свайпа с настоящим drag и длительное удержание на реальном UI этим прогоном полностью не подтверждены.

## 5. Gesture Arbitration

Кнопки прототипа использовались для проверки переходов отображаемой state machine. Это не заменяет полноценный touch-тест пальцем.

| Сценарий | Результат | Доказательство |
|---|---|---|
| Swipe | PASS: состояние после завершения вернулось в `IDLE` | [gesture_swipe_final_idle.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/gesture_swipe_final_idle.png) |
| Drag | PASS: отображалось `DRAG` | [gesture_drag.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/gesture_drag.png) |
| Widget touch | PASS как UI-симуляция: отображалось `POSSIBLE_SWIPE` | [gesture_widget_touch_possible_swipe.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/gesture_widget_touch_possible_swipe.png) |
| CANCEL | PASS: состояние вернулось в `IDLE` | [gesture_cancel_idle.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/gesture_cancel_idle.png) |
| Конфликт реального Swipe/Drag | Не подтверждён: реальные draggable-элементы отсутствуют | — |

На двух снимках присутствовало внешнее heads-up уведомление TikTok Lite. Это системный overlay устройства, не элемент прототипа и не дефект его разметки.

## 6. Drag Transaction

| Сценарий | Результат | Доказательство |
|---|---|---|
| Begin | PASS: `ACTIVE` | [drag_begin_active.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/drag_begin_active.png) |
| Begin → Preview | PASS: `PREVIEW` | [drag_preview.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/drag_preview.png) |
| Preview → Commit | PASS: `COMMIT` | [drag_commit.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/drag_commit.png) |
| Повторный Commit | PASS: повторное завершение не вызвало ошибку и не изменило состояние | [drag_second_commit_blocked.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/drag_second_commit_blocked.png) |
| Cancel после Commit | PASS: безопасно | [drag_cancel_after_commit_safe.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/drag_cancel_after_commit_safe.png) |
| Новая сессия после завершения | PASS | [drag_new_session.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/drag_new_session.png) |
| Cancel новой сессии | PASS | [drag_new_session_cancel.png](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/drag_new_session_cancel.png) |

Поскольку транзакция не подключена к реальным иконкам, папкам или `HomeWorkspaceModel`, сохранность пользовательской модели этим тестом не затрагивалась.

## 7. Logcat и стабильность

Перед финальной серией действий выполнено `adb logcat -c`, после действий сохранен полный лог:

[logcat_after_tests.txt](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/logcat_after_tests.txt)
[logcat_filtered.txt](C:/projects/Nokia/NokiaExperience2/outputs/device-tests/workspace-engine/logcat_filtered.txt)

Результат точечной проверки:

- `FATAL EXCEPTION`: 0;
- `ANR in` / `am_anr`: 0;
- падение процесса `com.nokiaexperience.launcher`: не обнаружено;
- `PrototypeActivity` оставалась resumed/current focus;
- PID процесса после тестов: `24372`.

Строки `AndroidRuntime: >>>>>> START ... RuntimeInit uid 2000` относятся к служебным ADB-командам `input` и не являются исключениями приложения.

## 8. Безопасность и сохранность данных

В рамках этого задания:

- `LauncherActivity` не запускалась;
- HOME launcher не менялся;
- приложение не удалялось и данные не очищались;
- профили, SharedPreferences, JSON, иконки и реальные widgets не изменялись;
- PrototypeActivity использовала собственные тестовые данные в памяти;
- `HomeWorkspaceModel` и производственный Home к прототипу не подключались.

Изменения исходного кода в рамках UX-теста не выполнялись. Существующий Git diff относится к ранее созданному прототипу и его регистрации, а не к этому тестовому прогону:

```text
 M app/build.gradle
 M app/src/main/AndroidManifest.xml
?? app/src/main/java/com/nokiaexperience/launcher/workspace/
?? app/src/test/
```

## 9. Ограничения и следующий шаг

Перед объявлением полной готовности необходимо отдельно проверить на устройстве:

1. настоящий drag жестом пальца по тестовому draggable-элементу;
2. конфликт drag и горизонтальной прокрутки;
3. long press с реальным временем удержания;
4. физические жесты, а не только `adb input swipe`;
5. подключение к реальным папкам, иконкам и AppWidgetHost — только после отдельного согласования и с тестовым профилем.

Текущий изолированный прототип работоспособен для следующего этапа разработки, но результатом этого прогона является **PASS WITH ISSUES**, а не полный PASS.
