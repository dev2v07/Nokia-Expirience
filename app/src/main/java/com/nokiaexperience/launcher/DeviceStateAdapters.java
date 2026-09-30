package com.nokiaexperience.launcher;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.media.session.MediaSessionManager;
import android.media.session.MediaController;
import android.media.MediaMetadata;
import android.content.ComponentName;
import java.util.List;

/** Real Android state only. Unknown values remain unknown. */
public final class DeviceStateAdapters {
    public int batteryPercent=-1;
    public boolean charging;
    public String connection="—";
    public String wifi="—";
    public String bluetooth="—";
    public String profile="—";
    public int notifications=-1;
    public int signalLevel=-1;
    public String track,artist;
    public boolean playing;
    private MediaController mediaController;

    @SuppressWarnings("deprecation")
    public void refresh(Context context) {
        try {
            Intent battery=context.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) {
                int level=battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
                int scale=battery.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
                batteryPercent=level>=0 && scale>0 ? Math.round(100f*level/scale) : -1;
                int status=battery.getIntExtra(BatteryManager.EXTRA_STATUS,-1);
                charging=status==BatteryManager.BATTERY_STATUS_CHARGING || status==BatteryManager.BATTERY_STATUS_FULL;
            }
        } catch (Exception ignored) { batteryPercent=-1; }
        try {
            ConnectivityManager manager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo network=manager==null?null:manager.getActiveNetworkInfo();
            if (network == null || !network.isConnected()) { connection="Нет сети"; wifi="Нет"; }
            else if (network.getType()==ConnectivityManager.TYPE_WIFI) { connection="Сеть"; wifi="Wi-Fi"; }
            else { connection="Сеть"; wifi="Нет"; }
        } catch (Exception ignored) { connection="—"; wifi="—"; }
        try {
            BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
            bluetooth=adapter==null?"Нет":adapter.isEnabled()?"Вкл":"Выкл";
        } catch (Exception ignored) { bluetooth="—"; }
        try {
            AudioManager audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
            profile=audio==null?"—":audio.getRingerMode()==AudioManager.RINGER_MODE_SILENT?"Без звука":audio.getRingerMode()==AudioManager.RINGER_MODE_VIBRATE?"Вибрация":"Обычный";
        } catch (Exception ignored) { profile="—"; }
        notifications=NokiaNotificationListener.activeCount();
        track=null;artist=null;playing=false;mediaController=null;
        try{
            MediaSessionManager sessions=(MediaSessionManager)context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            List<MediaController> controllers=sessions.getActiveSessions(new ComponentName(context,NokiaNotificationListener.class));
            for(MediaController controller:controllers){
                MediaMetadata metadata=controller.getMetadata();
                if(metadata==null)continue;
                String title=metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                if(title==null||title.isEmpty())continue;
                mediaController=controller;track=title;artist=metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                playing=controller.getPlaybackState()!=null&&controller.getPlaybackState().getState()==android.media.session.PlaybackState.STATE_PLAYING;
                break;
            }
        }catch(Exception ignored){}
    }
    public boolean mediaAction(String action){
        MediaController controller=mediaController;if(controller==null)return false;
        try{
            if(action.equals("previous"))controller.getTransportControls().skipToPrevious();
            else if(action.equals("next"))controller.getTransportControls().skipToNext();
            else if(playing)controller.getTransportControls().pause();else controller.getTransportControls().play();
            return true;
        }catch(Exception ignored){return false;}
    }
}
