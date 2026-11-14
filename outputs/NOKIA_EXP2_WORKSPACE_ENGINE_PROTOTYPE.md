# Nokia Experience 2.0 — Workspace Engine prototype

Дата: 2026-11-11
Ветка: `samsung-ux-architecture`
Контрольная точка: `nokia-exp-working-before-exp2`

## 1. Статус

Создан изолированный in-memory прототип Home Workspace Engine. Он не подключён к производственному Home и не использует пользовательские данные.

Изменены только:

- новые классы в `app/src/main/java/com/nokiaexperience/launcher/workspace/engine/prototype/`;
- новые unit-тесты в `app/src/test/java/com/nokiaexperience/launcher/workspace/engine/prototype/`;
- минимальная регистрация отдельной debug Activity в `AndroidManifest.xml`;
- стандартная JUnit 4 test dependency в `app/build.gradle`.

Не изменялись `LauncherActivity`, `HomeWorkspaceModel`, `MenuModel`, `MenuOrganizer`, `VisualProfile`, `IconStore`, `AssetRepository`, SharedPreferences, JSON, ресурсы и профили.

## 2. Созданные компоненты

### `PageManager`

Самостоятельно хранит только память прототипа:

- committed page count;
- current page;
- page content flags;
- temporary preview page;
- begin/confirm/cancel page creation;
- deletion only for empty pages;
- prohibition on deleting the last page;
- immutable-style snapshots for the UI.

### `ScrollController`

Чистая политика прокрутки без Android dependency:

- configurable distance and fling thresholds;
- one-page maximum transition;
- first/last page clamping;
- slow and fast swipe decisions;
- cancellation and explicit reset;
- no Samsung-specific timing claims.

### `GestureArbitrator`

Изолированно моделирует ownership жеста:

- `IDLE`;
- `POSSIBLE_SWIPE`;
- `SWIPE`;
- `DRAG`;
- `LONG_PRESS`;
- `CANCELLED`.

Widget touch блокирует автоматическое превращение в Swipe. Один pointer sequence не может одновременно владеть Swipe и Drag.

### `DragTransaction`

Чистая in-memory state machine:

- `IDLE`;
- `ACTIVE`;
- `PREVIEW`;
- `COMMIT`;
- `CANCEL`.

Она не знает о Home model, папках, иконках, SharedPreferences или widget IDs. `PREVIEW` ничего не сохраняет; `COMMIT` выполняется один раз; завершённую сессию нельзя повторно использовать без reset.

### `PrototypeActivity`

Отдельный нейтральный экран с:

- несколькими тестовыми страницами;
- горизонтальным визуальным pager;
- page indicator;
- preview/confirm/cancel/delete temporary page controls;
- gesture state display;
- drag transaction state display;
- тестовыми кнопками Swipe/Drag/Widget touch;
- тестовыми карточками без Nokia иконок.

Activity зарегистрирована только через debug action:

`com.nokiaexperience.launcher.action.OPEN_WORKSPACE_ENGINE_PROTOTYPE`

Она не добавлена в `HOME` или `LAUNCHER` intent filters.

## 3. Результаты чистых тестов

Добавлены JUnit-тесты:

- `PageManagerTest` — empty state, navigation bounds, temporary page confirm/cancel, empty page deletion;
- `ScrollControllerTest` — slow/fast/reverse/boundary/cancel and one-page limit;
- `GestureArbitratorTest` — Swipe, Drag, widget touch, cancellation and IDLE return;
- `DragTransactionTest` — begin, preview, one-shot commit, cancel and session reuse protection.

Дополнительно четыре чистых production-класса и smoke runner были скомпилированы локальным JDK и выполнены напрямую:

```text
PURE_PROTOTYPE_TESTS_PASSED
```

## 4. Результат Android-сборки

Gradle 9.5 был найден в локальном wrapper cache, Java найден в Android Studio JBR:

`C:\Program Files\Android\Android Studio\jbr`

Сборка была запущена командой `test assembleDebug`, но остановилась до компиляции из-за отсутствующего Android SDK:

```text
SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable
or by setting sdk.dir in local.properties.
```

Поэтому Android unit tests и APK-сборка пока не подтверждены. Успешный smoke test подтверждает только чистые Java-компоненты прототипа.

## 5. Как запустить debug-прототип после появления APK

После успешной Android-сборки и установки APK:

```powershell
adb shell am start -a com.nokiaexperience.launcher.action.OPEN_WORKSPACE_ENGINE_PROTOTYPE
```

Или явно по компоненту:

```powershell
adb shell am start -n com.nokiaexperience.launcher/.workspace.engine.prototype.PrototypeActivity
```

Экран использует только тестовые карточки и собственное in-memory состояние. После закрытия Activity данные не сохраняются.

## 6. Проверка изоляции

В новых production-классах нет ссылок на:

- `HomeWorkspaceModel`;
- `MenuModel`;
- `MenuOrganizer`;
- `SharedPreferences`;
- `home_workspace`;
- `menu_state`;
- `AppWidgetHost`;
- `AppWidgetManager`;
- `IconStore`;
- `AssetRepository`.

Debug Activity также не создаёт production models и не читает профильные preferences. Единственное изменение существующих файлов — регистрация Activity и тестовая dependency.

## 7. Оставшиеся ограничения

1. Реальный Android UI пока не собран из-за отсутствия SDK.
2. Pager-прототип проверяет policy, но не подключён к `HorizontalScrollView` производственного Home.
3. Drag Transaction пока не работает с реальными View, иконками, папками или widgets.
4. Page Manager intentionally keeps empty pages only in memory; persistence adapter ещё не создан.
5. Анимационные параметры являются настраиваемыми prototype values, а не измерениями Samsung Experience.
6. Поведение на реальном Samsung Galaxy A5 2017 ещё не проверялось.

## 8. Что делать перед следующим этапом

1. Предоставить Android SDK/API 26+ и повторить `test assembleDebug`.
2. Установить APK в отдельный debug-сеанс и проверить Activity через explicit intent.
3. Проверить визуально page creation/cancel/delete и one-page scrolling.
4. Добавить Android-level tests только после подтверждения чистой state machine.
5. Затем спроектировать адаптер snapshot/commit к `HomeWorkspaceModel`, но не подключать его без отдельного согласования.

Пользовательские данные, профили, иконки, папки и рабочий Nokia Home в этой работе не изменялись.
