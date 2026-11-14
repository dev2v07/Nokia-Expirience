# Nokia Experience UI specification based on RM-320 evidence

## Status of this specification

This is the implementation target for the future native Android launcher, not Symbian code. It separates verified source facts from decisions that must wait for MIF/MBM/SKN decoding or a rendered RM-320 reference. No modern Material component should substitute for a classic S60 interaction merely because it is convenient.

## Canonical canvas and scaling

### Verified reference modes

| Parameter | Value | Evidence |
| --- | --- | --- |
| Primary canvas | **240 × 320 px**, portrait | `system/data/Wsini.ini` |
| Aspect ratio | **3:4** | Derived directly from the configured canvas |
| Landscape canvas | **320 × 240 px** | `Wsini.ini` |
| Landscape softkey side | right at rotation 0; left at rotation 180 | `Wsini.ini` |
| Window mode | `COLOR16MU` | `Wsini.ini` |
| Transition capability | absolute fading declared | `Wsini.ini` |

### Android rendering rule

All classic UI layout is authored in the 240 × 320 coordinate space first. A `S60CanvasMapper` calculates a uniform scale from the available width and never independently stretches X and Y. On a 1,080 px-wide Galaxy A5 2017 display, the exact width scale is 4.5 and the canonical composition is 1,080 × 1,440 px; the remaining vertical space must be handled deliberately as a themed extension/background, never by deforming the original grid.

The implementation must tolerate the Android navigation area and Samsung system behaviour. The custom Nokia status bar and softkeys are launcher content only; they do not replace Android's system bars in other applications.

## Visual authority and colour policy

The visual authority is the active RM-320 skin, not generic Internet N95 icon packs. The five skin families under `resource/skins` and their `.skn` descriptors are the source of truth for panels, highlights, chrome and colours. The dump confirms the source packages but does not expose their colour records as readable hex values.

Therefore:

- Do not invent a fixed blue/grey palette and call it extracted.
- Create named theme tokens — `canvas`, `statusPane`, `softkeyPane`, `listBackground`, `highlightFill`, `primaryText`, `secondaryText`, `separator`, `disabledText` — then populate them only after a skin decoder/rendered reference yields exact values.
- Keep texture/gradient/highlight assets as source-derived drawables where possible; use programmatic shapes only for missing states.
- `private/10281CFB/bgimages/*` belongs to the multimedia menu. It is an optional specialised background source, not evidence that it is the normal S60 home wallpaper.

## Home Screen — Active Standby

### Evidence and composition

The home screen corresponds to S60 **Active Standby**: `Idle.exe`, `Idle_reg.rsc`, `Idle_aif.mif`, `Idle.r01`–`.r13`, `ActiveIdleSettings.r*`, `SoActiveIdleRes.r*`, and active-idle plug-ins are present. The launcher home must preserve this model: a persistent wallpaper/status layer with context information and defined shortcut/plug-in zones, rather than a free-form Android widget page.

| Zone | Behaviour requirement | Evidence/status |
| --- | --- | --- |
| Wallpaper/canvas | One continuous themed background beneath the S60 content. User may select a compatible image; classic mode defaults to a validated N95-derived choice. | Active-idle wallpaper plug-in resources found; exact default wallpaper pending theme decode. |
| Status pane | Show time, cellular state, battery/charging, Bluetooth/Wi-Fi/notification/profile states using N95-style glyphs. | Shared Avkon/call-status/Bluetooth/WLAN image sets found. Exact glyph IDs pending decode. |
| Active-standby content | Calendar/profile/notification/shortcut rows are controlled by the home state, not arbitrary resizable widgets. | Active-idle plugin/settings resources found. Exact factory arrangement needs emulator/render confirmation. |
| Focus | A single item or region is active; D-pad changes focus predictably. Touch gives that item focus before activation. | Required S60 navigation model. |
| Softkey pane | Always rendered as the bottom command surface of the Nokia canvas; labels/actions come from the current home state. | Enhanced Softkeys resource family found. |

### Sizes

