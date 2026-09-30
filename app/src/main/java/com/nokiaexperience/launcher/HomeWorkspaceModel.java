package com.nokiaexperience.launcher;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Launcher-owned Home placement; the dock and installed-app discovery are independent. */
public final class HomeWorkspaceModel {
    public static final int COLUMNS=4, ROWS=3, CELLS=COLUMNS*ROWS;
    public static final class Placement {
        public final String key;
        public int page,cell;
        Placement(String key,int page,int cell){this.key=key;this.page=page;this.cell=cell;}
    }
    private final SharedPreferences prefs;
    private final LinkedHashMap<String,Placement> placed=new LinkedHashMap<>();
    private final LinkedHashMap<String,String> folderNames=new LinkedHashMap<>();
    private final LinkedHashMap<String,ArrayList<String>> folderItems=new LinkedHashMap<>();
    public HomeWorkspaceModel(SharedPreferences prefs){this.prefs=prefs;load();}
    private void load(){
        try{
            JSONObject state=new JSONObject(prefs.getString("home_workspace","{}"));
            JSONArray cells=state.optJSONArray("cells");
            if(cells!=null)for(int i=0;i<cells.length();i++){
                JSONObject item=cells.optJSONObject(i);if(item==null)continue;
                String key=item.optString("key","");
                int page=item.optInt("page",0),cell=item.optInt("cell",-1);
                if(!key.isEmpty()&&page>=0&&cell>=0&&cell<CELLS)placed.put(key,new Placement(key,page,cell));
            }
            JSONObject folders=state.optJSONObject("folders");
            if(folders!=null){java.util.Iterator<String> keys=folders.keys();while(keys.hasNext()){
                String id=keys.next();JSONObject folder=folders.optJSONObject(id);if(folder==null)continue;
                folderNames.put(id,folder.optString("name","Папка"));
                ArrayList<String> members=new ArrayList<>();JSONArray array=folder.optJSONArray("items");
                if(array!=null)for(int i=0;i<array.length();i++)members.add(array.optString(i));
                folderItems.put(id,members);
            }}
        }catch(Exception ignored){}
    }
    private void save(){
        try{
            JSONObject state=new JSONObject(),folders=new JSONObject();JSONArray cells=new JSONArray();
            for(Placement p:placed.values())cells.put(new JSONObject().put("key",p.key).put("page",p.page).put("cell",p.cell));
            for(String id:folderNames.keySet()){
                JSONArray members=new JSONArray();for(String key:folderItems.get(id))members.put(key);
                folders.put(id,new JSONObject().put("name",folderNames.get(id)).put("items",members));
            }
            state.put("cells",cells).put("folders",folders);
            prefs.edit().putString("home_workspace",state.toString()).apply();
        }catch(Exception ignored){}
    }
    public List<Placement> placements(){return new ArrayList<>(placed.values());}
    public Placement get(String key){return placed.get(key);}
    public String at(int page,int cell){for(Placement p:placed.values())if(p.page==page&&p.cell==cell)return p.key;return null;}
    public int pageCount(){int last=0;for(Placement p:placed.values())last=Math.max(last,p.page);return last+1;}
    public boolean isFolder(String key){return folderItems.containsKey(key);}
    public String folderName(String key){return folderNames.get(key);}
    public List<String> folderItems(String key){ArrayList<String> list=folderItems.get(key);return list==null?new ArrayList<>():new ArrayList<>(list);}
    public void add(String key){
        if(placed.containsKey(key))return;
        for(ArrayList<String> list:folderItems.values())if(list.contains(key))return;
        for(int page=0;;page++)for(int cell=0;cell<CELLS;cell++)if(at(page,cell)==null){placed.put(key,new Placement(key,page,cell));save();return;}
    }
    public void place(String key,int page,int cell){
        if(page<0||cell<0||cell>=CELLS)return;
        for(ArrayList<String> members:folderItems.values())members.remove(key);
        Placement old=placed.get(key);String displaced=at(page,cell);
        if(displaced!=null&&!displaced.equals(key)){
            Placement other=placed.get(displaced);
            if(old!=null){other.page=old.page;other.cell=old.cell;}
            else placed.remove(displaced);
        }
        if(old==null)placed.put(key,new Placement(key,page,cell));
        else{old.page=page;old.cell=cell;}
        if(displaced!=null&&!displaced.equals(key)&&old==null)addAtFree(new Placement(displaced,0,0));
        save();
    }
    /** Insert at a grid position, shifting neighbours instead of swapping them. */
    public void reorder(String key,int page,int cell){
        if(page<0||cell<0||cell>=CELLS)return;
        Placement moving=placed.get(key);
        if(moving!=null&&moving.page==page&&moving.cell==cell)return;
        for(ArrayList<String> members:folderItems.values())members.remove(key);
        int oldPage=moving==null?-1:moving.page,oldCell=moving==null?-1:moving.cell;
        if(moving!=null)placed.remove(key);
        if(oldPage==page){
            if(oldCell<cell){for(Placement p:placed.values())if(p.page==page&&p.cell>oldCell&&p.cell<=cell)p.cell--;}
            else{for(Placement p:placed.values())if(p.page==page&&p.cell>=cell&&p.cell<oldCell)p.cell++;}
        }else{
            int target=page*CELLS+cell,free=target;
            while(at(free/CELLS,free%CELLS)!=null)free++;
            for(int position=free;position>target;position--){
                String shifted=at((position-1)/CELLS,(position-1)%CELLS);
                if(shifted!=null){Placement p=placed.get(shifted);p.page=position/CELLS;p.cell=position%CELLS;}
            }
        }
        placed.put(key,moving==null?new Placement(key,page,cell):moving);
        if(moving!=null){moving.page=page;moving.cell=cell;}
        save();
    }
    private void addAtFree(Placement item){
        for(int page=0;;page++)for(int cell=0;cell<CELLS;cell++)if(at(page,cell)==null){item.page=page;item.cell=cell;placed.put(item.key,item);return;}
    }
    public void remove(String key){
        placed.remove(key);for(ArrayList<String> list:folderItems.values())list.remove(key);
        if(isFolder(key)){
            ArrayList<String> members=folderItems.remove(key);folderNames.remove(key);
            // Deleting a folder must not silently remove its applications from Home.
            if(members!=null)for(int i=members.size()-1;i>=0;i--)if(!placed.containsKey(members.get(i)))
                addAtFree(new Placement(members.get(i),0,0));
        }
        save();
    }
    public String createFolder(String first,String second){
        Placement origin=placed.get(second);if(origin==null)origin=placed.get(first);
        int page=origin==null?0:origin.page,cell=origin==null?0:origin.cell;
        placed.remove(first);placed.remove(second);
        for(ArrayList<String> members:folderItems.values()){members.remove(first);members.remove(second);}
        String id="homefolder:"+System.currentTimeMillis();
        folderNames.put(id,"Папка");ArrayList<String> members=new ArrayList<>();
        members.add(second);members.add(first);folderItems.put(id,members);
        if(at(page,cell)!=null){
            boolean found=false;
            for(int p=0;!found;p++)for(int c=0;c<CELLS;c++)if(at(p,c)==null){page=p;cell=c;found=true;break;}
        }
        placed.put(id,new Placement(id,page,cell));save();return id;
    }
    public void moveIntoFolder(String key,String folder){
        if(!isFolder(folder)||key.equals(folder))return;
        placed.remove(key);for(ArrayList<String> list:folderItems.values())list.remove(key);
        folderItems.get(folder).add(key);save();
    }
    public void removeFromFolder(String key){
        for(ArrayList<String> members:folderItems.values())members.remove(key);
        add(key);
    }
    public void rename(String folder,String name){if(isFolder(folder)&&!name.trim().isEmpty()){folderNames.put(folder,name.trim());save();}}
}
