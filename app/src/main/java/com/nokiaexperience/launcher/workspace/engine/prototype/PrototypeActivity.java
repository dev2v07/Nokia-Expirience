package com.nokiaexperience.launcher.workspace.engine.prototype;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Standalone in-memory prototype screen. It is reachable only by its explicit debug intent
 * and deliberately does not construct or access the production launcher models.
 */
public final class PrototypeActivity extends Activity {
    private final PageManager pages = new PageManager(3);
    private final ScrollController scroll = new ScrollController(new ScrollController.Config(72f, 900f));
    private final GestureArbitrator gestures = new GestureArbitrator();
    private final DragTransaction drag = new DragTransaction();
    private final DragController dragEngine = new DragController(drag);
    private WorkspaceSurface surface;
    private TextView pageStatus;
    private TextView gestureStatus;
    private TextView dragStatus;
    private TextView diagnosticStatus;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Workspace Engine Prototype");
        buildUi();
    }

    private void buildUi() {
        ScrollView outer = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(Color.rgb(24, 28, 34));
        outer.addView(root);

        TextView title = text("Isolated Home Workspace Engine", 22, Color.WHITE);
        root.addView(title, matchWrap());
        root.addView(text("In-memory prototype • no Nokia data or widgets", 13, Color.LTGRAY), matchWrap());

        pageStatus = text("", 16, Color.WHITE);
        root.addView(pageStatus, matchWrap());
        surface = new WorkspaceSurface(this, pages, scroll, gestures, dragEngine);
        surface.setListener(value -> {
            if (diagnosticStatus != null) diagnosticStatus.setText("Touch diagnostic: " + value);
            updateStatuses();
        });
        root.addView(surface, new LinearLayout.LayoutParams(-1, dp(390)));

        LinearLayout pageButtons = row();
        pageButtons.addView(button("‹ Page", v -> movePage(-1)), weight());
        pageButtons.addView(button("Page ›", v -> movePage(1)), weight());
        root.addView(pageButtons, matchWrap());

        LinearLayout creation = row();
        creation.addView(button("Preview +", v -> previewPage()), weight());
        creation.addView(button("Confirm page", v -> confirmPage()), weight());
        creation.addView(button("Cancel page", v -> cancelPage()), weight());
        creation.addView(button("Delete empty", v -> deletePage()), weight());
        root.addView(creation, matchWrap());

        gestureStatus = text("Gesture: IDLE", 15, Color.CYAN);
        root.addView(gestureStatus, matchWrap());
        LinearLayout gestureButtons = row();
        gestureButtons.addView(button("Swipe", v -> simulateSwipe()), weight());
        gestureButtons.addView(button("Drag", v -> simulateDrag()), weight());
        gestureButtons.addView(button("Widget touch", v -> simulateWidgetTouch()), weight());
        gestureButtons.addView(button("Cancel", v -> cancelGesture()), weight());
        root.addView(gestureButtons, matchWrap());

        dragStatus = text("Drag transaction: IDLE", 15, Color.YELLOW);
        root.addView(dragStatus, matchWrap());
        diagnosticStatus = text("Touch diagnostic: IDLE", 13, Color.LTGRAY);
        root.addView(diagnosticStatus, matchWrap());
        LinearLayout dragButtons = row();
        dragButtons.addView(button("Begin", v -> beginDrag()), weight());
        dragButtons.addView(button("Preview", v -> previewDrag()), weight());
        dragButtons.addView(button("Commit", v -> commitDrag()), weight());
        dragButtons.addView(button("Cancel", v -> cancelDrag()), weight());
        root.addView(dragButtons, matchWrap());

        setContentView(outer);
        render();
    }

    private void movePage(int delta) {
        if (delta < 0) pages.previous(); else pages.next();
        render();
    }

    private void previewPage() { pages.beginPageCreationPreview(); render(); }
    private void confirmPage() { pages.confirmPageCreation(); render(); }
    private void cancelPage() { pages.cancelPageCreation(); render(); }
    private void deletePage() { pages.deleteEmptyPage(pages.currentPage()); render(); }

    private void simulateSwipe() {
        gestures.reset(); gestures.pointerDown(false); gestures.move(-40, 0, 8); gestures.end(); updateStatuses();
    }
    private void simulateDrag() {
        gestures.reset(); gestures.pointerDown(false); gestures.longPress(); gestures.beginDrag(); updateStatuses();
    }
    private void simulateWidgetTouch() {
        gestures.reset(); gestures.pointerDown(true); gestures.move(-100, 0, 8); updateStatuses();
    }
    private void cancelGesture() { gestures.cancel(); gestures.reset(); updateStatuses(); }

    private void beginDrag() { dragEngine.reset(); dragEngine.begin("card-a"); updateStatuses(); }
    private void previewDrag() { dragEngine.preview(pages.currentPage(), dp(28), dp(74), null); updateStatuses(); }
    private void commitDrag() { dragEngine.commit(); updateStatuses(); }
    private void cancelDrag() { if (dragEngine.active() != null) dragEngine.cancel(); updateStatuses(); }

    private void render() {
        PageManager.Snapshot snapshot = pages.snapshot();
        pageStatus.setText("Page " + (snapshot.currentPage + 1) + " / " + snapshot.committedPageCount +
            (snapshot.previewPage == null ? "" : "  • preview page pending"));
        surface.requestLayout();
        surface.invalidate();
        updateStatuses();
    }

    private void updateStatuses() {
        if (gestureStatus != null) gestureStatus.setText("Gesture: " + gestures.state());
        if (dragStatus != null) dragStatus.setText("Drag transaction: " + drag.state());
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); view.setPadding(dp(6), dp(6), dp(6), dp(6)); return view;
    }
    private Button button(String label, View.OnClickListener listener) { Button b = new Button(this); b.setText(label); b.setOnClickListener(listener); return b; }
    private LinearLayout row() { LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); return row; }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, -2, 1f); }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
