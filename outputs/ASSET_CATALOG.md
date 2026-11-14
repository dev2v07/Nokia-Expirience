# RM-320 asset catalog for Nokia Experience

## Catalog rules

Paths below are relative to extracted `data/drives/z/rm-320/`. “Use” means technical suitability, not permission to redistribute. Nokia artwork, tones, fonts, names and trademarks need rights review before inclusion in a public Android build. The catalog groups repeated language/resource variants when every individual file serves the same role; it does not pretend that all 8,059 files are visual assets.

| Original path | Type | Purpose | Resolution/size | Android usage |
| --- | --- | --- | --- | --- |
| `system/data/Wsini.ini` | Text configuration | Display modes and orientation behaviour | 240×320 portrait; 320×240 landscape | Canonical layout and orientation specification; do not package. |
| `private/101F4CD2/Content/appshelldata.xml` | XML | Ordered S60 app-shell root/folder hierarchy with UIDs and image bindings | 7,268 B | Source for default Nokia menu model; transcribe into app data, do not execute. |
| `resource/apps/Menu.mif` | Symbian MIF | Main menu folder/icon imagery referenced by app-shell XML | 36,006 B; element dimensions pending decode | Highest-priority export/recreation source for folder/category visuals. |
| `resource/apps/menu_aif.mif` | Symbian MIF application icon | Main Menu app icon | 1,931 B | Export/recreate as menu entry icon after decoder validation. |
| `private/10207254/themes/.../AppShell_Grid.o0000`–`o0013` | Symbian binary theme payload | Grid presentation theme assets | 7 files, 12,915–13,395 B | Decode/inspect for grid chrome; do not treat raw payload as drawable. |
| `private/10207254/themes/.../AppShell_List.o0000`–`o0013` | Symbian binary theme payload | List presentation theme assets | 7 files, 12,836–13,373 B | Decode/inspect for list chrome. |
| `resource/m3gicons/musicf.m3g` | JSR-184 M3G | Music-folder visual | 7,748 B | Reference/optional 2D recreation, not an Android resource. |
| `resource/m3gicons/toolsf.m3g` | JSR-184 M3G | Tools-folder visual | 6,240 B | Reference/optional 2D recreation. |
| `resource/m3gicons/connectf.m3g` | JSR-184 M3G | Connectivity-folder visual | 3,841 B | Reference/optional 2D recreation. |
| `resource/m3gicons/utilf.m3g` | JSR-184 M3G | Utilities-folder visual | 7,654 B | Reference/optional 2D recreation. |
| `resource/m3gicons/mediaf.m3g` | JSR-184 M3G | Media-folder visual | 6,577 B | Reference/optional 2D recreation. |
| `resource/m3gicons/off.m3g` | JSR-184 M3G | Office-folder visual | 4,197 B | Reference/optional 2D recreation. |
| `resource/m3gicons/appf.m3g` | JSR-184 M3G | Applications-folder visual | 4,374 B | Reference/optional 2D recreation. |
| `resource/m3gicons/<UID>.m3g` | JSR-184 M3G | Per-application 3D icon set: 73 UID-addressed resources were found | 80 M3G total, 2,088–32,706 B | Correlate UIDs with `appshelldata.xml`; export/recreate only after visual review. |
| `resource/apps/*_aif.mif` | Symbian MIF | Application icon resources — 80 AIF image files, including Phone, Phonebook, Calendar, CamMojave, FileManager, ProfileApp, Mce, Logs | 526–77,108 B | Primary app-icon extraction set; store approved exports as `drawable-nodpi`. |
| `resource/apps/avkon2.mif` | Symbian MIF | Shared S60 control, indicator and common icon art | 406,512 B | Decode into named source sprites; use for custom status/chrome only after ID mapping. |
| `resource/apps/avkon2.mbm` | Symbian MBM | Companion common bitmap package | 95,882 B | Decode with masks; same review pipeline as MIF. |
| `resource/Avkon.r01`–`.r13` | Symbian localisation resources | Common UI labels and input/command vocabulary | 25,231–26,069 B each | Terminology reference; parse/relocalise in Android strings, never load raw. |
| `resource/enhancedsoftkeysbitmaps.mbm` | Symbian MBM | Enhanced softkey chrome assets | 2,891 B | Decode as visual reference / potential source; rebuild accessible softkey text in Android. |
| `resource/apps/callstatus.mif` | Symbian MIF | Call-status/indicator imagery candidate set | 67,699 B | High-priority status-bar icon extraction/ID mapping. |
| `resource/apps/callstatus.mbm` | Symbian MBM | Companion call-status bitmaps | 1,895 B | Decode with MIF set. |
| `resource/apps/BtUi.mif`, `btnotif.mif`, `WlanSnifferPlugin.mif` | Symbian MIF | Bluetooth, Bluetooth notifications and WLAN UI icon candidates | 3,844 B; 1,686 B; 15,970 B | Map only approved icons to Android state renderer. |
| `resource/apps/IdsContactsUiImpIcons.mif`, `IdsMessagesUiImpIcons.mif`, `IdsFilesUiImpIcons.mif`, `IdsCalTodoUiImpIcons.mif`, `IdsNotesUiImpIcons.mif` | Symbian MIF | Information/notification icon sets | 1,250–15,527 B | Candidate count/status glyph source. |
| `resource/apps/commondialogs.mif`, `toolbaricons.mif` | Symbian MIF | Common dialog/toolbar chrome | 9,352 B; 3,638 B | Reference for menus/dialogs; decode before inclusion. |
| `resource/skins/101f84b9/Series60Skin.mif/.mbm` | Symbian skin assets | Base Series 60 skin | 659,094 B + 72,463 B | Highest-priority skin decode set; bind with `.skn`. |
| `resource/skins/2000a62a/NSeries07_01.mif/.mbm` | Symbian skin assets | NSeries skin variant 01 | 320,539 B + 463,404 B | Decode; compare against rendered RM-320 active theme. |
| `resource/skins/2000a62b/NSeries07_02.mif/.mbm` | Symbian skin assets | NSeries skin variant 02 | 202,141 B + 221,831 B | Decode; compare. |
| `resource/skins/2000a62d/NSeries07_04.mif/.mbm` | Symbian skin assets | NSeries skin variant 04 | 297,050 B + 6,130 B | Decode; compare. |
| `resource/skins/2000a62e/NSeries07_05.mif/.mbm` | Symbian skin assets | NSeries skin variant 05 | 321,565 B + 464,210 B | Decode; compare. |
| `private/10207114/import/<skin UID>/*.skn` | Symbian SKN descriptors | Theme metadata that binds skin assets | 5 files, 14,523–38,036 B | Parse for colours, image IDs and layout bindings; no direct Android use. |
| `resource/apps/Idle_aif.mif` | Symbian MIF | Active Standby icon | 3,286 B | Icon reference only. |
| `resource/apps/Idle.r01`–`.r13` | Symbian localisation resources | Standby mode labels/configuration | 642–662 B | Extract terminology and validate home state labels. |
| `resource/ActiveIdleSettings.r*`, `resource/apps/SoActiveIdleRes.r*`, `resource/GSActiveIdlePluginRsc.r*` | Symbian localisation resources | Active-standby settings and plug-in labels | 84–686 B per file | Behaviour/label evidence; parse for final spec. |
| `resource/apps/Startup.mbm` | Symbian MBM | Startup visuals / likely animation frames | 743,739 B | Decode, catalogue frames and optionally recreate a launcher-entry sequence. |
| `resource/apps/Startup.mif` | Symbian MIF | Startup companion image | 463 B | Decode/correlate with Startup MBM. |
| `resource/apps/STARTUP.r01`–`.r13` | Symbian localisation resources | Startup/offline/failure messages | 653–700 B each | Reference wording only. |
| `resource/apps/SysApShutdownAnimation.rsc` | Symbian compiled resource | System shutdown animation definition | 59 B | Behaviour evidence; not portable. |
| `data/Sounds/Digital/Nokia Tune.aac` | AAC audio | Original ringtone reference | 28,561 B | Technically playable; rights clearance and opt-in only. |
| `data/Sounds/Digital/Alarm 1.aac`, `Message 1.aac`, `Email alert.aac`, `Chat alert.aac`, `Beep.aac` | AAC audio | Alarm/message/alert feedback candidates | 6,131–50,156 B | Curate only after rights/audio UX decision. |
| `data/Sounds/Digital/*.mid` | Standard MIDI | 16 selectable melodies/alerts | 325–16,149 B | Use only if Android playback/rights policy permits; alternatively reference timing/tone. |
| `system/Sounds/Digital/CamMojave*.wav` | WAV audio | Camera capture/focus/start/stop sounds | 3,564–78,912 B | Camera-intent reference only; do not play on behalf of third-party camera without a user action. |
| `private/10281CFB/multimediamenustartup.wav`, `multimediamenubrowse.wav`, `multimediamenuselect.wav` | WAV audio | Multimedia-menu feedback | 398,824 B; 6,756 B; 21,944 B | Optional isolated multimedia mode; not default main-menu audio. |
| `resource/Fonts/S60ZDIGI.ttf` | TrueType | Series 60 ZDigi Regular display face | 34,340 B | Candidate for optional validated digital clock; check rights/glyphs. |
| `resource/Fonts/nohindisnr60.ttf` | TrueType | Nokia Hindi S60 Regular | 438,940 B | Do not use as Latin UI default; rights/glyph review. |
| `resource/Fonts/nohindissb60.ttf` | TrueType | Nokia Hindi S60 small-bold variant | 442,840 B | Same restriction. |
| `resource/Fonts/nohinditsb60.ttf` | TrueType | Nokia Hindi TitleSmBd S60 | 455,636 B | Same restriction. |
| `private/10281CFB/bgimages/default/image_1.jpg`–`image_5.jpg` | JPEG | Default multimedia-menu image set | 668–720 × 460–480 | Optional multimedia screen imagery only. |
| `private/10281CFB/bgimages/graphics1.jpg`, `graphics2.jpg` | JPEG | Multimedia background options | 480 × 640 each | Optional multimedia background; inspect/crop deliberately. |
| `private/10281CFB/bgimages/graphics3.jpg` | JPEG | Multimedia background option | 689 × 480 | Optional multimedia background. |
| `private/10281CFB/brushed_metal.png` | PNG | Multimedia texture | 128 × 128 | Optional derived texture only if source context is preserved. |
| `private/10281CFB/selector.png` | PNG | Multimedia selector | 64 × 64 | Reference for specialised menu selection state. |
| `private/102072C3/background_large.jpg` | JPEG | Music-player/visualisation background candidate | 352 × 352 | Optional media feature, not home-screen default proof. |
| `private/20007803/ncdskins/default/folder.png` | PNG | Catalog/download folder icon | 42 × 42 | Use only for Catalog-style item, not as generic S60 folder without validation. |
| `private/20007803/ncdskins/default/*.svg` | SVG | Catalog item status indicators | 9 SVG files, 1,612–12,074 B | Technically convertible; specialised catalog use only. |
| `resource/apps/CamMojave.mif/.mbm` | Symbian MIF/MBM | Camera UI and icon set | 119,556 B + 79,318 B | Camera launch visual reference; export approved icons. |
| `resource/apps/FileManager.mif`, `FileManager_aif.mif` | Symbian MIF | File Manager icon/UI imagery | 5,758 B; 1,793 B | Source for custom Android storage screen iconography. |
| `resource/apps/ProfileApp_aif.mif`, `pslnicon.mif` | Symbian MIF | Profiles application imagery | 3,224 B; 10,035 B | Profiles screen icon/reference. |
| `resource/apps/Phone_aif.mif`, `Phonebook_aif.mif`, `Mce_aif.mif`, `Calendar_aif.mif`, `ClockApp_aif.mif` | Symbian MIF | Core app icons | 1,368–4,135 B | Map to Android intents/launcher entries after decode. |
| `resource/apps/Phone.r*`, `Phonebook.r*`, `Mce.r*`, `FileManager.r*`, `profileapp.r*` | Symbian localisation resources | Core app command text and menu semantics | 1,499–14,597 B per language file | Use to preserve terms/action vocabulary; parse then localise normally. |
| `private/101ffa91/cliparts/Cliparts01.mbm`–`Cliparts11.mbm` | Symbian MBM | Messaging/editor clip-art | 7,664–59,718 B | Out of launcher scope; do not prioritise. |
| `private/101ffa91/frames/frames01.mbm` | Symbian MBM | Messaging/editor frames | 1,214,249 B | Out of launcher scope; preserve as reference only. |
| `SYM.rom` | ARM ROM image | Complete raw firmware image | 60,600,320 B | Never package or convert to APK. Use only if Z: extraction lacks an identified resource. |

## Conversion acceptance criteria

Before an extracted asset becomes an Android deliverable:

1. Record its original path, image/resource ID, decoded dimensions, alpha/mask behaviour and visible purpose.
2. Capture a matching RM-320/EKA2L1 reference screen where possible.
3. Export at native resolution, preserve alpha, and compare visually against the reference; no crude nearest-neighbour enlargement.
4. Place fixed-pixel legacy artwork in `drawable-nodpi`; use generated/vector/programmatic states only when source extraction cannot support the required Android state.
5. Record licence/rights status. If unclear, recreate the visual behaviour rather than shipping the original binary asset.
