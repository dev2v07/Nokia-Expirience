package com.nokiaexperience.launcher.workspace.engine.prototype;

import org.junit.Test;
import static org.junit.Assert.*;

public class PageManagerTest {
    @Test public void emptyStartsWithOnePage() {
        PageManager pages = new PageManager(0);
        assertEquals(1, pages.pageCount());
        assertEquals(0, pages.currentPage());
        assertFalse(pages.pageHasContent(0));
    }

    @Test public void navigationIsClamped() {
        PageManager pages = new PageManager(3);
        assertFalse(pages.goTo(-1));
        assertTrue(pages.goTo(2));
        assertFalse(pages.next());
        assertTrue(pages.previous());
        assertEquals(1, pages.currentPage());
    }

    @Test public void pagePreviewCanCommitOrCancel() {
        PageManager pages = new PageManager(1);
        assertTrue(pages.beginPageCreationPreview());
        assertEquals(1, pages.pageCount());
        assertEquals(Integer.valueOf(1), pages.previewPage());
        assertTrue(pages.cancelPageCreation());
        assertEquals(1, pages.pageCount());
        assertTrue(pages.beginPageCreationPreview());
        assertTrue(pages.confirmPageCreation());
        assertEquals(2, pages.pageCount());
        assertEquals(1, pages.currentPage());
    }

    @Test public void onlyEmptyPageCanBeDeletedAndOnePageRemains() {
        PageManager pages = new PageManager(2);
        pages.setPageContent(1, true);
        assertFalse(pages.deleteEmptyPage(1));
        pages.setPageContent(1, false);
        assertTrue(pages.deleteEmptyPage(1));
        assertEquals(1, pages.pageCount());
        assertFalse(pages.deleteEmptyPage(0));
    }
}
