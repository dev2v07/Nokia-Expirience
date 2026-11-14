# Nokia Experience 2 — P0 Production Touch Pipeline Audit

## Scope and baseline

Audit performed read-only on branch `samsung-ux-architecture`. Initial Git state was preserved:

```text
M app/build.gradle
M app/src/main/AndroidManifest.xml
M app/src/main/java/com/nokiaexperience/launcher/LauncherActivity.java
?? app/src/main/java/com/nokiaexperience/launcher/Exp2HomeDragEngine.java
?? app/src/main/java/com/nokiaexperience/launcher/workspace/
?? app/src/test/
```

These changes pre-existed this audit; no production file was edited.

## Architecture found

For Exp2 icon input, the effective path is:

`LauncherActivity.dispatchTouchEvent` → `WorkspacePager.dispatchTouchEvent` → visual child `OnTouchListener` → `Exp2HomeDragEngine` → `GestureArbitrator` / `DragTransaction` → Host callbacks in `LauncherActivity` → `HomeWorkspaceModel` only on commit.

The legacy icon path remains in source and is selected when `home_engine_v2` is false. Widgets use a separate Android `startDragAndDrop` path.

## Ownership findings

The Exp2 icon engine is the single owner after the child visual listener receives the sequence. It correctly separates Tap, Swipe, Long Press, Drag, Preview, Commit and Cancel in static code. `HomeWorkspaceModel` is not called by `previewReorder()` or `cancel()`.

However, the Activity and pager still contain independent gesture policy:

- Activity-level vertical swipe-to-Drawer can synthesize `ACTION_CANCEL` before child completion (`LauncherActivity.java:1056-1080`).
- `WorkspacePager` schedules an empty-page long press while the surface listener also schedules one (`LauncherActivity.java:523-530,1083-1094`).
- `WorkspacePager` performs manual settle on UP (`LauncherActivity.java:537-543`) while icon sequences perform page movement inside Exp2 engine (`Exp2HomeDragEngine.java:136-142`).
- Widgets use a different ownership and persistence path (`LauncherActivity.java:1773-1818,1881-1901`).

These are confirmed structural overlaps. Their user-visible failure frequency is `UNKNOWN` until physical device tracing.

## Persistence boundary

For Exp2 icon drag, the boundary is clear:

`preview → no model write`; `ACTION_UP + transaction.commit → reorder/createFolder/moveIntoFolder → HomeWorkspaceModel.save()`; `ACTION_CANCEL → restore/cleanup → no model write`.

The JSON key remains `home_workspace`; profile preference separation remains `nokia_experience`/`nokia_exp_2` (`LauncherActivity.java:165-205`, `HomeWorkspaceModel.java:23-53`).

## Samsung comparison

| Mechanism | Samsung 9.0.12.48 | Nokia Exp2 | Difference |
|---|---|---|---|
| Gesture ownership | `DragManager` | `Exp2HomeDragEngine` for icons, other owners for Activity/pager/widgets | Nokia has multiple domains and duplicated empty-area scheduling |
| Page navigation | `Workspace/PagedView` | `WorkspacePager` plus engine callbacks | Nokia manually settles pager and engine independently |
| Drag lifecycle | `DragManager` | Engine `MotionEvent` transaction for icons; Android DragEvent for widgets | Not one cross-component lifecycle |
| Drop policy | `WorkspaceDragController` | Host `commit()` and surface `OnDragListener` | Folder/reorder policy exists in two flag-dependent forms |
| Reorder preview | `WorkspaceReorderController` | `previewReorder()` translations | Nokia preview is visual-only and simpler |
| Folder policy | `FolderController` target/drop policy | target cell + timeout/target checks | Exact Nokia policy should be tested around edge-hover |
| Edge hover | `DragScroller`, delayed runnable | `EDGE_HOVER_TIMEOUT_MS=650` | Similar concept, separate implementation |
| Persistence | drop handlers | `HomeWorkspaceModel` methods call `save()` | Boundary is clear for Exp2 icon path |
| Cancel | `DragManager.cancelDrag` | engine cancel + Android DragEvent end paths | Multiple cancellation channels |

Samsung evidence source: `Research/Samsung_Experience_Comparative_Analysis/Decompiled/9.0.12.48/` and its method index. Samsung static findings are not runtime claims.

## Confirmed defects / risks

### P0 candidate — empty-area long press has two schedulers

`WorkspacePager.dispatchTouchEvent` schedules Edit Mode for an empty point, while `emptyHomeTouch` schedules Edit Mode/options independently. This can produce duplicated or competing callbacks. The duplicate UI outcome is not yet physically verified, so classify as `STATICALLY_CONFIRMED RISK`, not confirmed runtime failure.

### P1 candidate — external Activity swipe can cancel Home sequence

The vertical Drawer recognizer runs at Activity level and sends a synthetic cancel into `super`. A physical icon gesture with sufficient upward movement may therefore be intercepted by a higher-level policy. Runtime reproduction is required.

### P1 candidate — duplicated page settle policy

The pager performs a manual `smoothScrollTo` on UP while the icon engine can already have moved page state during Swipe. This is a structural duplication, not yet proven to cause skipped pages.

### Not confirmed

A simultaneous legacy app-icon DragController is not proven active when `home_engine_v2=true`; the source installs legacy icon long-click/drag listeners only in the false branch. Do not report this as an active conflict without runtime evidence.

## Recommended next step, without code changes in this stage

1. Instrument or log the existing engine only in a separately approved change, or use debugger/logcat if already available.
2. Physically test: tap, icon long press, swipe on icon, swipe empty, edge drag, cancel, and widget touch.
3. First corrective target should be consolidation of empty-area long-press ownership: one scheduler, while preserving Nokia context options and Exp2 Edit Mode semantics.
4. Next, define whether page navigation is owned by `WorkspacePager` or `Exp2HomeDragEngine` per sequence; do not let both commit navigation.
5. Keep `HomeWorkspaceModel` as the only persistent source and preserve the existing feature flag.

## Status

P0 static audit: `COMPLETE`.

Physical device tracing: `NOT_VERIFIED`.

Production changes: none.
