# Nokia Exp2 P2 — Page Navigation and Gesture Conflict Report

Дата проверки: 2026-11-12
Ветка: `samsung-ux-architecture`
Устройство: Nokia B, `NOKIA_B`, `SM-A520F`, Android 8.0 / API 26

## Итог

Статус: **PASS WITH LIMITATIONS**.

Реализована минимальная консолидация управления страницами для Exp2 без изменения формата данных. Обычный pager и Exp2 icon engine теперь используют единую точку `settleWorkspace()`, а `WorkspacePager` не выполняет второй settle для pointer sequence, которую забрала иконка. APK собирается и безопасно обновлён на Nokia B через `adb install -r`.

Полное физическое перетаскивание пальцем и достоверная проверка edge-hover на устройстве этим запуском не подтверждены: применялись синтетические ADB-события.

## Найденные конфликты

1. `Exp2HomeDragEngine.Host.movePageForSwipe()` и `movePageForDrag()` напрямую меняли `workspacePage` и прокрутку, обходя общий путь pager.
2. `WorkspacePager.dispatchTouchEvent()` после доставки события дочернему icon View мог выполнить собственный `settleWorkspace()` на `ACTION_UP`, то есть один pointer sequence имел два потенциальных владельца перехода.
3. Activity-level vertical swipe-to-Drawer не учитывал активную последовательность Exp2 icon View и мог отменить её раньше Drag engine.
4. При отмене Drag исходная страница восстанавливалась отдельным прямым вызовом прокрутки, а не через общий settle path.
5. Folder policy могла получить drop, отпущенный в edge-зоне, после edge-hover.

P1-дублирование пустой области не возвращалось: `pendingHomeLongPress` по-прежнему планируется только в `emptyHomeTouch()`.

## Изменённые файлы

- `app/src/main/java/com/nokiaexperience/launcher/LauncherActivity.java`
- `app/src/main/java/com/nokiaexperience/launcher/Exp2HomeDragEngine.java`
- Отчёт: `outputs/NOKIA_EXP2_P2_PAGE_NAVIGATION_REPORT.md`

Резервные копии исходников созданы до P2:

- `LauncherActivity.java.p2-backup-20261112_020259`
- `Exp2HomeDragEngine.java.p2-backup-20261112_020259`

Другие уже существовавшие изменения рабочего дерева не переписывались.

## Новое распределение ответственности

- `GestureArbitrator` — состояние владельца текущего жеста.
- `Exp2HomeDragEngine` — pointer sequence иконки, Long Press, Drag, edge-hover, Preview/Commit/Cancel.
- `WorkspacePager` — обычный swipe по поверхности и settle только если sequence не принадлежит icon engine.
- `settleWorkspace()` — единая точка ограничения индекса, `workspacePage`, `smoothScrollTo()` и обновления виджетов.
- `HomeWorkspaceModel` — единственный источник постоянного состояния.
- `LauncherActivity` — маршрутизация, запуск приложений и существующая интеграция без нового контроллера.
- Android widget DragEvent pipeline оставлен без переписывания.

## Реализованные изменения

### Обычный swipe

После `super.dispatchTouchEvent(ACTION_DOWN)` pager фиксирует, забрала ли последовательность icon engine. Для пустой поверхности `ACTION_UP` выполняет один `settleWorkspace()`. Для icon sequence pager пропускает собственный settle; swipe/drag path engine остаётся единственным владельцем.

### Drag и edge-hover

Оба пути смены страницы вызывают `settleWorkspace()`. После успешного edge-hover очищаются временная folder target и таймер folder confirmation. Отпускание в edge-зоне не подтверждает Folder Drop.

### Drawer arbitration

Activity-level vertical swipe-to-Drawer не перехватывает pointer sequence, пока Exp2 engine владеет icon sequence. Это не меняет Drawer для остальных профилей и не затрагивает виджеты.

### Cancel / restore

Восстановление исходной страницы при Cancel также проходит через `settleWorkspace()`. Существующие очистка preview, отмена транзакции, снятие `requestDisallowInterceptTouchEvent` и сброс transient state сохранены.

