package com.nokiaexperience.launcher.workspace.engine.prototype;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

/**
 * Real Android touch surface for the isolated prototype. It owns no production launcher data.
 * The surface deliberately keeps model mutation behind DragController commit/cancel boundaries.
 */
public final class WorkspaceSurface extends ViewGroup {
    private static final long EDGE_HOVER_TIMEOUT_MS = 650L;
    public interface Listener { void onWorkspaceChanged(String diagnostic); }

    private final PageManager pages;
    private final ScrollController scroll;
    private final GestureArbitrator gestures;
    private final DragController drag;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, TextView> cards = new HashMap<>();
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int touchSlop;
    private final int longPressTimeout;
    private final int edgeSize;
    private Listener listener;
    private float downX;
    private float downY;
    private int pointerId = MotionEvent.INVALID_POINTER_ID;
    private String hitId;
    private long longPressAt;
    private long edgeSince;
    private boolean edgeTriggered;
    private String lastDrop = "none";
    private String lastDiagnostic = "IDLE";

    public WorkspaceSurface(Context context, PageManager pages, ScrollController scroll,
                            GestureArbitrator gestures, DragController drag) {
        super(context);
        this.pages = pages; this.scroll = scroll; this.gestures = gestures; this.drag = drag;
        setWillNotDraw(false);
        setBackgroundColor(Color.rgb(24, 28, 34));
        ViewConfiguration config = ViewConfiguration.get(context);
        touchSlop = config.getScaledTouchSlop();
        longPressTimeout = ViewConfiguration.getLongPressTimeout();
        edgeSize = dp(56);
        gridPaint.setColor(Color.rgb(55, 67, 82));
        gridPaint.setStrokeWidth(dp(1));
        addCard("card-a", "CARD A");
        addCard("card-b", "CARD B");
        addCard("card-c", "CARD C");
        drag.addItem("card-a", 0, dp(28), dp(74));
        drag.addItem("card-b", 0, dp(250), dp(74));
        drag.addItem("card-c", 1, dp(28), dp(74));
    }

    public void setListener(Listener listener) { this.listener = listener; }
    public String diagnostic() { return lastDiagnostic; }
    public String lastDrop() { return lastDrop; }
    public int longPressTimeoutMs() { return longPressTimeout; }

    private void addCard(String id, String label) {
        TextView card = new TextView(getContext());
        card.setText(label + "\nlong press + drag");
        card.setTextColor(Color.WHITE); card.setTextSize(17); card.setGravity(android.view.Gravity.CENTER);
        card.setFocusable(false); card.setClickable(false); card.setBackground(cardBackground(Color.rgb(46, 58, 72)));
        cards.put(id, card); addView(card, new LayoutParams(dp(200), dp(130)));
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = Math.max(dp(360), MeasureSpec.getSize(heightSpec));
        setMeasuredDimension(width, height);
        for (int i = 0; i < getChildCount(); i++) getChildAt(i).measure(
            MeasureSpec.makeMeasureSpec(dp(200), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(dp(130), MeasureSpec.EXACTLY));
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (DragController.Item item : drag.items()) {
            TextView card = cards.get(item.id);
            boolean visible = item.page == pages.currentPage();
            if (drag.active() != null && item == drag.active()) visible = true;
            card.setVisibility(visible ? VISIBLE : INVISIBLE);
            int page = item.page;
            float x = item.x; float y = item.y;
            if (drag.active() == item) { page = drag.previewPage(); x = drag.previewX(); y = drag.previewY(); }
            if (page != pages.currentPage() && drag.active() != item) continue;
            int l = Math.round(x), t = Math.round(y);
            card.layout(l, t, l + dp(200), t + dp(130));
            card.setBackground(cardBackground(drag.active() == item ? Color.rgb(0, 145, 170) : Color.rgb(46, 58, 72)));
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth(), height = getHeight();
        canvas.drawText("Page " + (pages.currentPage() + 1) + " / " + pages.pageCount(), dp(16), dp(30), textPaint(Color.WHITE, dp(22)));
        canvas.drawText("Touch cards: long press uses " + longPressTimeout + " ms", dp(16), dp(54), textPaint(Color.LTGRAY, dp(13)));
        for (int x = dp(20); x < width; x += dp(74)) canvas.drawLine(x, dp(66), x, height - dp(10), gridPaint);
        for (int y = dp(70); y < height; y += dp(65)) canvas.drawLine(dp(10), y, width - dp(10), y, gridPaint);
        Paint diagnostic = textPaint(Color.CYAN, dp(11));
        canvas.drawText("Owner: " + gestures.state() + "  Drag: " + drag.transaction().state(), dp(16), height - dp(44), diagnostic);
        canvas.drawText("Drop: " + lastDrop + "  pointer: " + pointerId, dp(16), height - dp(27), diagnostic);
        canvas.drawText("Event: " + lastDiagnostic, dp(16), height - dp(10), diagnostic);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) { return true; }

    @Override public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) return down(event);
        if (action == MotionEvent.ACTION_MOVE) return move(event);
        // ACTION_UP and ACTION_CANCEL terminate the active sequence even when the
        // platform reports a changed action index after pointer bookkeeping.
        if (action == MotionEvent.ACTION_UP) return up(event);
        if (action == MotionEvent.ACTION_CANCEL) { cancel("ACTION_CANCEL"); return true; }
        return true;
    }

