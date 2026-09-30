package com.nokiaexperience.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import android.content.SharedPreferences;

/** N95-inspired hierarchy; exact app UIDs are not Android launch targets. */
public final class MenuModel {
    public static final class Entry {
        public final String key;
        public final String label;
        public final String icon;
        public final ArrayList<Entry> children = new ArrayList<>();
        public final String packageName;
        public Entry(String key, String label, String icon) { this(key, label, icon, null); }
        public Entry(String key, String label, String icon, String packageName) {
            this.key=key; this.label=label; this.icon=icon; this.packageName=packageName;
        }
        public boolean folder() { return !children.isEmpty() || key.equals("installed") || key.startsWith("userfolder:"); }
        public Entry add(Entry... items) { Collections.addAll(children, items); return this; }
    }

    public final Entry root = new Entry("root", "Меню", "app");
    private final Context context;
    private final SharedPreferences prefs;
    private final ArrayList<Entry> installedCache=new ArrayList<>();
    public final MenuOrganizer organizer;

    public MenuModel(Context context,SharedPreferences prefs) {
        this.context=context.getApplicationContext();
        this.prefs=prefs;
        organizer=new MenuOrganizer(prefs);
        Entry music = new Entry("musicfolder", "Музыка", "music").add(
            app("music", "Плеер", "music"), app("radio", "Радио", "fm_radio"));
        Entry connectivity = new Entry("connectivity", "Связь", "sync").add(
            app("wifi", "Wi-Fi", "settings"), app("bluetooth", "Bluetooth", "settings"), app("network", "Сеть", "settings"));
        Entry utilities = new Entry("utilities", "Утилиты", "calculator").add(
            app("calculator", "Калькулятор", "calculator"), app("clock", "Часы", "clock"), app("files", "Файлы", "files"));
        Entry tools = new Entry("tools", "Средства", "settings").add(
            app("settings_android", "Настройки", "settings"), connectivity, utilities,
            app("calendar", "Календарь", "calendar"), app("camera", "Камера", "camera"));
        Entry media = new Entry("media", "Медиа", "gallery").add(
            app("gallery", "Галерея", "gallery"), app("camera", "Камера", "camera"), app("music", "Плеер", "music"));
        Entry office = new Entry("office", "Офис", "books").add(
            app("calendar", "Календарь", "calendar"), app("files", "Файлы", "files"), app("calculator", "Калькулятор", "calculator"));
        Entry apps = new Entry("applications", "Приложения", "app").add(
            app("clock", "Часы", "clock"), app("calendar", "Календарь", "calendar"),
            app("camera", "Камера", "camera"),
            new Entry("installed", "Все приложения", "app"));
        root.add(app("phone", "Телефон", "phone"), app("contacts", "Контакты", "contacts"),
            app("messages", "Сообщения", "mms"), app("camera", "Камера", "camera"),
            app("gallery", "Галерея", "gallery"), app("browser", "Браузер", "browser"),
            music, app("calendar", "Календарь", "calendar"), app("clock", "Часы", "clock"),
            app("files", "Файлы", "files"), tools, apps, media, office,
            app("settings_android", "Android Settings", "settings"),
            new Entry("installed", "Все приложения", "app"));
        refreshInstalled();
    }

    private static Entry app(String key,String label,String icon) { return new Entry(key,label,icon); }

    public void refreshInstalled(){
        installedCache.clear();
        Intent query=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm=context.getPackageManager();HashSet<String> seen=new HashSet<>();
        try{
            for(ResolveInfo info:pm.queryIntentActivities(query,0)){
                if(info.activityInfo==null||info.activityInfo.packageName.equals(context.getPackageName()))continue;
                String pkg=info.activityInfo.packageName,cls=info.activityInfo.name,key="installed:"+pkg+"/"+cls;
                if(seen.add(key)){CharSequence name=info.loadLabel(pm);installedCache.add(new Entry(key,name==null?pkg:name.toString(),"",pkg));}
            }
        }catch(Exception ignored){}
    }
    public List<Entry> children(Entry folder) {
        if(folder.key.equals("root")){
            ArrayList<Entry> all=new ArrayList<>(folder.children);
            if(!prefs.getBoolean("show_android_settings",true))all.removeIf(e->e.key.equals("settings_android"));
            for(java.util.Map.Entry<String,String> f:organizer.folders().entrySet())all.add(new Entry(f.getKey(),f.getValue(),"app"));
            Collections.sort(all,Comparator.comparingInt((Entry e)->organizer.rank(e.key)));
            return all;
        }
        if(!folder.key.equals("installed") && !folder.key.startsWith("userfolder:")) return folder.children;
        ArrayList<Entry> result = new ArrayList<>();
        for (Entry entry : installedCache) {
            String key=entry.key;
            String assigned=organizer.assignedFolder(key);
            boolean belongs=folder.key.equals("installed")?assigned==null:folder.key.equals(assigned);
            if (belongs && !organizer.hidden(key)) result.add(entry);
        }
        Collections.sort(result,Comparator.comparingInt((Entry e)->organizer.rank(e.key)).thenComparing(e->e.label.toLowerCase()));
        return result;
    }

    public List<Entry> search(String query){
        ArrayList<Entry> all=new ArrayList<>();
        all.addAll(children(new Entry("installed","", "")));
        for(java.util.Map.Entry<String,String> f:organizer.folders().entrySet())all.addAll(children(new Entry(f.getKey(),f.getValue(),"app")));
        ArrayList<Entry> found=new ArrayList<>();
        for(Entry e:all)if(e.label.toLowerCase().contains(query.toLowerCase()))found.add(e);
        Collections.sort(found,Comparator.comparing(e->e.label.toLowerCase()));return found;
    }
}
