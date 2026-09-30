package com.nokiaexperience.launcher;

import android.graphics.Canvas;
import android.graphics.RectF;

/** Uniform reference-canvas transform; view size already excludes Android window insets. */
public final class S60CanvasMapper {
    public final RectF bounds = new RectF();
    public float scale = 1f, offsetX, offsetY;
    public int referenceWidth, referenceHeight;

    public void update(int width, int height, VisualProfile profile) {
        boolean landscape = width > height;
        referenceWidth = profile.referenceWidth(landscape);
        referenceHeight = profile.referenceHeight(landscape);
        scale = Math.min(width/(float)referenceWidth, height/(float)referenceHeight);
        offsetX = (width-referenceWidth*scale)/2f;
        offsetY = (height-referenceHeight*scale)/2f;
        bounds.set(offsetX, offsetY, offsetX+referenceWidth*scale, offsetY+referenceHeight*scale);
    }
    public void enter(Canvas canvas) { canvas.save(); canvas.translate(offsetX,offsetY); canvas.scale(scale,scale); }
    public void exit(Canvas canvas) { canvas.restore(); }
    public float x(float screenX) { return (screenX-offsetX)/scale; }
    public float y(float screenY) { return (screenY-offsetY)/scale; }
    public boolean contains(float x,float y) { return bounds.contains(x,y); }
}
