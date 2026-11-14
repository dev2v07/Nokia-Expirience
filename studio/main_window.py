from __future__ import annotations

import json
import os
import sys
from pathlib import Path

from PySide6.QtCore import Qt, QTimer
from PySide6.QtGui import QAction, QColor, QFont
from PySide6.QtWidgets import QApplication, QDialog, QDialogButtonBox, QDoubleSpinBox, QDockWidget, QFileDialog, QLabel, QLineEdit, QListWidget, QListWidgetItem, QMainWindow, QMessageBox, QSpinBox, QSplitter, QStatusBar, QTreeWidget, QTreeWidgetItem, QWidget, QVBoxLayout, QFormLayout

from .android import ADB_DEFAULT, build_apk, capture_screenshot, device_info, export_project, install_apk, probe_device
from .canvas import PhoneCanvas
from .inspector import Inspector
from .models import StudioProject
from .live import LiveCapture


ANDROID_ROOT = Path(os.environ.get("NOKIA_ANDROID_ROOT", Path(__file__).resolve().parents[1]))


class StudioWindow(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("Nokia Experience Studio")
        self.resize(1500, 940)
        self.project = StudioProject.default()
        self.current_screen = "Home Screen 1"
        self.canvas = PhoneCanvas()
        self.inspector = Inspector()
        self.live_capture = None
        self.live_timer = QTimer(self); self.live_timer.setInterval(1200); self.live_timer.timeout.connect(self.capture_live)
        self.layer_tree = QTreeWidget(); self.layer_tree.setHeaderLabels(["Screens / Layers"])
        self._build_ui()
        self._load_screen()

    def _build_ui(self):
        self.setStyleSheet("""
        QMainWindow,QWidget{background:#101822;color:#e9f4ff;font-family:'Segoe UI';}
        QToolBar{background:#152333;border:0;padding:6px;spacing:6px;}
        QTreeWidget,QListWidget,QLineEdit,QDoubleSpinBox{background:#172433;border:1px solid #2d4a63;border-radius:6px;padding:5px;color:#e9f4ff;}
        QDockWidget{titlebar-close-icon:none;}
        QPushButton{background:#1d3e59;border:1px solid #6eb8e9;border-radius:6px;padding:7px;}
        QPushButton:hover{background:#285a7c;}
        QGraphicsView{border:1px solid #2d5875;}
        """)
        toolbar = self.addToolBar("Studio")
        actions = (("Новый", self.new_project), ("Открыть JSON", self.open_project), ("Открыть Android", self.open_android_project), ("Сохранить", self.save_project), ("Отменить", self.undo), ("Повторить", self.redo), ("Импорт ресурсов", self.import_asset), ("Тема", self.edit_theme), ("Сетка", self.edit_grid), ("LIVE DEVICE", self.toggle_live), ("APPLY TO DEVICE", self.apply_to_device), ("Экспорт Android", self.export_android), ("Собрать APK", self.build), ("Установить APK", self.install), ("Скриншот Nokia B", self.screenshot), ("Устройство", self.device_probe))
        for text, slot in actions:
            action = QAction(text, self); action.triggered.connect(slot)
            if text == "LIVE DEVICE":
                action.setCheckable(True); self.live_action = action
            toolbar.addAction(action)
        body = QSplitter(Qt.Orientation.Horizontal)
        left = QWidget(); left_layout = QVBoxLayout(left); left_layout.addWidget(QLabel("Экраны и слои")); left_layout.addWidget(self.layer_tree)
        body.addWidget(left); body.addWidget(self.canvas); body.addWidget(self.inspector); body.setSizes([260, 850, 300])
        self.setCentralWidget(body)
        self.layer_tree.itemClicked.connect(self._tree_clicked)
        self.canvas.selected.connect(self.inspector.show_layer)
        self.canvas.changed.connect(self._canvas_changed)
        self.inspector.changed.connect(self._inspector_changed)
        self.setStatusBar(QStatusBar())
        self.statusBar().showMessage("Nokia Experience Studio · visual model")

    def _load_screen(self):
        self.layer_tree.clear()
        for screen_name, screen in self.project.data["screens"].items():
            root = QTreeWidgetItem([screen_name]); root.setData(0, Qt.ItemDataRole.UserRole, ("screen", screen_name)); self.layer_tree.addTopLevelItem(root)
            for layer in screen.get("layers", []):
                child = QTreeWidgetItem([layer.get("name", layer.get("id"))]); child.setData(0, Qt.ItemDataRole.UserRole, ("layer", screen_name, layer.get("id"))); root.addChild(child)
            root.setExpanded(screen_name == self.current_screen)
        screen = self.project.data["screens"].get(self.current_screen, {})
        self.canvas.load_screen(screen, self.project.data["device"])

    def _tree_clicked(self, item, _column):
        data = item.data(0, Qt.ItemDataRole.UserRole)
        if data and data[0] == "screen":
            self.current_screen = data[1]; self._load_screen()
        elif data and data[0] == "layer":
            layer = next((x for x in self.project.data["screens"][data[1]].get("layers", []) if x.get("id") == data[2]), None)
            self.inspector.show_layer(layer)
            if layer and data[2] in self.canvas.items_by_id:
                self.canvas.items_by_id[data[2]].setSelected(True)

    def _canvas_changed(self, layer):
        self.statusBar().showMessage(f"Changed: {layer.get('name')}")
        self.inspector.show_layer(layer)

    def _inspector_changed(self, layer):
        screen = self.project.data["screens"].get(self.current_screen, {})
        existing = next((x for x in screen.get("layers", []) if x.get("id") == layer.get("id")), None)
        if not existing:
            return
        self.project.mutate(lambda data: existing.update(layer))
        self.canvas.apply_layer(layer)
        self.inspector.show_layer(existing)
        self.statusBar().showMessage(f"Edited: {layer.get('name')}")

    def new_project(self):
        self.project = StudioProject.default(); self.current_screen = "Home Screen 1"; self._load_screen()

    def open_project(self):
        filename, _ = QFileDialog.getOpenFileName(self, "Open Studio project", str(Path.cwd()), "JSON (*.json)")
        if filename:
            self.project = StudioProject.load(Path(filename)); self.current_screen = next(iter(self.project.data.get("screens", {})), "Home Screen 1"); self._load_screen()

    def open_android_project(self):
        folder = QFileDialog.getExistingDirectory(self, "Open Nokia Android project", str(ANDROID_ROOT))
        if not folder:
            return
        config = Path(folder) / "studio" / "project.json"
        self.project = StudioProject.load(config) if config.exists() else StudioProject.default()
        self.project.path = config
        self.current_screen = next(iter(self.project.data.get("screens", {})), "Home Screen 1")
        self._load_screen()
        self.statusBar().showMessage(f"Android project: {folder}")

    def save_project(self):
        path = self.project.path or Path.cwd() / "studio" / "project.json"
        self.project.save(path); self.statusBar().showMessage(f"Saved: {path}")

    def undo(self):
        if self.project.undo(): self._load_screen()

    def redo(self):
        if self.project.redo(): self._load_screen()

    def import_asset(self):
        filename, _ = QFileDialog.getOpenFileName(self, "Import image or SVG", str(Path.home()), "Images (*.png *.jpg *.jpeg *.svg)")
        if filename:
            self.project.data.setdefault("assets", {}).setdefault("imports", []).append(filename); self.statusBar().showMessage(f"Imported asset: {filename}")

    def edit_theme(self):
        dialog = QDialog(self); dialog.setWindowTitle("Theme Studio"); form = QFormLayout(dialog)
        theme = self.project.data.setdefault("theme", {})
        accent = QLineEdit(theme.get("accent", "#208FFF")); status = QLineEdit(theme.get("statusIconColor", "#FFFFFF"))
        opacity = QDoubleSpinBox(); opacity.setRange(0, 1); opacity.setSingleStep(.05); opacity.setValue(float(theme.get("glassOpacity", .35)))
        blur = QSpinBox(); blur.setRange(0, 40); blur.setValue(int(theme.get("blurRadius", 12)))
        form.addRow("Accent", accent); form.addRow("Status icon color", status); form.addRow("Glass opacity", opacity); form.addRow("Blur radius", blur)
        buttons = QDialogButtonBox(QDialogButtonBox.StandardButton.Save | QDialogButtonBox.StandardButton.Cancel); form.addRow(buttons); buttons.accepted.connect(dialog.accept); buttons.rejected.connect(dialog.reject)
        if dialog.exec():
            self.project.mutate(lambda data: data["theme"].update({"accent": accent.text(), "statusIconColor": status.text(), "glassOpacity": opacity.value(), "blurRadius": blur.value()}))
            self._load_screen(); self.statusBar().showMessage("Theme updated")

    def edit_grid(self):
        dialog = QDialog(self); dialog.setWindowTitle("Grid Designer"); form = QFormLayout(dialog)
        grid = self.project.data.setdefault("grids", {}).setdefault("home", {})
        columns = QSpinBox(); columns.setRange(1, 8); columns.setValue(int(grid.get("columns", 4)))
        rows = QSpinBox(); rows.setRange(1, 8); rows.setValue(int(grid.get("rows", 3)))
        widgets = QSpinBox(); widgets.setRange(0, 4); widgets.setValue(int(grid.get("widgetSlots", 2)))
        form.addRow("Columns", columns); form.addRow("Rows", rows); form.addRow("Widget slots", widgets)
        note = QLabel("Exp2 runtime contract: 4×3 and separate Dock; unsupported layouts are visual-only."); note.setWordWrap(True); form.addRow(note)
        buttons = QDialogButtonBox(QDialogButtonBox.StandardButton.Save | QDialogButtonBox.StandardButton.Cancel); form.addRow(buttons); buttons.accepted.connect(dialog.accept); buttons.rejected.connect(dialog.reject)
        if dialog.exec():
            self.project.mutate(lambda data: data["grids"]["home"].update({"columns": columns.value(), "rows": rows.value(), "widgetSlots": widgets.value(), "dockSeparate": True}))
            self.canvas.viewport().update(); self.statusBar().showMessage("Grid metadata updated")

    def export_android(self):
        ok, message = export_project(self.project, ANDROID_ROOT); QMessageBox.information(self, "Android export", message if ok else f"Export failed: {message}")

    def build(self):
        self.export_android(); code, output = build_apk(ANDROID_ROOT); QMessageBox.information(self, "Gradle", output[-4000:] or f"Exit code {code}")

    def install(self):
        code, output = install_apk(ANDROID_ROOT, self.project.data["device"].get("serial", ""))
        QMessageBox.information(self, "ADB install", output or f"Exit code {code}")

    def screenshot(self):
        output = Path.cwd() / "captures" / "nokia_b_latest.png"
        code, message = capture_screenshot(self.project.data["device"].get("serial", ""), output)
        QMessageBox.information(self, "Device screenshot", message if code == 0 else f"Capture failed: {message}")

    def device_probe(self):
        info = device_info(self.project.data["device"].get("serial", ""))
        self.statusBar().showMessage(probe_device() if info["online"] != "true" else f"ONLINE · {info['model']} · Android {info['android']} / API {info['api']} · {info['size']} · {info['density']}")

    def toggle_live(self, checked: bool):
        self.canvas.set_live_mode(checked)
        if checked:
            self.capture_live(); self.live_timer.start(); self.statusBar().showMessage("LIVE DEVICE · connecting…")
        else:
            self.live_timer.stop(); self.statusBar().showMessage("DESIGN MODE · live frame hidden")

    def capture_live(self):
        if self.live_capture and self.live_capture.isRunning():
            return
        serial = self.project.data["device"].get("serial", "")
        self.live_capture = LiveCapture(ADB_DEFAULT, serial, self)
        self.live_capture.frame.connect(self.canvas.set_live_frame)
        self.live_capture.state.connect(self.statusBar().showMessage)
        self.live_capture.finished.connect(self.live_capture.deleteLater)
        self.live_capture.start()

    def apply_to_device(self):
        answer = QMessageBox.question(self, "Apply to Nokia B", "Экспортировать, собрать и установить APK на Nokia B? Данные не очищаются.")
        if answer != QMessageBox.StandardButton.Yes:
            return
        ok, message = export_project(self.project, ANDROID_ROOT)
        if not ok:
            QMessageBox.warning(self, "Apply", message); return
        code, output = build_apk(ANDROID_ROOT)
        if code:
            QMessageBox.warning(self, "Apply / build", output[-4000:]); return
        code, output = install_apk(ANDROID_ROOT, self.project.data["device"].get("serial", ""))
        if code:
            QMessageBox.warning(self, "Apply / install", output[-4000:]); return
        self.capture_live(); QMessageBox.information(self, "Apply", "Конфигурация установлена. Новый кадр запрошен.")

    def closeEvent(self, event):
        self.live_timer.stop()
        if self.live_capture and self.live_capture.isRunning():
            self.live_capture.wait(2000)
        super().closeEvent(event)


def run():
    app = QApplication(sys.argv)
    app.setApplicationName("Nokia Experience Studio")
    window = StudioWindow(); window.show()
    sys.exit(app.exec())
