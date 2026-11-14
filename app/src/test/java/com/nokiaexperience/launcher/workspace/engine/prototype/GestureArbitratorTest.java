package com.nokiaexperience.launcher.workspace.engine.prototype;

import org.junit.Test;
import static org.junit.Assert.*;

public class GestureArbitratorTest {
    @Test public void horizontalMoveOwnsSwipe() {
        GestureArbitrator g = new GestureArbitrator();
        assertTrue(g.pointerDown(false));
        assertTrue(g.move(-30f, 2f, 10f));
        assertEquals(GestureArbitrator.State.SWIPE, g.state());
        assertTrue(g.end());
        assertEquals(GestureArbitrator.State.IDLE, g.state());
    }

    @Test public void dragAndSwipeCannotOwnOneGesture() {
        GestureArbitrator g = new GestureArbitrator();
        assertTrue(g.pointerDown(false));
        assertTrue(g.longPress());
        assertTrue(g.beginDrag());
        assertFalse(g.move(-40f, 0f, 10f));
        assertEquals(GestureArbitrator.State.DRAG, g.state());
    }

    @Test public void widgetTouchDoesNotBecomeSwipe() {
        GestureArbitrator g = new GestureArbitrator();
        assertTrue(g.pointerDown(true));
        assertFalse(g.move(-100f, 0f, 10f));
        assertEquals(GestureArbitrator.State.POSSIBLE_SWIPE, g.state());
    }

    @Test public void cancellationCanReturnToIdle() {
        GestureArbitrator g = new GestureArbitrator();
        assertTrue(g.pointerDown(false));
        assertTrue(g.cancel());
        assertEquals(GestureArbitrator.State.CANCELLED, g.state());
        g.reset();
        assertEquals(GestureArbitrator.State.IDLE, g.state());
    }
}
