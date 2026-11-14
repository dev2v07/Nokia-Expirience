# Nokia Exp2 P0 — Drag sequence

Статус: `STATICALLY_CONFIRMED`; physical drag: `NOT_VERIFIED`.

1. Icon visual View receives `ACTION_DOWN` and calls `Exp2HomeDragEngine.down()`.
2. Long-press timeout starts `DragTransaction.ACTIVE` and then `GestureArbitrator.DRAG`.
3. `ACTION_MOVE` calls `edgeHover()`, obtains page bounds, computes visual translation, resolves target cell and calls `Host.preview()` (`Exp2HomeDragEngine.java:113-133`).
4. `Host.preview()` stores hover coordinates and calls `previewReorder()` (`LauncherActivity.java:379-383,593-610`). This method only animates sibling Views; it does not call `HomeWorkspaceModel`.
5. Edge-hover uses `EDGE_HOVER_TIMEOUT_MS=650` and calls `Host.movePageForDrag()` once per latch (`Exp2HomeDragEngine.java:175-183`).
6. `ACTION_UP` computes target cell and folder confirmation (`Exp2HomeDragEngine.java:143-152`).
7. Successful `DragTransaction.commit()` calls `Host.commit()` exactly once from this path; host then calls model mutation and `refreshWorkspace()` (`LauncherActivity.java:384-390`).

## Pointer ownership

The icon engine receives a real `MotionEvent` sequence through child `OnTouchListener`. It does not call Android `startDragAndDrop()` for icons. The legacy `startAppDrag()` path exists but is installed only when the feature flag is disabled. Therefore parallel icon Drag engines are not proven active in Exp2; the remaining shared Activity/Widget paths are separate risk areas.
