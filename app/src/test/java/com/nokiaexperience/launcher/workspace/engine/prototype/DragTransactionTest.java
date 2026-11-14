package com.nokiaexperience.launcher.workspace.engine.prototype;

import org.junit.Test;
import static org.junit.Assert.*;

public class DragTransactionTest {
    @Test public void previewDoesNotCommitAndCommitIsOneShot() {
        DragTransaction d = new DragTransaction();
        assertTrue(d.begin("item-1"));
        assertTrue(d.preview("cell-2"));
        assertEquals(DragTransaction.State.PREVIEW, d.state());
        assertTrue(d.commit());
        assertEquals(DragTransaction.State.COMMIT, d.state());
        assertFalse(d.commit());
        assertFalse(d.cancel());
    }

    @Test public void cancelIsSafeAndSessionCannotBeReusedWithoutReset() {
        DragTransaction d = new DragTransaction();
        assertTrue(d.begin("item-1"));
        assertTrue(d.cancel());
        assertFalse(d.cancel());
        assertFalse(d.begin("item-2"));
        d.reset();
        assertTrue(d.begin("item-2"));
    }

    @Test public void commitCanOnlyFollowAnActiveSession() {
        DragTransaction d = new DragTransaction();
        assertFalse(d.commit());
        assertFalse(d.preview("target"));
        assertTrue(d.begin("item"));
        assertTrue(d.commit());
    }
}
