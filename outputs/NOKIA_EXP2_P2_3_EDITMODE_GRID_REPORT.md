# Nokia Experience 2 — P2.3 Edit Mode, Workspace Gestures & Grid Settings

Дата: 2026-11-12
Устройство: Nokia B `NOKIA_B`, SM-A520F, Android 8.0/API 26.

## Итог

Статус: **PASS WITH LIMITATIONS**.

Исправлены тёмные полосы в миниатюрах, горизонтальная прокрутка ленты Edit Mode и long press на свободной области второй страницы рядом с виджетом. Отдельный пункт сетки добавлен в существующие «Настройки экрана». Данные профилей не изменялись. Физические действия пальцем недоступны в автоматизированной сессии и отмечены отдельно.

## 1. Тёмные полосы в миниатюрах

Причина была не в отсутствии wallpaper, а в несоответствии aspect ratio: bitmap страницы имел пропорции реального `Workspace`, а `ImageView` помещался в значительно более высокую карточку с `FIT_CENTER`. Незанятая часть `ImageView` показывала тёмный фон `0xD9364E60`, формируя верхнюю и нижнюю полосы.

Исправление в `LauncherActivity.java`:

- высота карточки рассчитывается от фактических `workspacePager/source` width и height;
- область snapshot получает тот же aspect ratio, что и реальная страница;
- `ImageView` сохраняет равномерный масштаб и не растягивает иконки;
- искусственный тёмный фон thumbnail удалён;
- wallpaper и содержимое страницы продолжают рисоваться единым snapshot-слоем.

Доказательство: [edit-mode-final2.png](C:/projects/Nokia/NokiaExperience2/work/p2_3-device-test/edit-mode-final2.png). На карточках нет широких тёмных прямоугольников; wallpaper и иконки занимают область страницы целиком.

## 2. Горизонтальная лента Edit Mode

Причина отсутствия прокрутки — общий Activity-level upper Home router перехватывал sequence внутри Edit Mode, потому что считал координаты ленты верхней Home-зоной.

Изменения:

- upper Home router отключён при `exp2EditMode == true`;
- добавлен минимальный `EditPreviewScrollView` на базе `HorizontalScrollView`;
- после `touchSlop` и подтверждения горизонтального направления лента отменяет дочерний click и прокручивается напрямую;
- короткий tap остаётся click карточки;
- `WorkspacePager` Home не используется для ленты Edit Mode.

Реальная синтетическая проверка на Nokia B:

- свайп влево показал страницы 2 и transient-страницу 3: [edit-swipe-left-final2.png](C:/projects/Nokia/NokiaExperience2/work/p2_3-device-test/edit-swipe-left-final2.png);
- свайп вправо вернул страницы 1 и 2: [edit-swipe-right-final2.png](C:/projects/Nokia/NokiaExperience2/work/p2_3-device-test/edit-swipe-right-final2.png).

## 3. Long Press на второй странице около виджета

`WidgetScrollView` ранее передавал свободную область в обычный `ScrollView`, если на странице вообще существовал виджет. Теперь на `ACTION_DOWN` выполняется hit-test реальных видимых `WidgetFrame`:

- касание внутри виджета остаётся у AppWidget pipeline;
- касание в свободной области вокруг виджета получает существующий единственный `emptyHomeTouch()`;
- горизонтальное движение отменяет long press;
- второй scheduler не добавлялся.

Проверка: после перехода на вторую страницу синтетическое удержание в свободной области ниже виджета открыло Edit Mode с выделением «Экран 2»: [long-press-page2-free-around-widget.png](C:/projects/Nokia/NokiaExperience2/work/p2_3-device-test/long-press-page2-free-around-widget.png).

Удержание непосредственно внутри интерактивного виджета корректно открыло его штатные handles, а не Edit Mode: [long-press-page2-near-widget-final.png](C:/projects/Nokia/NokiaExperience2/work/p2_3-device-test/long-press-page2-near-widget-final.png).

