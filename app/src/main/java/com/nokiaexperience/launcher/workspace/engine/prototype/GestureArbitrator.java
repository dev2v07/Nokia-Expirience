package com.nokiaexperience.launcher.workspace.engine.prototype;

/** Pure ownership policy: one pointer sequence can have one owner only. */
public final class GestureArbitrator {
    public enum State { IDLE, POSSIBLE_SWIPE, POSSIBLE_DRAG, SWIPE, DRAG, LONG_PRESS, CANCELLED }

    private State state = State.IDLE;
    private boolean widgetTouch;

    public State state() { return state; }

    public boolean pointerDown(boolean isWidgetTouch) {
        return pointerDown(isWidgetTouch, false);
    }

    public boolean pointerDown(boolean isWidgetTouch, boolean hasDragTarget) {
        if (state != State.IDLE) return false;
        widgetTouch = isWidgetTouch;
        state = hasDragTarget ? State.POSSIBLE_DRAG : State.POSSIBLE_SWIPE;
        return true;
    }

    public boolean move(float dx, float dy, float touchSlop) {
        if (state != State.POSSIBLE_SWIPE && state != State.POSSIBLE_DRAG) return false;
        if (widgetTouch) return false;
        if (Math.max(Math.abs(dx), Math.abs(dy)) < touchSlop) return false;
        if (Math.abs(dx) <= Math.abs(dy)) {
            state = State.CANCELLED;
            return false;
        }
        state = State.SWIPE;
        return true;
    }

    public boolean longPress() {
        if (state != State.POSSIBLE_SWIPE && state != State.POSSIBLE_DRAG) return false;
        if (widgetTouch) return false;
        state = State.LONG_PRESS;
        return true;
    }

    public boolean beginDrag() {
        if (state != State.LONG_PRESS) return false;
        state = State.DRAG;
        return true;
    }

    public boolean end() {
        if (state != State.SWIPE && state != State.DRAG && state != State.LONG_PRESS) return false;
        state = State.IDLE;
        widgetTouch = false;
        return true;
    }

    public boolean cancel() {
        if (state == State.IDLE || state == State.CANCELLED) return false;
        state = State.CANCELLED;
        widgetTouch = false;
        return true;
    }

    public void reset() { state = State.IDLE; widgetTouch = false; }
}
