# Nokia Exp2 P0 — complete touch pipeline

```text
Activity.dispatchTouchEvent
  ├─ vertical swipe-to-drawer path → synthetic ACTION_CANCEL → Drawer
  └─ WorkspacePager.dispatchTouchEvent
       ├─ empty surface OnTouchListener → emptyHomeTouch → Edit Mode/options timer
       └─ icon visual OnTouchListener (Exp2 flag)
            → Exp2HomeDragEngine
                 ├─ POSSIBLE_DRAG / POSSIBLE_SWIPE
                 ├─ horizontal MOVE over slop → SWIPE → Host.movePageForSwipe
                 ├─ long-press timeout → LONG_PRESS → DRAG
                 │    → preview translation + edge hover + target cell
                 │    → ACTION_UP → Transaction.commit → Host.commit → Model.save
                 └─ ACTION_CANCEL → restore origin + cancel preview + no Model.save
```

## Ownership table

| Event | First relevant receiver | Decision owner | UI mutation | Model mutation | Commit |
|---|---|---|---|---|---|
| Icon DOWN | visual child listener | `Exp2HomeDragEngine.down` | stores origin, disallow intercept | no | no |
| Icon MOVE before slop | engine | `GestureArbitrator` | none | no | no |
| Icon horizontal MOVE | engine | `GestureArbitrator` → SWIPE | page smooth scroll on UP | no | no |
| Icon long press | engine Handler | `GestureArbitrator`/`DragTransaction` | alpha/haptic | no | no |
| Icon drag MOVE | engine | engine + Host preview | translation/reorder preview | no | no |
| Icon UP after drag | engine | `DragTransaction.commit` + Host | refresh/animation | yes | yes |
| Icon CANCEL | engine or external super cancel | engine if delivered | restore | no | no |
| Empty Home DOWN | surface listener and pager path | `emptyHomeTouch` plus pager timer | pending timer | no | no |
| Widget long press | `WidgetFrame.dispatchTouchEvent` | WidgetFrame | handles/drag shadow | later profile prefs | Android DragEvent |

## Component matrix

| Component | Tap | Long press | Swipe | Drag | Drop | Cancel |
|---|---|---|---|---|---|---|
| `LauncherActivity.dispatchTouchEvent` | observes | cancels on movement | vertical Drawer | can synthesize cancel | no | external super cancel |
| `WorkspacePager` | observes/settles | schedules empty-page Edit Mode | manual settle/fling | parent interception | no | releases intercept |
| `Exp2HomeDragEngine` | owns icon tap | owns icon long press | owns icon horizontal classification | owns icon preview | owns icon commit decision | owns icon cancel |
| `GestureArbitrator` | state policy | state transition | state transition | state transition | no | reset policy |
| `HomeWorkspaceModel` | no | no | no | no preview | writes | no |
| legacy Android Drag listeners | only when flag off for app icons; active for widgets | legacy long click | no | Android DragEvent | writes in listeners | `DRAG_ENDED` |

## Confirmed conflicts / non-conflicts

### Confirmed risk

1. Empty Home has both `WorkspacePager` and `emptyHomeTouch` long-press scheduling paths.
2. Activity vertical swipe can inject `ACTION_CANCEL` above child handling.
3. Pager manually settles after `super.dispatchTouchEvent`, while the icon engine independently performs page movement.
4. Icon MotionEvent and widget Android DragEvent have separate ownership/persistence lifecycles.

### Not proven as active conflict

With `home_engine_v2=true`, legacy app-icon `OnLongClickListener` and app-icon `OnDragListener` are not installed in the `else` branch. Therefore a simultaneous old/new app-icon drag path is not statically proven.
