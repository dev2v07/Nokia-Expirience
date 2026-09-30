package com.nokiaexperience.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

/** Semantic asset slots. Manual app icons are provisional; MBM art is tagged by source. */
public final class AssetRepository {
    private final Context context;
    private final LruCache<String, Bitmap> cache = new LruCache<String,Bitmap>(12*1024) {
        protected int sizeOf(String key, Bitmap value) { return Math.max(1,value.getByteCount()/1024); }
    };
    public AssetRepository(Context context) { this.context=context.getApplicationContext(); }

    public Bitmap getManualIcon(String key) {
        if (key == null || key.isEmpty()) return null;
        String slot = "manual:"+key;
        Bitmap bitmap=cache.get(slot);
        if (bitmap != null) return bitmap;
        try (java.io.InputStream stream=context.getAssets().open("manual/"+key+".png")) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig=Bitmap.Config.ARGB_8888;
            bitmap=BitmapFactory.decodeStream(stream,null,options);
            if (bitmap != null) cache.put(slot,bitmap);
        } catch (Exception ignored) { }
        return bitmap;
    }

    public Bitmap getN95Bitmap(String name) {
        String slot="n95:"+name;
        Bitmap bitmap=cache.get(slot);
        if (bitmap != null) return bitmap;
        try (java.io.InputStream stream=context.getAssets().open("n95/"+name+".png")) {
            bitmap=BitmapFactory.decodeStream(stream);
            if (bitmap != null) cache.put(slot,bitmap);
        } catch (Exception ignored) { }
        return bitmap;
    }

    public Bitmap installedIcon(String packageName) {
        if (packageName == null) return null;
        String slot="installed:"+packageName;
        Bitmap bitmap=cache.get(slot);
        if (bitmap != null) return bitmap;
        try {
            Drawable drawable=context.getPackageManager().getApplicationIcon(packageName);
            if (drawable instanceof BitmapDrawable) bitmap=((BitmapDrawable)drawable).getBitmap();
            else {
                bitmap=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);
                android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);
                drawable.setBounds(0,0,96,96); drawable.draw(canvas);
            }
            if (bitmap != null) cache.put(slot,bitmap);
        } catch (Exception ignored) { }
        return bitmap;
    }
}
