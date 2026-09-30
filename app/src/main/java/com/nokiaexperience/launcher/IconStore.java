package com.nokiaexperience.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.LruCache;
import java.io.InputStream;
import java.util.Locale;

/** Nokia manual artwork shared by both profiles; per-app SAF overrides win. */
public final class IconStore {
    private final Context context;
    private final SharedPreferences prefs;
    private final AssetRepository assets;
    private final LruCache<String,Bitmap> custom=new LruCache<String,Bitmap>(8*1024){
        @Override protected int sizeOf(String key,Bitmap image){return Math.max(1,image.getByteCount()/1024);}
    };
    public IconStore(Context context,SharedPreferences prefs,AssetRepository assets){
        this.context=context.getApplicationContext();this.prefs=prefs;this.assets=assets;
    }
    public void set(String key,Uri uri){prefs.edit().putString("icon_"+key,uri.toString()).apply();custom.remove(key);}
    public void reset(String key){prefs.edit().remove("icon_"+key).apply();custom.remove(key);}
    public Drawable icon(MenuModel.Entry entry){
        Bitmap image=custom(entry.key);
        if(image==null)image=assets.getManualIcon(semantic(entry));
        if(image!=null)return new BitmapDrawable(context.getResources(),image);
        if(entry.packageName!=null){
            Bitmap installed=assets.installedIcon(entry.packageName);
            if(installed!=null)return new BitmapDrawable(context.getResources(),installed);
        }
        return context.getPackageManager().getDefaultActivityIcon();
    }
    private Bitmap custom(String key){
        String value=prefs.getString("icon_"+key,null);
        if(value==null)return null;
        Bitmap cached=custom.get(key);if(cached!=null)return cached;
        BitmapFactory.Options probe=new BitmapFactory.Options();probe.inJustDecodeBounds=true;
        try(InputStream in=context.getContentResolver().openInputStream(Uri.parse(value))){BitmapFactory.decodeStream(in,null,probe);}
        catch(Exception ignored){return null;}
        try(InputStream in=context.getContentResolver().openInputStream(Uri.parse(value))){
            BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
            while(probe.outWidth/options.inSampleSize>256||probe.outHeight/options.inSampleSize>256)options.inSampleSize*=2;
            Bitmap bitmap=BitmapFactory.decodeStream(in,null,options);
            if(bitmap!=null){custom.put(key,bitmap);return bitmap;}
        }catch(Exception ignored){}
        return null;
    }
    private String semantic(MenuModel.Entry e){
        if(e.folder())return "files";
        switch(e.key){
            case "phone":return "contacts";
            case "messages":return "mms";
            case "files":return "files";
            case "settings_launcher":case "settings_android":return "settings";
            case "chrome":case "browser":return "browser";
            default:break;
        }
        if(!e.icon.isEmpty())return e.icon;
        String label=e.label.toLowerCase(Locale.ROOT);
        String pkg=e.packageName==null?"":e.packageName.toLowerCase(Locale.ROOT);
        if(label.contains("chrome")||label.contains("browser")||pkg.contains("chrome"))return "browser";
        if(label.contains("camera")||label.contains("камера"))return "camera";
        if(label.contains("contact")||label.contains("контакт")||label.contains("phone")||label.contains("телефон"))return "contacts";
        if(label.contains("message")||label.contains("сообщ"))return "mms";
        if(label.contains("gallery")||label.contains("галере"))return "gallery";
        if(label.contains("music")||label.contains("музык"))return "music";
        if(label.contains("calendar")||label.contains("календар"))return "calendar";
        if(label.contains("clock")||label.contains("часы"))return "clock";
        if(label.contains("setting")||label.contains("настрой"))return "settings";
        if(label.contains("file")||label.contains("файл"))return "files";
        return "";
    }
}
