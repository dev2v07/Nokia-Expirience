package com.nokiaexperience.launcher;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.nokiaexperience.launcher.workspace.engine.prototype.DragTransaction;
import com.nokiaexperience.launcher.workspace.engine.prototype.GestureArbitrator;

/**
 * Production Home adapter for Nokia Exp 2 only. It owns one real icon pointer sequence,
 * while HomeWorkspaceModel remains the only persistent model. Drawer and widgets do not use it.
 */
public final class Exp2HomeDragEngine {
    public interface Host {
        int currentPage();
        int pageCount();
        int[] pageBoundsOnScreen(int page);
        int edgeDirection(float rawX);
        void movePageForSwipe(int direction);
        void movePageForDrag(int direction);
        String keyAt(int page, int cell);
        boolean isFolder(String key);
        void preview(String key, View view, int page, int cell);
        void commit(String key, int page, int cell, String folderTarget);
        void cancelPreview(String key, View view);
        void restoreOriginPage();
        void tapped(String key);
    }

    private static final long EDGE_HOVER_TIMEOUT_MS = 650L;
    private static final long FOLDER_CONFIRM_TIMEOUT_MS = 250L;
    private final Host host;
    private final GestureArbitrator gestures = new GestureArbitrator();
    private final DragTransaction transaction = new DragTransaction();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int touchSlop;
    private final int longPressTimeout;
    private View activeView;
    private String activeKey;
    private int originPage;
    private int originCell;
    private int targetPage;
    private int pointerId = MotionEvent.INVALID_POINTER_ID;
    private float downRawX, downRawY, touchOffsetX, touchOffsetY;
    private long edgeSince, folderSince;
    private boolean edgeLatched;
    private String hoveredTarget;
    private boolean moved;

    public Exp2HomeDragEngine(Host host, ViewConfiguration configuration) {
        this.host = host;
        touchSlop = configuration.getScaledTouchSlop();
        longPressTimeout = ViewConfiguration.getLongPressTimeout();
    }

    public GestureArbitrator.State gestureState() { return gestures.state(); }
    public DragTransaction.State transactionState() { return transaction.state(); }
    public boolean active() { return activeKey != null; }
    /** True while this pointer sequence belongs to an Exp2 icon, including tap/possible-drag. */
    public boolean pointerSequenceActive() { return activeKey != null; }

