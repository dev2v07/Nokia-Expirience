package com.nokiaexperience.launcher.workspace.engine.prototype;

import java.util.ArrayList;
import java.util.List;

/** In-memory page state for the isolated prototype. It has no Android or persistence dependency. */
public final class PageManager {
    public static final class Snapshot {
        public final int committedPageCount;
        public final int currentPage;
        public final Integer previewPage;
        public final List<Boolean> pageContent;

        private Snapshot(int count, int current, Integer preview, List<Boolean> content) {
            committedPageCount = count;
            currentPage = current;
            previewPage = preview;
            pageContent = content;
        }
    }

    private final ArrayList<Boolean> content = new ArrayList<>();
    private int currentPage;
    private Integer previewPage;

    public PageManager() { this(1); }

    public PageManager(int initialPageCount) {
        int count = Math.max(1, initialPageCount);
        for (int i = 0; i < count; i++) content.add(false);
    }

    public int pageCount() { return content.size(); }
    public int currentPage() { return currentPage; }
    public Integer previewPage() { return previewPage; }

    public boolean goTo(int page) {
        if (page < 0 || page >= content.size()) return false;
        currentPage = page;
        return true;
    }

    public boolean next() { return goTo(currentPage + 1); }
    public boolean previous() { return goTo(currentPage - 1); }

    public boolean pageHasContent(int page) {
        return page >= 0 && page < content.size() && content.get(page);
    }

    public void setPageContent(int page, boolean hasContent) {
        if (page >= 0 && page < content.size()) content.set(page, hasContent);
    }

    /** Creates a transient page preview. It is not part of committed pageCount. */
    public boolean beginPageCreationPreview() {
        if (previewPage != null) return false;
        previewPage = content.size();
        return true;
    }

    public boolean confirmPageCreation() {
        if (previewPage == null) return false;
        content.add(false);
        currentPage = content.size() - 1;
        previewPage = null;
        return true;
    }

    public boolean cancelPageCreation() {
        if (previewPage == null) return false;
        previewPage = null;
        return true;
    }

    /** Deletes only an empty committed page; the minimum workspace page remains. */
    public boolean deleteEmptyPage(int page) {
        if (content.size() <= 1 || page < 0 || page >= content.size() || pageHasContent(page)) return false;
        content.remove(page);
        if (currentPage >= content.size()) currentPage = content.size() - 1;
        else if (currentPage > page) currentPage--;
        if (previewPage != null && previewPage > page) previewPage--;
        return true;
    }

    public Snapshot snapshot() {
        return new Snapshot(content.size(), currentPage, previewPage,
            new ArrayList<>(content));
    }
}
