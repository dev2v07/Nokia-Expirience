package com.nokiaexperience.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;

/** No vendor package names; every external action is checked before launch. */
public final class AndroidActionResolver {
    private final Activity activity;
    public AndroidActionResolver(Activity activity) { this.activity=activity; }

    public void launch(MenuModel.Entry entry) {
        if (entry.key.equals("settings_launcher")) { if (activity instanceof LauncherActivity) ((LauncherActivity)activity).openSettings(); return; }
        Intent intent=intentFor(entry);
        if (intent != null && launchChecked(intent)) return;
        new AlertDialog.Builder(activity).setTitle(entry.label)
            .setMessage("Совместимое приложение на этом устройстве не найдено.")
            .setPositiveButton("Настройки Android",(d,w)->launchChecked(new Intent(Settings.ACTION_SETTINGS)))
            .setNegativeButton("Отмена",null).show();
    }

    private boolean launchChecked(Intent intent) {
        try {
            activity.startActivity(intent); return true;
        } catch (RuntimeException ignored) { return false; }
    }

    private Intent category(String category) { return new Intent(Intent.ACTION_MAIN).addCategory(category); }

    private Intent intentFor(MenuModel.Entry entry) {
        String key=entry.key;
        if (key.startsWith("installed:")) {
            int slash=key.indexOf('/');
            if (slash>10) return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(new ComponentName(key.substring(10,slash),key.substring(slash+1)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        }
        switch (key) {
            case "phone": return new Intent(Intent.ACTION_DIAL);
            case "contacts": return new Intent(Intent.ACTION_VIEW,ContactsContract.Contacts.CONTENT_URI);
            case "messages": return category("android.intent.category.APP_MESSAGING");
            case "camera": return new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            case "gallery": return new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
            case "browser": return new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com"));
            case "calendar": return category("android.intent.category.APP_CALENDAR");
            case "clock": return category("android.intent.category.APP_CLOCK");
            case "music": return category("android.intent.category.APP_MUSIC");
            case "files": return new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
            case "settings_android": return new Intent(Settings.ACTION_SETTINGS);
            case "wifi": return new Intent(Settings.ACTION_WIFI_SETTINGS);
            case "bluetooth": return new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            case "network": return new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            case "radio": return category("android.intent.category.APP_MUSIC");
            case "calculator": return category("android.intent.category.APP_CALCULATOR");
            default: return null;
        }
    }
}
