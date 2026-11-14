package com.nokiaexperience.launcher.workspace.engine.prototype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** In-memory drag model. Preview coordinates never mutate committed item positions. */
public final class DragController {
    public static final class Item {
        public final String id;
        public int page;
        public float x;
        public float y;

        private Item(String id, int page, float x, float y) {
            this.id = id; this.page = page; this.x = x; this.y = y;
        }
    }

    private final DragTransaction transaction;
    private final ArrayList<Item> items = new ArrayList<>();
    private Item active;
    private int originalPage;
    private float originalX;
    private float originalY;
    private int previewPage;
    private float previewX;
    private float previewY;
    private String previewTarget;

    public DragController(DragTransaction transaction) { this.transaction = transaction; }

    public void addItem(String id, int page, float x, float y) {
        if (find(id) != null) throw new IllegalArgumentException("duplicate item: " + id);
        items.add(new Item(id, page, x, y));
    }

    public List<Item> items() { return Collections.unmodifiableList(items); }
    public DragTransaction transaction() { return transaction; }
    public Item active() { return active; }
    public int previewPage() { return previewPage; }
    public float previewX() { return previewX; }
    public float previewY() { return previewY; }
    public String previewTarget() { return previewTarget; }
    public int originalPage() { return originalPage; }
    public float originalX() { return originalX; }
    public float originalY() { return originalY; }

    public boolean begin(String id) {
        if (active != null || !transaction.begin(id)) return false;
        active = find(id);
        if (active == null) { transaction.cancel(); return false; }
        originalPage = active.page; originalX = active.x; originalY = active.y;
        previewPage = originalPage; previewX = originalX; previewY = originalY; previewTarget = null;
        return true;
    }

    public boolean preview(int page, float x, float y, String targetId) {
        if (active == null || (transaction.state() != DragTransaction.State.ACTIVE
            && transaction.state() != DragTransaction.State.PREVIEW)) return false;
        previewPage = page; previewX = x; previewY = y; previewTarget = targetId;
        return transaction.preview(targetId == null ? "free-page-" + page : targetId);
    }

    /** Commits once. A drop on another item swaps the two committed positions. */
    public boolean commit() {
        if (active == null || (transaction.state() != DragTransaction.State.ACTIVE
            && transaction.state() != DragTransaction.State.PREVIEW)) return false;
        Item target = previewTarget == null ? null : find(previewTarget);
        if (target != null && target != active) {
            int p = target.page; float x = target.x; float y = target.y;
            target.page = originalPage; target.x = originalX; target.y = originalY;
            active.page = p; active.x = x; active.y = y;
        } else {
            active.page = previewPage; active.x = previewX; active.y = previewY;
        }
        boolean committed = transaction.commit();
        if (committed) active = null;
        return committed;
    }

    public boolean cancel() {
        if (active == null) return false;
        boolean cancelled = transaction.cancel();
        if (cancelled) active = null;
        return cancelled;
    }

    public void reset() {
        if (active != null) throw new IllegalStateException("active drag");
        transaction.reset();
    }

    private Item find(String id) {
        for (Item item : items) if (item.id.equals(id)) return item;
        return null;
    }
}
