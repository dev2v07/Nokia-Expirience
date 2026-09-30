package com.nokiaexperience.launcher;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.content.Context;
import android.telecom.TelecomManager;
import android.provider.Telephony;
import java.util.Locale;

/** Optional: Android grants this only after the user enables notification access. */
public final class NokiaNotificationListener extends NotificationListenerService {
    public static final class Item {
        public final String title,summary,source,key;
        public final long time;
        Item(String title,String summary,String source,String key,long time){this.title=title;this.summary=summary;this.source=source;this.key=key;this.time=time;}
    }
    private static volatile NokiaNotificationListener active;
    public void onListenerConnected() { active=this; sendUpdate(); }
    public void onListenerDisconnected() { active=null; sendUpdate(); }
    public void onNotificationPosted(StatusBarNotification notification) { sendUpdate(); }
    public void onNotificationRemoved(StatusBarNotification notification) { sendUpdate(); }
    private void sendUpdate() {
        android.content.Intent intent=new android.content.Intent("com.nokiaexperience.launcher.NOTIFICATIONS_CHANGED");
        intent.setPackage(getPackageName()); sendBroadcast(intent);
    }
    public static int activeCount() {
        NokiaNotificationListener listener=active;
        if (listener==null) return -1;
        try { StatusBarNotification[] items=listener.getActiveNotifications(); return items==null?-1:items.length; }
        catch (Exception ignored) { return -1; }
    }
    /** Only active notifications are indicators. No notification access means no guessed state. */
    public static int statusIndicators(Context context) {
        NokiaNotificationListener listener=active;
        if(listener==null)return 0;
        try {
            String smsPackage=Telephony.Sms.getDefaultSmsPackage(context);
            TelecomManager telecom=(TelecomManager)context.getSystemService(Context.TELECOM_SERVICE);
            String dialerPackage=telecom==null?null:telecom.getDefaultDialerPackage();
            int result=0;
            StatusBarNotification[] items=listener.getActiveNotifications();
            if(items==null)return 0;
            for(StatusBarNotification item:items){
                Notification notification=item.getNotification();
                String category=notification.category;
                String source=item.getPackageName();
                if((notification.flags&Notification.FLAG_ONGOING_EVENT)==0&&
                    (Notification.CATEGORY_MESSAGE.equals(category)||
                     (smsPackage!=null&&smsPackage.equals(source))))result|=1;
                CharSequence title=notification.extras.getCharSequence(Notification.EXTRA_TITLE);
                String lower=title==null?"":title.toString().toLowerCase(Locale.ROOT);
                boolean missedTitle=lower.contains("missed call")||lower.contains("пропущ");
                if("missed_call".equals(category)||
                    (missedTitle&&(Notification.CATEGORY_CALL.equals(category)||
                     (dialerPackage!=null&&dialerPackage.equals(source)))))result|=2;
            }
            return result;
        }catch(Exception ignored){return 0;}
    }
    public static String[] titles() {
        NokiaNotificationListener listener=active;
        if(listener==null)return new String[0];
        try {
            StatusBarNotification[] items=listener.getActiveNotifications();
            if(items==null)return new String[0];
            String[] titles=new String[Math.min(items.length,12)];
            for(int i=0;i<titles.length;i++) {
                CharSequence title=items[i].getNotification().extras.getCharSequence(android.app.Notification.EXTRA_TITLE);
                titles[i]=title==null?items[i].getPackageName():title.toString();
            }
            return titles;
        } catch(Exception ignored) { return new String[0]; }
    }
    public static Item[] items(){
        NokiaNotificationListener listener=active;if(listener==null)return new Item[0];
        try{
            StatusBarNotification[] notifications=listener.getActiveNotifications();
            if(notifications==null)return new Item[0];
            Item[] result=new Item[Math.min(20,notifications.length)];
            for(int i=0;i<result.length;i++){
                StatusBarNotification n=notifications[i];
                CharSequence title=n.getNotification().extras.getCharSequence(android.app.Notification.EXTRA_TITLE);
                CharSequence summary=n.getNotification().extras.getCharSequence(android.app.Notification.EXTRA_TEXT);
                result[i]=new Item(title==null?n.getPackageName():title.toString(),summary==null?"":summary.toString(),n.getPackageName(),n.getKey(),n.getPostTime());
            }
            return result;
        }catch(Exception ignored){return new Item[0];}
    }
    public static boolean open(int index){
        NokiaNotificationListener listener=active;if(listener==null)return false;
        try{
            StatusBarNotification[] notifications=listener.getActiveNotifications();
            if(index<0||index>=notifications.length)return false;
            android.app.PendingIntent action=notifications[index].getNotification().contentIntent;
            if(action==null)return false;action.send();return true;
        }catch(Exception ignored){return false;}
    }
    public static boolean clearAll(){
        NokiaNotificationListener listener=active;if(listener==null)return false;
        try{listener.cancelAllNotifications();return true;}catch(Exception ignored){return false;}
    }
}
