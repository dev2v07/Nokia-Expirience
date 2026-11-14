# Nokia Exp2 P2.1 — Home Navigation, Edit Mode and Glass UI

Дата: 2026-11-12
Ветка: `samsung-ux-architecture`
Устройство: Nokia B `NOKIA_B`, `SM-A520F`, Android 8.0 / API 26

## Итоговый статус

**PASS WITH LIMITATIONS**

Исправления внесены в настоящий Nokia Home и существующий Exp2 Edit Mode. Формат `home_workspace`, `menu_state`, SharedPreferences, AppWidgetHost и профили не менялись. APK собран, установлен через `adb install -r`, unit-тесты прошли.

Физические проверки пальцем не выполнялись; ADB-жесты отмечены как синтетические.

## Первопричины и исправления

### 1–2. Swipe по пустой зоне и странице с виджетом

Причина: верхняя область Home была отдельным `ScrollView` виджетов. Когда текущая страница не имела видимого виджета, `widgetScroller` становился `INVISIBLE`, но продолжал занимать место; горизонтальный MotionEvent не доходил до `WorkspacePager`. Даже при наличии виджета вертикальный widget pipeline не имел горизонтального forwarding.

Исправление:

- добавлен `WidgetScrollView`;
- горизонтальное движение после `touchSlop` перехватывается только как горизонтальный gesture;
- вертикальное движение остаётся штатным `ScrollView`/AppWidget pipeline;
- переход выполняется через единственный `settleWorkspace()`;
- пустой `widgetScroller` остаётся touch-доступным;
- пустая область без виджетов продолжает использовать `emptyHomeTouch()` для P1 Long Press.

`WorkspacePager` и `Exp2HomeDragEngine` не получили второго механизма хранения страниц или второго persistence path.

### 3. Выбор страницы из Edit Mode

Причина: обработчик карточки только менял `workspacePage`, прокручивал pager и оставлял overlay Edit Mode открытым.

Исправление:

```java
settleWorkspace(page);
exitExp2EditMode();
```

Теперь выбор карточки закрывает Edit Mode и открывает выбранную страницу без дополнительного Back.

### 4. Заголовок Edit Mode

Заголовочная зона уменьшена: верхний padding `20dp → 8dp`, высота heading `52dp → 42dp`, высота кнопки добавления `48dp → 40dp`. Системные отступы и нижняя панель сохранены.

### 5. Пропорции миниатюр

`ImageView.ScaleType.FIT_XY` заменён на `FIT_CENTER`. Snapshot страницы продолжает строиться из реального `Workspace` через Canvas, но теперь масштабируется равномерно с сохранением aspect ratio.

### 6. Glass UI

Edit Mode получил отдельный стеклянный слой:

- blurred wallpaper backdrop сохраняется и используется как фон;
- glass surfaces получили полупрозрачный base layer;
- добавлен световой градиент sheen;
- сохранены тонкие светлые borders;
- активная карточка выделяется отдельной границей;
- нижняя панель и кнопки используют тот же glass drawable;
- тяжёлый blur на каждом кадре не используется.

Это совместимая с Android 8 реализация без системного blur API и без размытия текста/контролов.

### 7. Работа после перезапуска

Добавлен дополнительный `workspacePager.post(this::rebuildWorkspace)` после построения Home. Это гарантирует повторную сборку страниц после получения реальных размеров pager. Виджеты учитываются в `homePageCount()`, а горизонтальные жесты верхней зоны теперь не зависят от наличия AppShortcut.

## Изменённые файлы

- `app/src/main/java/com/nokiaexperience/launcher/LauncherActivity.java`
- `outputs/NOKIA_EXP2_P2_1_HOME_AND_EDITMODE_FIX_REPORT.md`

Предыдущие изменения P1/P2 сохранены. Резервные копии P2.1:

- `LauncherActivity.java.p2_1-backup-20261112_022211`
- `Exp2HomeDragEngine.java.p2_1-backup-20261112_022211`

## Сборка и тесты

- `:app:testDebugUnitTest` — **BUILD SUCCESSFUL**.
- `assembleDebug` — **BUILD SUCCESSFUL**.
- `git diff --check` — пройден.
- Gradle запускался с JDK 17 и `GRADLE_USER_HOME=C:\gradle-home-nokia`.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Проверка Nokia B

- `adb install -r` — **Success**.
- Samsung A не использовался.
- `LauncherActivity` запущена штатно.
- Синтетический swipe по нижней pager-зоне после перезапуска перевёл Home на страницу с виджетом и TikTok.
- Синтетический выбор второй карточки Edit Mode немедленно закрыл overlay и открыл выбранный рабочий стол.
- Edit Mode визуально показал компактный заголовок, горизонтальную ленту, индикатор, нижнюю glass-панель и световые границы карточек.
- В логах после проверки нет `FATAL EXCEPTION` и `ANR in`.

Артефакты:

- [Edit Mode screenshot](../work/p2_1-device-test/edit-mode-synthetic.png)
- [Home after selecting page](../work/p2_1-device-test/after-page2-card-tap.png)
- [Home after restart swipe](../work/p2_1-device-test/pager-swipe-left-v2.png)
- [Final restart swipe](../work/p2_1-device-test/final-page-swipe-left.png)
- [Final logcat](../work/p2_1-device-test/logcat-final.txt)

Все ADB swipe — синтетические события, не физический палец.

## Данные

Хэши до финальной установки и после запуска/синтетического swipe совпали:

| Файл | До | После |
|---|---|---|
| `nokia_exp_2.xml` | `f960b7ddb0af86233fb3a6fff5eeed48d3f549410b1a62bc0b91f8b579585ad0` | `f960b7ddb0af86233fb3a6fff5eeed48d3f549410b1a62bc0b91f8b579585ad0` |
| `nokia_experience.xml` | `134cb873820a9294e66e5c1d1b438e5bca1f6bb09112661fcd4175a9ebcd6fdd` | `134cb873820a9294e66e5c1d1b438e5bca1f6bb09112661fcd4175a9ebcd6fdd` |

Не выполнялись `uninstall`, `pm clear`, миграция JSON или изменение профилей.

## Ограничения

1. Свайп поверх реального виджета проверен синтетически, но не физическим пальцем.
2. Физический выбор карточки Edit Mode и физический swipe после перезапуска требуют ручной проверки.
3. Snapshot строится из текущего View hierarchy; отдельная отрисовка невидимого виджета на другой странице не добавлялась, чтобы не менять AppWidgetHost lifecycle.
4. Полноценное измерение FPS и сравнение с Samsung не выполнялись.

Следующий обязательный шаг — ручная проверка пальцем на Nokia B: свайп по пустой верхней зоне, свайп поверх виджета, выбор карточки страницы и повторная проверка после перезапуска.
