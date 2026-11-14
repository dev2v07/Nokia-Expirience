from __future__ import annotations

import subprocess
from pathlib import Path

from PySide6.QtCore import QThread, Signal
from PySide6.QtGui import QImage


class LiveCapture(QThread):
    frame = Signal(QImage)
    state = Signal(str)

    def __init__(self, adb: Path, serial: str, parent=None):
        super().__init__(parent)
        self.adb = adb
        self.serial = serial

    def run(self):
        try:
            result = subprocess.run([str(self.adb), "-s", self.serial, "exec-out", "screencap", "-p"], capture_output=True, timeout=8)
            image = QImage.fromData(result.stdout, "PNG") if result.returncode == 0 else QImage()
            if image.isNull():
                self.state.emit("OFFLINE · ADB frame unavailable")
            else:
                self.frame.emit(image)
                self.state.emit(f"LIVE DEVICE · {image.width()}×{image.height()} · {self.serial}")
        except Exception as exc:
            self.state.emit(f"OFFLINE · {exc}")
