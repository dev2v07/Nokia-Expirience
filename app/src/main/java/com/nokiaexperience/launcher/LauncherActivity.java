package com.nokiaexperience.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.KeyguardManager;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.bluetooth.BluetoothAdapter;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.view.VelocityTracker;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Android launcher core. Nokia artwork is a replaceable visual layer, not Symbian emulation. */
public final class LauncherActivity extends Activity {
    private static final int PICK_ICON=101, PICK_WALLPAPER=102, PICK_WIDGET=103, BIND_WIDGET=104, CONFIG_WIDGET=105;
    private static final int READ_WALLPAPER=106;
    private SharedPreferences prefs;
    private SharedPreferences exp2Prefs;
    private MenuModel menu;
    private AssetRepository assets;
    private HomeWorkspaceModel workspaceModel;
    private IconStore icons;
    private AndroidActionResolver resolver;
    private DeviceStateAdapters device;
    private NokiaSounds sounds;
    private FrameLayout root,home,drawer;
    private FrameLayout exp2EditOverlay;
    private LinearLayout exp2EditPreviewRow;
    private TextView exp2EditIndicator;
    private boolean exp2EditMode;
    private LinearLayout widgetColumn,dock,drawerShell,homeContent,workspaceStrip;
    private ScrollView widgetScroller;
    private WorkspacePager workspacePager;
    private Exp2HomeDragEngine exp2HomeDrag;
    private int workspacePage;
    private int homeDragOriginPage;
    private String activeDragKey;
    private View activeDragView;
    private Runnable edgeTurn;
    private Runnable drawerToHome;
    private int dragEdge;
    private boolean homeEdgeLatched;
    private int hoverCell=-1,hoverPage=-1;
    private boolean crossedHomeEdge,homeDragMoved,settingsVisible;
    private int settingsDepth;
    private int edgeDropCell;
    private boolean drawerDragActive,drawerCrossedEdge,drawerDragMoved;
    private float drawerDragStartX,drawerDragStartY;
    private int drawerEdge,drawerHoverIndex=-1,drawerDropIndex;
    private String exp2DropIconKey;
    private boolean drawerEdgeLatched;
    private Runnable drawerEdgeTurn;
    private DrawerPager pager;
    private LinearLayout pageStrip;
    private boolean drawerDirty=true;
    private final ArrayList<MenuModel.Entry> drawerEntries=new ArrayList<>();
    private TextView pageIndicator;
    private int currentPage;
    private View statusGlyph;
    private TelephonyManager telephony;
    private boolean serviceAvailable=true;
    private String statusTime="",statusDate="",statusOperator="";
    private boolean wifiEnabled,wifiConnected,bluetoothEnabled,bluetoothConnected,headsetConnected;
    private boolean alarmSet,nfcEnabled,airplaneMode,powerSaveMode,doNotDisturb;
    private int wifiLevel=-1,notificationIndicators;
    private final PhoneStateListener signalListener=new PhoneStateListener(){
        @Override public void onSignalStrengthsChanged(SignalStrength signal){device.signalLevel=!serviceAvailable||signal==null?-1:signal.getLevel();if(statusGlyph!=null)statusGlyph.invalidate();}
        @Override public void onServiceStateChanged(ServiceState service){serviceAvailable=service!=null&&service.getState()==ServiceState.STATE_IN_SERVICE;if(!serviceAvailable)device.signalLevel=-1;refreshStatusData();}
    };
    private ImageView wallpaper;
    private StartupView startup;
    private MediaPlayer startupPlayer;
    private StudioRuntimeConfig studioRuntime;
    private boolean startupPending,drawerOpen,receiverRegistered,startupAudioFinished;
    private boolean wallpaperFallback;
    private Drawable cachedDrawerSource;
    private Bitmap cachedDrawerBitmap;
    private int cachedDrawerBlur=-1;
    private String folderId,iconTarget;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private AppWidgetHost widgetHost;
    private AppWidgetManager widgetManager;
    private WidgetFrame editingWidget;
    private final Map<Integer,Bitmap> exp2PageBitmapCache=new HashMap<>();
    private Bitmap exp2BlurredBackdrop;
    private int pendingWidgetId=-1;
    private float swipeX,swipeY;
    private boolean swipeConsumed;
    private boolean upperSwipeCandidate,upperSwipeConsumed;
    private boolean exp2AddPageInProgress;
    private Runnable pendingHomeLongPress;
    private int touchSlop;
    private final BroadcastReceiver changes=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){
            String action=i.getAction();
            if(action!=null && action.startsWith("android.intent.action.PACKAGE")){menu.refreshInstalled();refreshGrid();refreshWorkspace();}
            if(Intent.ACTION_WALLPAPER_CHANGED.equals(action))loadWallpaper();
            refresh();
        }
    };
    private final Runnable tick=new Runnable(){@Override public void run(){refresh();handler.postDelayed(this,30000);}};

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("nokia_experience",MODE_PRIVATE);
        exp2Prefs=getSharedPreferences("nokia_exp_2",MODE_PRIVATE);
        seedExp2Profile();
        String savedStyle=prefs.getString("style","exp2");
        if(!savedStyle.equals("exp2")&&!savedStyle.equals("n95")&&!savedStyle.equals("5530"))
            prefs.edit().putString("style","exp2").apply();
        assets=new AssetRepository(this);loadProfileData();
        studioRuntime=StudioRuntimeConfig.load(this);
        resolver=new AndroidActionResolver(this);device=new DeviceStateAdapters();sounds=new NokiaSounds(this,prefs);
        widgetHost=new AppWidgetHost(this,5530);widgetManager=AppWidgetManager.getInstance(this);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        getWindow().setFormat(PixelFormat.OPAQUE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        updateSystemStatusBar();
        root=new FrameLayout(this){
            @Override public boolean dispatchDragEvent(DragEvent event){
                boolean handled=super.dispatchDragEvent(event);
                if(event.getAction()==DragEvent.ACTION_DRAG_ENDED&&activeDragKey!=null){
                    Log.d("NokiaDrag","root ended drawer="+drawerDragActive+" key="+activeDragKey);
                    if(drawerDragActive&&drawerOpen)endDrawerDrag(event);else endWorkspaceDrag(event);
                }
                return handled;
            }
        };root.setBackgroundColor(Color.BLACK);setContentView(root);
        touchSlop=ViewConfiguration.get(this).getScaledTouchSlop();
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(homeContent!=null)homeContent.setPadding(dp(12),drawerOpen||settingsVisible?insets.getSystemWindowInsetTop()+dp(3):dp(3),dp(12),insets.getSystemWindowInsetBottom()+dp(18));
            if(drawerShell!=null)drawerShell.setPadding(dp(10),Math.max(insets.getSystemWindowInsetTop(),dp(58))+dp(4),dp(10),insets.getSystemWindowInsetBottom()+dp(6));
            return insets;
        });
        applyOrientation();buildHome();buildDrawer();
        // One shared Launcher status layer stays above Home, Drawer and Exp2 Edit Mode.
        root.addView(statusGlyph,new FrameLayout.LayoutParams(-1,dp(58),Gravity.TOP));
        loadWallpaper();refresh();
        startupPending=state==null && prefs.getBoolean("startup_animation",true);
    }
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private String interfaceStyle(){
        String style=prefs.getString("style","exp2");
        return style.equals("n95")||style.equals("5530")?style:"exp2";
    }
    private boolean exp2(){return interfaceStyle().equals("exp2");}
    private SharedPreferences profilePrefs(){return exp2()?exp2Prefs:prefs;}
    private void seedExp2Profile(){
        if(!exp2Prefs.getBoolean("initialized",false)){
            SharedPreferences.Editor copy=exp2Prefs.edit()
                .putString("menu_state",prefs.getString("menu_state","{}"))
                .putString("home_workspace",prefs.getString("home_workspace","{}"))
                .putString("widgets",prefs.getString("widgets",""));
            String widgets=prefs.getString("widgets","");
            if(!widgets.isEmpty())for(String id:widgets.split(","))try{
                int widgetId=Integer.parseInt(id);
                copy.putInt("widget_page_"+id,prefs.getInt("widget_page_"+widgetId,0));
                copy.putInt("widget_width_"+id,prefs.getInt("widget_width_"+widgetId,-1));
                copy.putInt("widget_height_"+id,prefs.getInt("widget_height_"+widgetId,180));
            }catch(NumberFormatException ignored){}
            copy.putBoolean("initialized",true).apply();
        }
        if(!exp2Prefs.getBoolean("profile_settings_initialized",false)){
            SharedPreferences.Editor settings=exp2Prefs.edit();
            settings.putString("theme_color",prefs.getString("theme_color","profile"));
            settings.putInt("window_transparency",windowTransparencyFrom(prefs));
            settings.putInt("drawer_dim",prefs.getInt("drawer_dim",38));
            settings.putInt("blur",prefs.getInt("blur",1));
            settings.putInt("drawer_rows",prefs.getInt("drawer_rows",6));
            settings.putInt("dock_count",prefs.getInt("dock_count",3));
            for(int i=0;i<4;i++)if(prefs.contains("shortcut_"+i))
                settings.putString("shortcut_"+i,prefs.getString("shortcut_"+i,""));
            settings.putBoolean("profile_settings_initialized",true).apply();
        }
    }
    private void loadProfileData(){
        SharedPreferences data=profilePrefs();
        menu=new MenuModel(this,data);workspaceModel=new HomeWorkspaceModel(data);
        icons=new IconStore(this,data,assets,exp2());
    }
    private int accent(){
        if(interfaceStyle().equals("n95"))return 0xFF338BEB;
        if(interfaceStyle().equals("5530"))return 0xFFF04A3D;
        switch(profilePrefs().getString("theme_color","profile")){
            case "red":return 0xFFFF3636;
            case "blue":return 0xFF208FFF;
            case "black":return 0xFF181A22;
            case "orange":return 0xFFFF9712;
            case "cyan":return 0xFF00D9F2;
            case "white":return 0xFFFFFFFF;
            default:return 0xFF208FFF;
        }
    }
    private int statusIconColor(){
        if(exp2() && studioRuntime!=null && studioRuntime.statusIconColor()!=Color.TRANSPARENT)
            return studioRuntime.statusIconColor();
        String value=profilePrefs().getString("status_icon_color","white");
        if("accent".equals(value))return accent();
        if("cyan".equals(value))return 0xFF8DEBFF;
        if("yellow".equals(value))return 0xFFFFE58A;
        return Color.WHITE;
    }
    private String statusIconColorLabel(){
        String value=profilePrefs().getString("status_icon_color","white");
        if("accent".equals(value))return "акцентный";
        if("cyan".equals(value))return "голубой";
        if("yellow".equals(value))return "жёлтый";
        return "белый";
    }
    private void chooseStatusIconColor(){
        String[] choices={"Белый","Акцентный цвет темы","Голубой","Жёлтый"};
        String current=profilePrefs().getString("status_icon_color","white");
        int selected="accent".equals(current)?1:"cyan".equals(current)?2:"yellow".equals(current)?3:0;
        new AlertDialog.Builder(this).setTitle("Цвет иконок статус-бара").setSingleChoiceItems(choices,selected,(dialog,which)->{
            String value=which==1?"accent":which==2?"cyan":which==3?"yellow":"white";
            profilePrefs().edit().putString("status_icon_color",value).apply();
            if(statusGlyph!=null)statusGlyph.invalidate();dialog.dismiss();showSettings();
        }).setNegativeButton("Отмена",null).show();
    }
    private int windowTransparency(){
        return windowTransparencyFrom(profilePrefs());
    }
    private int windowTransparencyFrom(SharedPreferences source){
        int value=35;
        try{value=source.contains("window_transparency")?source.getInt("window_transparency",35):
            source.contains("opacity_menu")?100-source.getInt("opacity_menu",65):35;}
        catch(ClassCastException ignored){}
        return Math.max(0,Math.min(90,value));
    }
    private int panel(){
        int transparency=windowTransparency();
        int alpha=Math.round(230*(100-transparency)/100f);
        int color=accent();
        float strength=interfaceStyle().equals("5530")?0.31f:color==Color.WHITE?0.82f:0.73f;
        int rgb=(Math.round(Color.red(color)*strength)<<16)|(Math.round(Color.green(color)*strength)<<8)|Math.round(Color.blue(color)*strength);
        return (alpha<<24)|rgb;
    }
    private GradientDrawable background(int color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private AlertDialog showNokiaDialog(AlertDialog.Builder builder){
        AlertDialog dialog=builder.create();dialog.show();
        int color=accent();int red=210+Color.red(color)*45/255;
        int green=210+Color.green(color)*45/255;int blue=210+Color.blue(color)*45/255;
        int alpha=Math.max(125,240-windowTransparency()*12/10);
        GradientDrawable shape=background(Color.argb(alpha,red,green,blue),10);
        shape.setStroke(dp(1),accent());dialog.getWindow().setBackgroundDrawable(shape);
        return dialog;
    }
    private AlertDialog showSettingsDialog(AlertDialog.Builder builder){
        settingsDepth++;settingsVisible=true;updateSystemStatusBar();
        AlertDialog dialog=showNokiaDialog(builder);
        dialog.setOnDismissListener(d->{settingsDepth=Math.max(0,settingsDepth-1);settingsVisible=settingsDepth>0;updateSystemStatusBar();});
        return dialog;
    }
    private LinearLayout settingsTitle(String title,Runnable onBack){
        LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=label("‹",30,Color.DKGRAY);heading.addView(back,new LinearLayout.LayoutParams(dp(54),dp(52)));
        TextView text=label(title,20,Color.DKGRAY);text.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
        heading.addView(text,new LinearLayout.LayoutParams(0,dp(52),1));
        back.setOnClickListener(v->onBack.run());
        return heading;
    }
    private int dockRadius(){return interfaceStyle().equals("n95")?5:interfaceStyle().equals("5530")?9:15;}
    private void updateSystemStatusBar(){
        // Launcher owns the single visual status layer (StatusGlyph).  Do not
        // reveal Android's native status bar when Drawer/Edit/Settings opens:
        // it would be drawn over the SVG glyphs at the same top inset.
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        int flags=View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            |View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(flags);
        if(root!=null)root.requestApplyInsets();
    }
    private TextView label(String text,int size,int color){
        TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);return t;
    }
    private MenuModel.Entry special(String key){
        if(key.equals("phone"))return new MenuModel.Entry("phone",defaultLabel(new Intent(Intent.ACTION_DIAL),"Телефон","Phone"),"contacts");
        if(key.equals("chrome"))return new MenuModel.Entry("chrome","Chrome","browser");
        if(key.equals("camera"))return new MenuModel.Entry("camera",defaultLabel(new Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),"Камера","Camera"),"camera");
        for(MenuModel.Entry e:menu.root.children)if(e.key.equals(key))return e;
        for(MenuModel.Entry e:allInstalled())if(e.key.equals(key))return e;
        return new MenuModel.Entry(key,key,"app");
    }
    private String defaultLabel(Intent action,String russian,String other){
        if(Locale.getDefault().getLanguage().equals("ru"))return russian;
        try{android.content.pm.ResolveInfo info=getPackageManager().resolveActivity(action,0);
            if(info!=null){CharSequence label=info.loadLabel(getPackageManager());if(label!=null&&!label.toString().isEmpty())return label.toString();}}
        catch(Exception ignored){}
        return other;
    }
    private List<MenuModel.Entry> allInstalled(){
        ArrayList<MenuModel.Entry> result=new ArrayList<>();
        result.addAll(menu.children(new MenuModel.Entry("installed","","")));
        for(String id:menu.organizer.folders().keySet())result.addAll(menu.children(new MenuModel.Entry(id,"","app")));
        return result;
    }
    private void buildHome(){
        home=new FrameLayout(this);home.setBackgroundColor(Color.BLACK);root.addView(home,new FrameLayout.LayoutParams(-1,-1));
        wallpaper=new ImageView(this);wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        home.addView(wallpaper,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout content=new LinearLayout(this);homeContent=content;content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12),dp(3),dp(12),dp(18));home.addView(content,new FrameLayout.LayoutParams(-1,-1));
        statusGlyph=new StatusGlyph(this);
        statusGlyph.setOnClickListener(v->showStatusActions());
        // Preserve the established Home geometry while the actual glyph is shared at root level.
        content.addView(new View(this),new LinearLayout.LayoutParams(-1,dp(43)));
        LinearLayout mid=new LinearLayout(this);mid.setOrientation(LinearLayout.VERTICAL);
        content.addView(mid,new LinearLayout.LayoutParams(-1,0,1));
        widgetColumn=new LinearLayout(this);widgetColumn.setOrientation(LinearLayout.VERTICAL);
        widgetScroller=new WidgetScrollView(this);widgetScroller.setVerticalScrollBarEnabled(false);
        widgetScroller.setFillViewport(false);widgetScroller.addView(widgetColumn,new ScrollView.LayoutParams(-1,-2));
        mid.addView(widgetScroller,new LinearLayout.LayoutParams(-1,0,0.55f));
        widgetColumn.setOnDragListener((v,event)->{
            if(event.getAction()==DragEvent.ACTION_DROP&&String.valueOf(event.getLocalState()).startsWith("widget:")){
                moveWidgetBefore(String.valueOf(event.getLocalState()),null);return true;
            }
            return true;
        });
        restoreWidgets();
        workspacePager=new WorkspacePager(this);workspacePager.setHorizontalScrollBarEnabled(false);
        workspacePager.setFillViewport(true);workspacePager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        workspaceStrip=new LinearLayout(this);workspaceStrip.setOrientation(LinearLayout.HORIZONTAL);
        workspacePager.addView(workspaceStrip,new HorizontalScrollView.LayoutParams(-2,-1));
        mid.addView(workspacePager,new LinearLayout.LayoutParams(-1,0,0.45f));
        exp2HomeDrag=new Exp2HomeDragEngine(new Exp2HomeDragEngine.Host(){
            @Override public int currentPage(){return workspacePage;}
            @Override public int pageCount(){return workspaceStrip==null?1:workspaceStrip.getChildCount();}
            @Override public int[] pageBoundsOnScreen(int page){
                if(workspaceStrip==null||page<0||page>=workspaceStrip.getChildCount())return null;
                View v=workspaceStrip.getChildAt(page);int[] location=new int[2];v.getLocationOnScreen(location);
                return new int[]{location[0],location[1],v.getWidth(),v.getHeight()};
            }
            @Override public int edgeDirection(float rawX){
                if(workspacePager==null)return 0;int[] location=new int[2];workspacePager.getLocationOnScreen(location);
                int edge=dp(32);return rawX-location[0]<edge?-1:rawX-location[0]>workspacePager.getWidth()-edge?1:0;
            }
            @Override public void movePageForSwipe(int direction){
                int count=Math.max(1,workspaceStrip==null?homePageCount():workspaceStrip.getChildCount());
                int next=Math.max(0,Math.min(count-1,workspacePage+direction));
                if(next!=workspacePage)settleWorkspace(next);
            }
            @Override public void movePageForDrag(int direction){
                int next=Math.max(0,Math.min(pageCount()-1,workspacePage+direction));
                if(next!=workspacePage)settleWorkspace(next);
            }
            @Override public String keyAt(int page,int cell){return workspaceModel==null?null:workspaceModel.at(page,cell);}
            @Override public boolean isFolder(String key){return key!=null&&workspaceModel.isFolder(key);}
            @Override public void preview(String key,View view,int page,int cell){
                if(activeDragKey==null)homeDragOriginPage=workspacePage;
                activeDragKey=key;activeDragView=view;
                hoverPage=page;hoverCell=cell;previewReorder(page,cell);
            }
            @Override public void commit(String key,int page,int cell,String folderTarget){
                if(folderTarget!=null&&workspaceModel.isFolder(folderTarget))workspaceModel.moveIntoFolder(key,folderTarget);
                else if(folderTarget!=null&&!workspaceModel.isFolder(key))workspaceModel.createFolder(key,folderTarget);
                else workspaceModel.reorder(key,page,cell);
                clearDragPreview();finishWorkspaceDrag();refreshWorkspace();
            }
            @Override public void cancelPreview(String key,View view){clearDragPreview();finishWorkspaceDrag();}
            @Override public void restoreOriginPage(){
                if(workspacePager!=null)settleWorkspace(homeDragOriginPage);
            }
            @Override public void tapped(String key){MenuModel.Entry entry=homeEntry(key);if(entry!=null){if(workspaceModel.isFolder(key))openHomeFolder(key);else launch(entry);}}
        },ViewConfiguration.get(this));
        workspacePager.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            if(r-l!=or-ol||b-t!=ob-ot)workspacePager.post(this::rebuildWorkspace);
        });
        rebuildWorkspace();
        workspacePager.post(this::rebuildWorkspace);
        View gap=new View(this);
        content.addView(gap,new LinearLayout.LayoutParams(-1,dp(18)));
        dock=new LinearLayout(this);dock.setGravity(Gravity.CENTER);
        dock.setBackground(background(panel(),dockRadius()));content.addView(dock,new LinearLayout.LayoutParams(-1,dp(86)));
        buildDock();
        home.setOnTouchListener(this::emptyHomeTouch);
    }
    private void buildDock(){
        if(dock==null)return;dock.removeAllViews();
        String[] defaults={"phone","chrome","camera","contacts"};
        for(int i=0;i<dockCount();i++){
            final int slot=i;
            MenuModel.Entry e=special(profilePrefs().getString("shortcut_"+i,defaults[i]));
            LinearLayout cell=iconCell(e,74);
            dock.addView(cell,new LinearLayout.LayoutParams(0,-1,1));
            cell.setOnClickListener(v->{sounds.tap();launch(e);});
            cell.setOnLongClickListener(v->{chooseShortcut(slot);return true;});
        }
    }
    private int dockCount(){return Math.max(3,Math.min(4,profilePrefs().getInt("dock_count",3)));}
    private MenuModel.Entry homeEntry(String key){
        if(workspaceModel.isFolder(key))return new MenuModel.Entry(key,workspaceModel.folderName(key),"files");
        if(key.equals("phone")||key.equals("chrome")||key.equals("camera"))return special(key);
        for(MenuModel.Entry entry:allInstalled())if(entry.key.equals(key))return entry;
        return null;
    }
    private void refreshWorkspace(){if(workspacePager!=null)workspacePager.post(this::rebuildWorkspace);}
    private void rebuildWorkspace(){
        if(workspacePager==null||workspaceStrip==null||workspacePager.getWidth()<=0||workspacePager.getHeight()<=0)return;
        workspaceStrip.removeAllViews();
        int width=workspacePager.getWidth(),height=workspacePager.getHeight();
        // Keep one empty destination page available during a continuous edge drag.
        int pages=Math.max(2,(exp2()?homePageCount():workspaceModel.pageCount())+1);workspacePage=Math.min(workspacePage,pages-1);
        int cellWidth=width/HomeWorkspaceModel.COLUMNS,cellHeight=height/HomeWorkspaceModel.ROWS;
        for(int page=0;page<pages;page++){
            final int pageIndex=page;
            FrameLayout surface=new FrameLayout(this);
            workspaceStrip.addView(surface,new LinearLayout.LayoutParams(width,height));
            if(exp2()&&page==pages-1){
                TextView nextPage=label("+",34,0x99FFFFFF);
                nextPage.setContentDescription("Новый рабочий стол при отпускании иконки");
                surface.addView(nextPage,new FrameLayout.LayoutParams(-1,-1));
            }
            surface.setOnTouchListener(this::emptyHomeTouch);
            surface.setOnDragListener((v,event)->{
                if(event.getAction()==DragEvent.ACTION_DRAG_STARTED)return event.getLocalState() instanceof String;
                if(event.getAction()==DragEvent.ACTION_DRAG_LOCATION){trackWorkspaceDrag(v,event,pageIndex);return true;}
                if(event.getAction()==DragEvent.ACTION_DRAG_ENDED){endWorkspaceDrag(event);return true;}
                if(event.getAction()==DragEvent.ACTION_DROP){
                    String key=String.valueOf(event.getLocalState());
                    if(key.startsWith("widget:")){moveWidgetToPage(key,pageIndex);finishWorkspaceDrag();return true;}
                    int column=Math.max(0,Math.min(HomeWorkspaceModel.COLUMNS-1,(int)event.getX()/Math.max(1,cellWidth)));
                    int row=Math.max(0,Math.min(HomeWorkspaceModel.ROWS-1,(int)event.getY()/Math.max(1,cellHeight)));
                    int target=row*HomeWorkspaceModel.COLUMNS+column;
                    HomeWorkspaceModel.Placement original=workspaceModel.get(key);
                    if(original!=null&&original.page==pageIndex&&original.cell==target)return false;
                    String occupied=workspaceModel.at(pageIndex,target);
                    float fractionX=(event.getX()-column*cellWidth)/Math.max(1f,cellWidth);
                    float fractionY=(event.getY()-row*cellHeight)/Math.max(1f,cellHeight);
                    boolean onIcon=fractionX>0.22f&&fractionX<0.78f&&fractionY>0.16f&&fractionY<0.84f;
                    if(onIcon&&occupied!=null&&!occupied.equals(key)){
                        if(workspaceModel.isFolder(occupied))workspaceModel.moveIntoFolder(key,occupied);
                        else if(!workspaceModel.isFolder(key))workspaceModel.createFolder(key,occupied);
                        else workspaceModel.reorder(key,pageIndex,target);
                    }else workspaceModel.reorder(key,pageIndex,target);
                    completeWorkspaceDrop();return true;
                }
                return true;
            });
            for(HomeWorkspaceModel.Placement placement:workspaceModel.placements()){
                if(placement.page!=page)continue;
                MenuModel.Entry entry=homeEntry(placement.key);if(entry==null)continue;
                LinearLayout cell=iconCell(entry,Math.round(cellHeight/getResources().getDisplayMetrics().density));
                FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(cellWidth,cellHeight);
                cp.leftMargin=(placement.cell%HomeWorkspaceModel.COLUMNS)*cellWidth;
                cp.topMargin=(placement.cell/HomeWorkspaceModel.COLUMNS)*cellHeight;
                surface.addView(cell,cp);
                cell.setTag(entry.key);
                if(exp2HomeEngineEnabled()){
                    // The engine owns the complete pointer sequence. Mark the
                    // container clickable so Android dispatches touch events to
                    // this listener instead of treating the visual children as
                    // the terminal target.
                    cell.setClickable(true);cell.setLongClickable(true);
                    View.OnTouchListener exp2Touch=(v,event)->exp2HomeDrag.onIconTouch(cell,entry.key,pageIndex,event);
                    // The visual children receive the actual pointer. Keeping
                    // the parent listener too would let the same DOWN bubble
                    // into a second arbitration attempt on API 26.
                    cell.setOnTouchListener(null);
                    for(int child=0;child<cell.getChildCount();child++){
                        View visual=cell.getChildAt(child);visual.setClickable(true);visual.setLongClickable(true);visual.setOnTouchListener(exp2Touch);
                    }
                }else{
                    cell.setOnClickListener(v->{if(workspaceModel.isFolder(entry.key))openHomeFolder(entry.key);else launch(entry);});
                    cell.setOnLongClickListener(v->{startAppDrag(v,entry.key);return true;});
                }
                if(!exp2HomeEngineEnabled())cell.setOnDragListener((v,event)->{
                    if(event.getAction()==DragEvent.ACTION_DRAG_STARTED)return event.getLocalState() instanceof String;
                    if(event.getAction()==DragEvent.ACTION_DRAG_LOCATION){trackWorkspaceDrag(v,event,pageIndex);return true;}
                if(event.getAction()==DragEvent.ACTION_DRAG_ENDED){endWorkspaceDrag(event);return true;}
                    if(event.getAction()==DragEvent.ACTION_DROP){
                        String source=String.valueOf(event.getLocalState());
                        if(source.startsWith("widget:")){moveWidgetToPage(source,pageIndex);finishWorkspaceDrag();return true;}
                        if(source.equals(entry.key))return false;
                        float x=event.getX()/Math.max(1f,v.getWidth()),y=event.getY()/Math.max(1f,v.getHeight());
                        boolean onIcon=x>0.22f&&x<0.78f&&y>0.16f&&y<0.84f;
                        if(onIcon){
                            if(workspaceModel.isFolder(entry.key))workspaceModel.moveIntoFolder(source,entry.key);
                            else if(!workspaceModel.isFolder(source))workspaceModel.createFolder(source,entry.key);
                            else workspaceModel.reorder(source,pageIndex,placement.cell);
                        }else workspaceModel.reorder(source,pageIndex,placement.cell);
                        completeWorkspaceDrop();return true;
                    }
                    return true;
                });
            }
        }
        workspacePager.scrollTo(workspacePage*width,0);
    }
    private boolean exp2HomeEngineEnabled(){return exp2()&&exp2Prefs!=null&&exp2Prefs.getBoolean("home_engine_v2",true);}
    private final class WorkspacePager extends HorizontalScrollView{
        private float downX,downY;private boolean flung;private boolean pagerOwnsSequence;
        WorkspacePager(Context context){super(context);}
        @Override public boolean dispatchTouchEvent(MotionEvent event){
            if(event.getAction()==MotionEvent.ACTION_DOWN){
                downX=event.getX();downY=event.getY();flung=false;pagerOwnsSequence=false;requestDisallowInterceptTouchEvent(true);
                cancelHomeLongPress();
            }
            if(event.getAction()==MotionEvent.ACTION_MOVE){
                float dx=event.getX()-downX,dy=event.getY()-downY;
                if(Math.abs(dx)>touchSlop||Math.abs(dy)>touchSlop)cancelHomeLongPress();
                if(Math.abs(dx)>touchSlop&&Math.abs(dx)>Math.abs(dy))requestDisallowInterceptTouchEvent(true);
            }
            boolean handled=super.dispatchTouchEvent(event);
            if(event.getAction()==MotionEvent.ACTION_DOWN){
                // The child icon listener claims its sequence during super.dispatchTouchEvent.
                // Empty-surface gestures remain owned by this pager.
                pagerOwnsSequence=exp2HomeDrag==null||!exp2HomeDrag.pointerSequenceActive();
            }
            if(event.getAction()==MotionEvent.ACTION_UP&&pagerOwnsSequence&&!flung&&getWidth()>0){
                float dx=event.getX()-downX;
                int next=Math.abs(dx)>getWidth()*0.18f?workspacePage+(dx<0?1:-1):Math.round((float)getScrollX()/getWidth());
                settleWorkspace(next);
                requestDisallowInterceptTouchEvent(false);
            }
            if(event.getAction()==MotionEvent.ACTION_UP||event.getAction()==MotionEvent.ACTION_CANCEL){
                pagerOwnsSequence=false;requestDisallowInterceptTouchEvent(false);
            }
            return handled;
        }
        // Empty-surface long press is owned by emptyHomeTouch(). Keeping the
        // pager free of a second timer prevents one pointer sequence from
        // scheduling both Edit Mode and the Nokia context-menu policy.
        @Override public void fling(int velocityX){flung=true;settleWorkspace(workspacePage+(velocityX>0?1:-1));}
    }
    /** Widgets preserve vertical scrolling; horizontal gestures use Home's single settle owner. */
    private final class WidgetScrollView extends ScrollView{
        private float downX,downY;private boolean horizontalGesture,launcherGesture;
        WidgetScrollView(Context context){super(context);}
        @Override public boolean onInterceptTouchEvent(MotionEvent event){
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();horizontalGesture=false;launcherGesture=!touchOnVisibleWidget(event.getRawX(),event.getRawY());if(launcherGesture)return true;}
            else if(action==MotionEvent.ACTION_MOVE&&!horizontalGesture){
                float dx=event.getX()-downX,dy=event.getY()-downY;
                if(Math.abs(dx)>touchSlop&&Math.abs(dx)>Math.abs(dy)){
                    horizontalGesture=true;requestDisallowInterceptTouchEvent(true);return true;
                }
            }
            if(launcherGesture&&(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL))launcherGesture=false;
            return super.onInterceptTouchEvent(event);
        }
        @Override public boolean onTouchEvent(MotionEvent event){
            if(launcherGesture){
                boolean handled=emptyHomeTouch(this,event);
                if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL)launcherGesture=false;
                return handled;
            }
            if(horizontalGesture){
                int action=event.getActionMasked();
                if(action==MotionEvent.ACTION_UP){
                    float dx=event.getX()-downX;
                    if(Math.abs(dx)>getWidth()*0.18f)settleWorkspace(workspacePage+(dx<0?1:-1));
                    horizontalGesture=false;requestDisallowInterceptTouchEvent(false);return true;
                }
                if(action==MotionEvent.ACTION_CANCEL){horizontalGesture=false;requestDisallowInterceptTouchEvent(false);return true;}
                return true;
            }
            if(!hasVisibleWidget())return emptyHomeTouch(this,event);
            return super.onTouchEvent(event);
        }
        private boolean touchOnVisibleWidget(float rawX,float rawY){
            if(widgetColumn==null)return false;
            for(int i=0;i<widgetColumn.getChildCount();i++){
                View child=widgetColumn.getChildAt(i);if(child.getVisibility()!=View.VISIBLE)continue;
                int[] location=new int[2];child.getLocationOnScreen(location);
                if(rawX>=location[0]&&rawX<location[0]+child.getWidth()&&rawY>=location[1]&&rawY<location[1]+child.getHeight())return true;
            }
            return false;
        }
    }
    private void settleWorkspace(int page){
        int last=exp2()?Math.max(0,workspaceStrip.getChildCount()-1):workspaceStrip.getChildCount()-1;
        workspacePage=Math.max(0,Math.min(page,Math.max(0,last)));
        workspacePager.smoothScrollTo(workspacePage*workspacePager.getWidth(),0);
        updateVisibleWidgets();
    }
    private void trackWorkspaceDrag(View target,DragEvent event,int page){
        if(activeDragKey==null||workspacePager==null)return;
        int[] point=new int[2],pagerPoint=new int[2],surfacePoint=new int[2];
        target.getLocationOnScreen(point);workspacePager.getLocationOnScreen(pagerPoint);
        float screenX=point[0]+event.getX(),screenY=point[1]+event.getY();
        if(!drawerDragActive&&(Math.abs(screenX-drawerDragStartX)>dp(26)||Math.abs(screenY-drawerDragStartY)>dp(26)))homeDragMoved=true;
        int edge=screenX-pagerPoint[0]<dp(32)?-1:screenX-pagerPoint[0]>workspacePager.getWidth()-dp(32)?1:0;
        if(edge!=dragEdge){cancelEdgeTurn();dragEdge=edge;homeEdgeLatched=false;}
        if(edge!=0&&!homeEdgeLatched&&edgeTurn==null){
            edgeTurn=()->{
                edgeTurn=null;
                if(activeDragKey!=null&&dragEdge!=0){
                    int next=Math.max(0,Math.min(workspacePage+dragEdge,workspaceStrip.getChildCount()-1));
                    if(next!=workspacePage){
                        homeEdgeLatched=true;
                        Log.d("NokiaDrag","edge "+workspacePage+" -> "+next+" key="+activeDragKey);
                        int row=hoverCell<0?0:hoverCell/HomeWorkspaceModel.COLUMNS;
                        edgeDropCell=row*HomeWorkspaceModel.COLUMNS+(dragEdge>0?HomeWorkspaceModel.COLUMNS-1:0);
                        crossedHomeEdge=true;clearDragPreview();workspacePage=next;
                        workspacePager.smoothScrollTo(next*workspacePager.getWidth(),0);updateVisibleWidgets();
                    }
                }
            };
            handler.postDelayed(edgeTurn,430);
        }
        View surface=workspaceStrip.getChildAt(page);if(surface==null)return;
        surface.getLocationOnScreen(surfacePoint);
        int column=Math.max(0,Math.min(HomeWorkspaceModel.COLUMNS-1,(int)(screenX-surfacePoint[0])/Math.max(1,surface.getWidth()/HomeWorkspaceModel.COLUMNS)));
        int row=Math.max(0,Math.min(HomeWorkspaceModel.ROWS-1,(int)(screenY-surfacePoint[1])/Math.max(1,surface.getHeight()/HomeWorkspaceModel.ROWS)));
        int cell=row*HomeWorkspaceModel.COLUMNS+column;
        if(hoverPage!=page||hoverCell!=cell){hoverPage=page;hoverCell=cell;previewReorder(page,cell);}
    }
    private void previewReorder(int page,int target){
        HomeWorkspaceModel.Placement moving=workspaceModel.get(activeDragKey);
        View surface=workspaceStrip.getChildAt(page);if(!(surface instanceof ViewGroup))return;
        int cellWidth=surface.getWidth()/HomeWorkspaceModel.COLUMNS,cellHeight=surface.getHeight()/HomeWorkspaceModel.ROWS;
        ViewGroup group=(ViewGroup)surface;
        for(int i=0;i<group.getChildCount();i++){
            View child=group.getChildAt(i);HomeWorkspaceModel.Placement p=workspaceModel.get(String.valueOf(child.getTag()));
            if(p==null||child==activeDragView)continue;
            int destination=p.cell;
            if(moving!=null&&moving.page==page){
                if(moving.cell<target&&p.cell>moving.cell&&p.cell<=target)destination=p.cell-1;
                else if(moving.cell>target&&p.cell>=target&&p.cell<moving.cell)destination=p.cell+1;
            }else if(p.cell>=target)destination=p.cell+1;
            if(destination>=HomeWorkspaceModel.CELLS)destination=p.cell;
            child.animate().translationX((destination%HomeWorkspaceModel.COLUMNS-p.cell%HomeWorkspaceModel.COLUMNS)*cellWidth)
                .translationY((destination/HomeWorkspaceModel.COLUMNS-p.cell/HomeWorkspaceModel.COLUMNS)*cellHeight)
                .setDuration(110).start();
        }
    }
    private void clearDragPreview(){
        if(workspaceStrip==null)return;
        for(int p=0;p<workspaceStrip.getChildCount();p++){
            View page=workspaceStrip.getChildAt(p);
            if(page instanceof ViewGroup){ViewGroup group=(ViewGroup)page;
                for(int i=0;i<group.getChildCount();i++)group.getChildAt(i).animate().translationX(0).translationY(0).setDuration(90).start();}
        }
        hoverCell=-1;hoverPage=-1;
    }
    private void cancelEdgeTurn(){if(edgeTurn!=null){handler.removeCallbacks(edgeTurn);edgeTurn=null;}}
    private void finishWorkspaceDrag(){
        cancelEdgeTurn();dragEdge=0;homeEdgeLatched=false;clearDragPreview();
        if(activeDragView!=null)activeDragView.animate().alpha(1f).setDuration(90).start();
        activeDragView=null;activeDragKey=null;crossedHomeEdge=false;
    }
    private void endWorkspaceDrag(DragEvent event){
        Log.d("NokiaDrag","ended result="+event.getResult()+" edge="+crossedHomeEdge+" page="+workspacePage+" key="+activeDragKey);
        String held=activeDragKey;
        boolean showContext=held!=null&&!drawerDragActive&&!event.getResult()&&!homeDragMoved&&!crossedHomeEdge;
        if(exp2()&&activeDragKey!=null&&!event.getResult()&&crossedHomeEdge&&!drawerDragActive){
            workspacePage=homeDragOriginPage;
            workspacePager.smoothScrollTo(workspacePage*workspacePager.getWidth(),0);
            updateVisibleWidgets();
        }else if(activeDragKey!=null&&!event.getResult()&&crossedHomeEdge){
            if(activeDragKey.startsWith("widget:"))moveWidgetToPage(activeDragKey,workspacePage);
            else{workspaceModel.reorder(activeDragKey,workspacePage,edgeDropCell);refreshWorkspace();}
        }
        finishWorkspaceDrag();
        if(showContext){MenuModel.Entry entry=homeEntry(held);if(entry!=null)root.post(()->showHomeItemOptions(entry));}
    }
    private void completeWorkspaceDrop(){
        finishWorkspaceDrag();drawerOpen=false;drawer.setVisibility(View.GONE);updateSystemStatusBar();refreshWorkspace();
    }
    private void moveWidgetToPage(String source,int page){
        try{profilePrefs().edit().putInt("widget_page_"+Integer.parseInt(source.substring(7)),page).apply();
            workspacePage=page;updateVisibleWidgets();if(exp2())refreshWorkspace();}
        catch(NumberFormatException ignored){}
    }
    private int homePageCount(){
        int count=workspaceModel.pageCount();
        for(String id:profilePrefs().getString("widgets","").split(",")){
            try{count=Math.max(count,profilePrefs().getInt("widget_page_"+Integer.parseInt(id),0)+1);}
            catch(NumberFormatException ignored){}
        }
        return Math.max(1,count);
    }
    private boolean hasVisibleWidget(){
        if(widgetColumn==null)return false;
        for(int i=0;i<widgetColumn.getChildCount();i++)
            if(widgetColumn.getChildAt(i).getVisibility()==View.VISIBLE)return true;
        return false;
    }
    private void updateVisibleWidgets(){
        if(widgetColumn==null)return;
        boolean any=false;
        for(int i=0;i<widgetColumn.getChildCount();i++){
            View child=widgetColumn.getChildAt(i);
            if(child instanceof WidgetFrame){
                boolean visible=profilePrefs().getInt("widget_page_"+((WidgetFrame)child).widgetId,0)==workspacePage;
                child.setVisibility(visible?View.VISIBLE:View.GONE);any|=visible;
            }
        }
        // Keep the widget row in the weighted Home layout when empty so app cells do not shift.
        // Keep the gesture surface present even when the current page has no widget.
        // INVISIBLE would leave this whole upper Home area outside both the pager
        // and WidgetScrollView's horizontal arbitration after a restart.
        if(widgetScroller!=null)widgetScroller.setVisibility(View.VISIBLE);
    }
    private void openHomeFolder(String id){
        List<String> keys=workspaceModel.folderItems(id);ArrayList<MenuModel.Entry> entries=new ArrayList<>();
        for(String key:keys){MenuModel.Entry entry=homeEntry(key);if(entry!=null)entries.add(entry);}
        String[] labels=new String[entries.size()];for(int i=0;i<labels.length;i++)labels[i]=entries.get(i).label;
        new AlertDialog.Builder(this).setTitle(workspaceModel.folderName(id))
            .setItems(labels,(d,index)->launch(entries.get(index)))
            .setNeutralButton("Изменить",(d,w)->editHomeFolder(id))
            .setNegativeButton("Закрыть",null).show();
    }
    private void editHomeFolder(String id){
        new AlertDialog.Builder(this).setTitle(workspaceModel.folderName(id))
            .setItems(new String[]{"Переименовать","Извлечь приложение","Удалить папку"},(d,index)->{
                if(index==0){EditText name=new EditText(this);name.setText(workspaceModel.folderName(id));
                    new AlertDialog.Builder(this).setTitle("Переименовать папку").setView(name)
                        .setPositiveButton("OK",(a,b)->{workspaceModel.rename(id,name.getText().toString());refreshWorkspace();})
                        .setNegativeButton("Отмена",null).show();}
                else if(index==1){
                    List<String> keys=workspaceModel.folderItems(id);String[] names=new String[keys.size()];
                    for(int i=0;i<names.length;i++){MenuModel.Entry entry=homeEntry(keys.get(i));names[i]=entry==null?keys.get(i):entry.label;}
                    new AlertDialog.Builder(this).setTitle("Извлечь из папки").setItems(names,(a,b)->{
                        workspaceModel.removeFromFolder(keys.get(b));refreshWorkspace();
                    }).show();
                }else new AlertDialog.Builder(this).setTitle("Удалить папку?")
                    .setPositiveButton("Удалить",(a,b)->{workspaceModel.remove(id);refreshWorkspace();})
                    .setNegativeButton("Отмена",null).show();
            }).show();
    }
    private void startAppDrag(View view,String key){
        activeDragKey=key;activeDragView=view;crossedHomeEdge=false;homeDragMoved=false;
        homeDragOriginPage=workspacePage;
        drawerDragActive=drawerOpen;drawerCrossedEdge=false;drawerDragMoved=false;
        int[] origin=new int[2];view.getLocationOnScreen(origin);
        drawerDragStartX=origin[0]+view.getWidth()/2f;drawerDragStartY=origin[1]+view.getHeight()/2f;
        View shadow=view instanceof ViewGroup&&((ViewGroup)view).getChildCount()>0?((ViewGroup)view).getChildAt(0):view;
        if(view.startDragAndDrop(android.content.ClipData.newPlainText("app",key),new View.DragShadowBuilder(shadow),key,0)){
            view.animate().alpha(0.25f).setDuration(90).start();
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        }else{activeDragKey=null;activeDragView=null;}
    }
    private void showHomeItemOptions(MenuModel.Entry entry){
        if(workspaceModel.isFolder(entry.key)){editHomeFolder(entry.key);return;}
        showNokiaDialog(new AlertDialog.Builder(this).setTitle(entry.label)
            .setItems(new String[]{"Удалить с экрана","Изменить иконку","Переместить в док","Сведения о приложении"},(d,index)->{
                if(index==0){workspaceModel.remove(entry.key);refreshWorkspace();}
                else if(index==1)pickIcon(entry.key);
                else if(index==2)chooseDockSlot(entry);
                else if(entry.packageName!=null)safeStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+entry.packageName)));
            }));
    }
    private void buildDrawer(){
        drawer=new FrameLayout(this);drawer.setVisibility(View.GONE);
        root.addView(drawer,new FrameLayout.LayoutParams(-1,-1));
        ImageView blurred=new ImageView(this);blurred.setTag("blur");blurred.setScaleType(ImageView.ScaleType.CENTER_CROP);
        drawer.addView(blurred,new FrameLayout.LayoutParams(-1,-1));
        View dim=new View(this);dim.setTag("dim");
        drawer.addView(dim,new FrameLayout.LayoutParams(-1,-1));
        drawerShell=new LinearLayout(this);drawerShell.setOrientation(LinearLayout.VERTICAL);
        drawerShell.setPadding(dp(10),dp(58)+dp(4),dp(10),dp(6));
        drawer.addView(drawerShell,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setTag("drawerHeader");
        header.setBackgroundColor(Color.TRANSPARENT);
        drawerShell.addView(header,new LinearLayout.LayoutParams(-1,dp(40)));
        TextView back=label("‹",27,Color.WHITE);header.addView(back,new LinearLayout.LayoutParams(dp(35),-1));
        back.setOnClickListener(v->closeDrawer());
        TextView title=label("Меню",21,accent());title.setTag("drawerTitle");title.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        header.addView(title,new LinearLayout.LayoutParams(0,-1,1));
        TextView options=label("⋮",25,Color.WHITE);header.addView(options,new LinearLayout.LayoutParams(dp(35),-1));
        options.setOnClickListener(v->showDrawerOptions());
        pager=new DrawerPager(this);pager.setHorizontalScrollBarEnabled(false);pager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        pager.setFillViewport(true);pageStrip=new LinearLayout(this);pageStrip.setOrientation(LinearLayout.HORIZONTAL);
        pager.addView(pageStrip,new HorizontalScrollView.LayoutParams(-2,-1));
        pager.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            if(drawerOpen&&(r-l!=or-ol||b-t!=ob-ot))pager.post(this::rebuildPages);
        });
        drawerShell.addView(pager,new LinearLayout.LayoutParams(-1,0,1));
        pager.setOnDragListener((v,event)->{
            if(event.getAction()==DragEvent.ACTION_DRAG_STARTED)return event.getLocalState() instanceof String;
            if(event.getAction()==DragEvent.ACTION_DRAG_LOCATION){trackDrawerDrag(v,event);return true;}
            if(event.getAction()==DragEvent.ACTION_DRAG_ENDED){endDrawerDrag(event);return true;}
            if(event.getAction()==DragEvent.ACTION_DROP){
                String source=String.valueOf(event.getLocalState());
                if(source!=null&&folderId!=null){menu.organizer.moveToFolder(source,folderId);refreshGrid();return true;}
                if(source!=null&&!source.startsWith("widget:")){
                    applyDrawerDrop(source,drawerIndexAt(v,event),drawerDropOnIcon(v,event));return true;
                }
                return false;
            }
            return event.getAction()!=DragEvent.ACTION_DROP;
        });
        pageIndicator=label("",13,0xFFE4F2FA);
        drawerShell.addView(pageIndicator,new LinearLayout.LayoutParams(-1,dp(27)));
        pageIndicator.setOnDragListener((v,event)->{
            if(event.getAction()==DragEvent.ACTION_DRAG_STARTED){pageIndicator.setText("На главный экран");return true;}
            if(event.getAction()==DragEvent.ACTION_DRAG_ENTERED){
                if(drawerToHome!=null)handler.removeCallbacks(drawerToHome);
                drawerToHome=()->{drawerToHome=null;folderId=null;drawerOpen=false;drawer.setVisibility(View.GONE);updateSystemStatusBar();};
                handler.postDelayed(drawerToHome,320);return true;
            }
            if(event.getAction()==DragEvent.ACTION_DRAG_EXITED&&drawerToHome!=null){handler.removeCallbacks(drawerToHome);drawerToHome=null;}
            if(event.getAction()==DragEvent.ACTION_DROP){
                if(drawerToHome!=null){handler.removeCallbacks(drawerToHome);drawerToHome=null;}
                String key=String.valueOf(event.getLocalState());
                if(!key.startsWith("widget:")){workspaceModel.add(key);refreshWorkspace();closeDrawer();return true;}
            }
            if(event.getAction()==DragEvent.ACTION_DRAG_ENDED){
                if(drawerToHome!=null){handler.removeCallbacks(drawerToHome);drawerToHome=null;}
                endWorkspaceDrag(event);updatePageIndicator(Math.max(1,pageStrip.getChildCount()));
            }
            return true;
        });
        updateDrawerBackground();
    }
    private LinearLayout iconCell(MenuModel.Entry entry,int heightDp){
        LinearLayout cell=new LinearLayout(this);cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(3),dp(1),dp(3),dp(1));
        int iconSize=Math.max(dp(25),Math.min(dp(55),dp(heightDp-28)));
        ImageView icon=new ImageView(this);icon.setImageDrawable(icons.icon(entry));icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        cell.addView(icon,new LinearLayout.LayoutParams(iconSize,iconSize));
        TextView text=label(entry.label,12,Color.WHITE);text.setSingleLine(true);text.setEllipsize(TextUtils.TruncateAt.END);
        text.setShadowLayer(3,0,1,Color.BLACK);
        cell.addView(text,new LinearLayout.LayoutParams(-1,dp(24)));
        cell.setMinimumHeight(dp(heightDp));
        cell.setBackgroundColor(Color.TRANSPARENT);
        return cell;
    }
    private LinearLayout drawerCell(MenuModel.Entry entry,int rowDp){
        LinearLayout cell=iconCell(entry,rowDp);
        cell.setTag(entry.key);
        cell.setOnClickListener(v->{
            if(entry.folder()){sounds.tap();folderId=entry.key;currentPage=0;refreshGrid();}
            else launch(entry);
        });
        cell.setOnLongClickListener(v->{startAppDrag(v,entry.key);return true;});
        cell.setOnDragListener((v,event)->{
            if(event.getAction()==DragEvent.ACTION_DRAG_STARTED)return event.getLocalState() instanceof String;
            if(event.getAction()==DragEvent.ACTION_DRAG_LOCATION){trackDrawerDrag(v,event);return true;}
            if(event.getAction()==DragEvent.ACTION_DRAG_ENDED){
                endDrawerDrag(event);
                return true;
            }
            if(event.getAction()==DragEvent.ACTION_DROP){
                String source=String.valueOf(event.getLocalState());
                applyDrawerDrop(source,drawerEntries.indexOf(entry),drawerDropOnIcon(v,event));
                return true;
            }return true;
        });
        return cell;
    }
    private void rebuildPages(){
        if(pager==null||pageStrip==null||pager.getWidth()<=0||pager.getHeight()<=0)return;
        drawerEntries.clear();drawerEntries.addAll(items());
        int rows=profilePrefs().getInt("drawer_rows",6),perPage=3*rows;
        int pageWidth=pager.getWidth(),pageHeight=pager.getHeight();
        int pageCount=Math.max(1,(drawerEntries.size()+perPage-1)/perPage);
        currentPage=Math.max(0,Math.min(currentPage,pageCount-1));
        pageStrip.removeAllViews();
        int rowHeight=pageHeight/rows,rowDp=Math.round(rowHeight/getResources().getDisplayMetrics().density);
        for(int page=0;page<pageCount;page++){
            LinearLayout column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);
            column.setOnDragListener((v,event)->{
                if(event.getAction()==DragEvent.ACTION_DRAG_STARTED)return event.getLocalState() instanceof String;
                if(event.getAction()==DragEvent.ACTION_DRAG_LOCATION){trackDrawerDrag(v,event);return true;}
                if(event.getAction()==DragEvent.ACTION_DROP){
                    String key=String.valueOf(event.getLocalState());
                    if(!key.startsWith("widget:")){
                        applyDrawerDrop(key,drawerIndexAt(v,event),drawerDropOnIcon(v,event));return true;
                    }
                }
                if(event.getAction()==DragEvent.ACTION_DRAG_ENDED){endDrawerDrag(event);return true;}
                return true;
            });
            pageStrip.addView(column,new LinearLayout.LayoutParams(pageWidth,pageHeight));
            for(int row=0;row<rows;row++){
                LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.TOP);
                column.addView(line,new LinearLayout.LayoutParams(-1,rowHeight));
                for(int col=0;col<3;col++){
                    int index=page*perPage+row*3+col;
                    View cell=index<drawerEntries.size()?drawerCell(drawerEntries.get(index),rowDp):new View(this);
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);
                    lp.leftMargin=dp(2);lp.rightMargin=dp(2);line.addView(cell,lp);
                }
            }
        }
        drawerDirty=false;pager.scrollTo(currentPage*pageWidth,0);updatePageIndicator(pageCount);
    }
    private void updatePageIndicator(int count){
        if(pageIndicator!=null)pageIndicator.setText(count<=1?"":(currentPage+1)+" / "+count);
    }
    private int drawerIndexAt(View target,DragEvent event){
        int[] where=new int[2],pagerWhere=new int[2];target.getLocationOnScreen(where);pager.getLocationOnScreen(pagerWhere);
        float x=where[0]+event.getX()-pagerWhere[0],y=where[1]+event.getY()-pagerWhere[1];
        int rows=profilePrefs().getInt("drawer_rows",6),perPage=3*rows;
        int column=Math.max(0,Math.min(2,(int)(x/Math.max(1,pager.getWidth()/3))));
        int row=Math.max(0,Math.min(rows-1,(int)(y/Math.max(1,pager.getHeight()/rows))));
        return Math.min(drawerEntries.size()-1,currentPage*perPage+row*3+column);
    }
    private boolean drawerDropOnIcon(View target,DragEvent event){
        exp2DropIconKey=null;
        if(exp2()){
            exp2DropIconKey=exp2CenteredDrawerIcon(target,event);
            return exp2DropIconKey!=null;
        }
        int[] where=new int[2],pagerWhere=new int[2];target.getLocationOnScreen(where);pager.getLocationOnScreen(pagerWhere);
        float x=where[0]+event.getX()-pagerWhere[0],y=where[1]+event.getY()-pagerWhere[1];
        float cellWidth=Math.max(1f,pager.getWidth()/3f),cellHeight=Math.max(1f,pager.getHeight()/(float)profilePrefs().getInt("drawer_rows",6));
        float fractionX=(x%cellWidth)/cellWidth,fractionY=(y%cellHeight)/cellHeight;
        return fractionX>0.22f&&fractionX<0.78f&&fractionY>0.15f&&fractionY<0.78f;
    }
    private String exp2CenteredDrawerIcon(View target,DragEvent event){
        if(drawerEntries.isEmpty())return null;
        int[] where=new int[2],pagerWhere=new int[2];
        target.getLocationOnScreen(where);pager.getLocationOnScreen(pagerWhere);
        float x=where[0]+event.getX()-pagerWhere[0],y=where[1]+event.getY()-pagerWhere[1];
        int rows=profilePrefs().getInt("drawer_rows",6),perPage=3*rows;
        float width=pager.getWidth()/3f,height=pager.getHeight()/(float)rows;
        int column=(int)(x/Math.max(1f,width)),row=(int)(y/Math.max(1f,height));
        if(column<0||column>=3||row<0||row>=rows)return null;
        float fractionX=(x-column*width)/Math.max(1f,width);
        float fractionY=(y-row*height)/Math.max(1f,height);
        if(fractionX<0.29f||fractionX>0.71f||fractionY<0.13f||fractionY>0.65f)return null;
        int index=currentPage*perPage+row*3+column;
        return index<drawerEntries.size()?drawerEntries.get(index).key:null;
    }
    private void applyDrawerDrop(String source,int index,boolean onIcon){
        if(index<0||index>=drawerEntries.size()||source.startsWith("widget:"))return;
        MenuModel.Entry target=drawerEntries.get(index);
        if(exp2()&&onIcon&&exp2DropIconKey!=null)
            for(MenuModel.Entry entry:drawerEntries)if(entry.key.equals(exp2DropIconKey)){target=entry;break;}
        if(source.equals(target.key)){
            boolean context=!drawerDragMoved;View held=activeDragView;
            finishDrawerDrag();
            if(context&&held!=null){MenuModel.Entry selected=target;held.post(()->edit(selected,held));}
            return;
        }
        if(onIcon&&target.key.startsWith("userfolder:")&&source.startsWith("installed:"))
            menu.organizer.moveToFolder(source,target.key);
        else if(onIcon&&source.startsWith("installed:")&&target.key.startsWith("installed:")){
            String folder=menu.organizer.createFolder("Папка");
            menu.organizer.moveToFolder(target.key,folder);menu.organizer.moveToFolder(source,folder);
            menu.organizer.moveBefore(drawerEntries,folder,target.key);
        }else menu.organizer.moveToIndex(drawerEntries,source,index);
        finishDrawerDrag();refreshGrid();
    }
    private void trackDrawerDrag(View target,DragEvent event){
        if(!drawerDragActive||pager==null||pageStrip==null)return;
        int[] where=new int[2],pagerWhere=new int[2];target.getLocationOnScreen(where);pager.getLocationOnScreen(pagerWhere);
        float x=where[0]+event.getX()-pagerWhere[0];
        if(Math.abs(where[0]+event.getX()-drawerDragStartX)>dp(26)||
            Math.abs(where[1]+event.getY()-drawerDragStartY)>dp(26))drawerDragMoved=true;
        int edge=x<dp(32)?-1:x>pager.getWidth()-dp(32)?1:0;
        if(edge!=drawerEdge){cancelDrawerEdge();drawerEdge=edge;drawerEdgeLatched=false;}
        if(edge!=0&&!drawerEdgeLatched&&drawerEdgeTurn==null){
            drawerEdgeTurn=()->{
                drawerEdgeTurn=null;
                if(drawerDragActive&&drawerEdge!=0){
                    int next=Math.max(0,Math.min(currentPage+drawerEdge,pageStrip.getChildCount()-1));
                    if(next!=currentPage){drawerEdgeLatched=true;drawerCrossedEdge=true;clearDrawerPreview();settlePage(next,true);}
                }
            };
            handler.postDelayed(drawerEdgeTurn,380);
        }
        int index=drawerIndexAt(target,event);
        if(exp2()){
            String hovered=exp2CenteredDrawerIcon(target,event);
            if(hovered!=null&&!hovered.equals(activeDragKey)){
                drawerDropIndex=index;
                if(drawerHoverIndex!=-1)clearDrawerPreview();
                return;
            }
        }
        if(index!=drawerHoverIndex){drawerHoverIndex=index;drawerDropIndex=index;previewDrawerReorder(index);}
    }
    private void previewDrawerReorder(int targetIndex){
        if(currentPage>=pageStrip.getChildCount())return;
        View page=pageStrip.getChildAt(currentPage);if(!(page instanceof ViewGroup))return;
        int source=-1;for(int i=0;i<drawerEntries.size();i++)if(drawerEntries.get(i).key.equals(activeDragKey)){source=i;break;}
        int rows=profilePrefs().getInt("drawer_rows",6),perPage=3*rows,pageStart=currentPage*perPage;
        int cellWidth=pager.getWidth()/3,cellHeight=pager.getHeight()/rows;
        ViewGroup lines=(ViewGroup)page;
        for(int r=0;r<lines.getChildCount();r++){
            View row=lines.getChildAt(r);if(!(row instanceof ViewGroup))continue;
            ViewGroup cells=(ViewGroup)row;
            for(int c=0;c<cells.getChildCount();c++){
                View child=cells.getChildAt(c);int index=pageStart+r*3+c,destination=index;
                if(index>=drawerEntries.size()||child==activeDragView)continue;
                if(source>=pageStart&&source<pageStart+perPage){
                    if(source<targetIndex&&index>source&&index<=targetIndex)destination=index-1;
                    else if(source>targetIndex&&index>=targetIndex&&index<source)destination=index+1;
                }else if(index>=targetIndex)destination=index+1;
                if(destination>=pageStart+perPage)destination=index;
                child.animate().translationX((destination%3-index%3)*cellWidth)
                    .translationY(((destination-pageStart)/3-(index-pageStart)/3)*cellHeight).setDuration(100).start();
            }
        }
    }
    private void clearDrawerPreview(){
        if(pageStrip==null)return;
        for(int p=0;p<pageStrip.getChildCount();p++){
            View page=pageStrip.getChildAt(p);if(!(page instanceof ViewGroup))continue;
            ViewGroup lines=(ViewGroup)page;
            for(int r=0;r<lines.getChildCount();r++){
                View row=lines.getChildAt(r);if(!(row instanceof ViewGroup))continue;
                ViewGroup cells=(ViewGroup)row;
                for(int c=0;c<cells.getChildCount();c++)cells.getChildAt(c).animate()
                    .translationX(0).translationY(0).setDuration(90).start();
            }
        }
        drawerHoverIndex=-1;
    }
    private void cancelDrawerEdge(){if(drawerEdgeTurn!=null){handler.removeCallbacks(drawerEdgeTurn);drawerEdgeTurn=null;}}
    private void finishDrawerDrag(){
        cancelDrawerEdge();drawerEdge=0;drawerEdgeLatched=false;drawerCrossedEdge=false;drawerDragActive=false;
        clearDrawerPreview();finishWorkspaceDrag();
    }
    private void endDrawerDrag(DragEvent event){
        String held=activeDragKey;View heldView=activeDragView;
        boolean showContext=drawerDragActive&&!event.getResult()&&!drawerDragMoved&&!drawerCrossedEdge;
        if(drawerDragActive&&!event.getResult()&&drawerCrossedEdge&&activeDragKey!=null){
            menu.organizer.moveToIndex(drawerEntries,activeDragKey,drawerDropIndex);
            refreshGrid();
        }
        if(drawerDragActive)finishDrawerDrag();
        if(showContext&&heldView!=null)for(MenuModel.Entry entry:drawerEntries)if(entry.key.equals(held)){
            heldView.post(()->edit(entry,heldView));break;
        }
    }
    private void settlePage(int proposed,boolean animate){
        if(pager==null)return;
        int count=Math.max(1,pageStrip.getChildCount());
        currentPage=Math.max(0,Math.min(proposed,count-1));
        int target=currentPage*pager.getWidth();
        if(animate)pager.smoothScrollTo(target,0);else pager.scrollTo(target,0);
        updatePageIndicator(count);
    }
    private final class DrawerPager extends HorizontalScrollView{
        private boolean flung;
        private float downX;
        DrawerPager(Context context){super(context);}
        @Override public boolean dispatchTouchEvent(MotionEvent event){
            if(event.getAction()==MotionEvent.ACTION_DOWN){downX=event.getX();flung=false;}
            boolean handled=super.dispatchTouchEvent(event);
            if(event.getAction()==MotionEvent.ACTION_UP&&!flung&&getWidth()>0){
                float dx=event.getX()-downX;
                int next=Math.abs(dx)>getWidth()*0.18f?currentPage+(dx<0?1:-1):
                    Math.round((float)getScrollX()/getWidth());
                settlePage(next,true);
            }
            return handled;
        }
        @Override public void fling(int velocityX){
            flung=true;settlePage(currentPage+(velocityX>0?1:-1),true);
        }
    }
    private List<MenuModel.Entry> items(){
        ArrayList<MenuModel.Entry> list=new ArrayList<>();
        if(folderId==null){
            for(Map.Entry<String,String> f:menu.organizer.folders().entrySet())list.add(new MenuModel.Entry(f.getKey(),f.getValue(),"files"));
            list.addAll(menu.children(new MenuModel.Entry("installed","","")));
            list.add(new MenuModel.Entry("settings_launcher","Nokia Experience · Настройки","settings"));
            list.add(new MenuModel.Entry("settings_android","Android Settings","settings"));
        }else list.addAll(menu.children(new MenuModel.Entry(folderId,"","files")));
        list.sort(java.util.Comparator.comparingInt(e->menu.organizer.rank(e.key)));
        return list;
    }
    private String drawerSignature(){
        StringBuilder signature=new StringBuilder();
        for(MenuModel.Entry e:items())signature.append(e.key).append('\u0001').append(e.label).append('\u0002');
        return signature.toString();
    }
    private void refreshGrid(){
        drawerDirty=true;
        if(drawerShell!=null){TextView title=drawerShell.findViewWithTag("drawerTitle");
            if(title!=null)title.setText(folderId==null?"Меню":menu.organizer.folders().getOrDefault(folderId,"Папка"));}
        if(drawerOpen&&pager!=null)pager.post(this::rebuildPages);
    }
    private void openDrawer(){
        boolean wasFolder=folderId!=null;folderId=null;currentPage=0;
        drawerOpen=true;drawer.setVisibility(View.VISIBLE);updateSystemStatusBar();updateDrawerBackground();
        if(wasFolder||drawerDirty||pageStrip.getChildCount()==0)refreshGrid();
        else settlePage(0,false);
    }
    private void closeDrawer(){
        if(folderId!=null){folderId=null;currentPage=0;refreshGrid();return;}
        drawerOpen=false;drawer.setVisibility(View.GONE);updateSystemStatusBar();
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            swipeX=event.getX();swipeY=event.getY();swipeConsumed=false;
            upperSwipeCandidate=!exp2EditMode&&isInWidgetScrollArea(event.getRawX(),event.getRawY());upperSwipeConsumed=false;
            if(editingWidget!=null){
                int[] position=new int[2];editingWidget.getLocationOnScreen(position);
                if(event.getRawX()<position[0]||event.getRawX()>position[0]+editingWidget.getWidth()||
                    event.getRawY()<position[1]||event.getRawY()>position[1]+editingWidget.getHeight())hideWidgetHandles(editingWidget);
            }
        }
        if(swipeConsumed){if(event.getAction()==MotionEvent.ACTION_UP||event.getAction()==MotionEvent.ACTION_CANCEL)swipeConsumed=false;return true;}
        if(event.getAction()==MotionEvent.ACTION_MOVE){
            float dx=event.getX()-swipeX,dy=event.getY()-swipeY;
            if(Math.abs(dx)>touchSlop||Math.abs(dy)>touchSlop)cancelHomeLongPress();
            if(upperSwipeCandidate&&!upperSwipeConsumed&&Math.abs(dx)>touchSlop&&Math.abs(dx)>Math.abs(dy)){
                MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);cancel.recycle();upperSwipeConsumed=true;return true;
            }
            if(upperSwipeConsumed)return true;
            if(!drawerOpen&&(exp2HomeDrag==null||!exp2HomeDrag.pointerSequenceActive())&&
                dy<-dp(65)&&Math.abs(dy)>Math.abs(dx)*1.3f&&swipeY>root.getHeight()*0.30f){
                MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);cancel.recycle();openDrawer();swipeConsumed=true;return true;
            }
        }
        if(event.getAction()==MotionEvent.ACTION_UP){
            float dy=event.getY()-swipeY,dx=event.getX()-swipeX;
            if(upperSwipeConsumed){
                settleWorkspace(workspacePage+(dx<0?1:-1));upperSwipeConsumed=false;upperSwipeCandidate=false;return true;
            }
            if(Math.abs(dy)>dp(90)&&Math.abs(dy)>Math.abs(dx)*1.3f){
                if(drawerOpen&&dy>0){closeDrawer();return true;}
            }
            upperSwipeCandidate=false;
        }
        if(event.getAction()==MotionEvent.ACTION_CANCEL){upperSwipeConsumed=false;upperSwipeCandidate=false;}
        return super.dispatchTouchEvent(event);
    }
    private boolean isInWidgetScrollArea(float rawX,float rawY){
        if(widgetScroller==null||widgetScroller.getVisibility()!=View.VISIBLE)return false;
        int[] location=new int[2];widgetScroller.getLocationOnScreen(location);
        return rawX>=location[0]&&rawX<location[0]+widgetScroller.getWidth()&&
            rawY>=location[1]&&rawY<location[1]+widgetScroller.getHeight();
    }
    private void cancelHomeLongPress(){if(pendingHomeLongPress!=null){handler.removeCallbacks(pendingHomeLongPress);pendingHomeLongPress=null;}}
    private boolean emptyHomeTouch(View view,MotionEvent event){
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            cancelHomeLongPress();pendingHomeLongPress=()->{pendingHomeLongPress=null;
                if(exp2HomeEngineEnabled())enterExp2EditMode();else showHomeOptions();};
            handler.postDelayed(pendingHomeLongPress,ViewConfiguration.getLongPressTimeout());return true;
        }
        if(event.getAction()==MotionEvent.ACTION_MOVE){
            if(Math.abs(event.getRawX()-swipeX)>touchSlop||Math.abs(event.getRawY()-swipeY)>touchSlop)cancelHomeLongPress();
            return true;
        }
        if(event.getAction()==MotionEvent.ACTION_UP||event.getAction()==MotionEvent.ACTION_CANCEL){cancelHomeLongPress();return true;}
        return true;
    }
    @Override public void onBackPressed(){
        if(exp2EditMode){exitExp2EditMode();return;}
        if(drawerOpen)closeDrawer();/* Home remains the root destination. */
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(drawerOpen){folderId=null;drawerOpen=false;drawer.setVisibility(View.GONE);}updateSystemStatusBar();refresh();}
    @Override protected void onResume(){
        super.onResume();
        updateSystemStatusBar();
        try{widgetHost.startListening();}catch(Exception ignored){}
        if(!receiverRegistered){
            IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_WALLPAPER_CHANGED);
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIME_TICK);filter.addAction(Intent.ACTION_DATE_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
            filter.addAction(Intent.ACTION_HEADSET_PLUG);
            filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
            filter.addAction(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
            filter.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
            filter.addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED);
            filter.addAction("com.nokiaexperience.launcher.NOTIFICATIONS_CHANGED");
            registerReceiver(changes,filter);
            IntentFilter packages=new IntentFilter();
            packages.addAction(Intent.ACTION_PACKAGE_ADDED);packages.addAction(Intent.ACTION_PACKAGE_REMOVED);packages.addAction(Intent.ACTION_PACKAGE_CHANGED);
            packages.addDataScheme("package");registerReceiver(changes,packages);receiverRegistered=true;
        }
        String before=drawerSignature();menu.refreshInstalled();buildDock();
        if(!before.equals(drawerSignature())){refreshGrid();refreshWorkspace();}
        if(wallpaper.getDrawable()==null||(wallpaperFallback&&checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)==android.content.pm.PackageManager.PERMISSION_GRANTED))loadWallpaper();
        refresh();
        try{telephony=(TelephonyManager)getSystemService(TELEPHONY_SERVICE);if(telephony!=null)telephony.listen(signalListener,PhoneStateListener.LISTEN_SIGNAL_STRENGTHS|PhoneStateListener.LISTEN_SERVICE_STATE);}catch(Exception ignored){device.signalLevel=-1;}
        handler.removeCallbacks(tick);handler.post(tick);maybeStartup();
        if(!startupPending&&startup==null)maybeRequestWallpaperPermission();
    }
    @Override protected void onPause(){
        cancelHomeLongPress();
        if(exp2HomeDrag!=null&&exp2HomeDrag.active())exp2HomeDrag.cancel();
        handler.removeCallbacks(tick);stopStartupSound();
        if(startup!=null){startup.release();root.removeView(startup);startup=null;startupPending=true;}
        try{widgetHost.stopListening();}catch(Exception ignored){}
        try{if(telephony!=null)telephony.listen(signalListener,PhoneStateListener.LISTEN_NONE);}catch(Exception ignored){}
        if(receiverRegistered){unregisterReceiver(changes);receiverRegistered=false;}
        sounds.release();super.onPause();
    }
    @Override public void onWindowFocusChanged(boolean focus){
        super.onWindowFocusChanged(focus);
        if(focus){updateSystemStatusBar();maybeStartup();}
        else cancelHomeLongPress();
    }
    private void maybeStartup(){
        if(!startupPending||startup!=null)return;
        KeyguardManager keyguard=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        if(keyguard!=null&&keyguard.isKeyguardLocked())return;
        startupPending=false;
        startupAudioFinished=false;
        int startupFrameMs=startStartupSound();
        startup=new StartupView(this,startupFrameMs,this::stopStartupSound,()->{
            if(startup!=null){root.removeView(startup);startup=null;}
            maybeRequestWallpaperPermission();
        });
        startup.setHoldLastFrame(startupPlayer!=null);
        if(startupAudioFinished)startup.audioFinished();
        root.addView(startup,new FrameLayout.LayoutParams(-1,-1));
    }
    private int startStartupSound(){
        startupAudioFinished=!prefs.getBoolean("startup_sound",true);
        if(startupAudioFinished)return 65;
        try{
            AudioManager a=(AudioManager)getSystemService(AUDIO_SERVICE);
            if(a!=null&&a.getRingerMode()!=AudioManager.RINGER_MODE_NORMAL){startupAudioFinished=true;return 65;}
            startupPlayer=new MediaPlayer();
            startupPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            try(AssetFileDescriptor file=getAssets().openFd("n95/startup_sound_user.mp3")){
                startupPlayer.setDataSource(file.getFileDescriptor(),file.getStartOffset(),file.getLength());
            }
            startupPlayer.setOnCompletionListener(p->{startupAudioFinished=true;if(startup!=null)startup.audioFinished();});
            startupPlayer.setOnErrorListener((p,w,e)->{Log.e("NokiaStartup","MediaPlayer error "+w+"/"+e);stopStartupSound();return true;});
            startupPlayer.prepare();int duration=startupPlayer.getDuration();startupPlayer.start();
            // Twenty frames are kept; only the pacing is shortened so the
            // Connecting people sequence remains recognizable without
            // delaying Home for the old 1.3-4.4 second frame range.
            return Math.max(35,Math.min(100,duration>0?duration/20:65));
        }catch(Exception e){Log.e("NokiaStartup","sound unavailable",e);startupAudioFinished=true;stopStartupSound();return 65;}
    }
    private void stopStartupSound(){
        MediaPlayer p=startupPlayer;startupPlayer=null;
        if(p!=null){try{p.stop();}catch(Exception ignored){}try{p.release();}catch(Exception ignored){}}
    }
    private void refresh(){
        if(device==null)return;device.refresh(this);
        refreshStatusData();
    }
    @SuppressWarnings("deprecation")
    private void refreshStatusData(){
        Date now=new Date();Locale locale=Locale.getDefault();
        statusTime=java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT,locale).format(now);
        statusDate=new SimpleDateFormat("EEE dd/MM/yyyy",locale).format(now);
        statusOperator="";
        try{
            if(telephony==null)telephony=(TelephonyManager)getSystemService(TELEPHONY_SERVICE);
            if(telephony!=null&&telephony.getSimState()==TelephonyManager.SIM_STATE_READY){
                String name=telephony.getNetworkOperatorName();
                if(TextUtils.isEmpty(name))name=telephony.getSimOperatorName();
                if(name!=null)statusOperator=name.trim();
            }
        }catch(Exception ignored){}
        wifiEnabled=false;wifiConnected=false;wifiLevel=-1;
        try{
            WifiManager wifi=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
            wifiEnabled=wifi!=null&&wifi.isWifiEnabled();
            ConnectivityManager connectivity=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo info=connectivity==null?null:connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            wifiConnected=wifiEnabled&&info!=null&&info.isConnected();
            if(wifiConnected){WifiInfo connection=wifi.getConnectionInfo();
                if(connection!=null&&connection.getRssi()>-127)wifiLevel=WifiManager.calculateSignalLevel(connection.getRssi(),4);}
        }catch(Exception ignored){}
        bluetoothEnabled=false;bluetoothConnected=false;headsetConnected=false;
        try{BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
            bluetoothEnabled=adapter!=null&&adapter.isEnabled();
        }catch(Exception ignored){}
        try{AudioManager audio=(AudioManager)getSystemService(AUDIO_SERVICE);
            headsetConnected=audio!=null&&(audio.isWiredHeadsetOn()||audio.isBluetoothA2dpOn()||audio.isBluetoothScoOn());
            bluetoothConnected=bluetoothEnabled&&audio!=null&&(audio.isBluetoothA2dpOn()||audio.isBluetoothScoOn());
        }catch(Exception ignored){}
        alarmSet=false;
        try{alarmSet=!TextUtils.isEmpty(Settings.System.getString(getContentResolver(),Settings.System.NEXT_ALARM_FORMATTED));}catch(Exception ignored){}
        try{
            NfcAdapter nfc=NfcAdapter.getDefaultAdapter(this);
            nfcEnabled=nfc!=null&&nfc.isEnabled();
        }catch(Exception ignored){nfcEnabled=false;}
        try{airplaneMode=Settings.Global.getInt(getContentResolver(),Settings.Global.AIRPLANE_MODE_ON,0)!=0;}catch(Exception ignored){airplaneMode=false;}
        try{
            PowerManager power=(PowerManager)getSystemService(POWER_SERVICE);
            powerSaveMode=power!=null&&power.isPowerSaveMode();
        }catch(Exception ignored){powerSaveMode=false;}
        doNotDisturb=false;
        try{
            NotificationManager notifications=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            doNotDisturb=notifications!=null&&notifications.getCurrentInterruptionFilter()!=NotificationManager.INTERRUPTION_FILTER_ALL;
        }catch(Exception ignored){
            try{doNotDisturb=Settings.Global.getInt(getContentResolver(),"zen_mode",0)!=0;}catch(Exception ignoredAgain){doNotDisturb=false;}
        }
        notificationIndicators=NokiaNotificationListener.statusIndicators(this);
        if(statusGlyph!=null)statusGlyph.invalidate();
    }
    /** Returns the default-data SIM slot when Android exposes it; slot zero is the safe fallback. */
    private int activeSimSlot(){
        if(Build.VERSION.SDK_INT>=22){
            try{
                SubscriptionManager subscriptions=SubscriptionManager.from(this);
                int id=SubscriptionManager.getDefaultDataSubscriptionId();
                if(id!=SubscriptionManager.INVALID_SUBSCRIPTION_ID){
                    SubscriptionInfo info=subscriptions.getActiveSubscriptionInfo(id);
                    if(info!=null)return info.getSimSlotIndex()==1?1:0;
                }
            }catch(SecurityException ignored){}
            catch(Exception ignored){}
            try{
                // Android 8 exposes the selected data slot through the
                // readable global setting even when runtime phone-state
                // permission has not been granted to the launcher.
                int dataSlot=Settings.Global.getInt(getContentResolver(),"multi_sim_data_call",-1);
                if(dataSlot==1)return 1;
                if(dataSlot==0)return 0;
            }catch(Exception ignored){}
        }
        return 0;
    }
    private final class StatusGlyph extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        private final Paint notificationPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Drawable signalSegment,batterySegment,signalMark,signalMarkSim2,batteryMark;
        private final Drawable smsIcon,missedCallIcon,bluetoothIcon;
        private final Drawable[] frameworkWifiIcons=new Drawable[5];
        private final Drawable frameworkAlarm,frameworkNfc,frameworkAirplane,frameworkPowerSave;
        private final Drawable dndIcon;
        private final Path messageGlyph=new Path(),missedGlyph=new Path(),bluetoothGlyph=new Path(),headsetGlyph=new Path();
        private final Path nfcFallbackGlyph=new Path(),airplaneFallbackGlyph=new Path(),powerSaveFallbackGlyph=new Path();
        private final RectF[] notificationBounds={new RectF(),new RectF(),new RectF(),new RectF()};
        StatusGlyph(Context c){
            super(c);setContentDescription("Nokia: время, дата, сеть, Wi-Fi и батарея");
            signalSegment=exp2()?icon(R.drawable.p2_5_network_indicator):legacyIcon("signal_segment");
            batterySegment=exp2()?icon(R.drawable.p2_5_battery_indicator):legacyIcon("battery_segment");
            signalMark=exp2()?icon(R.drawable.p2_5_network_mark):legacyIcon("signal_mark");
            signalMarkSim2=exp2()?icon(R.drawable.p2_5_network_mark_sim2):signalMark;
            batteryMark=exp2()?icon(R.drawable.p2_5_battery_mark):legacyIcon("battery_mark");
            smsIcon=exp2()?icon(R.drawable.p2_5_sms):null;
            missedCallIcon=exp2()?icon(R.drawable.p2_5_missed_call):null;
            bluetoothIcon=exp2()?icon(R.drawable.p2_5_bluetooth):null;
            dndIcon=exp2()?icon(R.drawable.p2_5_dnd):null;
            for(int i=0;i<frameworkWifiIcons.length;i++){
                frameworkWifiIcons[i]=systemUiIcon("stat_sys_wifi_signal_"+i);
                if(frameworkWifiIcons[i]==null)frameworkWifiIcons[i]=frameworkIcon("stat_sys_wifi_signal_"+i);
            }
            frameworkAlarm=systemOrFrameworkIcon("stat_sys_alarm");
            frameworkNfc=systemOrFrameworkIcon("stat_sys_nfc");
            frameworkAirplane=systemOrFrameworkIcon("stat_sys_airplane_mode");
            frameworkPowerSave=systemOrFrameworkIcon("stat_sys_battery_saver");
            // Clean, size-matched traces of the supplied S60 status screenshots.
            // The tiny screenshot crops remain in assets as references, not firmware-original vectors.
            messageGlyph.moveTo(1.5f,3.5f);messageGlyph.lineTo(14.5f,3.5f);
            messageGlyph.lineTo(14.5f,12.5f);messageGlyph.lineTo(1.5f,12.5f);messageGlyph.close();
            messageGlyph.moveTo(1.8f,4f);messageGlyph.lineTo(8f,8.8f);messageGlyph.lineTo(14.2f,4f);
            missedGlyph.moveTo(4.5f,3.5f);missedGlyph.cubicTo(2f,4.5f,2f,8f,4.5f,10f);
            missedGlyph.cubicTo(5.8f,11f,7.2f,11.5f,10f,11.5f);
            missedGlyph.lineTo(13.5f,11.5f);missedGlyph.lineTo(13.5f,3f);
            missedGlyph.moveTo(10.5f,6f);missedGlyph.lineTo(13.5f,3f);
            missedGlyph.lineTo(15f,6f);
            bluetoothGlyph.moveTo(7f,1.3f);bluetoothGlyph.lineTo(12f,5f);
            bluetoothGlyph.lineTo(4f,11.5f);
            bluetoothGlyph.moveTo(7f,14.7f);bluetoothGlyph.lineTo(12f,10.8f);
            bluetoothGlyph.lineTo(4f,4.5f);
            bluetoothGlyph.moveTo(7f,1.3f);bluetoothGlyph.lineTo(7f,14.7f);
            headsetGlyph.moveTo(2.5f,9.5f);headsetGlyph.lineTo(2.5f,7.5f);
            headsetGlyph.cubicTo(2.5f,0.5f,13.5f,0.5f,13.5f,7.5f);
            headsetGlyph.lineTo(13.5f,9.5f);
            nfcFallbackGlyph.moveTo(3f,3f);nfcFallbackGlyph.lineTo(13f,3f);nfcFallbackGlyph.lineTo(13f,13f);nfcFallbackGlyph.lineTo(3f,13f);nfcFallbackGlyph.close();
            nfcFallbackGlyph.moveTo(5f,6f);nfcFallbackGlyph.cubicTo(7f,4f,9f,4f,11f,6f);
            nfcFallbackGlyph.moveTo(5f,10f);nfcFallbackGlyph.cubicTo(7f,8f,9f,8f,11f,10f);
            airplaneFallbackGlyph.moveTo(2f,8f);airplaneFallbackGlyph.lineTo(14f,8f);
            airplaneFallbackGlyph.moveTo(7f,8f);airplaneFallbackGlyph.lineTo(5f,3f);airplaneFallbackGlyph.lineTo(7f,3f);airplaneFallbackGlyph.lineTo(9f,8f);
            airplaneFallbackGlyph.moveTo(7f,8f);airplaneFallbackGlyph.lineTo(5f,13f);airplaneFallbackGlyph.lineTo(7f,13f);airplaneFallbackGlyph.lineTo(9f,8f);
            powerSaveFallbackGlyph.moveTo(9f,1.5f);powerSaveFallbackGlyph.lineTo(4f,8f);powerSaveFallbackGlyph.lineTo(7f,8f);powerSaveFallbackGlyph.lineTo(5.5f,14.5f);powerSaveFallbackGlyph.lineTo(12f,6.5f);powerSaveFallbackGlyph.lineTo(9f,6.5f);powerSaveFallbackGlyph.close();
            messageGlyph.computeBounds(notificationBounds[0],true);
            missedGlyph.computeBounds(notificationBounds[1],true);
            bluetoothGlyph.computeBounds(notificationBounds[2],true);
            headsetGlyph.computeBounds(notificationBounds[3],true);
            notificationBounds[3].union(1f,13f);notificationBounds[3].union(15f,13f);
            notificationPaint.setColor(Color.WHITE);
            notificationPaint.setStrokeCap(Paint.Cap.ROUND);
            notificationPaint.setStrokeJoin(Paint.Join.ROUND);
        }
        private Drawable icon(int id){
            try{return getResources().getDrawable(id,null);}catch(Exception ignored){return null;}
        }
        private Drawable legacyIcon(String name){
            try(InputStream stream=getAssets().open("reference/"+name+".png")){
                Bitmap bitmap=BitmapFactory.decodeStream(stream);
                return bitmap==null?null:new android.graphics.drawable.BitmapDrawable(getResources(),bitmap);
            }catch(Exception ignored){return null;}
        }
        private Drawable frameworkIcon(String name){
            try{
                int id=getResources().getIdentifier(name,"drawable","android");
                if(id==0)return null;
                return getResources().getDrawable(id,null);
            }catch(Exception ignored){return null;}
        }
        private Drawable systemUiIcon(String name){
            try{
                Context systemUi=createPackageContext("com.android.systemui",Context.CONTEXT_IGNORE_SECURITY);
                int id=systemUi.getResources().getIdentifier(name,"drawable","com.android.systemui");
                return id==0?null:systemUi.getResources().getDrawable(id,null);
            }catch(Exception ignored){return null;}
        }
        private Drawable systemOrFrameworkIcon(String name){
            Drawable drawable=systemUiIcon(name);
            return drawable==null?frameworkIcon(name):drawable;
        }
        private void stamp(Canvas canvas,Drawable image,float left,float top,float width,float height){
            if(image==null)return;image.setTint(statusIconColor());image.setBounds(Math.round(left),Math.round(top),Math.round(left+width),Math.round(top+height));image.draw(canvas);
        }
        private void indicator(Canvas canvas,Drawable image,float left,float top,float width,float height,int level){
            if(image==null)return;image.setTint(statusIconColor());canvas.save();
            // The supplied artwork is composed from the bottom segment up.
            // Clip from the bottom so a near-full charging level animates the
            // top segment instead of making the lowest segment blink.
            float fraction=Math.max(0,Math.min(7,level))/7f;
            canvas.clipRect(left,top+height*(1f-fraction),left+width,top+height);
            image.setBounds(Math.round(left),Math.round(top),Math.round(left+width),Math.round(top+height));image.draw(canvas);canvas.restore();
        }
        private void drawNotification(Canvas canvas,Path glyph,int kind,float left){
            canvas.save();
            if(exp2()){
                RectF bounds=notificationBounds[kind];
                float slot=dp(18);
                float scale=Math.min(slot/Math.max(1f,bounds.width()),slot/Math.max(1f,bounds.height()));
                float centerY=dp(12);
                canvas.translate(left+(slot-bounds.width()*scale)/2f-bounds.left*scale,
                    centerY-(bounds.top+bounds.height()/2f)*scale);
                canvas.scale(scale,scale);
            }else{
                canvas.translate(left,dp(3));
                float scale=dp(15)/16f;canvas.scale(scale,scale);
            }
            notificationPaint.setStyle(Paint.Style.STROKE);
            notificationPaint.setStrokeWidth(kind==1?2.2f:1.7f);
            canvas.drawPath(glyph,notificationPaint);
            if(kind==3){
                notificationPaint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(1f,8.5f,4f,13f,0.8f,0.8f,notificationPaint);
                canvas.drawRoundRect(12f,8.5f,15f,13f,0.8f,0.8f,notificationPaint);
            }
            canvas.restore();
        }
        private int drawNotifications(Canvas c){
            int count=0;float x=getWidth()-dp(62);
            if((notificationIndicators&1)!=0){
                if(smsIcon!=null)drawNotificationDrawable(c,smsIcon,x);else drawNotification(c,messageGlyph,0,x);
                x-=dp(20);count++;
            }
            if((notificationIndicators&2)!=0){
                if(missedCallIcon!=null)drawNotificationDrawable(c,missedCallIcon,x);else drawNotification(c,missedGlyph,1,x);
                x-=dp(20);count++;
            }
            if(bluetoothEnabled){
                if(bluetoothIcon!=null)drawNotificationDrawable(c,bluetoothIcon,x);else drawNotification(c,bluetoothGlyph,2,x);
                x-=dp(20);count++;
            }
            if(headsetConnected){drawNotification(c,headsetGlyph,3,x);x-=dp(20);count++;}
            if(alarmSet){drawFrameworkStatusIcon(c,frameworkAlarm,x);x-=dp(20);count++;}
            if(nfcEnabled){drawFrameworkStatusIcon(c,frameworkNfc,x,nfcFallbackGlyph);x-=dp(20);count++;}
            if(airplaneMode){drawFrameworkStatusIcon(c,frameworkAirplane,x,airplaneFallbackGlyph);x-=dp(20);count++;}
            if(powerSaveMode){drawFrameworkStatusIcon(c,frameworkPowerSave,x,powerSaveFallbackGlyph);count++;}
            if(doNotDisturb&&dndIcon!=null){x-=dp(20);drawNotificationDrawable(c,dndIcon,x);count++;}
            return count;
        }
        private void drawNotificationDrawable(Canvas c,Drawable icon,float centerX){
            int size=Math.round(dp(18));
            icon.setTint(statusIconColor());
            // All status glyphs share the same optical center. The previous
            // top=9dp slot put SVG notifications lower than SystemUI glyphs
            // that are centered at 12dp.
            float centerY=dp(12);
            icon.setBounds(Math.round(centerX-size/2f),Math.round(centerY-size/2f),
                Math.round(centerX+size/2f),Math.round(centerY+size/2f));
            icon.draw(c);
        }
        private void drawFrameworkStatusIcon(Canvas c,Drawable icon,float centerX){
            drawFrameworkStatusIcon(c,icon,centerX,null,0);
        }
        private void drawFrameworkStatusIcon(Canvas c,Drawable icon,float centerX,Path fallback){
            drawFrameworkStatusIcon(c,icon,centerX,fallback,0);
        }
        private void drawFrameworkStatusIcon(Canvas c,Drawable icon,float centerX,float opticalYOffset){
            drawFrameworkStatusIcon(c,icon,centerX,null,opticalYOffset);
        }
        private void drawFrameworkStatusIcon(Canvas c,Drawable icon,float centerX,Path fallback,float opticalYOffset){
            if(icon==null&&fallback==null)return;
            int size=Math.round(dp(18));
            if(icon!=null){
                icon.setTint(statusIconColor());
                // Keep native status glyphs on the same optical center line as
                // the Wi-Fi glyph (center Y = 12dp). The old fixed top=9dp
                // placed alarm/power-save lower than Wi-Fi.
                float centerY=dp(12)+opticalYOffset;
                icon.setBounds(Math.round(centerX-size/2f),Math.round(centerY-size/2f),
                    Math.round(centerX+size/2f),Math.round(centerY+size/2f));
                icon.draw(c);
            }else{
                c.save();
                c.translate(centerX-dp(9),dp(12)-dp(9));c.scale(dp(18)/15f,dp(18)/15f);
                notificationPaint.setColor(statusIconColor());notificationPaint.setStyle(Paint.Style.STROKE);notificationPaint.setStrokeWidth(1.7f);
                notificationPaint.setStrokeCap(Paint.Cap.ROUND);notificationPaint.setStrokeJoin(Paint.Join.ROUND);
                c.drawPath(fallback,notificationPaint);c.restore();
            }
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            p.setColor(Color.WHITE);p.setAlpha(255);
            int rightIcons=drawNotifications(c);
            float pixelsPerDp=getResources().getDisplayMetrics().density;
            // Match FRAME Экран in physical pixels: the supplied reference
            // uses tall 100px indicators and marks at the lower outer edges.
            float signalIndicatorLeft=dp(3f/pixelsPerDp);
            float signalIndicatorWidth=dp(42f/pixelsPerDp);
            float batteryIndicatorLeft=getWidth()-dp(46f/pixelsPerDp);
            float batteryIndicatorWidth=dp(43f/pixelsPerDp);
            float signalMarkWidth=dp(35f/pixelsPerDp);
            float signalMarkHeight=dp(38f/pixelsPerDp);
            float batteryMarkWidth=dp(40f/pixelsPerDp);
            float batteryMarkHeight=dp(46f/pixelsPerDp);
            // Optical centering correction for the transparent SVG margins.
            stamp(c,activeSimSlot()==1?signalMarkSim2:signalMark,dp(-2f/pixelsPerDp),dp(103f/pixelsPerDp),signalMarkWidth,signalMarkHeight);
            stamp(c,batteryMark,getWidth()-dp(36f/pixelsPerDp),dp(101f/pixelsPerDp),batteryMarkWidth,batteryMarkHeight);
            int signalBars=device.signalLevel<0?0:Math.min(7,Math.round(device.signalLevel*7f/4f));
            int batteryBars=device.batteryPercent<0?0:device.batteryPercent>=100?7:Math.max(1,(device.batteryPercent*7)/100);
            if(device.batteryPercent>=0&&device.batteryPercent<=15&&!device.charging){
                batteryBars=(SystemClock.uptimeMillis()/650)%2==0?Math.max(1,batteryBars):0;
                postInvalidateDelayed(650);
            }else if(device.charging&&device.batteryPercent>=0&&device.batteryPercent<100){
                batteryBars=Math.min(7,batteryBars+(int)((SystemClock.uptimeMillis()/340)%(8-batteryBars)));
                postInvalidateDelayed(340);
            }
            // Left and right groups use one X-zone each. The mark occupies a
            // separate lower slot; the right group is kept outside notifications.
            indicator(c,signalSegment,signalIndicatorLeft,dp(3f/pixelsPerDp),signalIndicatorWidth,dp(100f/pixelsPerDp),signalBars);
            indicator(c,batterySegment,batteryIndicatorLeft,0, batteryIndicatorWidth,dp(101f/pixelsPerDp),batteryBars);
            if(device.batteryPercent>=0){
                p.setTextAlign(Paint.Align.RIGHT);
                p.setTextSize(getResources().getDisplayMetrics().scaledDensity*8);
                c.drawText(device.batteryPercent+"%",getWidth()-dp(36f/pixelsPerDp),dp(135f/pixelsPerDp),p);
                p.setTextAlign(Paint.Align.LEFT);
            }
            p.setColor(Color.WHITE);p.setTypeface(android.graphics.Typeface.create("sans-serif-condensed",android.graphics.Typeface.BOLD));
            p.setShadowLayer(dp(2),0,dp(1),Color.BLACK);
            float textX=dp(19);
            p.setTextSize(getResources().getDisplayMetrics().scaledDensity*17);
            c.drawText(statusTime,textX,dp(19),p);
            float operatorX=textX+p.measureText(statusTime)+dp(7);
            float wifiX=getWidth()-dp(62+rightIcons*20);
            p.setTextSize(getResources().getDisplayMetrics().scaledDensity*11);
            if(!statusOperator.isEmpty()&&operatorX<wifiX-dp(8)){
                String operator=statusOperator;
                float room=wifiX-operatorX-dp(5);
                while(operator.length()>1&&p.measureText(operator)>room)operator=operator.substring(0,operator.length()-1);
                if(!operator.equals(statusOperator))operator+="…";
                c.drawText(operator,operatorX,dp(18),p);
            }
            p.setTypeface(android.graphics.Typeface.create("sans-serif-condensed",android.graphics.Typeface.NORMAL));
            p.setTextSize(getResources().getDisplayMetrics().scaledDensity*11);
            c.drawText(statusDate,textX,dp(37),p);
            p.clearShadowLayer();
            if(wifiEnabled){
                drawFrameworkWifi(c,wifiX-dp(2),dp(12),wifiConnected,wifiLevel);
            }
        }
        private void drawFrameworkWifi(Canvas c,float centerX,float centerY,boolean connected,int level){
            Drawable wifi=null;
            if(connected&&level>=0&&level<frameworkWifiIcons.length)wifi=frameworkWifiIcons[level];
            if(wifi==null&&connected)wifi=frameworkWifiIcons[0];
            if(wifi==null){drawWifiGlyph(c,centerX,centerY,connected,level);return;}
            int size=Math.round(dp(18));
            wifi.setTint(connected?statusIconColor():0x99FFFFFF);
            wifi.setBounds(Math.round(centerX-size/2f),Math.round(centerY-size/2f),
                Math.round(centerX+size/2f),Math.round(centerY+size/2f));
            wifi.draw(c);
        }
        private void drawWifiGlyph(Canvas c,float centerX,float centerY,boolean connected,int level){
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(dp(1.4f));
            p.setColor(connected?Color.WHITE:0x99FFFFFF);
            float r=dp(8);
            c.drawArc(new RectF(centerX-r,centerY-r,centerX+r,centerY+r),225,90,false,p);
            r=dp(5.2f);
            c.drawArc(new RectF(centerX-r,centerY-r,centerX+r,centerY+r),225,90,false,p);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(centerX,centerY+dp(4.3f),dp(1.5f),p);
            p.setStrokeCap(Paint.Cap.BUTT);
        }
    }
    private void showStatusActions(){
        String[] choices={"Батарея","Мобильная сеть","Wi-Fi","Bluetooth","Звук","Уведомления Android"};
        String[] actions={Settings.ACTION_BATTERY_SAVER_SETTINGS,Settings.ACTION_WIRELESS_SETTINGS,Settings.ACTION_WIFI_SETTINGS,Settings.ACTION_BLUETOOTH_SETTINGS,Settings.ACTION_SOUND_SETTINGS,Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS};
        new AlertDialog.Builder(this).setTitle("Состояние устройства").setItems(choices,(d,which)->safeStart(new Intent(actions[which]))).show();
    }
    public void launch(MenuModel.Entry e){
        if(e.key.equals("chrome")){
            MenuModel.Entry chrome=null;
            for(MenuModel.Entry app:allInstalled())if(app.label.equalsIgnoreCase("Chrome")){chrome=app;break;}
            resolver.launch(chrome==null?new MenuModel.Entry("browser","Browser","browser"):chrome);return;
        }
        resolver.launch(e);
    }
    public void openSettings(){showSettings();}
    private void safeStart(Intent intent){try{if(intent.resolveActivity(getPackageManager())!=null)startActivity(intent);else Toast.makeText(this,"Недоступно на этом устройстве",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Недоступно на этом устройстве",Toast.LENGTH_SHORT).show();}}
    private void edit(MenuModel.Entry e,View sourceView){
        ArrayList<String> actions=new ArrayList<>(Arrays.asList("Добавить на главный экран","В док","Изменить иконку","Сбросить иконку","Переместить в папку"));
        if(e.key.startsWith("userfolder:")){actions.add("Переименовать папку");actions.add("Удалить папку");}
        if(e.key.startsWith("installed:")){actions.add("Скрыть");actions.add("Удалить");actions.add("Сведения о приложении");}
        showNokiaDialog(new AlertDialog.Builder(this).setTitle(e.label).setItems(actions.toArray(new String[0]),(d,index)->{
            switch(actions.get(index)){
                case "Добавить на главный экран":workspaceModel.add(e.key);refreshWorkspace();Toast.makeText(this,"Добавлено на главный экран",Toast.LENGTH_SHORT).show();break;
                case "В док":chooseDockSlot(e);break;
                case "Изменить иконку":pickIcon(e.key);break;
                case "Сбросить иконку":icons.reset(e.key);refreshGrid();buildDock();refreshWorkspace();break;
                case "Переместить в папку":moveToFolder(e);break;
                case "Переименовать папку":renameFolder(e);break;
                case "Удалить папку":new AlertDialog.Builder(this).setTitle("Удалить папку?")
                    .setPositiveButton("Удалить",(confirm,which)->{menu.organizer.deleteFolder(e.key);refreshGrid();})
                    .setNegativeButton("Отмена",null).show();break;
                case "Скрыть":menu.organizer.hide(e.key);refreshGrid();break;
                case "Удалить":safeStart(new Intent(Intent.ACTION_DELETE,Uri.parse("package:"+e.packageName)));break;
                case "Сведения о приложении":safeStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+e.packageName)));break;
            }
        }));
    }
    private void reorder(MenuModel.Entry e){
        String[] actions={"В начало","Вверх","Вниз"};
        new AlertDialog.Builder(this).setTitle("Порядок: "+e.label).setItems(actions,(d,n)->{
            List<MenuModel.Entry> list=items();int index=list.indexOf(e);
            if(index<0)for(int i=0;i<list.size();i++)if(list.get(i).key.equals(e.key)){index=i;break;}
            if(index>=0){if(n==0)menu.organizer.moveBefore(list,e.key,list.get(0).key);else menu.organizer.move(list,index,n==1?-1:1);refreshGrid();}
        }).show();
    }
    private void chooseDockSlot(MenuModel.Entry e){
        String[] positions=new String[dockCount()];for(int i=0;i<positions.length;i++)positions[i]="Позиция "+(i+1);
        new AlertDialog.Builder(this).setTitle("Ярлык Home").setItems(positions,(d,n)->{
            profilePrefs().edit().putString("shortcut_"+n,e.key).apply();buildDock();
        }).show();
    }
    private void chooseShortcut(int slot){
        ArrayList<MenuModel.Entry> choices=new ArrayList<>(allInstalled());
        choices.add(0,special("phone"));choices.add(1,special("chrome"));choices.add(2,special("camera"));
        String[] labels=new String[choices.size()];for(int i=0;i<labels.length;i++)labels[i]=choices.get(i).label;
        new AlertDialog.Builder(this).setTitle("Ярлык "+(slot+1)).setItems(labels,(d,n)->{profilePrefs().edit().putString("shortcut_"+slot,choices.get(n).key).apply();buildDock();}).show();
    }
    private void moveToFolder(MenuModel.Entry e){
        ArrayList<String> ids=new ArrayList<>(),names=new ArrayList<>();
        ids.add("");names.add("Все приложения");
        for(Map.Entry<String,String> f:menu.organizer.folders().entrySet()){ids.add(f.getKey());names.add(f.getValue());}
        names.add("Создать папку…");
        new AlertDialog.Builder(this).setTitle("Переместить").setItems(names.toArray(new String[0]),(d,n)->{
            if(n==names.size()-1)createFolder(e.key);
            else if(n==0)menu.organizer.moveToApplications(e.key);
            else menu.organizer.moveToFolder(e.key,ids.get(n));
            refreshGrid();
        }).show();
    }
    private void createFolder(String appKey){
        EditText input=new EditText(this);input.setSingleLine(true);input.setHint("Название");
        new AlertDialog.Builder(this).setTitle("Новая папка").setView(input).setPositiveButton("Создать",(d,w)->{
            String name=input.getText().toString().trim();
            if(!name.isEmpty()){String id=menu.organizer.createFolder(name);if(appKey!=null)menu.organizer.moveToFolder(appKey,id);refreshGrid();}
        }).setNegativeButton("Отмена",null).show();
    }
    private void createFolderWith(String first,String second){
        EditText input=new EditText(this);input.setSingleLine(true);input.setHint("Название папки");
        new AlertDialog.Builder(this).setTitle("Новая папка").setView(input).setPositiveButton("Создать",(d,w)->{
            String name=input.getText().toString().trim();if(name.isEmpty())name="Папка";
            String id=menu.organizer.createFolder(name);menu.organizer.moveToFolder(first,id);menu.organizer.moveToFolder(second,id);refreshGrid();
        }).setNegativeButton("Отмена",null).show();
    }
    private void renameFolder(MenuModel.Entry e){
        EditText input=new EditText(this);input.setSingleLine(true);input.setText(e.label);
        new AlertDialog.Builder(this).setTitle("Переименовать").setView(input).setPositiveButton("OK",(d,w)->{
            menu.organizer.renameFolder(e.key,input.getText().toString().trim());refreshGrid();
        }).setNegativeButton("Отмена",null).show();
    }
    private void pickIcon(String key){
        iconTarget=key;
        safeStartForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"),PICK_ICON);
    }
    private void safeStartForResult(Intent i,int request){
        try{if(i.resolveActivity(getPackageManager())!=null)startActivityForResult(i,request);else Toast.makeText(this,"Системный выбор недоступен",Toast.LENGTH_SHORT).show();}
        catch(Exception e){Toast.makeText(this,"Системный выбор недоступен",Toast.LENGTH_SHORT).show();}
    }
    private void showHomeOptions(){
        showNokiaDialog(new AlertDialog.Builder(this).setTitle("Nokia Experience").setItems(new String[]{"Добавить виджет","Настройки","Сменить обои"},(d,n)->{
            if(n==0)addWidget();else if(n==1)showSettings();else chooseWallpaper();
        }));
    }

    /** Exp2-only edit mode. It is a presentation/controller layer over the existing Home model. */
    private void enterExp2EditMode(){
        if(!exp2()||exp2EditMode||root==null)return;
        cancelHomeLongPress();
        exp2EditMode=true;
        exp2EditOverlay=new FrameLayout(this);
        exp2EditOverlay.setBackgroundColor(Color.TRANSPARENT);
        exp2EditOverlay.setClickable(true);
        ImageView backdrop=new ImageView(this);backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap backdropBitmap=exp2EditBackdrop();if(backdropBitmap!=null)backdrop.setImageBitmap(backdropBitmap);
        exp2EditOverlay.addView(backdrop,new FrameLayout.LayoutParams(-1,-1));
        View veil=new View(this);veil.setBackgroundColor(0x551A3045);exp2EditOverlay.addView(veil,new FrameLayout.LayoutParams(-1,-1));
        int screenHeight=getResources().getDisplayMetrics().heightPixels;
        int bottomReserve=Math.round(screenHeight*99f/1920f);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(0,dp(8),0,bottomReserve);
        LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=label("Рабочие столы",17,Color.WHITE);heading.addView(title,new LinearLayout.LayoutParams(-1,dp(34)));
        title.setShadowLayer(dp(2),0,dp(1),0xCC102131);
        heading.setPadding(dp(38),0,dp(38),0);panel.addView(heading,new LinearLayout.LayoutParams(-1,dp(36)));
        exp2EditPreviewRow=new LinearLayout(this);exp2EditPreviewRow.setGravity(Gravity.CENTER_VERTICAL);
        EditPreviewScrollView previewsScroll=new EditPreviewScrollView(this);previewsScroll.setHorizontalScrollBarEnabled(false);previewsScroll.setFillViewport(false);
        previewsScroll.setTranslationY(-dp(8));
        previewsScroll.addView(exp2EditPreviewRow,new HorizontalScrollView.LayoutParams(-2,-1));
        panel.addView(previewsScroll,new LinearLayout.LayoutParams(-1,0,1));
        exp2EditIndicator=label("",13,Color.WHITE);exp2EditIndicator.setShadowLayer(dp(2),0,dp(1),0xCC102131);exp2EditIndicator.setTranslationY(-dp(20));panel.addView(exp2EditIndicator,new LinearLayout.LayoutParams(-1,dp(38)));
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);actions.setPadding(dp(42),dp(8),dp(42),dp(6));
        actions.setBackgroundColor(Color.TRANSPARENT);
        addExp2Action(actions,"Обои",this::chooseWallpaper);
        addExp2Action(actions,"Темы",this::showAppearanceSettings);
        addExp2Action(actions,"Виджеты",this::addWidget);
        addExp2Action(actions,"Настройки экрана",this::showSettings);
        int actionHeight=Math.round(screenHeight*207f/1920f);
        panel.addView(actions,new LinearLayout.LayoutParams(-1,actionHeight));
        exp2EditOverlay.addView(panel,new FrameLayout.LayoutParams(-1,-1));
        root.addView(exp2EditOverlay,new FrameLayout.LayoutParams(-1,-1));
        if(statusGlyph!=null)statusGlyph.bringToFront();
        rebuildExp2EditPreviews();
        exp2EditOverlay.setAlpha(0f);exp2EditOverlay.setTranslationY(dp(22));
        exp2EditOverlay.animate().alpha(1f).translationY(0f).setDuration(180).start();
        updateSystemStatusBar();
    }
    private void rebuildExp2EditPreviews(){
        if(exp2EditPreviewRow==null)return;exp2EditPreviewRow.removeAllViews();
        exp2PageBitmapCache.clear();
        int count=Math.max(1,homePageCount());
        for(int i=0;i<count;i++)addExp2PagePreview(i,false);
        // The last child remains an in-memory destination for Drag/Drop, but
        // Edit Mode exposes it as an explicit add-page action instead of
        // presenting it as an already existing screen.
        if(count<workspaceStrip.getChildCount())addExp2NewPageCard();
        updateExp2EditSelection();
    }
    private int exp2PreviewCardWidth(){return Math.round(getResources().getDisplayMetrics().widthPixels*688f/1080f);}
    private int exp2FullHomeWidth(){return Math.max(1,home==null?getResources().getDisplayMetrics().widthPixels:home.getWidth());}
    private int exp2FullHomeHeight(){return Math.max(1,home==null?getResources().getDisplayMetrics().heightPixels:home.getHeight());}
    private int exp2PreviewImageHeight(int cardWidth){return Math.max(dp(240),Math.round((cardWidth-dp(6))*exp2FullHomeHeight()/(float)exp2FullHomeWidth()));}
    private int exp2PreviewCardHeight(int cardWidth){return exp2PreviewImageHeight(cardWidth)+dp(34+28);}
    private void addExp2PagePreview(final int page,boolean temporary){
        FrameLayout card=new FrameLayout(this);card.setTag(page);
        int cardWidth=exp2PreviewCardWidth(),cardHeight=exp2PreviewCardHeight(cardWidth);
        card.setBackground(exp2Glass(page==workspacePage?0x88427899:(temporary?0x663C6178:0x66526B80),page==workspacePage?0xE8F1FAFF:0x88E7F5FF,10));
        card.setClipToOutline(true);
        ImageView thumbnail=new ImageView(this);thumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);thumbnail.setImageBitmap(exp2PageBitmap(page));
        thumbnail.setBackgroundColor(Color.TRANSPARENT);thumbnail.setClipToOutline(true);
        FrameLayout.LayoutParams imageLp=new FrameLayout.LayoutParams(-1,-1);imageLp.setMargins(dp(3),dp(34),dp(3),dp(30));card.addView(thumbnail,imageLp);
        TextView pageLabel=label("Экран "+(page+1)+(temporary?" · новый":""),12,Color.WHITE);pageLabel.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);pageLabel.setPadding(dp(8),0,0,0);
        pageLabel.setShadowLayer(dp(2),0,dp(1),0xCC102131);
        card.addView(pageLabel,new FrameLayout.LayoutParams(-1,dp(34),Gravity.TOP|Gravity.LEFT));
        TextView delete=label("×",27,Color.WHITE);delete.setContentDescription("Удалить экран "+(page+1));delete.setBackground(exp2Glass(0xD98A4658,0x99FFECEF,6));
        delete.setOnClickListener(v->deleteExp2Page(page));card.addView(delete,new FrameLayout.LayoutParams(dp(48),dp(34),Gravity.TOP|Gravity.RIGHT));
        card.setOnClickListener(v->{settleWorkspace(page);exitExp2EditMode();});
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(cardWidth,cardHeight);lp.setMargins(dp(6),dp(8),dp(6),dp(8));exp2EditPreviewRow.addView(card,lp);
    }
    private void addExp2NewPageCard(){
        int cardWidth=exp2PreviewCardWidth(),cardHeight=exp2PreviewCardHeight(cardWidth);
        FrameLayout card=new FrameLayout(this);card.setTag("add-page");
        card.setBackground(exp2Glass(0x665D7890,0xA6E7F5FF,10));card.setClipToOutline(true);
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setGravity(Gravity.CENTER);
        TextView plus=label("+",58,Color.WHITE);plus.setGravity(Gravity.CENTER);plus.setShadowLayer(dp(3),0,dp(2),0xCC102131);
        TextView caption=label("Новый\nрабочий стол",17,Color.WHITE);caption.setGravity(Gravity.CENTER);caption.setShadowLayer(dp(2),0,dp(1),0xCC102131);
        content.addView(plus,new LinearLayout.LayoutParams(-1,dp(82)));content.addView(caption,new LinearLayout.LayoutParams(-1,dp(64)));
        card.addView(content,new FrameLayout.LayoutParams(-1,-1));
        card.setContentDescription("Добавить новый рабочий стол");
        card.setOnClickListener(v->{if(exp2AddPageInProgress)return;exp2AddPageInProgress=true;addExp2Page();handler.postDelayed(()->exp2AddPageInProgress=false,500);});
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(cardWidth,cardHeight);lp.setMargins(dp(6),dp(8),dp(6),dp(8));exp2EditPreviewRow.addView(card,lp);
    }
    private final class EditPreviewScrollView extends HorizontalScrollView{
        private float downX,downY,lastX;private boolean horizontalDrag;private VelocityTracker velocity;
        EditPreviewScrollView(Context context){super(context);setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);setSmoothScrollingEnabled(true);}
        @Override public boolean dispatchTouchEvent(MotionEvent event){
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){downX=lastX=event.getX();downY=event.getY();horizontalDrag=false;velocity=VelocityTracker.obtain();velocity.addMovement(event);}
            else if(action==MotionEvent.ACTION_MOVE){
                if(velocity!=null)velocity.addMovement(event);
                float dx=event.getX()-downX,dy=event.getY()-downY;
                if(!horizontalDrag&&Math.abs(dx)>touchSlop&&Math.abs(dx)>Math.abs(dy)){
                    MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();horizontalDrag=true;
                }
                if(horizontalDrag){scrollBy(Math.round(lastX-event.getX()),0);lastX=event.getX();return true;}
            }else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
                boolean wasDragging=horizontalDrag;
                if(velocity!=null)velocity.addMovement(event);
                if(horizontalDrag&&action==MotionEvent.ACTION_UP&&velocity!=null){velocity.computeCurrentVelocity(1000);fling(Math.round(-velocity.getXVelocity()));}
                horizontalDrag=false;lastX=event.getX();if(velocity!=null){velocity.recycle();velocity=null;}if(wasDragging)return true;
            }
            return super.dispatchTouchEvent(event);
        }
        @Override public boolean onInterceptTouchEvent(MotionEvent event){
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();horizontalDrag=false;}
            else if(action==MotionEvent.ACTION_MOVE&&!horizontalDrag){
                float dx=event.getX()-downX,dy=event.getY()-downY;
                if(Math.abs(dx)>touchSlop&&Math.abs(dx)>Math.abs(dy)){horizontalDrag=true;requestDisallowInterceptTouchEvent(false);return true;}
            }else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)horizontalDrag=false;
            return super.onInterceptTouchEvent(event);
        }
        @Override public boolean onTouchEvent(MotionEvent event){
            boolean handled=super.onTouchEvent(event);
            if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL)horizontalDrag=false;
            return handled;
        }
    }
    private Bitmap exp2PageBitmap(int page){
        Bitmap cached=exp2PageBitmapCache.get(page);if(cached!=null&&!cached.isRecycled())return cached;
        View source=workspaceStrip==null||page<0||page>=workspaceStrip.getChildCount()?null:workspaceStrip.getChildAt(page);
        int width=exp2FullHomeWidth(),height=exp2FullHomeHeight();
        Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);
        drawExp2Wallpaper(canvas,width,height);
        drawExp2HomeView(canvas,statusGlyph);
        if(page==workspacePage)drawExp2HomeView(canvas,widgetScroller);
        drawExp2HomeView(canvas,source);
        drawExp2HomeView(canvas,dock);
        exp2PageBitmapCache.put(page,bitmap);
        return bitmap;
    }
    private void drawExp2HomeView(Canvas canvas,View view){
        if(view==null||view.getWidth()<=0||view.getHeight()<=0||home==null)return;
        int[] homeLocation=new int[2],viewLocation=new int[2];home.getLocationOnScreen(homeLocation);view.getLocationOnScreen(viewLocation);
        int left=viewLocation[0]-homeLocation[0],top=viewLocation[1]-homeLocation[1];
        canvas.save();canvas.translate(left,top);canvas.clipRect(0,0,view.getWidth(),view.getHeight());view.draw(canvas);canvas.restore();
    }
    private void drawExp2Wallpaper(Canvas canvas,int width,int height){
        if(wallpaper==null||wallpaper.getDrawable()==null){canvas.drawColor(0xFF3B5565);return;}
        Drawable source=wallpaper.getDrawable();
        int intrinsicWidth=Math.max(1,source.getIntrinsicWidth()),intrinsicHeight=Math.max(1,source.getIntrinsicHeight());
        float scale=Math.max(width/(float)intrinsicWidth,height/(float)intrinsicHeight);
        int drawWidth=Math.max(width,Math.round(intrinsicWidth*scale)),drawHeight=Math.max(height,Math.round(intrinsicHeight*scale));
        int left=(width-drawWidth)/2,top=(height-drawHeight)/2;
        Rect old=new Rect(source.getBounds());source.setBounds(left,top,left+drawWidth,top+drawHeight);source.draw(canvas);source.setBounds(old);
    }
    private Drawable exp2Glass(int fill,int stroke,int radius){
        GradientDrawable base=background(fill,radius);base.setStroke(Math.max(1,dp(1)),stroke);
        GradientDrawable sheen=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0x55FFFFFF,0x16FFFFFF,0x00FFFFFF});sheen.setCornerRadius(dp(radius));
        LayerDrawable glass=new LayerDrawable(new Drawable[]{base,sheen});
        glass.setLayerInset(1,dp(1),dp(1),dp(1),dp(1));return glass;
    }
    private Bitmap exp2EditBackdrop(){
        if(wallpaper==null||wallpaper.getDrawable()==null)return null;
        if(exp2BlurredBackdrop!=null&&!exp2BlurredBackdrop.isRecycled())return exp2BlurredBackdrop;
        try{
            Bitmap sample=Bitmap.createBitmap(90,160,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(sample);
            Drawable source=wallpaper.getDrawable();Rect old=new Rect(source.getBounds());source.setBounds(0,0,90,160);source.draw(canvas);source.setBounds(old);
            exp2BlurredBackdrop=blur(sample,5);return exp2BlurredBackdrop;
        }catch(Exception ignored){return null;}
    }
    private void updateExp2EditSelection(){
        if(exp2EditPreviewRow==null)return;int count=exp2EditPreviewRow.getChildCount();
        for(int i=0;i<count;i++){View card=exp2EditPreviewRow.getChildAt(i);boolean selected=card.getTag() instanceof Integer&&((Integer)card.getTag())==workspacePage;boolean add="add-page".equals(card.getTag());if(!add)card.setBackground(exp2Glass(selected?0x884D9BC2:0x66526B80,selected?0xE8FFFFFF:0x66E7F5FF,10));}
        if(exp2EditIndicator!=null)exp2EditIndicator.setText("Экран "+(Math.min(workspacePage,Math.max(0,homePageCount()-1))+1)+" из "+Math.max(1,homePageCount()));
    }
    private int countPageItems(int page){
        int n=0;for(HomeWorkspaceModel.Placement p:workspaceModel.placements())if(p.page==page)n++;
        for(String id:profilePrefs().getString("widgets","").split(","))try{if(profilePrefs().getInt("widget_page_"+Integer.parseInt(id),0)==page)n++;}catch(NumberFormatException ignored){}
        return n;
    }
    private void addExp2Action(LinearLayout row,String text,final Runnable action){
        LinearLayout button=new LinearLayout(this);button.setOrientation(LinearLayout.VERTICAL);button.setGravity(Gravity.CENTER);button.setBackground(exp2Glass(0x884B6C82,0xA6E7F5FF,8));
        int iconRes="Обои".equals(text)?android.R.drawable.ic_menu_gallery:
            "Темы".equals(text)?android.R.drawable.ic_menu_edit:
            "Виджеты".equals(text)?android.R.drawable.ic_menu_view:android.R.drawable.ic_menu_preferences;
        ImageView icon=new ImageView(this);icon.setImageResource(iconRes);icon.setColorFilter(Color.WHITE,android.graphics.PorterDuff.Mode.SRC_IN);button.addView(icon,new LinearLayout.LayoutParams(dp(28),dp(32)));
        TextView caption=label(text,12,Color.WHITE);caption.setShadowLayer(dp(2),0,dp(1),0xCC102131);caption.setGravity(Gravity.CENTER);caption.setMaxLines(2);button.addView(caption,new LinearLayout.LayoutParams(-1,0,1));
        button.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.setMargins(dp(3),0,dp(3),0);row.addView(button,lp);
    }
    private void addExp2Page(){
        int last=Math.max(0,workspaceStrip.getChildCount()-1);
        if(last<homePageCount()){Toast.makeText(this,"Новый рабочий стол недоступен",Toast.LENGTH_SHORT).show();return;}
        settleWorkspace(last);exitExp2EditMode();Toast.makeText(this,"Разместите элемент: экран сохранится после подтверждённого Drop",Toast.LENGTH_SHORT).show();
    }
    private void deleteExp2Page(final int page){
        if(countPageItems(page)>0){
            new AlertDialog.Builder(this).setTitle("Экран содержит элементы").setMessage("Сначала перенесите иконки и виджеты. Формат Home не изменён.").setPositiveButton("Понятно",null).show();return;
        }
        int transientPage=Math.max(0,workspaceStrip.getChildCount()-1);
        if(page!=transientPage){Toast.makeText(this,"Постоянные пустые страницы не представлены в текущей модели",Toast.LENGTH_SHORT).show();return;}
        settleWorkspace(Math.max(0,page-1));rebuildExp2EditPreviews();
    }
    private void exitExp2EditMode(){
        if(!exp2EditMode)return;exp2EditMode=false;
        if(exp2EditOverlay!=null){FrameLayout overlay=exp2EditOverlay;exp2EditOverlay=null;overlay.animate().alpha(0f).translationY(dp(22)).setDuration(150).withEndAction(()->root.removeView(overlay)).start();}
        updateSystemStatusBar();
    }
    private void showDrawerOptions(){
        showNokiaDialog(new AlertDialog.Builder(this).setTitle("Приложения").setItems(new String[]{"Создать папку","Восстановить скрытые","Настройки","Добавить виджет"},(d,n)->{
            if(n==0)createFolder(null);else if(n==1)restoreHidden();else if(n==2)showSettings();else addWidget();
        }));
    }
    private void restoreHidden(){
        List<String> hidden=menu.organizer.hiddenKeys();
        String[] labels=hidden.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("Скрытые приложения").setItems(labels,(d,n)->{menu.organizer.restore(hidden.get(n));refreshGrid();}).setNegativeButton("Закрыть",null).show();
    }
    private void showSettings(){
        String[] choices={"Внешний вид →","Цвет темы","Сетка приложений","Сетка рабочего стола Exp2: "+exp2GridLabel(),"Обои","Размытие обоев","Затемнение меню","Прозрачность окон","Ярлыки Home","Виджеты","Заставка","Звуки Nokia","Сброс меню","Home Engine Exp 2: "+(exp2HomeEngineEnabled()?"Вкл":"Выкл"),"Цвет иконок статус-бара: "+statusIconColorLabel()};
        final AlertDialog[] current=new AlertDialog[1];
        AlertDialog.Builder builder=new AlertDialog.Builder(this)
            .setCustomTitle(settingsTitle("Nokia Experience · Настройки",()->{if(current[0]!=null)current[0].dismiss();}))
            .setItems(choices,(d,n)->{
            switch(n){
                case 0:showAppearanceSettings();break;case 1:chooseThemeColor();break;case 2:chooseGrid();break;case 3:chooseExp2HomeGrid();break;case 4:chooseWallpaper();break;
                case 5:chooseBlur();break;case 6:chooseDim();break;case 7:chooseWindowTransparency();break;
                case 8:chooseDockSettings();break;case 9:addWidget();break;case 10:chooseStartup();break;case 11:chooseSounds();break;
                case 12:new AlertDialog.Builder(this).setTitle("Сброс меню").setPositiveButton("Сбросить",(x,w)->{menu.organizer.reset();refreshGrid();}).setNegativeButton("Отмена",null).show();break;
                case 13:toggleExp2HomeEngine();break;case 14:chooseStatusIconColor();break;
            }
        });
        current[0]=showSettingsDialog(builder);
    }
    private String exp2GridLabel(){
        if(!exp2())return "только Exp2";
        return "4x4+1 виджет".equals(profilePrefs().getString("exp2_home_grid","3x4+2widgets"))?"4 × 4 + 1 виджет":"3 × 4 + 2 виджета";
    }
    private void chooseExp2HomeGrid(){
        if(!exp2()){Toast.makeText(this,"Доступно только для Nokia Exp 2",Toast.LENGTH_SHORT).show();return;}
        String[] choices={"4 × 4 + 1 виджет","3 × 4 + 2 виджета"};
        int selected="4x4+1 виджет".equals(profilePrefs().getString("exp2_home_grid","3x4+2widgets"))?0:1;
        new AlertDialog.Builder(this).setTitle("Сетка рабочего стола Exp2\nDock не входит; модель хранит 12 ячеек")
            .setSingleChoiceItems(choices,selected,(dialog,which)->{
                if(which==0){
                    new AlertDialog.Builder(this).setTitle("Конфигурация не применена")
                        .setMessage("Вариант 4 × 4 требует расширения модели до 16 ячеек и изменения формата размещения. Чтобы не потерять иконки и не выполнять миграцию, сохранена безопасная конфигурация 3 × 4 + 2 виджета.")
                        .setPositiveButton("Понятно",null).show();
                }else{
                    profilePrefs().edit().putString("exp2_home_grid","3x4+2widgets").apply();
                    dialog.dismiss();Toast.makeText(this,"Применено: 3 × 4 + 2 виджета",Toast.LENGTH_SHORT).show();
                    refreshWorkspace();
                }
            }).setNegativeButton("Отмена",null).show();
    }
    private void toggleExp2HomeEngine(){
        if(!exp2()){Toast.makeText(this,"Доступно только для Nokia Exp 2",Toast.LENGTH_SHORT).show();return;}
        exp2Prefs.edit().putBoolean("home_engine_v2",!exp2HomeEngineEnabled()).apply();
        refreshWorkspace();showSettings();
    }
    private void showAppearanceSettings(){
        final AlertDialog[] current=new AlertDialog[1];
        AlertDialog.Builder builder=new AlertDialog.Builder(this)
            .setCustomTitle(settingsTitle("Внешний вид",()->{if(current[0]!=null)current[0].dismiss();showSettings();}))
            .setItems(new String[]{"Interface Style: "+interfaceStyleLabel(),"Цвет темы","Прозрачность окон","Затемнение меню","Размытие обоев"},(d,n)->{
                if(n==0)chooseStyle();else if(n==1)chooseThemeColor();else if(n==2)chooseWindowTransparency();
                else if(n==3)chooseDim();else chooseBlur();
            }).setNegativeButton("‹ Назад",(d,w)->showSettings());
        current[0]=showSettingsDialog(builder);
    }
    private String interfaceStyleLabel(){
        switch(interfaceStyle()){case "n95":return "N95";case "5530":return "5530";
            default:return "Nokia Exp 2";}
    }
    private void chooseStyle(){
        String[] styles={"Nokia Exp 2","N95","5530"};
        String[] values={"exp2","n95","5530"};
        int selected=Arrays.asList(values).indexOf(interfaceStyle());
        showSettingsDialog(new AlertDialog.Builder(this).setTitle("Interface Style")
            .setSingleChoiceItems(styles,Math.max(0,selected),(d,n)->{
                prefs.edit().putString("style",values[n]).apply();d.dismiss();
                loadProfileData();folderId=null;currentPage=0;workspacePage=0;
                restoreWidgets();drawerDirty=true;
                applyWindowTransparency();buildDock();refreshGrid();refreshWorkspace();
                showAppearanceSettings();
            }).setNegativeButton("‹ Назад",(d,w)->showAppearanceSettings()));
    }
    private void chooseThemeColor(){
        String[] names={"Красный","Синий","Чёрный","Оранжевый","Голубой","Белый"};
        String[] values={"red","blue","black","orange","cyan","white"};
        int selected=Arrays.asList(values).indexOf(profilePrefs().getString("theme_color","profile"));
        showSettingsDialog(new AlertDialog.Builder(this).setTitle("Цвет Nokia UI")
            .setSingleChoiceItems(names,Math.max(0,selected),(d,n)->{
                profilePrefs().edit().putString("theme_color",values[n]).apply();d.dismiss();
                applyWindowTransparency();buildDock();refreshGrid();refreshWorkspace();
            }).setNegativeButton("‹ Назад",(d,w)->showAppearanceSettings()));
    }
    private void chooseDockSettings(){
        String[] choices=new String[dockCount()+1];
        choices[0]="Количество иконок: "+dockCount();
        for(int i=0;i<dockCount();i++)choices[i+1]="Позиция "+(i+1);
        new AlertDialog.Builder(this).setTitle("Ярлыки Home").setItems(choices,(d,n)->{
            if(n==0)new AlertDialog.Builder(this).setTitle("Иконок в доке")
                .setSingleChoiceItems(new String[]{"3","4"},dockCount()-3,(dialog,selected)->{
                    profilePrefs().edit().putInt("dock_count",selected+3).apply();dialog.dismiss();buildDock();
                }).show();
            else chooseShortcut(n-1);
        }).show();
    }
    private void chooseGrid(){
        new AlertDialog.Builder(this).setTitle("Сетка приложений").setSingleChoiceItems(new String[]{"3 × 5","3 × 6"},profilePrefs().getInt("drawer_rows",6)==5?0:1,(d,n)->{
            profilePrefs().edit().putInt("drawer_rows",n==0?5:6).apply();refreshGrid();d.dismiss();
        }).show();
    }
    private void chooseBlur(){
        new AlertDialog.Builder(this).setTitle("Размытие обоев").setSingleChoiceItems(new String[]{"Выкл","Низкое","Среднее","Сильное"},profilePrefs().getInt("blur",1),(d,n)->{
            profilePrefs().edit().putInt("blur",n).apply();updateDrawerBackground();d.dismiss();
        }).show();
    }
    private void chooseDim(){
        SeekBar bar=new SeekBar(this);bar.setMax(70);bar.setProgress(profilePrefs().getInt("drawer_dim",38));
        new AlertDialog.Builder(this).setTitle("Затемнение меню").setMessage("0–70 %, обои остаются видимыми").setView(bar)
            .setPositiveButton("Сохранить",(d,w)->{profilePrefs().edit().putInt("drawer_dim",bar.getProgress()).apply();updateDrawerBackground();}).setNegativeButton("Отмена",null).show();
    }
    private void chooseWindowTransparency(){
        SeekBar bar=new SeekBar(this);bar.setMax(90);bar.setProgress(windowTransparency());
        new AlertDialog.Builder(this).setTitle("Прозрачность окон")
            .setMessage("Панели Nokia, док и затемнение меню · 0–90 %").setView(bar)
            .setPositiveButton("Сохранить",(d,w)->{
                profilePrefs().edit().putInt("window_transparency",bar.getProgress()).apply();applyWindowTransparency();
            }).setNegativeButton("Отмена",null).show();
    }
    private void applyWindowTransparency(){
        if(dock!=null)dock.setBackground(background(panel(),dockRadius()));
        View header=drawerShell==null?null:drawerShell.findViewWithTag("drawerHeader");
        if(header!=null)header.setBackgroundColor(Color.TRANSPARENT);
        TextView title=drawerShell==null?null:drawerShell.findViewWithTag("drawerTitle");
        if(title!=null)title.setTextColor(accent()==Color.WHITE?Color.WHITE:accent());
        if(widgetColumn!=null)for(int i=0;i<widgetColumn.getChildCount();i++)
            widgetColumn.getChildAt(i).setBackground(exp2()?new ColorDrawable(Color.TRANSPARENT):background(panel(),10));
        updateDrawerBackground();
    }
    private void chooseStartup(){
        String[] labels={"Анимация запуска","Звук запуска"};
        boolean[] selected={prefs.getBoolean("startup_animation",true),prefs.getBoolean("startup_sound",true)};
        new AlertDialog.Builder(this).setTitle("Заставка").setMultiChoiceItems(labels,selected,(d,n,on)->{
            prefs.edit().putBoolean(n==0?"startup_animation":"startup_sound",on).apply();
        }).setPositiveButton("Готово",null).show();
    }
    private void chooseSounds(){
        String[] labels={"Звуки взаимодействия Nokia"};
        boolean[] selected={prefs.getBoolean("ui_sounds",true)};
        new AlertDialog.Builder(this).setTitle("Звуки").setMultiChoiceItems(labels,selected,(d,n,on)->prefs.edit().putBoolean("ui_sounds",on).apply()).setPositiveButton("Готово",null).show();
    }
    private void chooseWallpaper(){safeStartForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"),PICK_WALLPAPER);}
    private void loadWallpaper(){
        if(wallpaper==null)return;
        Drawable image=null;wallpaperFallback=false;
        try{
            WallpaperManager manager=WallpaperManager.getInstance(this);
            image=manager.getDrawable(); // Actual static Home wallpaper on Android 8 with read access.
        }catch(Exception e){android.util.Log.w("NokiaWallpaper","Current wallpaper is not readable",e);}
        if(image==null){
            wallpaperFallback=true;
            try{image=WallpaperManager.getInstance(this).getBuiltInDrawable();}catch(Exception ignored){}
        }
        wallpaper.setImageDrawable(image);
        exp2PageBitmapCache.clear();exp2BlurredBackdrop=null;
        cachedDrawerSource=null;
        cachedDrawerBlur=-1;
        updateDrawerBackground();
    }
    private void maybeRequestWallpaperPermission(){
        if(!wallpaperFallback||Build.VERSION.SDK_INT>32||checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)==android.content.pm.PackageManager.PERMISSION_GRANTED)return;
        if(prefs.getBoolean("wallpaper_permission_prompted",false))return;
        prefs.edit().putBoolean("wallpaper_permission_prompted",true).apply();
        new AlertDialog.Builder(this).setTitle("Системные обои")
            .setMessage("Разрешите Nokia Experience чтение изображений, чтобы показывать текущие обои Android на главном экране. Без доступа будет показан стандартный статичный фон устройства.")
            .setPositiveButton("Разрешить",(d,w)->requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},READ_WALLPAPER))
            .setNegativeButton("Позже",null).show();
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(request,permissions,grants);
        if(request==READ_WALLPAPER&&grants.length>0&&grants[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)loadWallpaper();
    }
    private void updateDrawerBackground(){
        if(drawer==null||wallpaper==null)return;
        ImageView layer=drawer.findViewWithTag("blur");View dim=drawer.findViewWithTag("dim");
        if(layer==null||dim==null)return;
        Drawable source=wallpaper.getDrawable();int level=profilePrefs().getInt("blur",1);
        if(source!=null&&level>0){
            if(source!=cachedDrawerSource||level!=cachedDrawerBlur||cachedDrawerBitmap==null){
                Bitmap sample=Bitmap.createBitmap(90,160,Bitmap.Config.ARGB_8888);
                Canvas canvas=new Canvas(sample);
                Rect originalBounds=new Rect(source.getBounds());
                source.setBounds(0,0,90,160);source.draw(canvas);source.setBounds(originalBounds);
                cachedDrawerBitmap=blur(sample,level*2);
                cachedDrawerSource=source;cachedDrawerBlur=level;
            }
            layer.setImageBitmap(cachedDrawerBitmap);
        }else layer.setImageDrawable(source);
        int alpha=Math.round(Math.max(0,Math.min(70,profilePrefs().getInt("drawer_dim",38)))*255/100f);
        dim.setBackgroundColor((alpha<<24)|0x0010233C);
    }
    private Bitmap blur(Bitmap input,int radius){
        int w=input.getWidth(),h=input.getHeight();int[] pixels=new int[w*h],out=new int[w*h];input.getPixels(pixels,0,w,0,0,w,h);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int r=0,g=0,b=0,n=0;
            for(int k=Math.max(0,x-radius);k<=Math.min(w-1,x+radius);k++){int p=pixels[y*w+k];r+=Color.red(p);g+=Color.green(p);b+=Color.blue(p);n++;}
            out[y*w+x]=Color.rgb(r/n,g/n,b/n);
        }
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int r=0,g=0,b=0,n=0;
            for(int k=Math.max(0,y-radius);k<=Math.min(h-1,y+radius);k++){int p=out[k*w+x];r+=Color.red(p);g+=Color.green(p);b+=Color.blue(p);n++;}
            pixels[y*w+x]=Color.rgb(r/n,g/n,b/n);
        }
        Bitmap result=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);result.setPixels(pixels,0,w,0,0,w,h);input.recycle();return result;
    }
    private void addWidget(){
        if(!getPackageManager().hasSystemFeature("android.software.app_widgets")){Toast.makeText(this,"Виджеты недоступны",Toast.LENGTH_SHORT).show();return;}
        if(WidgetPreviewPicker.show(this,widgetManager,this::selectWidget))return;
        pendingWidgetId=widgetHost.allocateAppWidgetId();
        Intent pick=new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,pendingWidgetId);
        safeStartForResult(pick,PICK_WIDGET);
    }
    private void selectWidget(AppWidgetProviderInfo info){
        pendingWidgetId=widgetHost.allocateAppWidgetId();
        boolean bound=false;
        try{bound=widgetManager.bindAppWidgetIdIfAllowed(pendingWidgetId,info.provider);}catch(Exception ignored){}
        if(bound){completeWidget(pendingWidgetId,widgetManager.getAppWidgetInfo(pendingWidgetId));return;}
        Intent bind=new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,pendingWidgetId)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,info.provider);
        safeStartForResult(bind,BIND_WIDGET);
    }
    private void restoreWidgets(){
        if(widgetColumn==null)return;widgetColumn.removeAllViews();
        editingWidget=null;
        String value=profilePrefs().getString("widgets","");
        if(!value.isEmpty())for(String item:value.split(","))try{attachWidget(Integer.parseInt(item));}catch(Exception ignored){}
        updateVisibleWidgets();
    }
    private void attachWidget(int id){
        AppWidgetProviderInfo info=widgetManager.getAppWidgetInfo(id);if(info==null)return;
        AppWidgetHostView view=widgetHost.createView(this,id,info);view.setAppWidget(id,info);
        int height=Math.max(100,profilePrefs().getInt("widget_height_"+id,180));
        int width=profilePrefs().getInt("widget_width_"+id,-1);
        WidgetFrame wrapper=new WidgetFrame(this,id,info);
        wrapper.setBackground(exp2()?new ColorDrawable(Color.TRANSPARENT):background(panel(),10));
        wrapper.addView(view,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(width<0?-1:dp(width),dp(height));lp.bottomMargin=dp(8);
        widgetColumn.addView(wrapper,lp);
        wrapper.setOnDragListener((v,event)->{
            if(event.getAction()==DragEvent.ACTION_DRAG_ENDED&&activeDragView==wrapper)
                endWorkspaceDrag(event);
            if(event.getAction()==DragEvent.ACTION_DROP){
                String source=String.valueOf(event.getLocalState());
                if(source.startsWith("widget:")&&!source.equals("widget:"+id)){moveWidgetBefore(source,"widget:"+id);return true;}
            }
            return true;
        });
    }
    private final class WidgetFrame extends FrameLayout{
        final int widgetId;final AppWidgetProviderInfo provider;
        private float downX,downY;private Runnable pending;private boolean editingGesture;
        WidgetFrame(Context context,int id,AppWidgetProviderInfo info){super(context);widgetId=id;provider=info;}
        @Override public boolean dispatchTouchEvent(MotionEvent event){
            if(event.getAction()==MotionEvent.ACTION_DOWN){
                downX=event.getX();downY=event.getY();editingGesture=false;cancelPending();
                pending=()->{
                    pending=null;editingGesture=true;
                    if(!exp2()){
                        MotionEvent cancel=MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),
                            MotionEvent.ACTION_CANCEL,downX,downY,0);
                        WidgetFrame.super.dispatchTouchEvent(cancel);cancel.recycle();
                    }
                    showWidgetHandles(this);
                    if(exp2())startWidgetDrag(this);
                };
                handler.postDelayed(pending,ViewConfiguration.getLongPressTimeout());
            }else if(event.getAction()==MotionEvent.ACTION_MOVE){
                if(Math.abs(event.getX()-downX)>touchSlop||Math.abs(event.getY()-downY)>touchSlop)cancelPending();
            }else if(event.getAction()==MotionEvent.ACTION_UP||event.getAction()==MotionEvent.ACTION_CANCEL){
                cancelPending();if(editingGesture){editingGesture=false;return true;}
            }
            return super.dispatchTouchEvent(event);
        }
        void cancelPending(){if(pending!=null){handler.removeCallbacks(pending);pending=null;}}
    }
    private void showWidgetHandles(WidgetFrame wrapper){
        if(editingWidget!=null&&editingWidget!=wrapper)hideWidgetHandles(editingWidget);
        editingWidget=wrapper;
        GradientDrawable border=background(exp2()?Color.TRANSPARENT:panel(),10);
        border.setStroke(dp(exp2()?1:2),exp2()?0xCCFFFFFF:accent());
        if(exp2())wrapper.setForeground(border);else wrapper.setBackground(border);
        View old=wrapper.findViewWithTag("widgetHandles");if(old!=null)wrapper.removeView(old);
        LinearLayout tools=new LinearLayout(this);tools.setTag("widgetHandles");tools.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        tools.setBackground(background(panel(),8));
        TextView move=label("✥",23,Color.WHITE);tools.addView(move,new LinearLayout.LayoutParams(dp(48),dp(48)));
        move.setContentDescription("Переместить виджет");
        move.setOnTouchListener((v,event)->{
            if(event.getAction()==MotionEvent.ACTION_DOWN){
                startWidgetDrag(wrapper);
                return true;
            }
            return true;
        });
        if((wrapper.provider.resizeMode&(AppWidgetProviderInfo.RESIZE_HORIZONTAL|AppWidgetProviderInfo.RESIZE_VERTICAL))!=0){
            TextView resize=label("↘",24,Color.WHITE);resize.setContentDescription("Изменить размер виджета");
            tools.addView(resize,new LinearLayout.LayoutParams(dp(48),dp(48)));
            resize.setOnTouchListener(new View.OnTouchListener(){
                float startX,startY;int initialWidth,initialHeight;
                @Override public boolean onTouch(View v,MotionEvent event){
                    if(event.getAction()==MotionEvent.ACTION_DOWN){
                        startX=event.getRawX();startY=event.getRawY();initialWidth=wrapper.getWidth();initialHeight=wrapper.getHeight();return true;
                    }
                    if(event.getAction()==MotionEvent.ACTION_MOVE||event.getAction()==MotionEvent.ACTION_UP){
                        LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)wrapper.getLayoutParams();
                        boolean horizontal=(wrapper.provider.resizeMode&AppWidgetProviderInfo.RESIZE_HORIZONTAL)!=0;
                        boolean vertical=(wrapper.provider.resizeMode&AppWidgetProviderInfo.RESIZE_VERTICAL)!=0;
                        if(horizontal)lp.width=Math.max(Math.max(dp(100),wrapper.provider.minResizeWidth),
                            Math.min(widgetColumn.getWidth(),initialWidth+Math.round(event.getRawX()-startX)));
                        if(vertical)lp.height=Math.max(Math.max(dp(80),wrapper.provider.minResizeHeight),
                            Math.min(Math.max(dp(400),widgetColumn.getHeight()),initialHeight+Math.round(event.getRawY()-startY)));
                        wrapper.setLayoutParams(lp);
                        if(event.getAction()==MotionEvent.ACTION_UP){
                            int widthDp=Math.round((lp.width<0?wrapper.getWidth():lp.width)/getResources().getDisplayMetrics().density);
                            int heightDp=Math.round(lp.height/getResources().getDisplayMetrics().density);
                            profilePrefs().edit().putInt("widget_width_"+wrapper.widgetId,widthDp)
                                .putInt("widget_height_"+wrapper.widgetId,heightDp).apply();
                            Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,widthDp);
                            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,widthDp);
                            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,heightDp);
                            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,heightDp);
                            try{widgetManager.updateAppWidgetOptions(wrapper.widgetId,options);}catch(Exception ignored){}
                        }
                        return true;
                    }
                    return true;
                }
            });
        }
        TextView remove=label("×",26,Color.WHITE);remove.setContentDescription("Удалить виджет");
        tools.addView(remove,new LinearLayout.LayoutParams(dp(48),dp(48)));
        remove.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Удалить виджет?")
            .setPositiveButton("Удалить",(d,w)->removeWidget(wrapper.widgetId)).setNegativeButton("Отмена",null).show());
        FrameLayout.LayoutParams controls=new FrameLayout.LayoutParams(-2,dp(48),Gravity.TOP|Gravity.RIGHT);
        wrapper.addView(tools,controls);
        wrapper.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
    }
    private void startWidgetDrag(WidgetFrame wrapper){
        String key="widget:"+wrapper.widgetId;
        activeDragKey=key;activeDragView=wrapper;
        homeDragOriginPage=workspacePage;crossedHomeEdge=false;homeDragMoved=false;
        boolean started=wrapper.startDragAndDrop(android.content.ClipData.newPlainText("widget",key),
            new View.DragShadowBuilder(wrapper),key,0);
        if(started)wrapper.animate().alpha(0.3f).setDuration(90).start();
    }
    private void hideWidgetHandles(WidgetFrame wrapper){
        if(wrapper==null)return;
        View controls=wrapper.findViewWithTag("widgetHandles");if(controls!=null)wrapper.removeView(controls);
        if(exp2())wrapper.setForeground(null);
        wrapper.setBackground(exp2()?new ColorDrawable(Color.TRANSPARENT):background(panel(),10));
        if(editingWidget==wrapper)editingWidget=null;
    }
    private void moveWidgetBefore(String source,String target){
        ArrayList<String> ids=new ArrayList<>(Arrays.asList(profilePrefs().getString("widgets","").split(",")));
        String id=source.substring("widget:".length());if(!ids.remove(id))return;
        int index=target==null?ids.size():ids.indexOf(target.substring("widget:".length()));
        ids.add(index<0?ids.size():index,id);
        profilePrefs().edit().putString("widgets",TextUtils.join(",",ids)).apply();restoreWidgets();
    }
    private void saveWidget(int id){
        String old=profilePrefs().getString("widgets","");profilePrefs().edit().putString("widgets",old.isEmpty()?""+id:old+","+id).apply();restoreWidgets();
    }
    private void removeWidget(int id){
        ArrayList<String> all=new ArrayList<>(Arrays.asList(profilePrefs().getString("widgets","").split(",")));
        all.remove(String.valueOf(id));profilePrefs().edit().putString("widgets",TextUtils.join(",",all))
            .remove("widget_width_"+id).remove("widget_height_"+id).apply();
        SharedPreferences other=exp2()?prefs:exp2Prefs;
        if(!Arrays.asList(other.getString("widgets","").split(",")).contains(String.valueOf(id)))
            try{widgetHost.deleteAppWidgetId(id);}catch(Exception ignored){}
        restoreWidgets();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PICK_ICON&&result==RESULT_OK&&data!=null&&data.getData()!=null&&iconTarget!=null){
            Uri uri=data.getData();try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
            icons.set(iconTarget,uri);iconTarget=null;buildDock();refreshGrid();refreshWorkspace();
        }else if(request==PICK_WALLPAPER&&result==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri uri=data.getData();try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
            prefs.edit().putString("wallpaper_fallback",uri.toString()).apply();
            try(InputStream stream=getContentResolver().openInputStream(uri)){WallpaperManager.getInstance(this).setStream(stream,null,true,WallpaperManager.FLAG_SYSTEM);}
            catch(Exception e){Toast.makeText(this,"Системные обои не изменены",Toast.LENGTH_SHORT).show();}
            loadWallpaper();
        }else if(request==PICK_WIDGET){
            int id=data==null?pendingWidgetId:data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,pendingWidgetId);
            if(result!=RESULT_OK){widgetHost.deleteAppWidgetId(id);return;}
            AppWidgetProviderInfo info=widgetManager.getAppWidgetInfo(id);
            if(info==null && data!=null){
                android.content.ComponentName provider=data.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER);
                if(provider!=null&&!widgetManager.bindAppWidgetIdIfAllowed(id,provider)){
                    safeStartForResult(new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,provider),BIND_WIDGET);return;
                }
                info=widgetManager.getAppWidgetInfo(id);
            }
            completeWidget(id,info);
        }else if(request==BIND_WIDGET){
            int id=data==null?pendingWidgetId:data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,pendingWidgetId);
            if(result==RESULT_OK)completeWidget(id,widgetManager.getAppWidgetInfo(id));else widgetHost.deleteAppWidgetId(id);
        }else if(request==CONFIG_WIDGET){
            int id=data==null?pendingWidgetId:data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,pendingWidgetId);
            if(result==RESULT_OK)saveWidget(id);else widgetHost.deleteAppWidgetId(id);
        }
    }
    private void completeWidget(int id,AppWidgetProviderInfo info){
        if(info==null){widgetHost.deleteAppWidgetId(id);Toast.makeText(this,"Виджет недоступен",Toast.LENGTH_SHORT).show();return;}
        pendingWidgetId=id;
        if(info.configure!=null){
            safeStartForResult(new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).setComponent(info.configure).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id),CONFIG_WIDGET);
        }else saveWidget(id);
    }
    private void applyOrientation(){
        String mode=prefs.getString("orientation","auto");
        setRequestedOrientation(mode.equals("portrait")?ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:mode.equals("landscape")?ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
}
