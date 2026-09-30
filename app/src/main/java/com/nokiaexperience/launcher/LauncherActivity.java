package com.nokiaexperience.launcher;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.graphics.drawable.StateListDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Android launcher core. Nokia artwork is a replaceable visual layer, not Symbian emulation. */
public final class LauncherActivity extends Activity {
    private static final int PICK_ICON=101, PICK_WALLPAPER=102, PICK_WIDGET=103, BIND_WIDGET=104, CONFIG_WIDGET=105;
    private static final int READ_WALLPAPER=106;
    private SharedPreferences prefs;
    private MenuModel menu;
    private AssetRepository assets;
    private HomeWorkspaceModel workspaceModel;
    private IconStore icons;
    private AndroidActionResolver resolver;
    private DeviceStateAdapters device;
    private NokiaSounds sounds;
    private FrameLayout root,home,drawer;
    private LinearLayout widgetColumn,dock,drawerShell,homeContent,workspaceStrip;
    private ScrollView widgetScroller;
    private WorkspacePager workspacePager;
    private int workspacePage;
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
    private int wifiLevel=-1,notificationIndicators;
    private final PhoneStateListener signalListener=new PhoneStateListener(){
        @Override public void onSignalStrengthsChanged(SignalStrength signal){device.signalLevel=!serviceAvailable||signal==null?-1:signal.getLevel();if(statusGlyph!=null)statusGlyph.invalidate();}
        @Override public void onServiceStateChanged(ServiceState service){serviceAvailable=service!=null&&service.getState()==ServiceState.STATE_IN_SERVICE;if(!serviceAvailable)device.signalLevel=-1;refreshStatusData();}
    };
    private ImageView wallpaper;
    private StartupView startup;
    private MediaPlayer startupPlayer;
    private boolean startupPending,drawerOpen,receiverRegistered;
    private boolean wallpaperFallback;
    private Drawable cachedDrawerSource;
    private Bitmap cachedDrawerBitmap;
    private int cachedDrawerBlur=-1;
    private String folderId,iconTarget;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private AppWidgetHost widgetHost;
    private AppWidgetManager widgetManager;
    private WidgetFrame editingWidget;
    private int pendingWidgetId=-1;
    private float swipeX,swipeY;
    private boolean swipeConsumed;
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
        menu=new MenuModel(this,prefs);workspaceModel=new HomeWorkspaceModel(prefs);
        assets=new AssetRepository(this);icons=new IconStore(this,prefs,assets);
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
            if(drawerShell!=null)drawerShell.setPadding(dp(10),insets.getSystemWindowInsetTop()+dp(4),dp(10),insets.getSystemWindowInsetBottom()+dp(6));
            return insets;
        });
        applyOrientation();buildHome();buildDrawer();loadWallpaper();refresh();
        startupPending=state==null && prefs.getBoolean("startup_animation",true);
    }
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private String interfaceStyle(){
        String style=prefs.getString("style","exp");
        return style.equals("n95")||style.equals("5530")?style:"exp";
    }
    private int accent(){
        if(interfaceStyle().equals("n95"))return 0xFF338BEB;
        if(interfaceStyle().equals("5530"))return 0xFFF04A3D;
        switch(prefs.getString("theme_color","profile")){
            case "red":return 0xFFFF3636;
            case "blue":return 0xFF208FFF;
            case "black":return 0xFF181A22;
            case "orange":return 0xFFFF9712;
            case "cyan":return 0xFF00D9F2;
            case "white":return 0xFFFFFFFF;
            default:return 0xFF208FFF;
        }
    }
    private int windowTransparency(){
        int value=35;
        try{value=prefs.contains("window_transparency")?prefs.getInt("window_transparency",35):
            prefs.contains("opacity_menu")?100-prefs.getInt("opacity_menu",65):35;}
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
        boolean homeFullscreen=!drawerOpen&&!settingsVisible;
        getWindow().setStatusBarColor(homeFullscreen?Color.TRANSPARENT:0xFF10273D);
        int flags=View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if(homeFullscreen)flags|=View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
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
        content.addView(statusGlyph,new LinearLayout.LayoutParams(-1,dp(43)));
        statusGlyph.setOnClickListener(v->showStatusActions());
        LinearLayout mid=new LinearLayout(this);mid.setOrientation(LinearLayout.VERTICAL);
        content.addView(mid,new LinearLayout.LayoutParams(-1,0,1));
        widgetColumn=new LinearLayout(this);widgetColumn.setOrientation(LinearLayout.VERTICAL);
        widgetScroller=new ScrollView(this);widgetScroller.setVerticalScrollBarEnabled(false);
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
        workspacePager.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            if(r-l!=or-ol||b-t!=ob-ot)workspacePager.post(this::rebuildWorkspace);
        });
        rebuildWorkspace();
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
            MenuModel.Entry e=special(prefs.getString("shortcut_"+i,defaults[i]));
            LinearLayout cell=iconCell(e,74);
            dock.addView(cell,new LinearLayout.LayoutParams(0,-1,1));
            cell.setOnClickListener(v->{sounds.tap();launch(e);});
            cell.setOnLongClickListener(v->{chooseShortcut(slot);return true;});
        }
    }
    private int dockCount(){return Math.max(3,Math.min(4,prefs.getInt("dock_count",3)));}
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
        int pages=Math.max(2,workspaceModel.pageCount()+1);workspacePage=Math.min(workspacePage,pages-1);
        int cellWidth=width/HomeWorkspaceModel.COLUMNS,cellHeight=height/HomeWorkspaceModel.ROWS;
        for(int page=0;page<pages;page++){
            final int pageIndex=page;
            FrameLayout surface=new FrameLayout(this);
            workspaceStrip.addView(surface,new LinearLayout.LayoutParams(width,height));
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
                cell.setOnClickListener(v->{if(workspaceModel.isFolder(entry.key))openHomeFolder(entry.key);else launch(entry);});
                cell.setOnLongClickListener(v->{startAppDrag(v,entry.key);return true;});
                cell.setOnDragListener((v,event)->{
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
    private final class WorkspacePager extends HorizontalScrollView{
        private float downX;private boolean flung;
        WorkspacePager(Context context){super(context);}
        @Override public boolean dispatchTouchEvent(MotionEvent event){
            if(event.getAction()==MotionEvent.ACTION_DOWN){downX=event.getX();flung=false;}
            boolean handled=super.dispatchTouchEvent(event);
            if(event.getAction()==MotionEvent.ACTION_UP&&!flung&&getWidth()>0){
                float dx=event.getX()-downX;
                int next=Math.abs(dx)>getWidth()*0.18f?workspacePage+(dx<0?1:-1):Math.round((float)getScrollX()/getWidth());
                settleWorkspace(next);
            }
            return handled;
        }
        @Override public void fling(int velocityX){flung=true;settleWorkspace(workspacePage+(velocityX>0?1:-1));}
    }
    private void settleWorkspace(int page){
        workspacePage=Math.max(0,Math.min(page,Math.max(0,workspaceStrip.getChildCount()-1)));
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
        if(activeDragKey!=null&&!event.getResult()&&crossedHomeEdge){
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
        try{prefs.edit().putInt("widget_page_"+Integer.parseInt(source.substring(7)),page).apply();
            workspacePage=page;updateVisibleWidgets();}
        catch(NumberFormatException ignored){}
    }
    private void updateVisibleWidgets(){
        if(widgetColumn==null)return;
        boolean any=false;
        for(int i=0;i<widgetColumn.getChildCount();i++){
            View child=widgetColumn.getChildAt(i);
            if(child instanceof WidgetFrame){
                boolean visible=prefs.getInt("widget_page_"+((WidgetFrame)child).widgetId,0)==workspacePage;
                child.setVisibility(visible?View.VISIBLE:View.GONE);any|=visible;
            }
        }
        // Keep the widget row in the weighted Home layout when empty so app cells do not shift.
        if(widgetScroller!=null)widgetScroller.setVisibility(any?View.VISIBLE:View.INVISIBLE);
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
        drawerShell.setPadding(dp(10),dp(4),dp(10),dp(6));
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
        int rows=prefs.getInt("drawer_rows",6),perPage=3*rows;
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
        int rows=prefs.getInt("drawer_rows",6),perPage=3*rows;
        int column=Math.max(0,Math.min(2,(int)(x/Math.max(1,pager.getWidth()/3))));
        int row=Math.max(0,Math.min(rows-1,(int)(y/Math.max(1,pager.getHeight()/rows))));
        return Math.min(drawerEntries.size()-1,currentPage*perPage+row*3+column);
    }
    private boolean drawerDropOnIcon(View target,DragEvent event){
        int[] where=new int[2],pagerWhere=new int[2];target.getLocationOnScreen(where);pager.getLocationOnScreen(pagerWhere);
        float x=where[0]+event.getX()-pagerWhere[0],y=where[1]+event.getY()-pagerWhere[1];
        float cellWidth=Math.max(1f,pager.getWidth()/3f),cellHeight=Math.max(1f,pager.getHeight()/(float)prefs.getInt("drawer_rows",6));
        float fractionX=(x%cellWidth)/cellWidth,fractionY=(y%cellHeight)/cellHeight;
        return fractionX>0.22f&&fractionX<0.78f&&fractionY>0.15f&&fractionY<0.78f;
    }
    private void applyDrawerDrop(String source,int index,boolean onIcon){
        if(index<0||index>=drawerEntries.size()||source.startsWith("widget:"))return;
        MenuModel.Entry target=drawerEntries.get(index);
        if(source.equals(target.key)){
            boolean context=!drawerDragMoved;View held=activeDragView;
            finishDrawerDrag();
            if(context&&held!=null)held.post(()->edit(target,held));
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
        if(index!=drawerHoverIndex){drawerHoverIndex=index;drawerDropIndex=index;previewDrawerReorder(index);}
    }
    private void previewDrawerReorder(int targetIndex){
        if(currentPage>=pageStrip.getChildCount())return;
        View page=pageStrip.getChildAt(currentPage);if(!(page instanceof ViewGroup))return;
        int source=-1;for(int i=0;i<drawerEntries.size();i++)if(drawerEntries.get(i).key.equals(activeDragKey)){source=i;break;}
        int rows=prefs.getInt("drawer_rows",6),perPage=3*rows,pageStart=currentPage*perPage;
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
            if(!drawerOpen&&dy<-dp(65)&&Math.abs(dy)>Math.abs(dx)*1.3f&&swipeY>root.getHeight()*0.30f){
                MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);cancel.recycle();openDrawer();swipeConsumed=true;return true;
            }
        }
        if(event.getAction()==MotionEvent.ACTION_UP){
            float dy=event.getY()-swipeY,dx=event.getX()-swipeX;
            if(Math.abs(dy)>dp(90)&&Math.abs(dy)>Math.abs(dx)*1.3f){
                if(drawerOpen&&dy>0){closeDrawer();return true;}
            }
        }
        return super.dispatchTouchEvent(event);
    }
    private void cancelHomeLongPress(){if(pendingHomeLongPress!=null){handler.removeCallbacks(pendingHomeLongPress);pendingHomeLongPress=null;}}
    private boolean emptyHomeTouch(View view,MotionEvent event){
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            cancelHomeLongPress();pendingHomeLongPress=()->{pendingHomeLongPress=null;showHomeOptions();};
            handler.postDelayed(pendingHomeLongPress,ViewConfiguration.getLongPressTimeout());return true;
        }
        if(event.getAction()==MotionEvent.ACTION_MOVE){
            if(Math.abs(event.getRawX()-swipeX)>touchSlop||Math.abs(event.getRawY()-swipeY)>touchSlop)cancelHomeLongPress();
            return true;
        }
        if(event.getAction()==MotionEvent.ACTION_UP||event.getAction()==MotionEvent.ACTION_CANCEL){cancelHomeLongPress();return true;}
        return true;
    }
    @Override public void onBackPressed(){if(drawerOpen)closeDrawer();/* Home remains the root destination. */}
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
        handler.removeCallbacks(tick);stopStartupSound();
        if(startup!=null){startup.release();root.removeView(startup);startup=null;startupPending=true;}
        try{widgetHost.stopListening();}catch(Exception ignored){}
        try{if(telephony!=null)telephony.listen(signalListener,PhoneStateListener.LISTEN_NONE);}catch(Exception ignored){}
        if(receiverRegistered){unregisterReceiver(changes);receiverRegistered=false;}
        sounds.release();super.onPause();
    }
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus){updateSystemStatusBar();maybeStartup();}}
    private void maybeStartup(){
        if(!startupPending||startup!=null)return;
        KeyguardManager keyguard=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        if(keyguard!=null&&keyguard.isKeyguardLocked())return;
        startupPending=false;
        startup=new StartupView(this,startStartupSound(),this::stopStartupSound,()->{
            if(startup!=null){root.removeView(startup);startup=null;}
            maybeRequestWallpaperPermission();
        });
        root.addView(startup,new FrameLayout.LayoutParams(-1,-1));
    }
    private int startStartupSound(){
        if(!prefs.getBoolean("startup_sound",true))return 85;
        try{
            AudioManager a=(AudioManager)getSystemService(AUDIO_SERVICE);
            if(a!=null&&a.getRingerMode()!=AudioManager.RINGER_MODE_NORMAL)return 85;
            startupPlayer=new MediaPlayer();
            startupPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            try(AssetFileDescriptor file=getAssets().openFd("n95/startup_sound_user.mp3")){
                startupPlayer.setDataSource(file.getFileDescriptor(),file.getStartOffset(),file.getLength());
            }
            startupPlayer.setOnErrorListener((p,w,e)->{Log.e("NokiaStartup","MediaPlayer error "+w+"/"+e);stopStartupSound();return true;});
            startupPlayer.prepare();int duration=startupPlayer.getDuration();startupPlayer.start();
            return Math.max(65,Math.min(220,duration>0?duration/20:85));
        }catch(Exception e){Log.e("NokiaStartup","sound unavailable",e);stopStartupSound();return 85;}
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
        notificationIndicators=NokiaNotificationListener.statusIndicators(this);
        if(statusGlyph!=null)statusGlyph.invalidate();
    }
    private final class StatusGlyph extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        private final Paint notificationPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Bitmap signalSegment,batterySegment,signalMark,batteryMark;
        private final Path messageGlyph=new Path(),missedGlyph=new Path(),bluetoothGlyph=new Path(),headsetGlyph=new Path();
        StatusGlyph(Context c){
            super(c);setContentDescription("Nokia: время, дата, сеть, Wi-Fi и батарея");
            signalSegment=sprite("signal_segment");batterySegment=sprite("battery_segment");
            signalMark=sprite("signal_mark");batteryMark=sprite("battery_mark");
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
            notificationPaint.setColor(Color.WHITE);
            notificationPaint.setStrokeCap(Paint.Cap.ROUND);
            notificationPaint.setStrokeJoin(Paint.Join.ROUND);
        }
        private Bitmap sprite(String name){
            try(InputStream stream=getAssets().open("reference/"+name+".png")){return BitmapFactory.decodeStream(stream);}
            catch(Exception ignored){return null;}
        }
        private void stamp(Canvas canvas,Bitmap image,float left,float top,float width,float height){
            if(image!=null)canvas.drawBitmap(image,null,new RectF(left,top,left+width,top+height),p);
        }
        private void drawNotification(Canvas canvas,Path glyph,int kind,float left){
            canvas.save();canvas.translate(left,dp(3));
            float scale=dp(15)/16f;canvas.scale(scale,scale);
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
            int count=0;float x=getWidth()-dp(35);
            if((notificationIndicators&1)!=0){drawNotification(c,messageGlyph,0,x);x-=dp(17);count++;}
            if((notificationIndicators&2)!=0){drawNotification(c,missedGlyph,1,x);x-=dp(17);count++;}
            if(bluetoothEnabled){drawNotification(c,bluetoothGlyph,2,x);x-=dp(17);count++;}
            if(headsetConnected){drawNotification(c,headsetGlyph,3,x);count++;}
            return count;
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            p.setColor(Color.WHITE);p.setAlpha(255);
            int rightIcons=drawNotifications(c);
            float signalX=dp(2);
            float batteryX=getWidth()-dp(15);
            stamp(c,signalMark,signalX,dp(25),dp(12),dp(10));
            stamp(c,batteryMark,batteryX,dp(23),dp(13),dp(14));
            int signalBars=device.signalLevel<0?0:Math.min(7,Math.round(device.signalLevel*7f/4f));
            int batteryBars=device.batteryPercent<0?0:device.batteryPercent>=100?7:Math.max(1,(device.batteryPercent*7)/100);
            if(device.batteryPercent>=0&&device.batteryPercent<=15&&!device.charging){
                batteryBars=(SystemClock.uptimeMillis()/650)%2==0?Math.max(1,batteryBars):0;
                postInvalidateDelayed(650);
            }else if(device.charging&&device.batteryPercent>=0&&device.batteryPercent<100){
                batteryBars=Math.min(7,batteryBars+(int)((SystemClock.uptimeMillis()/340)%(8-batteryBars)));
                postInvalidateDelayed(340);
            }
            for(int i=0;i<signalBars;i++)stamp(c,signalSegment,signalX+dp(1),dp(21-i*3),dp(10),dp(2.5f));
            for(int i=0;i<batteryBars;i++)stamp(c,batterySegment,batteryX+dp(1),dp(21-i*3),dp(11),dp(2.5f));
            p.setColor(Color.WHITE);p.setTypeface(android.graphics.Typeface.create("sans-serif-condensed",android.graphics.Typeface.BOLD));
            p.setShadowLayer(dp(2),0,dp(1),Color.BLACK);
            float textX=signalX+dp(19);
            p.setTextSize(getResources().getDisplayMetrics().scaledDensity*17);
            c.drawText(statusTime,textX,dp(19),p);
            float operatorX=textX+p.measureText(statusTime)+dp(7);
            float wifiX=batteryX-dp(21+rightIcons*17);
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
                p.setColor(wifiConnected?Color.WHITE:0x99FFFFFF);
                p.setTextSize(getResources().getDisplayMetrics().scaledDensity*9);
                c.drawText("W",wifiX-dp(8),dp(17),p);
                for(int i=0;i<4;i++){
                    p.setColor(wifiConnected&&wifiLevel>=i?Color.WHITE:0x66FFFFFF);
                    float h=dp(3+i*2);
                    c.drawRect(wifiX+dp(i*3),dp(19)-h,wifiX+dp(i*3+2),dp(19),p);
                }
            }
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
            prefs.edit().putString("shortcut_"+n,e.key).apply();buildDock();
        }).show();
    }
    private void chooseShortcut(int slot){
        ArrayList<MenuModel.Entry> choices=new ArrayList<>(allInstalled());
        choices.add(0,special("phone"));choices.add(1,special("chrome"));choices.add(2,special("camera"));
        String[] labels=new String[choices.size()];for(int i=0;i<labels.length;i++)labels[i]=choices.get(i).label;
        new AlertDialog.Builder(this).setTitle("Ярлык "+(slot+1)).setItems(labels,(d,n)->{prefs.edit().putString("shortcut_"+slot,choices.get(n).key).apply();buildDock();}).show();
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
        String[] choices={"Внешний вид →","Цвет темы","Сетка приложений","Обои","Размытие обоев","Затемнение меню","Прозрачность окон","Ярлыки Home","Виджеты","Заставка","Звуки Nokia","Сброс меню"};
        final AlertDialog[] current=new AlertDialog[1];
        AlertDialog.Builder builder=new AlertDialog.Builder(this)
            .setCustomTitle(settingsTitle("Nokia Experience · Настройки",()->{if(current[0]!=null)current[0].dismiss();}))
            .setItems(choices,(d,n)->{
            switch(n){
                case 0:showAppearanceSettings();break;case 1:chooseThemeColor();break;case 2:chooseGrid();break;case 3:chooseWallpaper();break;
                case 4:chooseBlur();break;case 5:chooseDim();break;case 6:chooseWindowTransparency();break;
                case 7:chooseDockSettings();break;case 8:addWidget();break;case 9:chooseStartup();break;case 10:chooseSounds();break;
                case 11:new AlertDialog.Builder(this).setTitle("Сброс меню").setPositiveButton("Сбросить",(x,w)->{menu.organizer.reset();refreshGrid();}).setNegativeButton("Отмена",null).show();
            }
        });
        current[0]=showSettingsDialog(builder);
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
        switch(interfaceStyle()){case "n95":return "N95";case "5530":return "5530";default:return "Nokia Exp";}
    }
    private void chooseStyle(){
        String[] styles={"N95","5530","Nokia Exp"};String[] values={"n95","5530","exp"};
        int selected=Arrays.asList(values).indexOf(interfaceStyle());
        showSettingsDialog(new AlertDialog.Builder(this).setTitle("Interface Style")
            .setSingleChoiceItems(styles,Math.max(0,selected),(d,n)->{
                prefs.edit().putString("style",values[n]).apply();d.dismiss();
                applyWindowTransparency();buildDock();refreshGrid();refreshWorkspace();
                showAppearanceSettings();
            }).setNegativeButton("‹ Назад",(d,w)->showAppearanceSettings()));
    }
    private void chooseThemeColor(){
        String[] names={"Красный","Синий","Чёрный","Оранжевый","Голубой","Белый"};
        String[] values={"red","blue","black","orange","cyan","white"};
        int selected=Arrays.asList(values).indexOf(prefs.getString("theme_color","profile"));
        showSettingsDialog(new AlertDialog.Builder(this).setTitle("Цвет Nokia UI")
            .setSingleChoiceItems(names,Math.max(0,selected),(d,n)->{
                prefs.edit().putString("theme_color",values[n]).apply();d.dismiss();
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
                    prefs.edit().putInt("dock_count",selected+3).apply();dialog.dismiss();buildDock();
                }).show();
            else chooseShortcut(n-1);
        }).show();
    }
    private void chooseGrid(){
        new AlertDialog.Builder(this).setTitle("Сетка приложений").setSingleChoiceItems(new String[]{"3 × 5","3 × 6"},prefs.getInt("drawer_rows",6)==5?0:1,(d,n)->{
            prefs.edit().putInt("drawer_rows",n==0?5:6).apply();refreshGrid();d.dismiss();
        }).show();
    }
    private void chooseBlur(){
        new AlertDialog.Builder(this).setTitle("Размытие обоев").setSingleChoiceItems(new String[]{"Выкл","Низкое","Среднее","Сильное"},prefs.getInt("blur",1),(d,n)->{
            prefs.edit().putInt("blur",n).apply();updateDrawerBackground();d.dismiss();
        }).show();
    }
    private void chooseDim(){
        SeekBar bar=new SeekBar(this);bar.setMax(70);bar.setProgress(prefs.getInt("drawer_dim",38));
        new AlertDialog.Builder(this).setTitle("Затемнение меню").setMessage("0–70 %, обои остаются видимыми").setView(bar)
            .setPositiveButton("Сохранить",(d,w)->{prefs.edit().putInt("drawer_dim",bar.getProgress()).apply();updateDrawerBackground();}).setNegativeButton("Отмена",null).show();
    }
    private void chooseWindowTransparency(){
        SeekBar bar=new SeekBar(this);bar.setMax(90);bar.setProgress(windowTransparency());
        new AlertDialog.Builder(this).setTitle("Прозрачность окон")
            .setMessage("Панели Nokia, док и затемнение меню · 0–90 %").setView(bar)
            .setPositiveButton("Сохранить",(d,w)->{
                prefs.edit().putInt("window_transparency",bar.getProgress()).apply();applyWindowTransparency();
            }).setNegativeButton("Отмена",null).show();
    }
    private void applyWindowTransparency(){
        if(dock!=null)dock.setBackground(background(panel(),dockRadius()));
        View header=drawerShell==null?null:drawerShell.findViewWithTag("drawerHeader");
        if(header!=null)header.setBackgroundColor(Color.TRANSPARENT);
        TextView title=drawerShell==null?null:drawerShell.findViewWithTag("drawerTitle");
        if(title!=null)title.setTextColor(accent()==Color.WHITE?Color.WHITE:accent());
        if(widgetColumn!=null)for(int i=0;i<widgetColumn.getChildCount();i++)
            widgetColumn.getChildAt(i).setBackground(background(panel(),10));
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
        Drawable source=wallpaper.getDrawable();int level=prefs.getInt("blur",1);
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
        int alpha=Math.round(Math.max(0,Math.min(70,prefs.getInt("drawer_dim",38)))*255/100f);
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
        String value=prefs.getString("widgets","");
        if(!value.isEmpty())for(String item:value.split(","))try{attachWidget(Integer.parseInt(item));}catch(Exception ignored){}
        updateVisibleWidgets();
    }
    private void attachWidget(int id){
        AppWidgetProviderInfo info=widgetManager.getAppWidgetInfo(id);if(info==null)return;
        AppWidgetHostView view=widgetHost.createView(this,id,info);view.setAppWidget(id,info);
        int height=Math.max(100,prefs.getInt("widget_height_"+id,180));
        int width=prefs.getInt("widget_width_"+id,-1);
        WidgetFrame wrapper=new WidgetFrame(this,id,info);wrapper.setBackground(background(panel(),10));
        wrapper.addView(view,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(width<0?-1:dp(width),dp(height));lp.bottomMargin=dp(8);
        widgetColumn.addView(wrapper,lp);
        wrapper.setOnDragListener((v,event)->{
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
                    MotionEvent cancel=MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_CANCEL,downX,downY,0);
                    WidgetFrame.super.dispatchTouchEvent(cancel);cancel.recycle();
                    showWidgetHandles(this);
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
        GradientDrawable border=background(panel(),10);border.setStroke(dp(2),accent());wrapper.setBackground(border);
        View old=wrapper.findViewWithTag("widgetHandles");if(old!=null)wrapper.removeView(old);
        LinearLayout tools=new LinearLayout(this);tools.setTag("widgetHandles");tools.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        tools.setBackground(background(panel(),8));
        TextView move=label("✥",23,Color.WHITE);tools.addView(move,new LinearLayout.LayoutParams(dp(48),dp(48)));
        move.setContentDescription("Переместить виджет");
        move.setOnTouchListener((v,event)->{
            if(event.getAction()==MotionEvent.ACTION_DOWN){
                String key="widget:"+wrapper.widgetId;
                activeDragKey=key;activeDragView=wrapper;
                if(wrapper.startDragAndDrop(android.content.ClipData.newPlainText("widget",key),
                    new View.DragShadowBuilder(wrapper),key,0))wrapper.animate().alpha(0.3f).setDuration(90).start();
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
                            prefs.edit().putInt("widget_width_"+wrapper.widgetId,widthDp)
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
    private void hideWidgetHandles(WidgetFrame wrapper){
        if(wrapper==null)return;
        View controls=wrapper.findViewWithTag("widgetHandles");if(controls!=null)wrapper.removeView(controls);
        wrapper.setBackground(background(panel(),10));if(editingWidget==wrapper)editingWidget=null;
    }
    private void moveWidgetBefore(String source,String target){
        ArrayList<String> ids=new ArrayList<>(Arrays.asList(prefs.getString("widgets","").split(",")));
        String id=source.substring("widget:".length());if(!ids.remove(id))return;
        int index=target==null?ids.size():ids.indexOf(target.substring("widget:".length()));
        ids.add(index<0?ids.size():index,id);
        prefs.edit().putString("widgets",TextUtils.join(",",ids)).apply();restoreWidgets();
    }
    private void saveWidget(int id){
        String old=prefs.getString("widgets","");prefs.edit().putString("widgets",old.isEmpty()?""+id:old+","+id).apply();restoreWidgets();
    }
    private void removeWidget(int id){
        ArrayList<String> all=new ArrayList<>(Arrays.asList(prefs.getString("widgets","").split(",")));
        all.remove(String.valueOf(id));prefs.edit().putString("widgets",TextUtils.join(",",all))
            .remove("widget_width_"+id).remove("widget_height_"+id).apply();
        try{widgetHost.deleteAppWidgetId(id);}catch(Exception ignored){}restoreWidgets();
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