    public boolean onIconTouch(View view, String key, int page, MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) return down(view, key, page, event);
        if (action == MotionEvent.ACTION_MOVE) return move(event);
        if (action == MotionEvent.ACTION_UP) { up(event); return true; }
        if (action == MotionEvent.ACTION_CANCEL) { cancel(); return true; }
        return true;
    }

    private boolean down(View view, String key, int page, MotionEvent event) {
        // A completed tap/cancel must never poison the next pointer sequence.
        // Recover defensively from a stale terminal arbitration state as well.
        if (activeKey != null) cancel();
        if (gestures.state() != GestureArbitrator.State.IDLE) gestures.reset();
        if (!gestures.pointerDown(false, true)) return false;
        activeView = view; activeKey = key; originPage = page; targetPage = page;
        originCell = findCell(page, key);
        pointerId = event.getPointerId(0);
        downRawX = event.getRawX(); downRawY = event.getRawY();
        int[] location = new int[2]; view.getLocationOnScreen(location);
        touchOffsetX = event.getRawX() - location[0];
        touchOffsetY = event.getRawY() - location[1];
        edgeSince = 0; edgeLatched = false; hoveredTarget = null; folderSince = 0; moved = false;
        view.getParent().requestDisallowInterceptTouchEvent(true);
        handler.postDelayed(() -> {
            if (activeKey != null && gestures.longPress()) {
                if (transaction.state() != DragTransaction.State.IDLE) transaction.reset();
                transaction.begin(activeKey);
                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                view.animate().alpha(0.28f).setDuration(90).start();
                gestures.beginDrag();
            }
        }, longPressTimeout);
        return true;
    }

    private boolean move(MotionEvent event) {
        if (event.getPointerCount() == 0 || pointerId == MotionEvent.INVALID_POINTER_ID) return true;
        float rawX = event.getRawX(), rawY = event.getRawY();
        float dx = rawX - downRawX, dy = rawY - downRawY;
        GestureArbitrator.State state = gestures.state();
        if (state == GestureArbitrator.State.POSSIBLE_DRAG || state == GestureArbitrator.State.POSSIBLE_SWIPE) {
            gestures.move(dx, dy, touchSlop);
            if (gestures.state() == GestureArbitrator.State.SWIPE) {
                moved = true; handler.removeCallbacksAndMessages(null);
            } else if (gestures.state() == GestureArbitrator.State.CANCELLED) {
                moved = true; handler.removeCallbacksAndMessages(null);
            }
            return true;
        }
        if (gestures.state() != GestureArbitrator.State.DRAG) return true;
        moved = true;
        edgeHover(rawX);
        int[] bounds = host.pageBoundsOnScreen(targetPage);
        if (bounds == null) return true;
        float localX = rawX - bounds[0] - touchOffsetX;
        float localY = rawY - bounds[1] - touchOffsetY;
        int[] current = new int[2]; activeView.getLocationOnScreen(current);
        float baseTranslationX = activeView.getTranslationX();
        float baseTranslationY = activeView.getTranslationY();
        float desiredLeft = rawX - touchOffsetX;
        float desiredTop = rawY - touchOffsetY;
        activeView.setTranslationX(desiredLeft - (current[0] - baseTranslationX));
        activeView.setTranslationY(desiredTop - (current[1] - baseTranslationY));
        int cell = cellAt(bounds, rawX, rawY);
        hoveredTarget = host.keyAt(targetPage, cell);
        if (activeKey.equals(hoveredTarget)) hoveredTarget = null;
        if (hoveredTarget == null) folderSince = 0;
        else if (folderSince == 0) folderSince = System.currentTimeMillis();
        host.preview(activeKey, activeView, targetPage, cell);
        return true;
    }

    private void up(MotionEvent event) {
        handler.removeCallbacksAndMessages(null);
        if (gestures.state() == GestureArbitrator.State.SWIPE) {
            int direction = event.getRawX() < downRawX ? 1 : -1;
            host.movePageForSwipe(direction);
            gestures.end(); cancelFields(); return;
        }
        if (gestures.state() == GestureArbitrator.State.DRAG && transaction.state() != DragTransaction.State.IDLE) {
            int[] bounds = host.pageBoundsOnScreen(targetPage);
            int cell = bounds == null ? originCell : cellAt(bounds, event.getRawX(), event.getRawY());
            String target = host.keyAt(targetPage, cell);
            if (activeKey.equals(target)) target = null;
            boolean releasedInEdge = host.edgeDirection(event.getRawX()) != 0;
            boolean folderConfirmed = !releasedInEdge && target != null && (host.isFolder(target)
                || (target.equals(hoveredTarget) && folderSince > 0
                && System.currentTimeMillis() - folderSince >= FOLDER_CONFIRM_TIMEOUT_MS));
            if (!folderConfirmed) target = null;
            if (transaction.commit()) host.commit(activeKey, targetPage, cell, target);
            gestures.end(); cancelFields(); return;
        }
        boolean tap = !moved && (gestures.state() == GestureArbitrator.State.POSSIBLE_DRAG
            || gestures.state() == GestureArbitrator.State.POSSIBLE_SWIPE);
        if (tap) host.tapped(activeKey);
        if (gestures.state() != GestureArbitrator.State.IDLE) gestures.end();
        cancelFields();
        // end() intentionally handles only owned gestures. A tap or a
        // cancelled candidate is still a finished pointer sequence.
        gestures.reset();
    }

    public void cancel() {
        handler.removeCallbacksAndMessages(null);
        if (activeKey != null) host.restoreOriginPage();
        if (transaction.state() == DragTransaction.State.ACTIVE || transaction.state() == DragTransaction.State.PREVIEW)
            transaction.cancel();
        if (activeKey != null) host.cancelPreview(activeKey, activeView);
        if (gestures.state() != GestureArbitrator.State.IDLE) gestures.cancel();
        gestures.reset(); cancelFields();
    }

    private void edgeHover(float rawX) {
        int direction = host.edgeDirection(rawX);
        if (direction == 0) { edgeSince = 0; edgeLatched = false; return; }
        if (edgeSince == 0) edgeSince = System.currentTimeMillis();
        if (!edgeLatched && System.currentTimeMillis() - edgeSince >= EDGE_HOVER_TIMEOUT_MS) {
            int before = host.currentPage(); host.movePageForDrag(direction);
            targetPage = host.currentPage(); edgeLatched = before != targetPage;
            if (edgeLatched) { hoveredTarget = null; folderSince = 0; }
        }
    }

    private int cellAt(int[] bounds, float rawX, float rawY) {
        int width = Math.max(1, bounds[2]), height = Math.max(1, bounds[3]);
        int column = Math.max(0, Math.min(HomeWorkspaceModel.COLUMNS - 1, (int)((rawX - bounds[0]) / (width / 4f))));
        int row = Math.max(0, Math.min(HomeWorkspaceModel.ROWS - 1, (int)((rawY - bounds[1]) / (height / 3f))));
        return row * HomeWorkspaceModel.COLUMNS + column;
    }

    private int findCell(int page, String key) {
        for (int cell = 0; cell < HomeWorkspaceModel.CELLS; cell++) if (key.equals(host.keyAt(page, cell))) return cell;
        return 0;
    }

    private void cancelFields() {
        if (activeView != null) { activeView.setTranslationX(0); activeView.setTranslationY(0); activeView.animate().alpha(1f).setDuration(90).start(); }
        if (activeKey != null) host.cancelPreview(activeKey, activeView);
        if (activeView != null && activeView.getParent() != null) activeView.getParent().requestDisallowInterceptTouchEvent(false);
        activeView = null; activeKey = null; pointerId = MotionEvent.INVALID_POINTER_ID; hoveredTarget = null; folderSince = 0;
        edgeSince = 0; edgeLatched = false; moved = false;
    }
}
