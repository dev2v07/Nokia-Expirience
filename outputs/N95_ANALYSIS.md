# Nokia N95 8GB RM-320 static firmware analysis

## Scope and method

This report records a read-only static examination of the supplied `N95%208GB%20%28S60v3%20-%20FP1%29.7z`. The original archive and the original files in Downloads were not changed. A working extraction was made outside the deliverables only for inspection; no Symbian executable, DLL, ROM image, SIS package, or script from the dump was run.

The supplied archive contains 8,060 files in 321 directories, totalling 158,538,557 bytes unpacked. It has two material payloads:

| Payload | Size | Finding |
| --- | ---: | --- |
| `data/drives/z/rm-320/` | 8,059 files | Extracted Symbian Z: drive: the primary, directly inspectable source for UI resources, applications, localisation and defaults. |
| `data/roms/rm-320/SYM.rom` | 60,600,320 bytes | Raw ARM-oriented Symbian ROM image. Its header is binary/ARM data and it contains Nokia/Symbian strings, but it is not an Android input and was not treated as executable content. |

The analysis uses three confidence labels:

- **Direct** — path, size, format signature or readable content was inspected.
- **Strong inference** — purpose follows from a component's file family and associated executable/resource names.
- **Pending decode** — the source exists but exact images, dimensions or colour records need a Symbian resource decoder or a rendered S60 session.

## Firmware layout

The extracted `Z:\rm-320` root is a product ROM filesystem, not an ordinary app directory.

| Directory | Files | Bytes | Role for this project |
| --- | ---: | ---: | --- |
| `cities` | 1 | 373,454 | Location/time-zone data; not launcher artwork. |
| `data` | 42 | 2,025,535 | User-facing tone library, including Nokia Tune, alarms, message alerts and MIDI tones. |
| `epoc32` | 1 | 63 | Build/import metadata. |
| `private` | 945 | 5,183,071 | UID-owned application data, app-shell menu definition, themes, multimedia-menu assets, configuration and registrations. |
| `resource` | 3,936 | 24,970,279 | Main UI resource root: MIF/MBM icons, RSC/localised strings, fonts, skins, M3G menu models and application artwork. |
| `space` | 4 | 48,985 | Space-management support data; not UI. |
| `sys/bin` | 2,848 | 61,952,214 | Symbian ARM executables/DLLs. Useful as component identifiers only; never run or port. |
| `system` | 281 | 3,382,838 | Legacy/support assets, display configuration, media templates, camera sounds and plug-in data. |

There are no usable Android packages in this layout. The 338 `.exe` files and 2,413 `.dll` files are Symbian binaries. The absence of traditional `.app` packages is expected for this Symbian generation: application registration is primarily represented by `private/10003a3f/apps/*_reg.rsc`, executable code in `sys/bin`, and resources in `resource/apps`.

## Screen and orientation — direct evidence

`system/data/Wsini.ini` is the authoritative screen configuration found in the dump:

| Mode | Logical pixels | Rotation in ROM config | Softkey position | Style |
| --- | ---: | ---: | --- | --- |
| Portrait | 240 × 320 | 270 | bottom in the normal portrait composition | `QVGA2` |
| Landscape 1 | 320 × 240 | 0 | right | `QVGA2` |
| Landscape 2 | 320 × 240 | 180 | left | `QVGA2` |

The file also declares `WINDOWMODE COLOR16MU`, `ABSOLUTEFADING`, and `NOREDRAWSTORING`. This establishes a 3:4 QVGA reference canvas and confirms that the historical UI was deliberately designed for portrait and both landscape directions. It does **not** by itself give every widget's bounding rectangle.

## UI component map

### Active standby / Home Screen

Direct component chain:

| Layer | Evidence |
| --- | --- |
| Active standby executable | `sys/bin/Idle.exe` (57,064 bytes) |
| App registration | `private/10003a3f/apps/Idle_reg.rsc` |
| Application icon | `resource/apps/Idle_aif.mif` (3,286 bytes) |
| Localised text | `resource/apps/Idle.r01`, `.r02`, `.r03`, `.r04`, `.r05`, `.r13` |
| Active-idle options/plugins | `resource/ActiveIdleSettings.r*`, `resource/apps/SoActiveIdleRes.r*`, `resource/GSActiveIdlePluginRsc.r*`, `sys/bin/SoActiveIdle.dll`, `sys/bin/GSActiveIdlePlugin.dll` |
| Wallpaper integration | `resource/aknswallpaperplugin.r*` |