## Проверки

### Статические

- `git diff --check` — пройден.
- `pendingHomeLongPress` — единственная постановка в `emptyHomeTouch()`; отмена сохраняется через `cancelHomeLongPress()`.
- P2 engine использует один Handler для Long Press/своих transient callbacks.
- Exp2 page move callbacks сведены к `settleWorkspace()`.

### Сборка

- `assembleDebug --no-daemon` — **BUILD SUCCESSFUL**.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Первый `test --no-daemon` — **FAIL**, инфраструктурно: Gradle test workers не находили `worker.org.gradle.process.internal.worker.GradleWorkerMain`, несмотря на наличие класса в `gradle-worker.jar`.
- Диагностика показала проблему запуска worker classpath через профиль Gradle с кириллическим путём `C:\Users\<local-user>\.gradle`.
- Установлен Temurin JDK 17 (`17.0.21`) и создан отдельный ASCII Gradle cache `C:\gradle-home-nokia` без изменения проекта.
- Повторный запуск с `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot` и `GRADLE_USER_HOME=C:\gradle-home-nokia`: `:app:testDebugUnitTest BUILD SUCCESSFUL`.

### Nokia B

- `adb devices -l`: Nokia B `NOKIA_B` доступен; Samsung A `SAMSUNG_A_REFERENCE` не использовался для установки.
- `adb install -r` — **Success**; uninstall и очистка данных не выполнялись.
- `LauncherActivity` запущена явно; `mResumedActivity` подтверждает `com.nokiaexperience.launcher/.LauncherActivity`.
- Размер экрана: `1080x1920`; физическая плотность `480`; override density `420`.
- Logcat после запуска и синтетических действий: новых `FATAL EXCEPTION` и `ANR in` не найдено.

Скриншоты и лог:

- [after-install.png](../work/p2-device-test/after-install.png)
- [empty-long-press.png](../work/p2-device-test/empty-long-press.png)
- [empty-long-press-1400.png](../work/p2-device-test/empty-long-press-1400.png)
- [swipe-left.png](../work/p2-device-test/swipe-left.png)
- [swipe-right.png](../work/p2-device-test/swipe-right.png)
- [logcat.txt](../work/p2-device-test/logcat.txt)

ADB swipe является синтетическим вводом. Скриншоты подтверждают отображение Home и отсутствие падения, но не подтверждают физический палец, устойчивый edge-hover или полный реальный Long Press + Drag.

## Данные

Хэши Nokia B до установки/тестов и после повторной установки/тестов совпали:

| Файл | До | После |
|---|---|---|
| `nokia_exp_2.xml` | `1240e3f1ab48d14c535c62b8d83d3a4cd1bc703f702a656e31296f1e010df95c` | `1240e3f1ab48d14c535c62b8d83d3a4cd1bc703f702a656e31296f1e010df95c` |
| `nokia_experience.xml` | `134cb873820a9294e66e5c1d1b438e5bca1f6bb09112661fcd4175a9ebcd6fdd` | `134cb873820a9294e66e5c1d1b438e5bca1f6bb09112661fcd4175a9ebcd6fdd` |

Формат `home_workspace`/`menu_state`, профили, иконки, папки, виджеты и SharedPreferences не мигрировались и не очищались.

## Ограничения перед PASS

1. Нужно выполнить физические сценарии на Nokia B: swipe по пустой области, Long Press и Drag иконки, edge-hover в обе стороны, Cancel и Drop на соседней странице.
2. Из-за Gradle Worker failure unit-тесты не были фактически запущены.
3. Синтетический сценарий long press через `adb input swipe` в данной проверке не дал надёжного визуального подтверждения входа в Edit Mode; это отмечено как непроверенное, а не как PASS.
4. Samsung A не менялся и не сравнивался в рамках P2.

Перед включением результата как окончательного production PASS необходима ручная проверка пальцем на Nokia B. Инфраструктурный блокер Gradle test worker устранён локальным ASCII-кэшем; системные версии проекта не менялись.