The only numerically verified home layout size is the **240 × 320** primary canvas. The dump does not expose a trustworthy standalone rectangle table for the status pane, active-standby rows or softkey height. Pixel heights/offsets must be measured from a decoded `NSeries07_*` skin plus an EKA2L1 reference screen before being frozen. Until then, the Android layout uses symbolic bands and avoids publishing invented “exact” pixel values.

## Nokia Status Bar

`NokiaStatusBar` is a custom view rendered within the launcher canvas. It is not a wrapper around Android's status bar.

| N95 semantic | Android source | Display rule |
| --- | --- | --- |
| Time | `java.time` / system clock | Update at minute boundary; retain N95 text placement/font rules once measured. |
| Battery and charging | `ACTION_BATTERY_CHANGED`, `BatteryManager` | Render an extracted/recreated N95 battery glyph and discrete/continuous fill according to the validated reference. |
| Cellular service/signal | `TelephonyManager`/`PhoneStateListener` where permission/state permit | Do not fabricate carrier or signal values when Android restricts access; use an unobtrusive unavailable state. |
| Wi-Fi | `ConnectivityManager`; optional `WifiManager` detail | Show only connected/available semantics permitted by Android and permission state. |
| Bluetooth | `BluetoothAdapter` state | Show adapter/state only; connection detail needs further capability/permission review. |
| Silent/profile | `AudioManager` and launcher profile state | Mirror Android ringer mode where controllable; otherwise state the launcher profile separately. |
| Notification summary | User-enabled `NotificationListenerService` | Show counts/categories, not a cloned Android shade. No listener permission means no third-party notification count. |

The status-bar art candidates are `resource/apps/avkon2.mif`, `callstatus.mif/.mbm`, `BtUi.mif`, `btnotif.mif`, WLAN plug-in imagery and the skin families. The final icon table must pair every icon ID with a captured reference before export.

## Main Menu and folders

### Structure

Use `appshelldata.xml` as the factory menu order, with Android availability filtering and explicit retained placeholders when an N95 function has no Android counterpart. The initial hierarchy is:

```text
Root
├─ Phone, Contacts, Messaging, Maps
├─ Music
│  ├─ Music Player, Music Shop, Podcasting, Visual Radio
├─ Operator menu, Media Gallery, Video Center, Mobile Search, Asia Maps, Browser, Get N-Gage
├─ Tools
│  ├─ Bluetooth, WLAN Wizard, Sync, Profiles, Settings, Logs, File manager
│  ├─ Connectivity
│  │  └─ Infrared, USB, Connection manager, Home Connect, Fax modem, GPS Data,
│  │     Landmarks, Wireless keyboard, Push-to-talk, Internet call
│  └─ Utilities
│     └─ Speed dial, 3D Audio, Voice commands, Voice mailbox, Device manager,
│        Settings Wizard, Memory card, Switch, Welcome, Help, About
├─ SIM ATK
├─ Applications
│  ├─ Clock, Calculator, Calendar, Application manager, Camera
│  ├─ Media: Voice recorder, RealPlayer, Flash, Instant Messaging, LifeBlog, DRM
│  └─ Office: PDF, Converter, ZIP, Barcode reader, Quickoffice, Notepad, Dictionaries
└─ Catalog / Download
```

### Modes and interaction

| Element | Required behaviour |
| --- | --- |
| Grid/list | Offer the firmware-confirmed `AppShell_Grid` and `AppShell_List` presentations. Do not call a Material RecyclerView layout “S60” without custom renderer/chrome. Exact grid cell count awaits reference capture. |
| Selection | One selected item has the authentic high-contrast S60 highlight/focus treatment. Selection is independent from launch. |
| Folder | Open as a new menu level; title, parent relationship, focus restoration and softkeys change together. |
| Sort/move | Implement only supported launcher functions; retain the S60 terms from source strings such as Move to folder, Folder name, Root folder, Options and Back. |
| Scroll | D-pad focus moves through off-screen items predictably; touch scroll does not lose selected-item state. |
| Open/back | Centre/Select/tap opens; Back returns one menu level before returning to Home. |
| Availability | A missing Android target renders a labelled Nokia-style placeholder/disabled item, never a dead crash target. |