`Idle.r01` contains readable references to Standby and Standby mode when decoded as a Symbian resource stream. This is direct confirmation that the home screen is the S60 active-standby subsystem, not an Android-style app drawer.

### Main Menu and folders

The most valuable discovery is `private/101F4CD2/Content/appshelldata.xml` (7,268 bytes). It is a readable S60 3.1 application-shell definition, including root order, folders, application UIDs and menu icon bindings. It specifies `Z:\resource\apps\Menu.mif` for folder imagery and connects each folder to an `.m3g` asset.

Root-level sequence in the firmware definition:

1. Phone, Contacts, Messaging, Maps.
2. **Music** folder: Music Player, Music Shop, Podcasting, Visual Radio.
3. Operator menu, Media Gallery, Video Center, Mobile Search, Asia Maps, Browser, Get N-Gage.
4. **Tools** folder: Bluetooth, WLAN Wizard, Sync, Profiles, Settings, Logs, File manager; then **Connectivity** and **Utilities** subfolders; then SIM ATK.
5. **Applications** folder: Clock, Calculator, Calendar, Application manager, Camera; then **Media** and **Office** subfolders.
6. Catalog / Download.

The two app-shell theme groups in `private/10207254/themes/...` are explicitly named `AppShell_Grid` and `AppShell_List`, with seven `o0000`–`o0005`/`o0013` binary resources each. This is direct evidence for both grid and list variants. Exact row/column geometry is pending rendering/decode rather than assumed from a modern launcher.

Related menu components:

| Purpose | Evidence |
| --- | --- |
| Menu executable | `sys/bin/menu2.exe` |
| App registration | `private/10003a3f/apps/menu2_reg.rsc` |
| Main menu imagery | `resource/apps/Menu.mif` (36,006 bytes) and `resource/apps/menu_aif.mif` |
| Menu strings | `resource/apps/menu2.r01`–`.r13` |
| 3D/icon menu payloads | 80 `.m3g` files in `resource/m3gicons/`, including `musicf.m3g`, `toolsf.m3g`, `connectf.m3g`, `utilf.m3g`, `mediaf.m3g`, `off.m3g`, `appf.m3g` |

The English-localised menu resource exposes real action vocabulary such as **Delete**, **Unable to delete folder**, **Move to folder**, **File already saved in selected folder**, **Folder name**, **Root folder**, **Mass memory**, **Options**, **Back**, **Open**, **Cancel** and **Move**. This confirms that folders are functional menu objects with move/delete constraints, not decorative visual groups.

### Shared S60 chrome: status area, softkeys, dialogs and selection

The shared UI kit is Avkon:

| Component | Evidence | Relevance |
| --- | --- | --- |
| Avkon code | `sys/bin/avkon.dll` (680,564 bytes) | S60 common control/look-and-feel implementation; identification only. |
| Avkon graphics | `resource/apps/avkon2.mif` (406,512 bytes), `avkon2.mbm` (95,882 bytes) | Highest-priority source for common icons, panes, highlights and chrome. Pending image extraction. |
| Avkon strings | `resource/Avkon.r01`–`.r13` | Contains common commands such as Edit, Options, Copy, Paste and input-mode labels. |
| Softkey implementation | `sys/bin/EnhancedSoftkeys.dll` plus `resource/enhancedsoftkeysbitmaps.mbm` (2,891 bytes) | Direct source for the softkey subsystem/bitmap treatment. |
| Call/status imagery | `resource/apps/callstatus.mif` (67,699 bytes), `callstatus.mbm` | Strong candidate set for call and indicator visuals. |
| Bluetooth/WLAN/notification imagery | `BtUi.mif`, `btnotif.mif`, `WlanSnifferPlugin.mif`, `Ids*Icons.mif`, `MceExtraIcons.mif` | Candidate source sets for status/notification/icon vocabulary. |
| Dialog/list/toolbar imagery | `commondialogs.mif`, `toolbaricons.mif`, `enhancedsoftkeysbitmaps.mbm` | Source for secondary chrome; pending decode. |

The exact bitmap boundaries and individual indicator names cannot be stated responsibly before decoding `avkon2.mif`, `callstatus.mif` and the skin assets. The filenames and companion UI resources establish their function; a MIF/MBM decoder is needed to associate image IDs with pixel sizes.

### Themes and graphics

Five NSeries skin families are present in both runtime resources and importable `.skn` descriptors:

