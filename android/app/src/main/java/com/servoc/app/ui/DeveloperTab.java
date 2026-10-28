package com.servoc.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.servoc.app.auth.RoleGate;

/** One PIN-protected developer workspace inside the unified HazRob application. */
public final class DeveloperTab {
    private final Context context;private final HazRobHost host;private final Runnable onUnlock;
    private final LinearLayout root;private final FrameLayout page;private final LinearLayout bar;
    private boolean unlocked=false;private int selected=0;
    private Button[] tabs;private AnalyticalLabTab chemCore;private DeveloperConnectionDiagnosticsTab connectionSetup;private CentralLibraryDashboardTab library;private View[] views;

    public DeveloperTab(Context c,HazRobHost host,Runnable onUnlock){
        context=c;this.host=host;this.onUnlock=onUnlock;
        root=UiKit.vbox(c);root.setBackgroundColor(0xFFF7F8FA);root.setPadding(UiKit.dp(c,6),UiKit.dp(c,6),UiKit.dp(c,6),UiKit.dp(c,10));
        bar=UiKit.hbox(c);
        HorizontalScrollView strip=new HorizontalScrollView(c);
        strip.setHorizontalScrollBarEnabled(false);
        strip.setFillViewport(false);
        strip.addView(bar,new HorizontalScrollView.LayoutParams(-2,-2));
        root.addView(strip,UiKit.full(c));
        page=new FrameLayout(c);root.addView(page,new LinearLayout.LayoutParams(-1,0,1f));
        renderLocked();
    }

    /** R0 belongs to operator Settings in V5; no developer-owned R0 instance. */
    public void release(){}

    public View view(){return root;}
    public boolean isUnlocked(){return unlocked;}
    public void requestUnlock(){if(unlocked)return;askPin();}

    private void renderLocked(){
        bar.removeAllViews();page.removeAllViews();
        LinearLayout box=UiKit.vbox(context);box.setGravity(Gravity.CENTER);box.setPadding(UiKit.dp(context,24),UiKit.dp(context,30),UiKit.dp(context,24),UiKit.dp(context,30));
        TextView title=UiKit.title(context,"المطور");title.setGravity(Gravity.CENTER);box.addView(title,UiKit.full(context));
        TextView note=UiKit.small(context,"مساحة التجارب المرجعية، المختبر التحليلي، تشخيص الاتصال وإدارة المكتبة المركزية.");note.setGravity(Gravity.CENTER);note.setTextColor(0xFF46515E);box.addView(note,UiKit.full(context));
        Button open=UiKit.button(context,"دخول المطور");open.setOnClickListener(v->askPin());LinearLayout.LayoutParams p=UiKit.full(context);p.topMargin=UiKit.dp(context,18);box.addView(open,p);
        page.addView(box,new FrameLayout.LayoutParams(-1,-1));
    }

    private void askPin(){
        EditText pin=UiKit.input(context,"");pin.setHint("كلمة المرور");pin.setGravity(Gravity.CENTER);pin.setSingleLine(true);pin.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        LinearLayout box=UiKit.vbox(context);box.setPadding(UiKit.dp(context,20),UiKit.dp(context,8),UiKit.dp(context,20),0);box.addView(pin,UiKit.full(context));
        AlertDialog d=new AlertDialog.Builder(context).setTitle("دخول المطور").setView(box).setNegativeButton("إلغاء",null).setPositiveButton("دخول",null).create();
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(RoleGate.DEVELOPER_PIN.equals(pin.getText().toString().trim())){d.dismiss();unlock();}else{pin.setText("");Toast.makeText(context,"كلمة المرور غير صحيحة",Toast.LENGTH_SHORT).show();}}));d.show();
    }

    /**
     * Unlock only installs the three developer destinations; experiments live inside the unified laboratory. Constructing every
     * laboratory screen eagerly used to make one SQLite/UI constructor failure
     * crash the complete developer workspace before a tab could be opened.
     */
    private void unlock(){
        if(unlocked)return;
        unlocked=true;
        if(onUnlock!=null)onUnlock.run();
        views=new View[3];
        String[] names={"المختبر","الاتصال","المكتبة"};
        tabs=new Button[names.length];bar.removeAllViews();
        for(int i=0;i<names.length;i++){
            final int x=i;Button b=UiKit.button(context,names[i]);
            b.setTextSize(13);b.setMinWidth(UiKit.dp(context,96));
            b.setOnClickListener(v->show(x));tabs[i]=b;
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);
            p.setMargins(UiKit.dp(context,2),0,UiKit.dp(context,2),0);
            bar.addView(b,p);
        }
        show(0);host.showToast("تم فتح مساحة المطور");
    }

    /** Returns a localized error surface on failure; a later selection can retry. */
    private View obtainView(int index){
        if(views[index]!=null)return views[index];
        try{
            switch(index){
                case 0:
                    if(chemCore==null)chemCore=new AnalyticalLabTab(context,host);
                    views[index]=chemCore.view();break;
                case 1:
                    if(connectionSetup==null)connectionSetup=new DeveloperConnectionDiagnosticsTab(context,host);
                    views[index]=connectionSetup.view();break;
                case 2:
                    if(library==null)library=new CentralLibraryDashboardTab(context,host);
                    views[index]=library.view();break;
                default:throw new IllegalArgumentException("Unknown developer tab");
            }
            if(views[index]==null)throw new IllegalStateException("Null developer screen");
            return views[index];
        }catch(RuntimeException ex){
            android.util.Log.e("TankDeveloper","Cannot open developer tab "+index,ex);
            // Do not cache a failure placeholder: returning to this tab retries.
            TextView warning=UiKit.small(context,
                    "تعذر فتح هذا القسم. بقية أقسام المطور متاحة؛ راجع سجل التشخيص وأعد المحاولة.");
            warning.setTextColor(0xFFB42318);
            warning.setPadding(UiKit.dp(context,16),UiKit.dp(context,20),
                    UiKit.dp(context,16),UiKit.dp(context,20));
            return warning;
        }
    }

    private void show(int index){
        if(!unlocked||views==null)return;
        selected=Math.max(0,Math.min(views.length-1,index));
        View screen=obtainView(selected);
        page.removeAllViews();
        page.addView(screen,new FrameLayout.LayoutParams(-1,-1));
        for(int i=0;i<tabs.length;i++){
            tabs[i].setTextColor(Color.BLACK);
            tabs[i].setBackground(UiKit.outline(context,i==selected?0xFFFFBD36:0xFFF1F3F6,0xFF8994A3));
        }
        refresh();
    }

    public void refresh(){
        if(!unlocked||views==null||views[selected]==null)return;
        try{
            if(selected==0&&chemCore!=null)chemCore.refresh();
            else if(selected==1&&connectionSetup!=null)connectionSetup.refresh();
            else if(selected==2&&library!=null)library.refresh();
        }catch(RuntimeException ex){
            android.util.Log.e("TankDeveloper","Developer refresh failure tab="+selected,ex);
            host.showToast("تعذر تحديث قسم المطور؛ البيانات المحلية محفوظة");
        }
    }

}
