# Nokia Exp2 P0 — Drop and persistence boundary

## Exp2 icon commit

`Exp2HomeDragEngine.up()` → `DragTransaction.commit()` → `Host.commit()` (`Exp2HomeDragEngine.java:143-153`). Host policy:

- folder target → `moveIntoFolder()`;
- ordinary target → `createFolder()` if target is not already a folder;
- otherwise → `reorder()` (`LauncherActivity.java:384-388`).

All three model operations call `HomeWorkspaceModel.save()`:

- `reorder()` at `HomeWorkspaceModel.java:83-103`;
- `createFolder()` at `HomeWorkspaceModel.java:118-130`;
- `moveIntoFolder()` at `HomeWorkspaceModel.java:132-135`.

## Preview boundary

`previewReorder()` only applies `ViewPropertyAnimator.translationX/Y` to sibling views (`LauncherActivity.java:593-610`). No model save is called there. This confirms PREVIEW is model-neutral for the icon reorder path.

## Cancel boundary

`Exp2HomeDragEngine.cancel()` calls `restoreOriginPage()`, cancels `DragTransaction`, calls `cancelPreview()`, resets `GestureArbitrator`, restores view translation/alpha and releases parent interception (`Exp2HomeDragEngine.java:165-203`). No direct persistence writer is called by this method. This is `STATICALLY_CONFIRMED`.

## Other persistence paths

`LauncherActivity` also writes Home state from explicit folder editing/removal, Drawer “add to Home”, and settings actions (`LauncherActivity.java:680-695,773,1355`). These are not Drag preview paths and must not be conflated with icon movement.

## Widget exception

Widgets use Android `startDragAndDrop()` and separate `WidgetFrame.dispatchTouchEvent()`/`OnDragListener` handling (`LauncherActivity.java:1773-1818,1881-1887`). Widget page and ordering writes use profile preferences (`LauncherActivity.java:645-647,1896-1901`), not `HomeWorkspaceModel.home_workspace`. This is a separate persistence domain.