| Theme UID directory | Runtime files | Descriptor | Combined MIF/MBM bytes |
| --- | --- | --- | ---: |
| `101f84b9` | `Series60Skin.mif`, `.mbm` | `private/10207114/import/101f84b9/Series60Skin.skn` | 731,557 |
| `2000a62a` | `NSeries07_01.mif`, `.mbm` | matching `NSeries07_01.skn` | 783,943 |
| `2000a62b` | `NSeries07_02.mif`, `.mbm` | matching `NSeries07_02.skn` | 423,972 |
| `2000a62d` | `NSeries07_04.mif`, `.mbm` | matching `NSeries07_04.skn` | 303,180 |
| `2000a62e` | `NSeries07_05.mif`, `.mbm` | matching `NSeries07_05.skn` | 785,775 |

These are the primary source for N95/S60 surfaces, highlight states, colour definitions, panel art and standard control appearance. `.skn` is a Symbian skin-description format, not a directly usable Android drawable. The matching MIF/MBM assets should be decoded into an inspected, documented source-asset set before any Android conversion decision.

Other directly viewable raster resources are not the core idle skin, but some are useful references:

| Path | Measured size | Finding |
| --- | ---: | --- |
| `private/10281CFB/bgimages/graphics1.jpg` | 480 × 640 | Multimedia-menu background option. |
| `private/10281CFB/bgimages/default/image_1.jpg`–`image_5.jpg` | 668–720 × 460–480 | Default multimedia-menu image set. |
| `private/10281CFB/bgimages/graphics2.jpg` | 480 × 640 | Multimedia-menu background option. |
| `private/10281CFB/brushed_metal.png` | 128 × 128 | Texture element. |
| `private/10281CFB/selector.png` | 64 × 64 | Multimedia-menu selector element. |
| `private/102072C3/background_large.jpg` | 352 × 352 | Music-player/visualisation-associated background candidate. |
| `private/20007803/ncdskins/default/folder.png` | 42 × 42 | Download/catalog folder icon, not necessarily the main S60 menu folder. |

`private/10281CFB/configuration.xml` identifies those assets as a **multimedia menu** configuration: sounds and Ken Burns effects are enabled, and image set 1 is selected. Its `multimediamenustartup.wav`, `multimediamenubrowse.wav` and `multimediamenuselect.wav` are therefore valid evidence for that specialised menu, not proof of the normal app menu's sound design.

### Startup and shutdown

| Resource | Evidence-based conclusion |
| --- | --- |
| `sys/bin/startup.exe` and `private/10003a3f/apps/StartUp_reg.rsc` | Firmware startup application. |
| `resource/apps/Startup.mbm` (743,739 bytes), `Startup.mif`, `STARTUP.r01`–`.r13` | Highest-priority startup visual/text resource family; `.r01` includes startup failure/offline-mode messages. |
| `resource/apps/SysApShutdownAnimation.rsc` | A shutdown-animation resource exists. |

The dump contains startup material but does not authorise replacing Android's boot animation, bootloader, or system UI. It can support an optional, short launcher-entry sequence after an Android home activity becomes visible.

### Fonts, sounds, text and localisation

**Fonts — direct:**

| File | Size | Embedded family metadata | Assessment |
| --- | ---: | --- | --- |
| `resource/Fonts/nohindisnr60.ttf` | 438,940 | Nokia Hindi S60 Regular | Real TrueType font; script-specific, not evidence of the Latin body font. |
| `resource/Fonts/nohindissb60.ttf` | 442,840 | Nokia Hindi S60 regular/small-bold family metadata | Real TrueType font; needs glyph-coverage verification. |
| `resource/Fonts/nohinditsb60.ttf` | 455,636 | Nokia Hindi TitleSmBd S60 | Real TrueType font; title style, script-specific. |
| `resource/Fonts/S60ZDIGI.ttf` | 34,340 | Series 60 ZDigi Regular | Real TrueType digital display face; potentially useful for an optional clock/reference specimen, not automatically the general UI face. |

The usual Latin S60 UI font is not exposed here as a straightforward, named Latin `.ttf`. Do not substitute the Hindi or digital file for the full UI without a glyph and visual validation. Font copyright/redistribution rights must be confirmed separately before shipping any original font.

**Sounds — direct:** 42 selectable tones under `data/Sounds/Digital`, including `Nokia Tune.aac` (28,561 bytes), alarms, message tones, Email alert, Beep, Chat alert and 16 MIDI files. Additional useful files include `system/Sounds/Digital/CamMojave*.wav`, `system/Sounds/Wakeup tone.aac`, and the multimedia-menu WAV trio above. AAC, WAV and MIDI can be decoded/played on Android, but use in a distributable launcher requires rights clearance and a deliberate audio-focus/silent-mode policy.

