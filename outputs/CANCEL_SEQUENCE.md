# Nokia Exp2 P0 — Cancel sequence

## `ACTION_CANCEL` on icon

`Exp2HomeDragEngine.onIconTouch()` routes `ACTION_CANCEL` to `cancel()` (`Exp2HomeDragEngine.java:63-69`). The method:

1. removes all engine callbacks;
2. restores origin page;
3. cancels an ACTIVE/PREVIEW transaction;
4. calls host preview cleanup;
5. cancels/resets `GestureArbitrator`;
6. restores translation/alpha;
7. re-enables parent interception and clears active fields (`Exp2HomeDragEngine.java:165-203`).

No `HomeWorkspaceModel` write is performed by this path. `STATICALLY_CONFIRMED`.

## External cancellation

Activity-level drawer opening creates a synthetic `ACTION_CANCEL` for `super.dispatchTouchEvent()` (`LauncherActivity.java:1066-1072`). Root-level Android Drag receives `ACTION_DRAG_ENDED` and calls either `endDrawerDrag()` or `endWorkspaceDrag()` (`LauncherActivity.java:180-187`). These are different cancellation/termination channels from the Exp2 MotionEvent engine.

## Risk

The code has safe cleanup inside each path, but there is no single shared cancellation coordinator across icon MotionEvent, Android DragEvent and widget DragEvent. Whether an external cancel can leave a stale `activeDragKey` or invoke legacy context behavior requires runtime instrumentation.
