package com.nokiaexperience.launcher.workspace.engine.prototype;

import org.junit.Test;
import static org.junit.Assert.*;

public class DragControllerTest {
    private DragController controller() {
        DragController c = new DragController(new DragTransaction());
        c.addItem("a", 0, 10, 10);
        c.addItem("b", 0, 300, 10);
        return c;
    }

    @Test public void previewDoesNotMutateCommittedItem() {
        DragController c = controller();
        assertTrue(c.begin("a"));
        assertTrue(c.preview(1, 20, 30, null));
        assertEquals(0, c.items().get(0).page);
        assertTrue(c.cancel());
        assertEquals(0, c.items().get(0).page);
    }

    @Test public void freeDropCommitsOnce() {
        DragController c = controller();
        assertTrue(c.begin("a"));
        assertTrue(c.preview(1, 20, 30, null));
        assertTrue(c.commit());
        assertEquals(1, c.items().get(0).page);
        assertFalse(c.commit());
    }

    @Test public void occupiedDropSwapsCommittedPositions() {
        DragController c = controller();
        assertTrue(c.begin("a"));
        assertTrue(c.preview(0, 300, 10, "b"));
        assertTrue(c.commit());
        assertEquals(300f, c.items().get(0).x, 0f);
        assertEquals(10f, c.items().get(1).x, 0f);
    }

    @Test public void cancelLeavesModelUntouched() {
        DragController c = controller();
        assertTrue(c.begin("a"));
        assertTrue(c.preview(2, 50, 50, null));
        assertTrue(c.cancel());
        assertEquals(0, c.items().get(0).page);
        assertEquals(DragTransaction.State.CANCEL, c.transaction().state());
    }
}