**Text/localisation — direct:** 377 `.rsc` files and six repeated localisation extensions `.r01`, `.r02`, `.r03`, `.r04`, `.r05`, `.r13` exist. `resource/apps` alone contains 190 files for each extension. They are Symbian compiled resources, not plain strings, but inspection reveals English command text in `.r01` samples and path/icon references. They are excellent terminology references; extracting all strings accurately requires a Symbian resource parser.

## File format findings

| Format | Quantity | What it is | Android readiness |
| --- | ---: | --- | --- |
| `.mif` | 333 | Symbian Multi-Image File; header observed as `42 23 23 34`. Holds application, skin and common UI images. | Decode/export required. Do not place raw in `res/`. |
| `.mbm` | 62 | Symbian multi-bitmap package; observed `0x10000037/0x10000042` style UID header. | Decode/export required. |
| `.skn` | 5 | Symbian skin descriptions bound to matching MIF/MBM graphics. | Parse as theme metadata; not directly drawable. |
| `.o000*` | 14 | AppShell Grid/List theme payloads under theme UID hierarchy, with MBM-like UID header. | Exact format/pixels pending decoder. |
| `.m3g` | 80 | JSR-184 Mobile 3D Graphics assets; signature `AB 4A 53 52 31 38 34`. | Treat as reference/optional recreation input, not Android drawable. |
| `.rsc`, `.r01`… | 3,623 resource/localisation items | Symbian compiled resource streams and language variants. | Parse for text/IDs; recreate UI in Kotlin/Views. |
| `.ttf` | 4 | TrueType fonts. | Technically Android-readable; licence and glyph review first. |
| `.aac`, `.wav`, `.mid` | 61 | Tone, alert, camera and multimedia sounds. | Technically usable after conversion/rights review. |
| `.jpg`, `.png`, `.gif`, `.svg` | 77 | Standard raster/vector content, mainly multimedia/catalog/web assets. | Directly inspectable; use only when purpose and rights fit. |
| `.exe`, `.dll`, `.ldd`, `.tsy` | 2,811+ | Symbian binaries/drivers/libraries. | Never run, convert, package or port. Identification only. |
| `SYM.rom` | 1 | Raw ARM-oriented ROM image. | Research source only; requires ROM tooling for deeper correlation. |

## Resources suitable for the future launcher

### Suitable after controlled extraction and review

- App/menu icon candidates in `resource/apps/*_aif.mif`, `Menu.mif`, `avkon2.mif`, `callstatus.mif`, and `resource/m3gicons`.
- Shared chrome candidates in `avkon2.mif/.mbm`, `enhancedsoftkeysbitmaps.mbm`, the five NSeries skin pairs and `.skn` descriptors.
- Startup imagery in `Startup.mbm` and optional launcher-entry sounds in the verified tone library.
- Original menu topology and labels from `appshelldata.xml` plus compiled localisation resources.
- The `S60ZDIGI.ttf` file only after assessing it as an optional limited-use font and confirming rights.

### Must be recreated in native Android

- All interactive home, menu, folder, status, softkey and navigation controls.
- Android's device-state collection, notification policy, focus, back handling, animation timing and touch input.
- Any unextractable MIF/MBM/SKN visual, any image that scales poorly, and every state not present in the ROM.
- A full-screen adaptation for Galaxy A5's 9:16 display while preserving the N95 3:4 canonical composition.
- All Symbian functionality itself. The launcher must call Android components or present its own limited Nokia-style screens; it must not emulate/port the OS.

## Required follow-up tools before pixel-perfect extraction

No additional tool was installed during this analysis. To turn the pending binary assets into a visual asset inventory, use an offline, trusted workflow in a disposable working copy:

1. **Symbian MIF/MBM converter** — for example the licensed Symbian SDK `mifconv`/`bmconv`, or a vetted open-source decoder — to export each image/mask with image IDs and dimensions.
2. **Symbian RSC parser** — to resolve exact strings, resource IDs, layout tables and language mapping for `.r01`–`.r13` and `.rsc`.
3. **Symbian skin decoder** — to bind `.skn` colour/layout definitions to MIF/MBM image IDs.
4. **EKA2L1 or equivalent RM-320-compatible emulator** — to render a real reference screen, confirm the active theme, measure pixel rectangles, and observe transitions. It is for observation only; no ROM code belongs in the Android product.
5. **ROM filesystem parser** only if the Z: extraction proves incomplete or a resource is present solely in `SYM.rom`.

Do not proceed to final asset conversion or redistribute Nokia artwork, fonts, sounds or trademarks until their licence/permission status is resolved.
