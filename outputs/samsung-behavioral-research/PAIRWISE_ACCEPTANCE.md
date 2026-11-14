# Pairwise Acceptance — Samsung A / Nokia B

Дата: 2026-11-12. Все команды выполнялись с явным `adb -s SERIAL`.

## Устройства

| Роль | Serial | Launcher | Состояние |
|---|---|---|---|
| Samsung A | `SAMSUNG_A_REFERENCE` | `com.sec.android.app.launcher/.activities.LauncherActivity` | OBSERVED |
| Nokia B | `NOKIA_B` | `com.nokiaexperience.launcher/.LauncherActivity` | OBSERVED |

Оба устройства: SM-A520F, Android 8.0.0/API26.

## Pairwise results

| Сценарий | Samsung A | Nokia B | Расхождение | Доказательство |
|---|---|---|---|---|
| Home baseline | OBSERVED: Samsung Home | OBSERVED: Nokia Home | Разный визуальный слой ожидаем | `SAMSUNG_A_REFERENCE_baseline.png`, `NOKIA_B_baseline.png` |
| Tap → запуск → Home | SYNTHETICALLY_TESTED: Google Play открылся | SYNTHETICALLY_TESTED: PlayerPro открылся | MATCHED по жизненному циклу | `*_after_verified_tap.png`, dumpsys `mResumedActivity` |
| Page count | OBSERVED: UI сообщает «Страница 3 из 3» | OBSERVED: Home-модель/виджетные страницы, отдельного page indicator в hierarchy нет | MISMATCH | `SAMSUNG_A_REFERENCE_baseline.xml`, `NOKIA_B_baseline.xml` |
| Slow swipe left | SYNTHETICALLY_TESTED: перешёл на страницу Galaxy Essentials | SYNTHETICALLY_TESTED: стартовая область была страницей с виджетом; однозначный переход не подтверждён этим прогоном | NOT_VERIFIED для B | `A_swipe_left_slow.png`, `B_swipe_left_slow.png` |
| Fast swipe right | SYNTHETICALLY_TESTED | SYNTHETICALLY_TESTED | Не измерено одинаковое число страниц | `A_swipe_right_fast.png`, `B_swipe_right_fast.png` |
| Long press empty Home → Edit Mode | SYNTHETICALLY_TESTED: Samsung preview | SYNTHETICALLY_TESTED: Nokia overlay | MISMATCH в составе действий | `A_edit_mode_synthetic.png`, `B_edit_synthetic.png` |
| Page previews | OBSERVED: Samsung preview cards | OBSERVED: Nokia cards «Экран 1/2/3 (новый)» | Different implementation; Nokia keeps Nokia visual | `A_edit_mode_synthetic.png`, `B_edit_synthetic.png` |
| Add page | OBSERVED as Samsung control; action not committed to avoid modifying A | UI control exists, action not committed to avoid changing B data | NOT_VERIFIED | `A_edit_mode.xml`, `B_edit_synthetic.xml` |
| Delete empty page | Control observed on A and B | Actual deletion not committed | NOT_VERIFIED | UI hierarchy files |
| Wallpaper / widgets / settings | Samsung exposes four bottom actions including Themes | Nokia exposes Wallpaper, Widgets, Settings, Add/Delete page | MISMATCH: Themes absent in Nokia | screenshots and XML |
| Long press icon | NOT_VERIFIED physically | NOT_VERIFIED physically | No acceptance | No physical pointer evidence |
| Long press + Drag | NOT_VERIFIED | NOT_VERIFIED | BLOCKED by ADB limitation | No physical MotionEvent trace |
| Folder drop | NOT_VERIFIED | NOT_VERIFIED | BLOCKED | No physical Drag sequence |
| Cancel Drag | NOT_VERIFIED | NOT_VERIFIED | BLOCKED | No physical Drag sequence |

Statuses distinguish observation from physical acceptance. `input swipe` is recorded as synthetic only.

## Technical interpretation

### Confirmed matches

- Both launchers remain usable after a normal tap and return to Home.
- Nokia uses its own `AndroidActionResolver`, not a Samsung provider.
- Nokia edit mode is integrated into production Home rather than a separate prototype Activity.
- No `home_workspace`, `menu_state`, widget IDs or Samsung settings were changed by this acceptance run.

### Confirmed mismatches

1. Samsung exposes an explicit `Темы` action in edit mode; Nokia does not currently expose an equivalent action.
2. Samsung exposes a page indicator/content description; Nokia baseline hierarchy does not expose an equivalent indicator.
3. Samsung preview shows established pages; Nokia preview includes a transient new page because the current JSON model cannot persist an empty page independently.

### Component ownership

- Tap/gesture lifecycle: `GestureArbitrator`, `Exp2HomeDragEngine`.
- Production Home rendering and page surface: `LauncherActivity.WorkspacePager` and `rebuildWorkspace()`.
- Persistent placement: `HomeWorkspaceModel` / `home_workspace`.
- Edit mode: Exp2-only overlay in `LauncherActivity`; no Samsung code or provider copied.
- Widget state: existing `AppWidgetHost`; not changed.

## Next change recommendation

Do not add another gesture controller. The next safe change should be a small Exp2-only parity pass:

1. expose a real page indicator in Nokia Home;
2. decide whether Nokia's existing theme settings can be opened as the `Темы` action;
3. test actual add/delete page actions only after a user-approved data-preserving policy is defined;
4. perform physical long-press/drag on both phones. ADB-only evidence is insufficient.

No production code was changed during this pairwise acceptance run.
