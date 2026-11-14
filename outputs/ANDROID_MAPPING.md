# Nokia Experience Android mapping and implementation plan

## Product boundary

Nokia Experience is a native Android Home launcher that recreates the S60/N95 visual language and navigation model. The RM-320 firmware is a forensic/reference source only. It is never a runtime dependency, APK input, emulator embedded in the app, or a source of executable Symbian code.

## Component mapping

| Original N95 component | Firmware evidence | Native Android implementation | Android integration/API | Compatibility note for Android 8 |
| --- | --- | --- | --- | --- |
| Active Standby / Home | `Idle.exe`, `Idle_aif.mif`, `ActiveIdleSettings.r*` | `NokiaHomeScene` custom View/Canvas scene and launcher state | Home activity; preferences; wallpaper source | Only the launcher surface is Nokia-styled; third-party apps remain Android. |
| N95 Status Pane | `avkon2.mif`, `callstatus.mif`, skin packages | `NokiaStatusBar` custom renderer | Battery broadcast, `BatteryManager`, `ConnectivityManager`, `TelephonyManager`, `BluetoothAdapter`, `AudioManager` | Android system bars cannot be globally replaced; sensitive fields need permissions or an unavailable state. |
| Main Menu | `menu2.exe`, `Menu.mif`, `appshelldata.xml`, AppShell Grid/List resources | `NokiaMenuScene` with S60 grid/list renderer and fixed focus model | `PackageManager`, intent resolution, preferences | Do not use Material app drawer/grid appearance. |
| N95 folders | App-shell XML Music/Tools/Applications tree | `NokiaFolderScene` backed by default tree plus user ordering | Local persistent storage / `SharedPreferences` initially | Android launch targets may disappear; retain placeholder/repair flow. |
| Application icon family | `*_aif.mif`, `avkon2.mif`, `.m3g`, skins | `NokiaIconRepository` + extracted/recreated `drawable-nodpi` assets | App package metadata only for dynamic third-party entries | Raw MIF/MBM/M3G cannot be loaded by Android. |
| Softkeys | `EnhancedSoftkeys.dll`, `enhancedsoftkeysbitmaps.mbm` | `NokiaSoftKeysView` with left/centre/right command state | Touch listeners, `KeyEvent` | Android Back/navigation controls remain separate OS concerns. |
| D-pad/OK/Menu navigation | S60 menu/idle model | `NokiaNavigationController` and explicit focus graph | `KEYCODE_DPAD_*`, `DPAD_CENTER`, Enter, Back, Menu if sent | Galaxy A5 is touch-first; all controls need touch equivalents. |
| Phone | Phone app app-shell entry | `NokiaAction` → system dialer | `Intent.ACTION_DIAL` | Avoid direct-call permission as a launcher default. |
| Contacts | Phonebook app entry | `NokiaAction` → Contacts | `ContactsContract` view/insert intents | Resolver/fallback required; no vendor package hard-coding. |
| Messaging | Mce/SMS entries and images | `NokiaAction` → installed/default SMS app | PackageManager-resolved messaging intent | A launcher cannot assume one SMS package or set the default SMS role. |
| Camera | CamMojave resources/app | `NokiaAction` → installed camera app | `MediaStore.ACTION_IMAGE_CAPTURE` or resolved camera launch intent | Camera result/capability differs by vendor app; do not imitate camera internals. |
| Gallery/Media gallery | `MediaGallery2`, `MediaPlayer` resources | `NokiaAction` → Gallery/Photos/media resolver | `ACTION_VIEW` with media URIs / resolver | No fixed Samsung Gallery component. |
| Browser/Maps | Browser, Maps entries | Resolved web/map intent | `ACTION_VIEW` with `https`/geo data when action is explicit | Online content must be optional; core launcher stays offline. |
| File Manager | FileManager resources and strings | Custom `NokiaFileManagerScene` | `READ_EXTERNAL_STORAGE`, `ACTION_OPEN_DOCUMENT`/Storage Access Framework where appropriate | No Android equivalent of Symbian C:/E:; never expose app-private/system storage as fake drives. |
| Profiles | ProfileApp/psln resources | Custom `NokiaProfilesScene` limited to available controls | `AudioManager`; notification-policy settings intent where user grants access | Android 8 has no Symbian profile system; Do Not Disturb access is special user-granted policy access. |
| Settings tree | GSApp/Settings resources | Nokia-styled settings index with intent hand-offs | `Settings.ACTION_SETTINGS`, `ACTION_WIFI_SETTINGS`, `ACTION_BLUETOOTH_SETTINGS`, `ACTION_SOUND_SETTINGS`, `ACTION_DISPLAY_SETTINGS`, `ACTION_DATE_SETTINGS` | Individual settings activities may vary by Samsung build; resolve/check each intent. |
| Notifications | `btnotif`, `Ids*Icons`, Mce icon resources | `NokiaNotificationSummary` | `NotificationListenerService` after explicit user enablement | No runtime notification permission on Android 8, but listener access is privileged special access. Do not imply full access without it. |
| Wallpaper | Active-idle wallpaper resources | Classic background chooser; optional actual system wallpaper action | `WallpaperManager` only if user asks to set system wallpaper | Prefer internal launcher background to minimise permissions. |
| Startup experience | Startup MBM/MIF/RSC | Short optional first-launch/launcher-entry overlay | Activity lifecycle, `SoundPool`/`MediaPlayer`, audio focus | Must never intercept Android boot, delay Home, or replace boot animation. |
| Sounds | AAC/WAV/MIDI tone library | `NokiaSoundManager` | `AudioManager`, audio focus, `SoundPool`/`MediaPlayer` | Respect ringer/silent state; rights review required for source sounds. |
| S60 transition effects | `ABSOLUTEFADING`, skin assets | `NokiaTransitionEngine` | `ValueAnimator`, custom Canvas/View drawing | Keep allocation-free and short; no Compose requirement. |

