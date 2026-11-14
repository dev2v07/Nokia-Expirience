from __future__ import annotations

from PySide6.QtCore import Signal
from PySide6.QtWidgets import QWidget, QFormLayout, QDoubleSpinBox, QLineEdit, QCheckBox, QLabel, QGroupBox


class Inspector(QWidget):
    changed = Signal(object)

    def __init__(self, parent=None):
        super().__init__(parent)
        self.layer = None
        self.form = QFormLayout(self)
        self.empty = QLabel("Выберите слой на Canvas")
        self.form.addRow(self.empty)
        self.fields = {}

    def show_layer(self, layer):
        self.layer = layer
        while self.form.rowCount():
            self.form.removeRow(0)
        if not layer:
            self.form.addRow(QLabel("Выберите слой на Canvas"))
            return
        title = QLabel(f"<b>{layer.get('name')}</b>")
        self.form.addRow(title)
        self.fields.clear()
        for key in ("x", "y", "width", "height", "opacity", "radius", "blur"):
            box = QDoubleSpinBox(); box.setRange(0, 5000); box.setDecimals(2); box.setValue(float(layer.get(key, 0)))
            box.valueChanged.connect(lambda value, k=key: self._set(k, value))
            self.fields[key] = box; self.form.addRow(key, box)
        for key in ("color", "border"):
            edit = QLineEdit(str(layer.get(key, "#FFFFFF")))
            edit.editingFinished.connect(lambda k=key, e=edit: self._set(k, e.text()))
            self.fields[key] = edit; self.form.addRow(key, edit)
        visible = QCheckBox(); visible.setChecked(bool(layer.get("visible", True))); visible.stateChanged.connect(lambda value: self._set("visible", bool(value)))
        locked = QCheckBox(); locked.setChecked(bool(layer.get("locked", False))); locked.stateChanged.connect(lambda value: self._set("locked", bool(value)))
        self.fields["visible"] = visible; self.fields["locked"] = locked
        self.form.addRow("Visible", visible); self.form.addRow("Locked", locked)
        export = QLabel("Exportable" if layer.get("exportable", False) else "Not Exportable")
        self.form.addRow("Runtime", export)

    def _set(self, key, value):
        if self.layer is not None:
            updated = dict(self.layer)
            updated[key] = value
            self.layer = updated
            self.changed.emit(updated)
