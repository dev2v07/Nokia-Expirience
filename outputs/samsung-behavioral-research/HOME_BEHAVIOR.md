# Home Behavior

## Samsung Experience

Статус: `OBSERVED` для синтетического ADB-сценария на устройстве A `SAMSUNG_A_REFERENCE`.

На Home наблюдаются page dots и отдельная навигация между рабочими столами. Долгий ADB tap по свободной области переводит Samsung Launcher в edit mode с preview страниц. Это наблюдение не считается физическим touch acceptance.

## Nokia Exp 2

Статус: `OBSERVED`, частично `VERIFIED` по предыдущим device-проверкам.

- Nokia Status Area, пользовательские иконки, Dock и виджет сохранены.
- Обычный запуск приложения восстановлен через существующий `AndroidActionResolver`.
- `GestureArbitrator` сбрасывается после tap/cancel.
- Производственная Home использует `HomeWorkspaceModel` и ключ `home_workspace`.
- Реальное физическое перемещение пальцем пока `NOT_VERIFIED`.

Парный acceptance: tap→launch→Home синтетически совпал на A и B; Samsung A показал 3 страницы, Nokia B — production Home с собственной моделью страниц.