    private boolean down(MotionEvent event) {
        pointerId = event.getPointerId(0); downX = event.getX(); downY = event.getY();
        hitId = hitCard(downX, downY); edgeSince = 0; edgeTriggered = false;
        boolean owns = gestures.pointerDown(false, hitId != null);
        if (!owns) return false;
        // The outer diagnostic ScrollView must not steal this pointer sequence.
        requestDisallowInterceptTouchEvent(true);
        scroll.begin();
        longPressAt = System.currentTimeMillis() + longPressTimeout;
        if (hitId != null) {
            handler.postDelayed(() -> {
                if (pointerId != MotionEvent.INVALID_POINTER_ID && gestures.longPress()) {
                    longPressAt = System.currentTimeMillis();
                    gestures.beginDrag();
                    drag.reset(); drag.begin(hitId);
                    requestDisallowInterceptTouchEvent(true);
                    publish("LONG_PRESS -> DRAG");
                }
            }, longPressTimeout);
        }
        publish("POSSIBLE_" + (hitId == null ? "SWIPE" : "DRAG"));
        return true;
    }

    private boolean move(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        float dx = x - downX, dy = y - downY;
        GestureArbitrator.State state = gestures.state();
        if (state == GestureArbitrator.State.POSSIBLE_SWIPE || state == GestureArbitrator.State.POSSIBLE_DRAG) {
            gestures.move(dx, dy, touchSlop);
            if (gestures.state() == GestureArbitrator.State.SWIPE) {
                handler.removeCallbacksAndMessages(null); publish("SWIPE");
            } else if (Math.max(Math.abs(dx), Math.abs(dy)) > touchSlop && gestures.state() == GestureArbitrator.State.CANCELLED) {
                handler.removeCallbacksAndMessages(null); scroll.cancel(); scroll.reset(); publish("CANCELLED");
            }
            return true;
        }
        if (gestures.state() == GestureArbitrator.State.DRAG) {
            edgeHover(x);
            String target = hitCard(x, y);
            if (hitId != null && hitId.equals(target)) target = null;
            drag.preview(pages.currentPage(), clamp(x - dp(100), 0, Math.max(0, getWidth() - dp(200))),
                clamp(y - dp(65), dp(66), Math.max(dp(66), getHeight() - dp(140))), target);
            publish("DRAG PREVIEW"); invalidate(); requestLayout();
        }
        return true;
    }

    private boolean up(MotionEvent event) {
        handler.removeCallbacksAndMessages(null);
        if (gestures.state() == GestureArbitrator.State.SWIPE) {
            ScrollController.Decision decision = scroll.finish(pages.currentPage(), pages.pageCount(), event.getX() - downX, 0);
            if (decision.moved) pages.goTo(decision.targetPage);
            gestures.end(); lastDrop = "swipe -> page " + (pages.currentPage() + 1); publish("SWIPE END");
        } else if (gestures.state() == GestureArbitrator.State.DRAG) {
            String target = hitCard(event.getX(), event.getY());
            if (hitId != null && hitId.equals(target)) target = null;
            drag.preview(pages.currentPage(), clamp(event.getX() - dp(100), 0, Math.max(0, getWidth() - dp(200))),
                clamp(event.getY() - dp(65), dp(66), Math.max(dp(66), getHeight() - dp(140))), target);
            boolean committed = drag.commit();
            gestures.end(); lastDrop = committed ? "COMMIT" + (target == null ? " free" : " swap") : "rejected";
            publish("DROP " + lastDrop);
        } else {
            if (gestures.state() == GestureArbitrator.State.LONG_PRESS) gestures.end();
            else if (gestures.state() == GestureArbitrator.State.POSSIBLE_SWIPE || gestures.state() == GestureArbitrator.State.POSSIBLE_DRAG) gestures.end();
            scroll.cancel(); scroll.reset(); publish("TAP/END");
        }
        pointerId = MotionEvent.INVALID_POINTER_ID; hitId = null; requestDisallowInterceptTouchEvent(false);
        render(); return true;
    }

    private void cancel(String reason) {
        handler.removeCallbacksAndMessages(null);
        if (drag.active() != null) drag.cancel();
        gestures.cancel(); gestures.reset(); scroll.cancel(); scroll.reset();
        lastDrop = "CANCEL (" + reason + ")"; pointerId = MotionEvent.INVALID_POINTER_ID; hitId = null;
        requestDisallowInterceptTouchEvent(false); publish(lastDrop); render();
    }

    private void edgeHover(float x) {
        boolean left = x < edgeSize, right = x > getWidth() - edgeSize;
        if (!left && !right) { edgeSince = 0; edgeTriggered = false; return; }
        if (edgeSince == 0) edgeSince = System.currentTimeMillis();
        if (!edgeTriggered && System.currentTimeMillis() - edgeSince >= EDGE_HOVER_TIMEOUT_MS) {
            int old = pages.currentPage();
            if (left) pages.previous(); else pages.next();
            edgeTriggered = old != pages.currentPage();
            if (edgeTriggered) publish("EDGE_HOVER -> page " + (pages.currentPage() + 1));
        }
    }

    private String hitCard(float x, float y) {
        for (DragController.Item item : drag.items()) {
            if (item.page != pages.currentPage() || item.id.equals(hitId)) continue;
            if (x >= item.x && x <= item.x + dp(200) && y >= item.y && y <= item.y + dp(130)) return item.id;
        }
        return null;
    }

    private void render() { requestLayout(); invalidate(); }
    private void publish(String value) { lastDiagnostic = value; if (listener != null) listener.onWorkspaceChanged(value); invalidate(); }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    private GradientDrawable cardBackground(int color) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(8)); return d; }
    private Paint textPaint(int color, float size) { Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(color); p.setTextSize(size); return p; }
}
