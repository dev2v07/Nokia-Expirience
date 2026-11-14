from __future__ import annotations

from PySide6.QtCore import Qt, Signal, QRectF
from PySide6.QtGui import QColor, QBrush, QPen, QPainter, QFont, QPixmap, QImage
from PySide6.QtWidgets import QGraphicsItem, QGraphicsRectItem, QGraphicsTextItem, QGraphicsView, QGraphicsScene


class LayerItem(QGraphicsRectItem):
    def __init__(self, layer: dict, on_change=None, parent=None):
        super().__init__(0, 0, float(layer.get("width", 100)), float(layer.get("height", 100)), parent)
        self.layer = layer
        self.on_change = on_change
        self.overlay = False
        self.setPos(float(layer.get("x", 0)), float(layer.get("y", 0)))
        self.setOpacity(float(layer.get("opacity", 1)))
        self.setFlag(QGraphicsItem.GraphicsItemFlag.ItemIsMovable, not layer.get("locked", False))
        self.setFlag(QGraphicsItem.GraphicsItemFlag.ItemIsSelectable, True)
        self.refresh_style()

    def refresh_style(self):
        fill = QColor(self.layer.get("color", "#20384A"))
        if self.overlay:
            fill.setAlpha(0)
        else:
            fill.setAlphaF(max(0.05, min(1.0, float(self.layer.get("opacity", 1)))) if self.layer.get("kind") != "background" else 0.65)
        border = QColor(self.layer.get("border", "#8FD6FF"))
        border.setAlpha(210)
        self.setBrush(QBrush(fill))
        self.setPen(QPen(border, 2))

    def itemChange(self, change, value):
        if change == QGraphicsItem.GraphicsItemChange.ItemPositionHasChanged:
            self.layer["x"] = round(self.pos().x())
            self.layer["y"] = round(self.pos().y())
            if self.on_change:
                self.on_change(self.layer)
        return super().itemChange(change, value)


class PhoneCanvas(QGraphicsView):
    selected = Signal(object)
    changed = Signal(object)

    def __init__(self, parent=None):
        super().__init__(parent)
        self.scene = QGraphicsScene(self)
        self.setScene(self.scene)
        self.setRenderHint(QPainter.RenderHint.Antialiasing)
        self.setRenderHint(QPainter.RenderHint.TextAntialiasing)
        self.setBackgroundBrush(QColor("#101822"))
        self.setDragMode(QGraphicsView.DragMode.RubberBandDrag)
        self.items_by_id = {}
        self.device_width = 1080
        self.device_height = 1920
        self.scene.selectionChanged.connect(self._selection_changed)
        self.live_item = None
        self.live_overlay = False

    def load_screen(self, screen: dict, device: dict):
        self.scene.clear()
        self.items_by_id.clear()
        self.device_width = int(device.get("width", 1080))
        self.device_height = int(device.get("height", 1920))
        self.scene.setSceneRect(0, 0, self.device_width, self.device_height)
        for layer in screen.get("layers", []):
            item = LayerItem(layer, self.changed.emit)
            self.scene.addItem(item)
            self.items_by_id[layer.get("id")] = item
        self.fitInView(self.scene.sceneRect(), Qt.AspectRatioMode.KeepAspectRatio)

    def set_live_frame(self, image: QImage):
        if image.isNull():
            return
        if self.live_item is None:
            from PySide6.QtWidgets import QGraphicsPixmapItem
            self.live_item = QGraphicsPixmapItem()
            self.live_item.setZValue(-1000)
            self.scene.addItem(self.live_item)
        pixmap = QPixmap.fromImage(image)
        scale = min(self.device_width / max(1, pixmap.width()), self.device_height / max(1, pixmap.height()))
        self.live_item.setPixmap(pixmap)
        self.live_item.setScale(scale)
        self.live_item.setPos((self.device_width - pixmap.width() * scale) / 2, (self.device_height - pixmap.height() * scale) / 2)
        self.live_item.setVisible(True)
        self.set_overlay_mode(True)
        self.fitInView(self.scene.sceneRect(), Qt.AspectRatioMode.KeepAspectRatio)

    def set_overlay_mode(self, enabled: bool):
        self.live_overlay = enabled
        for item in self.items_by_id.values():
            item.overlay = enabled
            item.refresh_style()

    def set_live_mode(self, enabled: bool):
        self.set_overlay_mode(enabled)
        if self.live_item:
            self.live_item.setVisible(enabled)

    def resizeEvent(self, event):
        super().resizeEvent(event)
        if self.scene.sceneRect().width():
            self.fitInView(self.scene.sceneRect(), Qt.AspectRatioMode.KeepAspectRatio)

    def _selection_changed(self):
        selected = self.scene.selectedItems()
        self.selected.emit(selected[0].layer if selected and hasattr(selected[0], "layer") else None)

    def apply_layer(self, layer: dict):
        item = self.items_by_id.get(layer.get("id"))
        if not item:
            return
        item.setPos(float(layer.get("x", 0)), float(layer.get("y", 0)))
        item.setRect(0, 0, float(layer.get("width", 100)), float(layer.get("height", 100)))
        item.setOpacity(float(layer.get("opacity", 1)))
        item.setFlag(QGraphicsItem.GraphicsItemFlag.ItemIsMovable, not layer.get("locked", False))
        item.refresh_style()

    def drawForeground(self, painter: QPainter, rect: QRectF):
        super().drawForeground(painter, rect)
        painter.save()
        painter.setPen(QPen(QColor(143, 214, 255, 90), 1, Qt.PenStyle.DashLine))
        for x in range(0, self.device_width + 1, self.device_width // 4):
            painter.drawLine(x, 0, x, self.device_height)
        for y in range(0, self.device_height + 1, self.device_height // 12):
            painter.drawLine(0, y, self.device_width, y)
        painter.restore()