## Proposed architecture

Use Kotlin and conventional Android Views/custom Canvas rendering. This is the safest fit for an Android 8 target and for a pixel-addressed 240×320 legacy design. Compose is not needed for Classic mode.

```text
NokiaLauncherActivity  (ACTION_MAIN + CATEGORY_HOME + CATEGORY_DEFAULT)
└─ NokiaShellView
   ├─ S60CanvasMapper             # 240×320 canonical geometry → device pixels
   ├─ NokiaStatusBar
   ├─ NokiaSceneHost
   │  ├─ NokiaHomeScene
   │  ├─ NokiaMenuScene
   │  ├─ NokiaFolderScene
   │  ├─ NokiaProfilesScene
   │  ├─ NokiaFileManagerScene
   │  └─ NokiaLauncherSettingsScene
   └─ NokiaSoftKeysView

presentation/
  navigation/   focus graph, back stack, key/touch mapping, command reducer
  renderer/     icon, text, pane, selection and animation renderers
domain/
  menu/         factory app-shell tree, folders, item availability, commands
  device/       immutable UI state: battery, network, profile, notification summary
platform/
  intents/      AndroidIntentNavigator and target resolver
  status/       battery/network/telephony/bluetooth/audio adapters
  storage/      user-visible Android storage adapter
  notifications/ optional notification-listener adapter
data/
  preferences/  theme, sound, mode, custom shortcut/order persistence
assets/
  source-map/   documented extracted asset ID → Android drawable mapping
```

Important rules:

- Keep Android platform calls behind adapters; the custom renderer receives a small immutable state model.
- Model softkeys as commands owned by the current scene/focus, never as hard-coded footer text.
- Keep a default app-shell tree copied from `appshelldata.xml`, then overlay user changes without mutating source assets.
- Maintain source asset provenance in the asset map; no anonymous downloaded Nokia-style icons.
- Decode bitmaps lazily, cache at display size, recycle lifecycle-bound images and avoid background polling.

## Android API and permission inventory

| Need | API/mechanism | Permission / user decision | Fallback |
| --- | --- | --- | --- |
| Register as launcher | Manifest `ACTION_MAIN`, `CATEGORY_HOME`, `CATEGORY_DEFAULT` | User chooses Nokia Experience as Home | App runs as ordinary activity until selected. |
| Enumerate/launch apps | `PackageManager`, launcher-category intents, `startActivity` | No special permission for normal visible activities | Hide/placeholder unresolved item. |
| Battery UI | Sticky `ACTION_BATTERY_CHANGED`, `BatteryManager` | None | Always available while launcher visible. |
| Connection state | `ConnectivityManager`, `NetworkCapabilities` | `ACCESS_NETWORK_STATE` | Connected/disconnected-only display. |
| Wi-Fi detail | `WifiManager` | `ACCESS_WIFI_STATE`; location may be required for some identifiers | Omit SSID/strength detail if unavailable. |
| Telephony signal | `TelephonyManager`, `PhoneStateListener` | Usually `READ_PHONE_STATE`; user grant may be denied | Service/signal unknown glyph; never fake data. |
| Bluetooth state | `BluetoothAdapter` | `BLUETOOTH` on Android 8 | Hide detail if adapter unavailable. |
| Ringer/profile state | `AudioManager` | None for reading basic state | Render launcher-only profile. |
| Do Not Disturb/profile actions | `NotificationManager` policy access | User enables notification-policy access | Deep-link to Settings; do not toggle without access. |
| Notification summary | `NotificationListenerService` | User manually enables Notification Access | Show app-local/phone-state items only. |
| File browser | `MediaStore`, `ACTION_OPEN_DOCUMENT`, `DocumentFile`; classic external storage APIs if justified | `READ_EXTERNAL_STORAGE` runtime grant | Browse only picked documents or show permission help. |
| Wallpaper | `WallpaperManager` | `SET_WALLPAPER` only when applying actual system wallpaper | Launcher-local background remains functional. |
| Sounds | `AudioManager`, `AudioFocusRequest` (API 26), `SoundPool`/`MediaPlayer` | No permission; user sound setting | Silent UI. |
| Settings hand-offs | `android.provider.Settings` actions | None, except special access screens | Resolve action and show Nokia-style explanation if missing. |
| App changes | package-added/removed broadcasts or lifecycle refresh | No permanent process required | Refresh menu when Launcher resumes. |

