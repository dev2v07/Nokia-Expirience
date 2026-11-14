# Nokia Experience 2.0 — Samsung UX architecture

Статус: аудит и проектирование. Массовая реализация до согласования не выполняется.

## Сохраняемые компоненты

- `LauncherActivity` — текущий контейнер Home, Drawer, drag-and-drop, widgets и настроек.
- `HomeWorkspaceModel` — существующая сетка 4×3 и сохранение `home_workspace`.
- `MenuModel` / `MenuOrganizer` — порядок, скрытие, папки и назначения приложений (`menu_state`).
- `VisualProfile` — профили N95, Nokia 5530 и Nokia Exp 2.
- `IconStore` / `AssetRepository` — пользовательские иконки и ресурсы.
- `AppWidgetHost`, `AppWidgetManager`, `AppWidgetHostView`, `WidgetPreviewPicker` — текущая реализация виджетов.
- `DeviceStateAdapters`, `NokiaNotificationListener`, `AndroidActionResolver` — системная интеграция.

Профили, сетка, иконки, соответствия пакетов, виджеты и существующие ключи SharedPreferences не сбрасываются.

## Важное различие с Samsung Experience

Это не перенос архитектуры Samsung и не общий storage:

| Узел | Nokia Exp 2 | Samsung Experience 9.0 |
|---|---|---|
| Home | `HomeWorkspaceModel`, сетка 4×3, `home_workspace` в `nokia_exp_2` | штатный `com.sec.android.app.launcher`, собственный Launcher3-derived provider и workspace state |
| App Drawer | `MenuModel` + `MenuOrganizer`, `menu_state` и свои папки | Samsung-specific drawer, permissions и системные интеграции |
| Папки | `homefolder:*` на Home и `userfolder:*` в Drawer; отдельные JSON-модели | контейнеры штатного Samsung Launcher; формат и lifecycle не считаются совместимыми |
| Drag & Drop | локальные `DragEvent`-обработчики в `LauncherActivity` | поведение Samsung используется только как UX-эталон |
| Виджеты | собственный `AppWidgetHost` с Android `AppWidgetManager` и ключами `widget_*` | Samsung Launcher использует тот же Android framework, но свои provider/permission/session-механизмы |
| Хранилище | отдельные `SharedPreferences`: `nokia_experience` и `nokia_exp_2` | Samsung storage/provider не читается и не изменяется |

Следовательно, нельзя считать наличие одинаковых названий (`Home`, `Folder`, `AppWidgetHost`) доказательством одинаковой реализации. Переносим только проверяемые UX-инварианты и оставляем Nokia-модели, форматы данных и жизненный цикл независимыми.

## Ограничение доказательности Samsung-исследования

В исследовательской базе подтверждены пакет, APK, providers, permissions, ресурсы и архитектурные зависимости Samsung Launcher. Живой UI-сеанс на разблокированном телефоне ранее не был полностью выполнен, поэтому точные покадровые жесты, папки и lifecycle виджетов Samsung не объявляются измеренными фактами. Они должны служить гипотезами для ручной проверки, а не основанием для копирования реализации.

## Механики, которые заимствуются у Samsung

Только поведение: явное состояние drag-сессии, пространственная обратная связь, предсказуемый reorder, создание папки только при финальном Drop на иконку, редактирование виджетов и непрерывные переходы. Визуальный стиль Nokia и текущие размеры сохраняются. Samsung APK и закрытый код не копируются.

## Предлагаемая минимальная архитектура

1. Сначала покрыть инварианты тестами/диагностическими сценариями: Drop на иконку создаёт папку, Drop между иконками меняет порядок, hover и отмена не коммитят состояние, переход по краю страницы не создаёт новую страницу автоматически.
2. При необходимости добавить небольшие локальные сущности `DragTransaction` и `SamsungUxMotion`. Не заменять `LauncherActivity` и не проводить массовый рефакторинг.
3. Сохранить текущие JSON/SharedPreferences-контракты: `home_workspace`, `menu_state`, `widgets`, `widget_page_*`, `widget_width_*`, `widget_height_*`, `icon_*`.
4. Для анимаций использовать совместимые API Android 8: `ViewPropertyAnimator`, `AnimatorSet`, `ObjectAnimator`, `PathInterpolator`, `DragEvent`; длительности сначала измерить на существующем поведении, а не выдумывать.

## План проверки

- Home: перемещение внутри страницы, между страницами, отмена, сохранение после перезапуска.
- Drawer: Drop на иконку, Drop между иконками, папки, переименование, отмена и edge-hover.
- Widgets: long press, move, resize, delete и восстановление после перезапуска.
- Профили: N95, Nokia 5530, Nokia Exp 2; отсутствие потери иконок и настроек.

## Решение на текущем этапе

Архитектура подготовлена, существующие механизмы в основном уже присутствуют. Следующий безопасный шаг после согласования — локальные тесты инвариантов и, только если они выявят необходимость, небольшой helper для транзакционного drag-and-drop. Миграция данных и массовые изменения не требуются.
