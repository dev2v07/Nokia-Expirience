# Nokia Exp2 P0 — Tap sequence

Статус доказательности: `STATICALLY_CONFIRMED`.

## Иконка при `home_engine_v2=true`

1. `LauncherActivity.dispatchTouchEvent(MotionEvent)` получает общий поток Activity (`LauncherActivity.java:1056-1080`). Он не запускает приложение напрямую.
2. `WorkspacePager.dispatchTouchEvent(MotionEvent)` получает поток pager (`LauncherActivity.java:520-545`), записывает начальную координату и передаёт событие в `super.dispatchTouchEvent`.
3. Android доставляет событие визуальному дочернему View иконки, которому в `rebuildWorkspace()` назначен `OnTouchListener` (`LauncherActivity.java:477-491`).
4. Listener вызывает `Exp2HomeDragEngine.onIconTouch(...)` (`Exp2HomeDragEngine.java:63-69`).
5. `ACTION_DOWN` переводит `GestureArbitrator` в `POSSIBLE_DRAG`, сохраняет pointer/origin и ставит системный long-press timeout (`Exp2HomeDragEngine.java:72-95`).
6. При неподвижном отпускании `ACTION_UP` вычисляется `tap`; вызывается `Host.tapped()` (`Exp2HomeDragEngine.java:155-163`).
7. `Host.tapped()` вызывает `launch(entry)` либо `openHomeFolder()` (`LauncherActivity.java:394`).

## Важное ограничение

Точный внутренний путь `launch(entry) → AndroidActionResolver` требует отдельного чтения метода `launch`; в текущем фрагменте подтверждено наличие единой точки `Host.tapped`, но не весь framework-level запуск.

## Потеря Tap

Tap не должен пройти после горизонтального движения: `GestureArbitrator.move()` переводит sequence в `SWIPE`, а двигатель удаляет pending callbacks (`Exp2HomeDragEngine.java:103-111`). После `ACTION_CANCEL` выполняется очистка. Потенциальный внешний конфликт: Activity-level вертикальный жест открытия Drawer может послать synthetic `ACTION_CANCEL` всему `super`-потоку (`LauncherActivity.java:1066-1072`). Его влияние именно на физический tap по иконке требует runtime-проверки.
