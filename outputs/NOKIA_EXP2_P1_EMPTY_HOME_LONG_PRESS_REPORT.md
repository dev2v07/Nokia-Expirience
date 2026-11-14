# Nokia Experience 2 — P1 Empty Home Long Press

## Result

The duplicate empty-workspace long-press scheduler was removed. `emptyHomeTouch()` is now the only production path that schedules `pendingHomeLongPress` for an empty Home surface.

The selection policy is preserved:

- Nokia Exp 2 with `home_engine_v2=true` → `enterExp2EditMode()`.
- Other profiles / legacy path → `showHomeOptions()`.

No changes were made to `Exp2HomeDragEngine`, `GestureArbitrator`, `DragController`, `DragTransaction`, `HomeWorkspaceModel`, JSON, widget storage, icons, or feature-flag semantics.

## Before-change safety

- Existing Git changes were preserved; no reset, clean, checkout, or uninstall was executed.
- Backup created before editing:
  `app/src/main/java/com/nokiaexperience/launcher/LauncherActivity.java.p1-backup-20261112_014928`
- Existing working-tree changes remain outside this focused change.

## Changed code

### Removed duplicate owner

Removed the `WorkspacePager.dispatchTouchEvent()` block that checked `emptyWorkspacePoint()` and posted a second Edit Mode callback. The pager still records movement, cancels the common pending callback on movement, performs page settling, and releases interception.

Reference: `LauncherActivity.java`, former lines 523–530.

### Retained single owner

`emptyHomeTouch()` remains responsible for:

- cancelling the previous callback on `ACTION_DOWN`;
- scheduling one callback with `ViewConfiguration.getLongPressTimeout()`;
- cancelling on movement beyond `touchSlop`;
- cancelling on `ACTION_UP` and `ACTION_CANCEL`;
- selecting Edit Mode versus Nokia Context Menu.

Reference: `LauncherActivity.java:1076–1090`.

### Focus-loss cleanup

`onWindowFocusChanged(false)` now calls `cancelHomeLongPress()`. Existing `onPause()` cleanup remains in place.

Reference: `LauncherActivity.java:1130–1138`.

## Invariants

| Invariant | Static result |
|---|---|
| At most one pending empty-area callback | `STATICALLY_VERIFIED` |
| Tap before timeout opens no menu/edit mode | `STATICALLY_VERIFIED` |
| Movement beyond touch slop cancels | `STATICALLY_VERIFIED` |
| `ACTION_CANCEL` clears callback | `STATICALLY_VERIFIED` |
| Focus loss clears callback | `STATICALLY_VERIFIED` |
| Edit Mode and context menu cannot be scheduled by two empty-area paths | `STATICALLY_VERIFIED` |
| Icon launch and Exp2 icon Drag unchanged | `STATICALLY_VERIFIED` |
| `home_workspace` is not touched by this change | `STATICALLY_VERIFIED` |
| Other profiles retain legacy context-menu selection | `STATICALLY_VERIFIED` |

## Verification

### Static checks

- `pendingHomeLongPress` scheduling in empty Home: one remaining call, inside `emptyHomeTouch()`.
- `WorkspacePager`: cancellation only; no second scheduling path.
- `onPause()` and window-focus loss both clear pending state.
- `git diff --check`: passed; only line-ending warnings were reported for pre-existing files.

Status: `STATICALLY_VERIFIED`.

### Unit tests and build

Gradle 9.5.0 was found in the local wrapper cache and Java was supplied locally through Android Studio JBR. `assembleDebug` completed successfully. The APK is:

`app/build/outputs/apk/debug/app-debug.apk`

The `test` task compiled the test sources but failed in the Gradle test worker with `Could not find or load main class worker.org.gradle.process.internal.worker.GradleWorkerMain`. This is an environment/test-worker failure; no assertion result is reported as passed.

- Build: `ASSEMBLE_DEBUG = PASS`
- Unit tests: `NOT_VERIFIED` (Gradle worker infrastructure failure)

### Nokia B device tests

ADB was found at `C:\projects\Nokia\platform-tools\adb.exe`. Nokia B `NOKIA_B` was identified as `SM-A520F`, Android `8.0.0`, API 26. Samsung A was not modified.

The APK was installed with `adb install -r` only. Its certificate matched the installed package: Android Debug certificate SHA-256 `5406b3bc641a6de095c666af6b2c9930b32592e6ec51c1ed55f1028416ed5f3b`. No uninstall or data clear was used.

Results on Nokia B:

| Scenario | Result | Status |
|---|---|---|
| Short synthetic tap on empty area | No Edit Mode/Context Menu observed | `ADB_SYNTHETICALLY_TESTED` |
| Synthetic long press on empty area | Exp2 Edit Mode opened with “Рабочие столы”, page cards and bottom actions | `ADB_SYNTHETICALLY_TESTED` |
| Movement beyond touch slop before timeout | Home remained visible; no Edit Mode/Context Menu | `ADB_SYNTHETICALLY_TESTED` |
| Edit Mode close via Back | Returned to Home | `ADB_SYNTHETICALLY_TESTED` |
| Ordinary app launch | `com.tbig.playerprotrial/.BrowsingActivity` became resumed | `ADB_SYNTHETICALLY_TESTED` |
| Page swipe | ADB swipe executed; no crash observed | `ADB_SYNTHETICALLY_TESTED` |
| FATAL/ANR scan | No matching app crash/ANR lines | `ADB_SYNTHETICALLY_TESTED` |
| Physical finger interaction | Not performed by Codex | `NOT_VERIFIED` |

Screenshots and raw UI/log artifacts are in `work/p1-device-test/`:

- `home-closed.png`
- `long-press.png`
- `move-cancel.png`
- `short-tap.png`
- `home.xml`, `short.xml`, `long.xml`, `cancel.xml`

### Data verification

The two profile preference files were read through `run-as` before installation and after all tests. Hashes were identical:

| File | Before | After | Result |
|---|---|---|---|
| `nokia_exp_2.xml` | `1240E3F1AB48D14C535C62B8D83D3A4CD1BC703F702A656E31296F1E010DF95C` | same | unchanged |
| `nokia_experience.xml` | `134CB873820A9294E66E5C1D1B438E5BCA1F6BB09112661FCD4175A9EBCD6FDD` | same | unchanged |

Status: `ADB_SYNTHETICALLY_TESTED`; data preservation confirmed.

Not tested on device: legacy Context Menu on N95/5530, physical finger long press, and detailed touch-focus-loss interruption.

## Data safety

No `pm clear`, uninstall, SharedPreferences reset, JSON migration, model-schema change, or device write was performed. The current source change does not call `HomeWorkspaceModel` from the empty-area long-press callback.

## Remaining issues

- Activity-level Drawer interception was intentionally not changed.
- Pager settle ownership was intentionally not changed.
- Widget Drag was intentionally not changed.
- Physical device acceptance is still required when `adb`/device access is available.

## Final status

`PASS WITH LIMITATIONS — STATIC + ADB SYNTHETIC VERIFICATION; PHYSICAL FINGER TEST PENDING`
