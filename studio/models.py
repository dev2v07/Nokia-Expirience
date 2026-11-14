from __future__ import annotations

import copy
import json
import os
import shutil
import tempfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any


@dataclass
class StudioProject:
    path: Path | None = None
    data: dict[str, Any] = field(default_factory=dict)
    undo_stack: list[dict[str, Any]] = field(default_factory=list)
    redo_stack: list[dict[str, Any]] = field(default_factory=list)

    @staticmethod
    def default() -> "StudioProject":
        screens = {}
        for name in ("Home Screen 1", "Home Screen 2", "Home Screen 3"):
            screens[name] = {"layers": default_layers(), "exportable": True}
        screens.update({
            "App Drawer": {"layers": [], "exportable": False},
            "Edit Mode": {"layers": [], "exportable": False},
            "Settings": {"layers": [], "exportable": False},
            "Status Bar": {"layers": default_status_layers(), "exportable": True},
            "Widgets": {"layers": [], "exportable": False},
            "Dialogs": {"layers": [], "exportable": False},
        })
        return StudioProject(data={
            "schema": 1,
            "name": "Nokia Experience 2",
            "device": {"model": "SM-A520F", "width": 1080, "height": 1920, "density": 420, "api": 26, "serial": ""},
            "screens": screens,
            "theme": {"name": "Nokia Glass", "accent": "#208FFF", "statusIconColor": "#FFFFFF", "glassOpacity": 0.35, "blurRadius": 12, "borderOpacity": 0.65, "cornerRadius": 15, "glowIntensity": 0.2},
            "grids": {"home": {"columns": 4, "rows": 3, "widgetSlots": 2, "dockSeparate": True}},
            "assets": {"wallpaper": "", "icons": {}},
            "export": {"managedAsset": "studio/project.json", "statusIconColor": True, "themeAccent": True},
        })

    def snapshot(self) -> dict[str, Any]:
        return copy.deepcopy(self.data)

    def restore(self, snapshot: dict[str, Any]) -> None:
        self.data = copy.deepcopy(snapshot)

    def mutate(self, fn) -> None:
        self.undo_stack.append(self.snapshot())
        self.redo_stack.clear()
        fn(self.data)

    def undo(self) -> bool:
        if not self.undo_stack:
            return False
        self.redo_stack.append(self.snapshot())
        self.restore(self.undo_stack.pop())
        return True

    def redo(self) -> bool:
        if not self.redo_stack:
            return False
        self.undo_stack.append(self.snapshot())
        self.restore(self.redo_stack.pop())
        return True

    def save(self, path: Path | None = None) -> Path:
        target = Path(path or self.path or Path("studio/project.json"))
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists():
            backup = target.with_suffix(target.suffix + ".bak")
            shutil.copy2(target, backup)
        fd, temp_name = tempfile.mkstemp(prefix="studio-", suffix=".json", dir=target.parent)
        try:
            with os.fdopen(fd, "w", encoding="utf-8") as stream:
                json.dump(self.data, stream, ensure_ascii=False, indent=2)
                stream.write("\n")
            os.replace(temp_name, target)
        finally:
            if os.path.exists(temp_name):
                os.unlink(temp_name)
        self.path = target
        return target

    def to_json(self) -> str:
        return json.dumps(self.data, ensure_ascii=False, indent=2) + "\n"

    @staticmethod
    def load(path: Path) -> "StudioProject":
        return StudioProject(path=path, data=json.loads(path.read_text(encoding="utf-8")))


def default_layers() -> list[dict[str, Any]]:
    layers = [
        ("Wallpaper", "background", 0, 0, 1080, 1920),
        ("Status Bar", "status", 0, 0, 1080, 155),
        ("Widget Area", "panel", 40, 180, 1000, 420),
        ("Application Grid", "grid", 40, 620, 1000, 760),
        ("Dock", "dock", 40, 1450, 1000, 260),
    ]
    return [layer_dict(name, kind, x, y, w, h) for name, kind, x, y, w, h in layers]


def default_status_layers() -> list[dict[str, Any]]:
    return [layer_dict("Status Bar", "status", 0, 0, 1080, 155)]


def layer_dict(name: str, kind: str, x: int, y: int, width: int, height: int) -> dict[str, Any]:
    return {"id": name.lower().replace(" ", "_"), "name": name, "kind": kind, "x": x, "y": y, "width": width, "height": height, "scale": 1.0, "opacity": 1.0, "visible": True, "locked": False, "color": "#20384A", "border": "#8FD6FF", "radius": 15, "blur": 0, "z": 0, "exportable": kind in {"status", "dock", "panel", "background"}}