## 4. Фактическая сетка

Текущая производственная модель до и после исправлений:

- `HomeWorkspaceModel.COLUMNS = 4`;
- `HomeWorkspaceModel.ROWS = 3`;
- `CELLS = 12`;
- Dock в эти 12 ячеек не входит;
- widget placement хранится отдельно через `widget_page_<id>` и не занимает Home cell;
- `HomeWorkspaceModel.java` не изменялся.

Следовательно, фактически поддерживается конфигурация **3 × 4 + до 2 виджетов**. Формат `home_workspace` и порядок существующих элементов сохранены.

## 5. Отдельная настройка сетки

В существующий `showSettings()` добавлен отдельный пункт:

`Сетка рабочего стола Exp2: 3 × 4 + 2 виджета`

Он находится в «Настройки экрана», не в заголовке, карточках или нижней панели Edit Mode. Диалог показывает оба требуемых варианта:

- `4 × 4 + 1 виджет`;
- `3 × 4 + 2 виджета`.

Проверка: [workspace-grid-settings-final.png](C:/projects/Nokia/NokiaExperience2/work/p2_3-device-test/workspace-grid-settings-final.png).

Вариант 4 × 4 не применяется автоматически: текущая модель принимает только 12 ячеек, поэтому применение потребовало бы изменения схемы/миграции и могло бы привести к потере или молчаливому переносу элементов. Приложение показывает это ограничение и сохраняет безопасный вариант 3 × 4. Это сознательное безопасное поведение, а не частичное применение.

## 6. Изменённые файлы

Основные изменения:

- [LauncherActivity.java](C:/projects/Nokia/NokiaExperience2/app/src/main/java/com/nokiaexperience/launcher/LauncherActivity.java) — aspect-ratio preview, Edit Mode scroll routing, widget free-area long press, настройка сетки.

`HomeWorkspaceModel.java`, JSON и AppWidgetHost не изменялись.

Rollback/backup-копии:

- `LauncherActivity.java.p2_3-backup-20261112_025449`;
- `HomeWorkspaceModel.java.p2_3-backup-20261112_025449`;
- ранее созданные P1/P2/P2.1/P2.2 backup-файлы сохранены.

## 7. Сборка и установка

- `:app:testDebugUnitTest` — **BUILD SUCCESSFUL**;
- `assembleDebug` — **BUILD SUCCESSFUL**;
- `git diff --check` — без ошибок;
- `adb -s NOKIA_B install -r app-debug.apk` — **Success**;
- пакет не удалялся, данные не очищались.

APK: [app-debug.apk](C:/projects/Nokia/NokiaExperience2/app/build/outputs/apk/debug/app-debug.apk).

## 8. Данные и стабильность

Хэши до и после совпали:

```
nokia_exp_2.xml       f960b7ddb0af86233fb3a6fff5eeed48d3f549410b1a62bc0b91f8b579585ad0
nokia_experience.xml  134cb873820a9294e66e5c1d1b438e5bca1f6bb09112661fcd4175a9ebcd6fdd
```

Выполнена проверка `logcat -d -t 5000`: совпадений `FATAL EXCEPTION` и `ANR in` нет. Полный лог: [logcat-final.txt](C:/projects/Nokia/NokiaExperience2/work/p2_3-device-test/logcat-final.txt).

## 9. Ограничения

- ADB `input swipe` и `input` long press являются синтетическими событиями, не физическим пальцем.
- Физический свайп Edit Mode, физический long press на второй странице и физическое вертикальное взаимодействие с виджетом требуют отдельной ручной проверки.
- Полноценная конфигурация 4 × 4 не включена, поскольку для неё требуется безопасное расширение `HomeWorkspaceModel` и отдельное согласование изменения схемы/миграции.
- Изменения N95, Nokia 5530 и Samsung A не выполнялись.
