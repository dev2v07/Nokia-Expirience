package com.nokiaexperience.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Visual chooser based only on provider-supplied Android widget metadata. */
public final class WidgetPreviewPicker {
    public interface Selection { void selected(AppWidgetProviderInfo info); }
    private WidgetPreviewPicker(){}
    public static boolean show(Activity activity,AppWidgetManager manager,Selection selection){
        List<AppWidgetProviderInfo> providers;
        try{providers=manager.getInstalledProviders();}catch(Exception e){return false;}
        if(providers==null||providers.isEmpty())return false;
        ArrayList<AppWidgetProviderInfo> home=new ArrayList<>();
        for(AppWidgetProviderInfo info:providers)
            if(info!=null&&(info.widgetCategory==0||(info.widgetCategory&AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)!=0))home.add(info);
        if(home.isEmpty())return false;
        GridView grid=new GridView(activity);grid.setNumColumns(2);grid.setVerticalSpacing(dp(activity,8));
        grid.setHorizontalSpacing(dp(activity,8));grid.setPadding(dp(activity,10),dp(activity,10),dp(activity,10),dp(activity,10));
        grid.setAdapter(new BaseAdapter(){
            public int getCount(){return home.size();}
            public Object getItem(int i){return home.get(i);}
            public long getItemId(int i){return i;}
            public View getView(int index,View old,ViewGroup parent){
                AppWidgetProviderInfo info=home.get(index);
                PackageManager pm=activity.getPackageManager();
                LinearLayout card=new LinearLayout(activity);card.setOrientation(LinearLayout.VERTICAL);
                card.setGravity(Gravity.CENTER);card.setPadding(dp(activity,8),dp(activity,8),dp(activity,8),dp(activity,6));
                card.setBackgroundColor(0xFFF1F3F5);
                ImageView preview=new ImageView(activity);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
                Drawable image=null;
                try{image=info.loadPreviewImage(activity,activity.getResources().getDisplayMetrics().densityDpi);}catch(Exception ignored){}
                boolean hasPreview=image!=null;
                if(image==null)try{image=info.loadIcon(activity,activity.getResources().getDisplayMetrics().densityDpi);}catch(Exception ignored){}
                preview.setImageDrawable(image);card.addView(preview,new LinearLayout.LayoutParams(-1,dp(activity,108)));
                String app=info.provider.getPackageName();
                try{app=pm.getApplicationLabel(pm.getApplicationInfo(app,0)).toString();}catch(Exception ignored){}
                TextView appText=text(activity,app,11,0xFF485363);card.addView(appText,new LinearLayout.LayoutParams(-1,dp(activity,19)));
                TextView name=text(activity,info.loadLabel(pm),13,Color.BLACK);card.addView(name,new LinearLayout.LayoutParams(-1,dp(activity,23)));
                float density=activity.getResources().getDisplayMetrics().density;
                String size=Math.max(1,Math.round(info.minWidth/density))+" × "+Math.max(1,Math.round(info.minHeight/density))+" dp";
                TextView dimensions=text(activity,hasPreview?size:size+" · нет превью",10,0xFF697586);
                card.addView(dimensions,new LinearLayout.LayoutParams(-1,dp(activity,20)));
                card.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1,dp(activity,187)));
                return card;
            }
        });
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Виджеты Android")
            .setView(grid).setNegativeButton("Отмена",null).create();
        grid.setOnItemClickListener((parent,view,position,id)->{dialog.dismiss();selection.selected(home.get(position));});
        dialog.show();
        return true;
    }
    private static TextView text(Activity activity,String value,int sp,int color){
        TextView t=new TextView(activity);t.setText(value);t.setTextSize(sp);t.setTextColor(color);
        t.setSingleLine(true);t.setEllipsize(android.text.TextUtils.TruncateAt.END);t.setGravity(Gravity.CENTER);
        return t;
    }
    private static int dp(Activity activity,int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
}
