package com.nokiaexperience.launcher.workspace.engine.prototype;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScrollControllerTest {
    private ScrollController controller() { return new ScrollController(new ScrollController.Config(40f, 800f)); }

    @Test public void slowAndFastSwipesMoveAtMostOnePage() {
        ScrollController c = controller();
        assertTrue(c.begin());
        assertEquals(1, c.finish(0, 5, -50f, 0f).targetPage);
        assertTrue(c.begin());
        assertEquals(2, c.finish(1, 5, 0f, 900f).targetPage);
    }

    @Test public void directionAndBoundsAreCorrect() {
        ScrollController c = controller();
        assertTrue(c.begin());
        assertEquals(0, c.finish(0, 3, 50f, 0f).targetPage);
        assertTrue(c.begin());
        assertEquals(2, c.finish(2, 3, -100f, 0f).targetPage);
        assertTrue(c.begin());
        assertFalse(c.finish(1, 3, 5f, 0f).moved);
    }

    @Test public void cancellationResetsOnlyThroughExplicitReset() {
        ScrollController c = controller();
        assertTrue(c.begin());
        assertTrue(c.cancel());
        assertFalse(c.begin());
        c.reset();
        assertTrue(c.begin());
    }
}
