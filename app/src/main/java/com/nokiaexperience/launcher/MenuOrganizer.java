package com.nokiaexperience.launcher;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistent launcher-only organisation. Package discovery always comes from PackageManager. */
public final class MenuOrganizer {
    private final SharedPreferences prefs;
    private final HashSet<String> hidden=new HashSet<>();
    private final LinkedHashMap<String,String> folders=new LinkedHashMap<>();
    private final HashMap<String,String> assignment=new HashMap<>();
    private final ArrayList<String> order=new ArrayList<>();
    public MenuOrganizer(SharedPreferences prefs){this.prefs=prefs;load();}
    private void load(){
        try{
            JSONObject root=new JSONObject(prefs.getString("menu_state","{}"));
            JSONArray h=root.optJSONArray("hidden");if(h!=null)for(int i=0;i<h.length();i++)hidden.add(h.optString(i));
            JSONObject f=root.optJSONObject("folders");if(f!=null){java.util.Iterator<String> it=f.keys();while(it.hasNext()){String key=it.next();folders.put(key,f.optString(key));}}
            JSONObject a=root.optJSONObject("assignment");if(a!=null){java.util.Iterator<String> it=a.keys();while(it.hasNext()){String key=it.next();assignment.put(key,a.optString(key));}}
            JSONArray o=root.optJSONArray("order");if(o!=null)for(int i=0;i<o.length();i++)order.add(o.optString(i));
        }catch(Exception ignored){}
    }
    private void save(){
        try{
            JSONObject root=new JSONObject(),f=new JSONObject(),a=new JSONObject();JSONArray h=new JSONArray(),o=new JSONArray();
            for(String key:hidden)h.put(key);
            for(Map.Entry<String,String> e:folders.entrySet())f.put(e.getKey(),e.getValue());
            for(Map.Entry<String,String> e:assignment.entrySet())a.put(e.getKey(),e.getValue());
            for(String key:order)o.put(key);
            root.put("hidden",h);root.put("folders",f);root.put("assignment",a);root.put("order",o);
            prefs.edit().putString("menu_state",root.toString()).apply();
        }catch(Exception ignored){}
    }
    public boolean hidden(String key){return hidden.contains(key);}
    public void hide(String key){hidden.add(key);save();}
    public void restore(String key){hidden.remove(key);save();}
    public List<String> hiddenKeys(){return new ArrayList<>(hidden);}
    public String assignedFolder(String key){return assignment.get(key);}
    public void moveToFolder(String key,String folderId){assignment.put(key,folderId);save();}
    public void moveToApplications(String key){assignment.remove(key);save();}
    public String createFolder(String name){String id="userfolder:"+System.currentTimeMillis();folders.put(id,name);save();return id;}
    public void renameFolder(String id,String name){if(folders.containsKey(id)){folders.put(id,name);save();}}
    public Map<String,String> folders(){return new LinkedHashMap<>(folders);}
    public int rank(String key){int i=order.indexOf(key);return i<0?Integer.MAX_VALUE:i;}
    public void move(List<MenuModel.Entry> visible,int index,int delta){
        if(index<0||index>=visible.size())return;
        for(MenuModel.Entry e:visible)if(!order.contains(e.key))order.add(e.key);
        String key=visible.get(index).key;int current=order.indexOf(key),target=Math.max(0,Math.min(order.size()-1,current+delta));
        order.remove(current);order.add(target,key);save();
    }
    public void moveBefore(List<MenuModel.Entry> visible,String source,String target){
        if(source.equals(target))return;
        for(MenuModel.Entry e:visible)if(!order.contains(e.key))order.add(e.key);
        order.remove(source);
        int at=order.indexOf(target);
        order.add(at<0?order.size():at,source);
        save();
    }
    /** Stable grid insertion, including positions on another drawer page. */
    public void moveToIndex(List<MenuModel.Entry> visible,String source,int targetIndex){
        ArrayList<String> keys=new ArrayList<>();
        for(MenuModel.Entry entry:visible)if(!entry.key.equals(source))keys.add(entry.key);
        if(keys.size()==visible.size())return;
        keys.add(Math.max(0,Math.min(targetIndex,keys.size())),source);
        ArrayList<String> previous=new ArrayList<>(order);order.clear();order.addAll(keys);
        for(String key:previous)if(!order.contains(key))order.add(key);
        save();
    }
    public void deleteFolder(String id){
        if(!folders.containsKey(id))return;
        folders.remove(id);
        assignment.entrySet().removeIf(e->id.equals(e.getValue()));
        order.remove(id);
        save();
    }
    public void reset(){hidden.clear();folders.clear();assignment.clear();order.clear();save();}
}
