# Nokia Exp2 P0 — Swipe sequence

Статус: `STATICALLY_CONFIRMED`; runtime visual acceptance: `NOT_VERIFIED`.

## Swipe по иконке

`Activity.dispatchTouchEvent` → `WorkspacePager.dispatchTouchEvent` → child visual `OnTouchListener` → `Exp2HomeDragEngine.onIconTouch` → `GestureArbitrator.move` → `SWIPE` → `ACTION_UP` → `Host.movePageForSwipe` (`LauncherActivity.java:368-372`, `Exp2HomeDragEngine.java:136-142`).

`movePageForSwipe()` изменяет `workspacePage`, вызывает `workspacePager.smoothScrollTo()` и `updateVisibleWidgets()`. Это отдельный путь, не обычный fling HorizontalScrollView.

## Swipe по пустой области

Пустая page surface имеет `setOnTouchListener(this::emptyHomeTouch)` (`LauncherActivity.java:443`). Этот listener возвращает `true` на DOWN/MOVE/UP (`LauncherActivity.java:1083-1094`). Pager после `super.dispatchTouchEvent` дополнительно вызывает `settleWorkspace()` на UP (`LauncherActivity.java:537-543`). Следовательно, pager может получить поток, но child listener также является terminal consumer; точное поведение зависит от Android dispatch и требует device test.

## Перехват

На `ACTION_DOWN` `WorkspacePager` вызывает `requestDisallowInterceptTouchEvent(true)` (`LauncherActivity.java:523-526`). Наиболее вероятный риск — не конкуренция `HorizontalScrollView` с Exp2 engine, а ручное дублирование settle поверх стандартного scroll pipeline.

## Drawer gesture

`LauncherActivity.dispatchTouchEvent()` имеет отдельный вертикальный swipe-to-drawer путь и отправляет `ACTION_CANCEL` в `super` перед открытием Drawer (`LauncherActivity.java:1066-1072`). Это внешний владелец, который может прервать Home sequence.
