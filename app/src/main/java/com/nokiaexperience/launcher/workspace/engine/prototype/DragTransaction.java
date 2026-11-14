package com.nokiaexperience.launcher.workspace.engine.prototype;

/** In-memory PREVIEW/COMMIT/CANCEL transaction. It never owns or writes persistence. */
public final class DragTransaction {
    public enum State { IDLE, ACTIVE, PREVIEW, COMMIT, CANCEL }

    private State state = State.IDLE;
    private String item;
    private String target;

    public State state() { return state; }
    public String item() { return item; }
    public String target() { return target; }

    public boolean begin(String itemKey) {
        if (state != State.IDLE || itemKey == null || itemKey.isEmpty()) return false;
        item = itemKey; target = null; state = State.ACTIVE; return true;
    }

    public boolean preview(String targetKey) {
        if ((state != State.ACTIVE && state != State.PREVIEW) || targetKey == null) return false;
        target = targetKey; state = State.PREVIEW; return true;
    }

    /** The call itself represents the explicit Drop confirmation. */
    public boolean commit() {
        if (state != State.ACTIVE && state != State.PREVIEW) return false;
        state = State.COMMIT; return true;
    }

    public boolean cancel() {
        if (state == State.COMMIT || state == State.CANCEL) return false;
        if (state == State.IDLE) return false;
        state = State.CANCEL; return true;
    }

    public void reset() {
        if (state == State.ACTIVE || state == State.PREVIEW) throw new IllegalStateException("active transaction");
        state = State.IDLE; item = null; target = null;
    }
}
