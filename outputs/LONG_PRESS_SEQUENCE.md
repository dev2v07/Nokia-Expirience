# Nokia Exp2 P0 — Long Press sequence

Статус: `STATICALLY_CONFIRMED`; физический runtime-путь не выполнялся этим аудитом.

## Иконка Exp2

- `ACTION_DOWN`: `Exp2HomeDragEngine.down()` сохраняет `activeView`, `activeKey`, `originPage`, `originCell`, `pointerId`, offset и запрещает перехват родителем (`Exp2HomeDragEngine.java:72-86`).
- Таймер: `handler.postDelayed(..., ViewConfiguration.getLongPressTimeout())` (`Exp2HomeDragEngine.java:87-95`).
- При срабатывании `GestureArbitrator.longPress()` → `DragTransaction.begin()` → haptic/alpha → `GestureArbitrator.beginDrag()` (`Exp2HomeDragEngine.java:87-94`).
- До timeout движение больше `touchSlop` вызывает `GestureArbitrator.move()`. Горизонтальное движение становится `SWIPE`; вертикальное — `CANCELLED` (`GestureArbitrator.java:23-33`).
- Ранний `ACTION_UP` удаляет callback и классифицируется как Tap, если движение не было (`Exp2HomeDragEngine.java:136-163`).

## Контекстное меню

Старая ветка `startAppDrag()` и `OnLongClickListener` устанавливаются только когда `exp2HomeEngineEnabled()` ложно (`LauncherActivity.java:492-495`). Поэтому статически подтверждено, что обычный legacy long-click не назначается Exp2-иконке одновременно с новым listener. Однако `showHomeItemOptions()` существует и вызывается из legacy `endWorkspaceDrag()` при неуспешном Android Drag (`LauncherActivity.java:627-640`); его фактическое появление при Exp2-физическом жесте нужно проверить на устройстве.

## Пустой Home

Для пустой области отдельный `emptyHomeTouch()` ставит свой long-press Runnable (`LauncherActivity.java:1083-1094`), а `WorkspacePager.dispatchTouchEvent()` также ставит Exp2 Edit Mode Runnable при `emptyWorkspacePoint()` (`LauncherActivity.java:523-530`). Это два разных места планирования long-press для свободной области. Их одновременное срабатывание статически возможно и является подтверждённой зоной риска; фактическое двойное открытие UI — `UNKNOWN` до runtime-проверки.
