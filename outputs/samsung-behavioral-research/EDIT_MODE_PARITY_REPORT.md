# Nokia Experience 2 — Edit Mode Parity Report

Дата: 2026-11-12

## Устройства

| Устройство | Serial | Разрешение | Плотность | Launcher |
|---|---|---:|---:|---|
| Samsung A | `SAMSUNG_A_REFERENCE` | 1080×1920 | 480 dpi | Samsung Experience |
| Nokia B | `NOKIA_B` | 1080×1920 | 420 dpi override | Nokia Experience 2 |

Samsung A использовался только для наблюдения. APK устанавливался только на Nokia B через `adb install -r`.

## Измеренный Samsung reference

По `A_samsung_edit.xml`:

- активная центральная карточка: `[196,309][884,1351]` — 688×1042 px;
- соседняя видимая карточка начинается с x=904;
- кнопка удаления центральной карточки: `[785,309][884,408]` — 99×99 px;
- нижняя панель действий: y=1614..1821 — 207 px;
- кнопки Samsung: x=60..291, 303..534, 546..777, 789..1020 — ширина 231 px, промежуток 12 px;
- UI hierarchy содержит действия `Обои`, `Темы`, `Виджеты`, `Настройки экрана`;
- page preview содержит content descriptions вида `Страница N из 3`.

Скриншот Samsung: [A_samsung_edit.png](C:/projects/Nokia/NokiaExperience2/outputs/samsung-behavioral-research/edit-parity/A_samsung_edit.png)

## Что изменено в Nokia Exp2

Изменён только:

- `app/src/main/java/com/nokiaexperience/launcher/LauncherActivity.java`

Новая Exp2-only структура:

- заголовок «Рабочие столы»;
- горизонтальный `HorizontalScrollView` с карточками страниц;
- bitmap snapshot фактического `workspaceStrip` для каждой миниатюры;
- подсветка активной карточки;
- индикатор `Экран N из M`;
- кнопка `＋` отдельного добавления временного экрана;
- кнопка `×` удаления на каждой карточке;
- нижняя панель `Обои / Темы / Виджеты / Настройки экрана`;
- плавная alpha/translation-анимация входа и выхода.

«Темы» вызывает существующий `showAppearanceSettings()`, а не фиктивную кнопку. Остальные действия используют существующие `chooseWallpaper()`, `addWidget()` и `showSettings()`.

## Совместимость данных

- `home_workspace` не менялся;
- `menu_state` не менялся;
- SharedPreferences не сбрасывались;
- AppWidgetHost не изменялся;
- N95 и Nokia 5530 не затрагивались;
- пустая страница остаётся временной и становится постоянной только через существующий commit размещения;
- страница с содержимым не удаляется: показывается явное сообщение о необходимости предварительного переноса элементов.

## Проверка на Nokia B

- `assembleDebug`: `BUILD SUCCESSFUL`;
- `adb install -r`: `Success`;
- inode `/data/user/0/com.nokiaexperience.launcher`: `131107` до и после;
- Exp2 Edit Mode открыт на реальном Home;
- отображены snapshot-миниатюры, активная карточка, кнопки удаления и `＋`;
- выбор карточки изменяет активную страницу и индикатор;
- действие «Темы» открыло существующий экран «Внешний вид»;
- Back закрыл экран настроек и Edit Mode;
- после выхода активен `LauncherActivity`;
- FATAL EXCEPTION и ANR не обнаружены.

Скриншоты Nokia:

- [B_after_parity_empty.png](C:/projects/Nokia/NokiaExperience2/outputs/samsung-behavioral-research/edit-parity/B_after_parity_empty.png)
- [B_selected_page2.png](C:/projects/Nokia/NokiaExperience2/outputs/samsung-behavioral-research/edit-parity/B_selected_page2.png)
- [B_themes_action.png](C:/projects/Nokia/NokiaExperience2/outputs/samsung-behavioral-research/edit-parity/B_themes_action.png)
- [B_after_exit.png](C:/projects/Nokia/NokiaExperience2/outputs/samsung-behavioral-research/edit-parity/B_after_exit.png)

## Ограничения

- Физический палец не использовался; вход проверен синтетическим ADB long press.
- Фактическое удаление страницы с пользовательским содержимым не выполнялось, чтобы не менять данные без отдельного сценария переноса.
- Добавление постоянной пустой страницы не выполняется из-за ограничения текущего `HomeWorkspaceModel`.
- Pixel-perfect сравнение иконок/шрифтов Samsung не является целью: Nokia visual layer сохранён.

## Итог

Статус: `PASS WITH ISSUES`.

Структура Nokia Edit Mode теперь соответствует наблюдённой Samsung-модели: компактная горизонтальная лента реальных preview, выделение, page indicator, карточное удаление и нижние четыре действия. Оставшееся ограничение — безопасное управление постоянными пустыми страницами без изменения существующего JSON-контракта.