`Menu.mif` is the shared folder/icon-image package; `menu_aif.mif` is its application icon. Folder-category `.m3g` assets (`musicf`, `toolsf`, `connectf`, `utilf`, `mediaf`, `off`, `appf`) are reference material. The native Android menu should use 2D exported/recreated assets unless a separate performance study justifies a 3D recreation.

## Softkeys and navigation

### Softkeys

The softkey system has direct evidence in `EnhancedSoftkeys.dll` and `enhancedsoftkeysbitmaps.mbm`; it is a required interaction layer, not a static footer. The command model is three semantic slots:

| Slot | Typical home/menu purpose | Android activation |
| --- | --- | --- |
| Left | Options, Functions, context menu | Left touch zone or configurable key event |
| Centre | Select, Open, Activate | Centre touch zone, `DPAD_CENTER`, Enter |
| Right | Back, Exit, Cancel | Right touch zone and Android Back handling |

Labels must be computed from the focused screen/item. For example, a folder list may expose Options / Open / Back; a dialog may expose Select / Cancel. The raw bitmap package is visual reference only — text must remain accessible Android-rendered text, localisable and stateful.

### Input rules

| Input | S60-compatible mapping |
| --- | --- |
| DPAD up/down/left/right | Move focus by the active layout's deterministic neighbour relation. |
| DPAD centre or Enter | Activate the focused item / centre softkey. |
| Android Back | Right-softkey semantic first; one level up from folder; then normal Home launcher behaviour. |
| Android Menu if delivered by device | Open left-softkey Options. |
| Tap | Focus and activate target; use visible focus transition. |
| Long press | Only context actions that have an S60-equivalent Options/command meaning. |
| Swipe | Scroll only on scrollable lists/grids; no Material overscroll effects. |

## Animation and sound specification

| Event | Target style | Evidence | Implementation constraint |
| --- | --- | --- |
| Home → menu / Back | Short S60-style fade/transition, with focus preserved | `ABSOLUTEFADING` in display config | `ValueAnimator`/Canvas or custom View animation; no Material shared-axis transition. |
| Enter/leave folder | Brief transition plus title/focus change | AppShell Grid/List assets and folder model | Measure timing/direction in emulator before final values. |
| Selection | Instant highlight change, optional very short emphasis | Avkon/skin graphics | No ripple. |
| Multimedia menu | Optional pan/Ken-Burns imagery and menu browse/select/startup sounds | `private/10281CFB/configuration.xml` | Keep separate from normal menu; honour sound setting. |
| Launcher entry | Optional short startup sequence using `Startup.mbm` reference and a licensed sound | Startup resource family | Never delay Android boot or play on every Home press. |

Audio is opt-in, observes `AudioManager` ringer mode and audio focus, and must provide a silent setting. Original sound files and Nokia branding require rights clearance before shipment.

## Typography

Use short labels, small dense UI text and clear selected-state contrast, but do not select a font merely by visual guess. The dump provides Nokia Hindi S60 variants and `Series 60 ZDigi Regular`; it does not expose a clear, general Latin S60 body font as a ready-to-ship file. Final typography requires:

1. decoding/capturing the active skin and a reference UI screen;
2. measuring canonical text pixel height/baseline in QVGA coordinates;
3. choosing a legally shippable Latin fallback if original rights/glyph coverage do not permit use;
4. applying the same metrics through the S60 canvas mapper, not Android Material type scales.

## Explicit non-goals

- No Symbian process, binary, ROM segment, `.sis`, `.exe` or `.dll` is run, ported or packaged.
- No Android Material cards, drawer, ripple, rounded widget treatment or free-form widget grid in Classic mode.
- No attempt to replace Android's bootloader or system boot animation.
- No claim of pixel-exact status/softkey coordinates before the binary skin/image resources have been decoded and screen-captured.
