package com.nokiaexperience.launcher.workspace.engine.prototype;

/** Pure one-page-at-a-time scroll policy for API 26-compatible hosts. */
public final class ScrollController {
    public static final class Config {
        public final float minimumDistancePx;
        public final float flingVelocityPxPerSecond;

        public Config(float minimumDistancePx, float flingVelocityPxPerSecond) {
            if (minimumDistancePx < 0 || flingVelocityPxPerSecond < 0)
                throw new IllegalArgumentException("scroll thresholds must be non-negative");
            this.minimumDistancePx = minimumDistancePx;
            this.flingVelocityPxPerSecond = flingVelocityPxPerSecond;
        }
    }

    public enum State { IDLE, ACTIVE, CANCELLED }
    public enum Direction { NONE, PREVIOUS, NEXT }

    public static final class Decision {
        public final int targetPage;
        public final Direction direction;
        public final boolean moved;

        private Decision(int target, Direction direction, boolean moved) {
            targetPage = target; this.direction = direction; this.moved = moved;
        }
    }

    private final Config config;
    private State state = State.IDLE;

    public ScrollController(Config config) { this.config = config; }
    public State state() { return state; }
    public Config config() { return config; }

    public boolean begin() {
        if (state != State.IDLE) return false;
        state = State.ACTIVE;
        return true;
    }

    public Decision finish(int currentPage, int pageCount, float displacementPx, float velocityPxPerSecond) {
        if (state != State.ACTIVE) return decision(currentPage, Direction.NONE, false);
        int last = Math.max(0, pageCount - 1);
        Direction direction = Direction.NONE;
        if (displacementPx <= -config.minimumDistancePx || velocityPxPerSecond >= config.flingVelocityPxPerSecond)
            direction = Direction.NEXT;
        else if (displacementPx >= config.minimumDistancePx || velocityPxPerSecond <= -config.flingVelocityPxPerSecond)
            direction = Direction.PREVIOUS;
        int target = currentPage;
        if (direction == Direction.NEXT) target = Math.min(last, currentPage + 1);
        if (direction == Direction.PREVIOUS) target = Math.max(0, currentPage - 1);
        state = State.IDLE;
        return decision(target, target == currentPage ? Direction.NONE : direction, target != currentPage);
    }

    public boolean cancel() {
        if (state != State.ACTIVE) return false;
        state = State.CANCELLED;
        return true;
    }

    public void reset() { state = State.IDLE; }

    private static Decision decision(int target, Direction direction, boolean moved) {
        return new Decision(target, direction, moved);
    }
}
