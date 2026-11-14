package com.nokiaexperience.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import java.io.InputStream;

/** Launcher-only startup, never a system boot animation or lock screen. */
public final class StartupView extends View {
    private final int frameMs;
    private final Runnable onFinishing;
    private final Runnable complete;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final S60CanvasMapper mapper=new S60CanvasMapper();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private Bitmap current;
    private int frame=-1;
    private boolean finishing;
    private boolean holdLastFrame;
    private boolean framesComplete;
    private boolean audioComplete;
    private final long start=System.currentTimeMillis();
    private final Runnable tick=new Runnable(){
        public void run(){
            if(finishing)return;
            frame++;
            if(frame>=20){frame=19;framesComplete=true;invalidate();if(!holdLastFrame||audioComplete)finish();return;}
            loadFrame(frame);invalidate();handler.postDelayed(this,frameMs);
        }
    };
    public StartupView(Context context,int frameMs,Runnable onFinishing,Runnable complete){
        super(context);this.frameMs=frameMs;this.onFinishing=onFinishing;this.complete=complete;
        handler.post(tick);
    }
    public void setHoldLastFrame(boolean hold){holdLastFrame=hold;}
    public void audioFinished(){audioComplete=true;if(framesComplete)finish();}
    private void loadFrame(int index){
        if(current!=null){current.recycle();current=null;}
        String name=String.format(java.util.Locale.US,"n95/Startup_%03d.png",index);
        try(InputStream in=getContext().getAssets().open(name)){current=BitmapFactory.decodeStream(in);}
        catch(Exception ignored){finish();}
    }
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        if(current!=null){
            mapper.update(getWidth(),getHeight(),new VisualProfile.N95());
            paint.setAlpha(255);
            canvas.drawBitmap(current,null,new RectF(mapper.bounds),paint);
        }
    }
    public boolean onTouchEvent(MotionEvent event){
        if(event.getAction()==MotionEvent.ACTION_UP && System.currentTimeMillis()-start>250){finish();return true;}
        return true;
    }
    private void finish(){
        if(finishing)return;finishing=true;handler.removeCallbacks(tick);onFinishing.run();
        animate().alpha(0f).setDuration(220).setListener(new AnimatorListenerAdapter(){
            public void onAnimationEnd(Animator animation){if(current!=null){current.recycle();current=null;}complete.run();}
        }).start();
    }
    public void release(){handler.removeCallbacks(tick);animate().setListener(null);animate().cancel();if(current!=null){current.recycle();current=null;}}
}