Minimum SDK should be selected only after implementation support is confirmed; **API 23** is a sensible initial floor for a view-based implementation while Galaxy A5 Android 8 is **API 26**. Compile/target SDK must be chosen from the then-supported Android toolchain, without using APIs that are unavailable at runtime on API 26.

## Android 8 and Galaxy A5 risks

| Risk | Why it matters | Mitigation |
| --- | --- | --- |
| 3:4 N95 versus 9:16 phone | Full-screen stretching destroys the N95 proportions. | Uniform 240×320 mapper; use deliberate themed surplus area/letterbox strategy. |
| Samsung navigation/system UI | A custom status/softkey bar can visually conflict with Android's navigation area. | Treat Nokia panes as content; test immersive/non-immersive options on the actual device. |
| Notification visibility | Android does not hand a launcher all notifications by default. | Optional notification listener with explicit user enablement; graceful limited mode. |
| Telephony and Wi-Fi privacy | Signal/SSID details can be permission- and OEM-dependent. | Small capability-based state model and unknown-state assets. |
| Default app diversity | Dialer, SMS, Gallery, Camera, browser and maps differ by installed apps. | Resolve generic intents dynamically; never hard-code Samsung package names. |
| Storage model | Symbian-drive metaphors do not map to Android private/system folders. | Show user-visible storage roots/SAF documents; do not promise C:/E: access. |
| Background execution limits | A launcher should not run a permanent heavy observer service. | Register UI observers while visible; refresh on resume; NotificationListener only if opted in. |
| Audio behaviour | Auto-playing a “boot” sound on every Home press is intrusive and may ignore silent mode. | Optional, one-shot per explicit launch/session with audio focus and ringer checks. |
| Bitmap memory/performance | Legacy images can be scaled many times on 1,080p display. | Decode scaled/lazy, `drawable-nodpi` source assets, LRU cache, no animated giant bitmaps. |
| Rotations and physical keys | RM-320 has portrait/landscape softkey placement; Galaxy A5 input is touch-first. | Build one focus/command model fed by both touch and `KeyEvent`; test portrait/landscape. |

## Step-by-step development plan — do not start until approval

1. **Close the evidence gap.** In a copy of the dump, use trusted offline MIF/MBM/SKN/RSC decoders and an RM-320 emulator to capture the chosen active skin, exact status/softkey bands, icon IDs, grid geometry, font metrics and transition timing. Update the asset catalog with results.
2. **Create the empty native launcher foundation.** Only after approval: Kotlin/Gradle project, Home intent registration, custom View shell, 240×320 mapper, no implementation of Symbian binaries.
3. **Implement navigation and softkey state machine.** Keyboard/touch focus, Back behaviour, command ownership and a testable scene stack before styling deep screens.
4. **Build Classic Home and status renderer.** Theme tokens, wallpaper, time, battery and capability-safe state indicators. Validate 3:4 mapping on Galaxy A5.
5. **Build Main Menu and folders from the XML hierarchy.** Grid/list renderer, selected state, default N95 order, Android target resolver and unavailable-item treatment.
6. **Convert/recreate approved core assets.** Extract only documented MIF/MBM/SKN assets; add provenance and visual comparison captures. Do not bulk copy unidentified firmware art.
7. **Add Android integrations.** Generic intents for Phone, Contacts, Camera, Messaging, Gallery and Settings; then optional File Manager and Profiles with clear scope limits.
8. **Add optional notification summary, sounds and entry animation.** Every privileged feature remains opt-in and works in a reduced mode without permission.
9. **Performance and compatibility pass.** Cold/warm Home, menu/folder open, touch/D-pad, rotation, Android Back, unavailable apps, memory pressure, silent mode and actual Galaxy A5 Android 8 tests.
10. **Document and release only after evidence and rights checks.** Update all four analysis documents, package only approved assets, and confirm the launcher neither needs root nor modifies bootloader/system firmware.
