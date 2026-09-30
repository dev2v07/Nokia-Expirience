package com.nokiaexperience.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

/** Original N95 multimedia-menu cue, only inside the launcher UI. */
public final class NokiaSounds {
    private final Context context;
    private final SharedPreferences prefs;
    private SoundPool pool;
    private int select;
    public NokiaSounds(Context context,SharedPreferences prefs){this.context=context.getApplicationContext();this.prefs=prefs;init();}
    private void init(){
        try{
            pool=new SoundPool.Builder().setMaxStreams(2).setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
            try(AssetFileDescriptor file=context.getAssets().openFd("sounds/multimediamenuselect.wav")){
                select=pool.load(file,1);
            }
        }catch(Exception ignored){release();}
    }
    public void tap(){
        if(!prefs.getBoolean("ui_sounds",true))return;
        AudioManager audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if(audio!=null&&audio.getRingerMode()!=AudioManager.RINGER_MODE_NORMAL)return;
        if(pool==null)init();
        if(pool!=null&&select>0)try{pool.play(select,0.35f,0.35f,1,0,1f);}catch(Exception ignored){}
    }
    public void release(){if(pool!=null){pool.release();pool=null;}select=0;}
}
