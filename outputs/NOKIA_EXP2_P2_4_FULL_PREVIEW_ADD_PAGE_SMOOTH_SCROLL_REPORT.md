# Nokia Experience 2 — P2.4 report

Status: **PASS WITH LIMITATIONS**

P2.4 was integrated into the existing Exp2 Home/Edit Mode. No second launcher screen, data migration, uninstall, or preference reset was used.

## Implemented

### 1. Startup animation

`LauncherActivity.startStartupSound()` still uses the existing `StartupView` and its 20-frame animation. Only frame pacing was changed:

| Parameter | Before | After |
|---|---:|---:|
| Frame interval | 65–220 ms | 45–120 ms |
| Animated frames | 20 | 20 |
| Frame phase | 1.3–4.4 s | 0.9–2.4 s |
| Existing fade | 220 ms | 220 ms |

The animation was not removed and workspace construction remains independent of the frame loop. The exact cold-start completion time was not measured reliably through ADB because the device screenshot loop cannot provide a physical observation of the transition boundary; the static timing change is confirmed in source.

### 2. Full Home preview

The previous preview used only the middle `WorkspacePager` region, which caused a shortened image and excluded the top/bottom portions. `exp2PageBitmap()` now creates a bitmap using the complete Home dimensions and draws in one coordinate system:

1. current wallpaper/background;
2. top/status layer;
3. the safely available current-page widget layer;
4. the page Workspace content;
5. Dock.

The preview uses a uniform portrait aspect ratio and `FIT_CENTER`; no independent X/Y stretching or artificial dark letterbox fill is added. The verified frame is [edit-mode-full-preview.png](../work/p2_4-device-test/edit-mode-full-preview.png).

Limitation: widgets on a non-current page are not detached or reparented solely for a thumbnail. Therefore a widget is included when it is safely available in the live view; the AppWidgetHost lifecycle remains untouched.

### 3. Add-page card

`rebuildExp2EditPreviews()` appends a dedicated glass card after the persistent pages. It is not counted as a persistent page and has no delete action. The card contains `+` and `Новый рабочий стол`.

The click path uses the existing transient final workspace child, selects it through `settleWorkspace()`, exits Edit Mode, and displays:

> Разместите элемент: экран сохранится после подтверждённого Drop

The card itself does not write `home_workspace`; persistence remains at the existing confirmed Drop/Commit boundary. ADB verification produced [edit-add-card-retest3.png](../work/p2_4-device-test/edit-add-card-retest3.png) and [new-page-after-add-retest3.png](../work/p2_4-device-test/new-page-after-add-retest3.png). The second image shows the empty transient page and the confirmation message. No duplicate page was created by the test.

### 4. Smooth Edit Mode gallery

`EditPreviewScrollView` now keeps the existing touch-slop boundary, follows the finger with `scrollBy()` after horizontal ownership is established, and uses `VelocityTracker` plus the existing `HorizontalScrollView.fling()` on release. Snapshot and blur caches are reused; no bitmap or blur generation is performed on every `ACTION_MOVE`.

The return movement was captured in [edit-mode-fling-back.png](../work/p2_4-device-test/edit-mode-fling-back.png).

## Changed files and backups

P2.4 source changes are limited to:

- [LauncherActivity.java](../app/src/main/java/com/nokiaexperience/launcher/LauncherActivity.java) — startup pacing, complete preview, add-page card, and gallery fling.

Backups created before P2.4 edits:

- `app/src/main/java/com/nokiaexperience/launcher/LauncherActivity.java.p2_4-backup-20261112_031218`
- `app/src/main/java/com/nokiaexperience/launcher/StartupView.java.p2_4-backup-20261112_031218`

`StartupView.java` was not changed in P2.4.

## Build and installation

- `:app:testDebugUnitTest --no-daemon` — PASS.
- `assembleDebug --no-daemon` — PASS.
- `git diff --check` — PASS; Git only reported existing LF/CRLF conversion warnings.
- APK installed with `adb install -r` only on Nokia B (`NOKIA_B`) — `Success`.
- Samsung A (`SAMSUNG_A_REFERENCE`) was not modified.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Device evidence

Collected on Nokia B:

- [edit-mode-full-preview.png](../work/p2_4-device-test/edit-mode-full-preview.png)
- [edit-add-page.png](../work/p2_4-device-test/edit-mode-add-page.png)
- [edit-add-card-retest3.png](../work/p2_4-device-test/edit-add-card-retest3.png)
- [new-page-after-add-retest3.png](../work/p2_4-device-test/new-page-after-add-retest3.png)
- [logcat-final-2.txt](../work/p2_4-device-test/logcat-final-2.txt)

ADB logcat scan found no `FATAL EXCEPTION`, `ANR in`, or `am_anr` matches. These are synthetic ADB checks, not physical-finger confirmation.

Physical-finger tests were not performed in this run. In particular, physical confirmation remains open for startup perception, gallery inertia, full-screen page selection, and physical placement/Drop on the new page.

## Data safety

Remote SHA-256 values before and after the P2.4 install/test sequence are identical:

| File | SHA-256 |
|---|---|
| `shared_prefs/nokia_exp_2.xml` | `f960b7ddb0af86233fb3a6fff5eeed48d3f549410b1a62bc0b91f8b579585ad0` |
| `shared_prefs/nokia_experience.xml` | `134cb873820a9294e66e5c1d1b438e5bca1f6bb09112661fcd4175a9ebcd6fdd` |

The add-page test intentionally stopped before placing an item, so no persistent page commit was expected. No JSON format or SharedPreferences schema was changed.

## Remaining limitations

1. Physical finger verification is still required for the final acceptance scenarios.
2. Safe previewing of widgets on non-current pages is intentionally conservative; live widget views are not detached or reparented.
3. Exact startup perception and animation completion should be confirmed with a real cold-start observation or screen recording.
4. The existing working-tree changes from earlier P1–P2.3 work remain present and were not reset or overwritten.
