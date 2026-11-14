# Nokia Experience 2 — Edit Mode Visual Refinement Report

Дата: 2026-11-12

## Область изменений

Изменён только Exp2 Edit Mode в `LauncherActivity.java`. Обычный Home, N95, Nokia 5530, `HomeWorkspaceModel`, JSON, `AppWidgetHost`, пользовательские иконки и настройки профилей не изменялись.

## Устройства и параметры

| Параметр | Samsung A reference | Nokia B |
|---|---:|---:|
| Serial | `SAMSUNG_A_REFERENCE` | `NOKIA_B` |
| Model | SM-A520F | SM-A520F |
| Resolution | 1080×1920 | 1080×1920 |
| Density | 480 dpi | Physical 480 dpi, override 420 dpi |
| Launcher | Samsung Experience | Nokia Experience 2 |

System density was not changed.

## Что исправлено

### 1. Snapshot страниц

- Размер карточки рассчитывается по физическим пропорциям reference: `688/1080` и `1042/1920`.
- На Nokia B фактический результат — 688×1042 px.
- Snapshot рисуется в собственный bitmap с непрозрачной базовой заливкой.
- Canvas ограничивается границами конкретной страницы.
- Соседние страницы не попадают в bitmap.

### 2. Glass UI

- Добавлен отдельный размытый bitmap-слой обоев под Edit Mode.
- Overlay больше не показывает живые иконки Home сквозь кнопки.
- Карточки получили цельную матовую поверхность и светлую границу.
- Активная карточка выделяется отдельным цветом.
- Нижняя панель стала самостоятельным стеклянным слоем.
- Действия получили отдельные пиктограммы, подписи и контрастные границы.

### 3. Геометрия

После правки на Nokia B:

| Элемент | Samsung reference | Nokia после правки |
|---|---:|---:|
| Центральная карточка | 688×1042 px, top≈309 | 688×1042 px, top≈310 |
| Нижняя панель top | 1614 px | ≈1616 px |
| Нижняя панель height | 207 px | ≈207 px |
| Override density | не применялся | 420 dpi сохранён |

Композиция адаптируется в пикселях экрана, поэтому override density не используется как причина случайного смещения физических границ reference.

## Скриншоты

- Samsung reference: [A_samsung_edit.png](C:/projects/Nokia/NokiaExperience2/outputs/samsung-behavioral-research/edit-parity/A_samsung_edit.png)
- Nokia до refinement: [B_after_parity_empty.png](C:/projects/Nokia/NokiaExperience2/outputs/samsung-behavioral-research/edit-parity/B_after_parity_empty.png)
- Nokia после refinement: [B_after_visual_refinement_final.png](C:/projects/Nokia/NokiaExperience2/outputs/samsung-behavioral-research/edit-parity/B_after_visual_refinement_final.png)
- Nokia после выбора соседней страницы: [B_after_visual_refinement_selected.png](C:/projects/Nokia/NokiaExperience2/outputs/samsung-behavioral-research/edit-parity/B_after_visual_refinement_selected.png)
- Nokia после выхода: [B_after_visual_refinement_exit.png](C:/projects/Nokia/NokiaExperience2/outputs/samsung-behavioral-research/edit-parity/B_after_visual_refinement_exit.png)

## Функциональная проверка

| Сценарий | Результат | Тип проверки |
|---|---|---|
| Открытие Edit Mode | PASS | Синтетический ADB long press |
| Отображение реального содержимого страниц | PASS | Скриншот Nokia B |
| Отсутствие иконок соседних страниц внутри snapshot | PASS | Визуальная проверка |
| Выделение активной карточки | PASS | Визуальная проверка |
| Выбор соседней страницы | PASS | Синтетический tap |
| Нижняя стеклянная панель и читаемость | PASS | Скриншот Nokia B |
| Выход по Back | PASS | ADB keyevent |
| Возврат к обычному LauncherActivity | PASS | `dumpsys window` |
| FATAL EXCEPTION / ANR | Не обнаружены | Последний logcat |

Физическое касание пальцем отдельно не проверялось; ADB long press/tap обозначены как синтетические проверки.

## Сборка и установка

- `assembleDebug`: `BUILD SUCCESSFUL`.
- APK: `C:/projects/Nokia/NokiaExperience2/app/build/outputs/apk/debug/app-debug.apk`.
- Установка: `adb install -r` → `Success`.
- Data inode до/после: `131107` / `131107`.
- `uninstall`, очистка данных и изменение system density не выполнялись.

Команда `test` отдельно не считается успешной: Gradle unit-test worker завершился с `ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`. Это ошибка тестового окружения/classpath, а не ошибка компиляции Edit Mode.

## Оставшиеся отличия

- Пиктограммы Nokia используют доступные Android system drawables, а не закрытые Samsung assets.
- Точный blur Samsung не копируется; используется совместимый предварительно размытый bitmap для Android 8.
- Физический палец и frame-by-frame timing не измерялись.
- Удаление страницы с реальным пользовательским содержимым намеренно не выполнялось.

## Итог

Статус: **PASS WITH ISSUES**.

Визуальная композиция и стеклянные слои существенно приближены к Samsung reference, snapshot изолирован по странице, нижняя панель больше не пропускает содержимое Home, а пользовательские данные сохранены. Оставшиеся issues относятся к отсутствию физического touch-теста и неисправному Gradle unit-test worker окружению.
