# Nokia Experience 2 — P2.2 Home Touch & Glass Refinement

Дата проверки: 2026-11-12
Целевое устройство: Nokia B, `NOKIA_B`, SM-A520F, Android 8.0/API 26.

## Итог

Статус: **PASS WITH LIMITATIONS**.

Сборка, установка и синтетические проверки на Nokia B выполнены. Верхняя зона Home принимает горизонтальный свайп и long press через существующую маршрутизацию, Edit Mode показывает реальные обои в миниатюрах, заголовок стал компактнее, кнопка `+` из заголовка удалена, а внешняя рамка нижней панели снята. Физический палец в этой сессии недоступен, поэтому физические сценарии имеют статус `NOT_VERIFIED`.

## Первопричины и исправления

### Верхняя область Home

`WidgetScrollView` занимал верхнюю область, но горизонтальная последовательность могла остаться у дочернего/scroll-контейнера и не доходить до единого владельца `settleWorkspace()`. В `LauncherActivity.dispatchTouchEvent()` добавлен ограниченный fallback-router только для координат `WidgetScrollView`: после `touchSlop` и подтверждённого горизонтального направления дочерняя последовательность получает `ACTION_CANCEL`, затем на `ACTION_UP` выполняется ровно один `settleWorkspace()`. Вертикальные события не перехватываются и остаются у виджета. При отсутствии видимого виджета сохраняется существующий `emptyHomeTouch()` с одним `pendingHomeLongPress`.

Также верхняя зона остаётся видимой даже без виджета, поэтому она может принимать long press и свайп.

### Обои в миниатюрах

Источник — текущий drawable `WallpaperManager.getInstance(this).getDrawable()`, тот же источник, который используется Home. При создании snapshot сначала рисуется wallpaper в режиме center-crop, затем содержимое страницы `Workspace`. Кэш `exp2PageBitmapCache` предотвращает повторное построение неизменённых карточек; кэш очищается при смене обоев и перестроении страниц.

### Glass UI

Используется совместимый с API 26 кэшируемый backdrop: wallpaper уменьшается до `90×160`, затем проходит два прохода separable box blur — горизонтальный и вертикальный. Радиус для backdrop — `5`; текст и кнопки поверх него не размываются. Карточки получили прозрачные glass-слои, светлую границу, внутренний sheen/highlight и более отчётливое выделение активной карточки.

### Заголовок и нижняя панель

Заголовок уменьшен до компактной зоны с размером текста 17sp и высотой 34–36dp. Кнопка `+` и резервируемый под неё контейнер удалены. Добавление страницы остаётся существующим действием/механизмом временной страницы, а не элементом заголовка. Внешний фон action-row сделан прозрачным; стеклянные поверхности сохранены только у отдельных кнопок.

### Временная страница

Для Exp2 разрешён переход к уже существующей дополнительной странице `workspaceStrip`, создаваемой текущим transient-page механизмом. Формат `home_workspace` не менялся. Само переключение не выполняет persistence; сохранение возможно только через существующий подтверждённый Commit.

## Проверки сборки

Команды выполнялись с JDK 17 и `GRADLE_USER_HOME=C:\gradle-home-nokia`:

- `:app:testDebugUnitTest` — **BUILD SUCCESSFUL**.
- `assembleDebug` — **BUILD SUCCESSFUL**.
- `git diff --check` — ошибок форматирования не обнаружено; Git вывел только предупреждения о переводе LF/CRLF.
- APK: [app-debug.apk](C:/projects/Nokia/NokiaExperience2/app/build/outputs/apk/debug/app-debug.apk), размер 13,740,326 bytes, создан 2026-11-12 02:43:13.

## Установка и ADB-проверки

Установка выполнена только на `NOKIA_B`:

```
adb -s NOKIA_B install -r app-debug.apk
```

Результат: `Success`. Данные приложения не очищались, uninstall не выполнялся, Samsung A не подключался к установочному сценарию.

Синтетически проверено:

| Сценарий | Результат | Доказательство |
|---|---|---|
| Свайп из верхней зоны влево | PASS | [upper-swipe-left-final-2.png](C:/projects/Nokia/NokiaExperience2/work/p2_2-device-test/upper-swipe-left-final-2.png), открыта страница с виджетом |
| Long press в верхней пустой зоне | PASS | [upper-long-press-final-3.png](C:/projects/Nokia/NokiaExperience2/work/p2_2-device-test/upper-long-press-final-3.png) |
| Wallpaper в миниатюрах | PASS | тот же screenshot: фон grass wallpaper присутствует в карточках |
| Компактный заголовок без `+` | PASS | [upper-long-press-final-3.png](C:/projects/Nokia/NokiaExperience2/work/p2_2-device-test/upper-long-press-final-3.png) |
| Отдельные нижние glass-кнопки без общей рамки | PASS | [upper-long-press-final-3.png](C:/projects/Nokia/NokiaExperience2/work/p2_2-device-test/upper-long-press-final-3.png) |
| Текущая Activity | PASS | `LauncherActivity` в `dumpsys window/activity` |
| FATAL EXCEPTION / ANR | PASS | `logcat-final.txt`: `NO_FATAL_OR_ANR_MATCH` |

Дополнительные материалы: [home-final.png](C:/projects/Nokia/NokiaExperience2/work/p2_2-device-test/home-final.png), [upper-swipe-right-final.png](C:/projects/Nokia/NokiaExperience2/work/p2_2-device-test/upper-swipe-right-final.png), [upper-long-press-final.xml](C:/projects/Nokia/NokiaExperience2/work/p2_2-device-test/upper-long-press-final.xml), [logcat-final.txt](C:/projects/Nokia/NokiaExperience2/work/p2_2-device-test/logcat-final.txt).

ADB `input swipe` — синтетическое событие, не физический палец. Физические проверки свайпа поверх виджета, вертикального взаимодействия с виджетом, long press верхней зоны и временной страницы в этой сессии **NOT_VERIFIED**.

## Сохранность данных

Хэши до установки/тестов:

```
nokia_exp_2.xml       f960b7ddb0af86233fb3a6fff5eeed48d3f549410b1a62bc0b91f8b579585ad0
nokia_experience.xml  134cb873820a9294e66e5c1d1b438e5bca1f6bb09112661fcd4175a9ebcd6fdd
```

Хэши после установки и синтетических проверок совпали побайтно. Намеренного Drop или добавления страницы в тесте не выполнялось. `home_workspace`, `menu_state`, виджеты, папки и пользовательские иконки не сбрасывались.

## Изменённые файлы и rollback snapshots

Основной P2.2 код: [LauncherActivity.java](C:/projects/Nokia/NokiaExperience2/app/src/main/java/com/nokiaexperience/launcher/LauncherActivity.java). Связанный существующий P2/P2.1 код в рабочем дереве сохранён.

Rollback snapshots текущего состояния:

- `LauncherActivity.java.p2_2-rollback-20261112_0247`
- `Exp2HomeDragEngine.java.p2_2-rollback-20261112_0247`

В рабочем дереве также присутствуют ранее созданные P1/P2/P2.1 backup-файлы. Их не удалял.

## Ограничения и следующий шаг

Blur является кэшируемым программным box blur, а не системным real-time blur Android 8. Это снижает нагрузку и сохраняет совместимость API 26, но визуально может отличаться от аппаратного Samsung blur. Требуется физическая проверка пальцем на Nokia B для окончательной оценки верхней зоны, поведения реального AppWidget и временной страницы. До такой проверки итог остаётся `PASS WITH LIMITATIONS`.
