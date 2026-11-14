# Settings Mapping

| Samsung UX | Nokia implementation | Status |
|---|---|---|
| Home edit mode | Exp2-only overlay | REPLICATED |
| Widgets | existing AppWidgetHost / `addWidget()` | REPLICATED |
| Wallpaper | existing `chooseWallpaper()` | REPLICATED |
| Home settings | existing `showSettings()` | REPLICATED |
| Samsung-specific provider/settings | not copied | VERIFIED by design |

Samsung preference names и закрытые provider не использовались.
