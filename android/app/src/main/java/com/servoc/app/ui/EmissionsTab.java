package com.servoc.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.hazrob.tank.emissions.EmissionResearchEngine;
import com.hazrob.tank.emissions.EmissionSessionStore;
import com.hazrob.tank.emissions.EmissionReportPdf;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Dedicated subtab: named engines, one live MQ capture, source-status matrix and PDF.
 * No food engine dependencies, no gas ChemCore modification, no invented gas ppm.
 */
public final class EmissionsTab {
    private static final long DURATION_MS=60000;
    private final Context context;
    private final HazRobHost host;
    private final EmissionSessionStore store;
    private final ScrollView view;
    private final Spinner engineMenu;
    private final Button create,delete,measure,report;
    private final CheckBox includeParticles;
    private final TextView status,summary;
    private final ProgressBar progress;
    private final TableLayout table;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final List<EmissionSessionStore.Engine> engines=new ArrayList<>();
    private final Map<String,Double> sums=new LinkedHashMap<>();
    private final Map<String,Integer> counts=new LinkedHashMap<>();
    private final Map<String,Long> scanCycles=new LinkedHashMap<>();
    private String captureName="";
    private long captureEngineId=-1,startClock=0,startedAtMs=0;
    private int frames=0;
    private boolean capturing=false,capturePM=false;
    private Runnable tick;

    public EmissionsTab(Context c,HazRobHost host){
        context=c;this.host=host;store=new EmissionSessionStore(c);
        view=new ScrollView(c);view.setFillViewport(true);
        LinearLayout root=UiKit.vbox(c);
        root.setPadding(UiKit.dp(c,12),UiKit.dp(c,12),UiKit.dp(c,12),UiKit.dp(c,30));
        root.setBackgroundColor(0xFFF7F8FA);
        view.addView(root);
        root.addView(UiKit.title(c,"🏭 قياس انبعاثات المحركات والمولدات"),UiKit.full(c));
        TextView hint=UiKit.small(c,
            "إدارة محركات مستقلة وقراءات محفوظة لكل محرك. محرك التحليل النهائي ومراجع IFC الكمية سيُعتمدان لاحقًا. الإشارات الحالية لحساسات MQ ليست تركيزات SOx أو NOx أو CO₂ معتمدة.");
        hint.setTextColor(0xFF303D4F);
        root.addView(hint,UiKit.full(c));

        LinearLayout controls=UiKit.hbox(c);
        create=UiKit.button(c,"＋ استحداث");
        delete=UiKit.button(c,"حذف");
        controls.addView(create,UiKit.weight(1));
        controls.addView(delete,UiKit.weight(1));
        root.addView(controls,UiKit.full(c));
        engineMenu=new Spinner(c);
        UiKit.outlinedSpinner(engineMenu);
        root.addView(engineMenu,UiKit.full(c));

        includeParticles=new CheckBox(c);
        includeParticles.setText("تضمين قياس الجزيئات PM1 / PM2.5 / PM10 عند توفر الحساس");
        includeParticles.setTextColor(0xFF182638);
        includeParticles.setChecked(false);
        root.addView(includeParticles,UiKit.full(c));

        measure=UiKit.button(c,"▶ قياس الانبعاثات");
        root.addView(measure,UiKit.full(c));
        progress=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(60);
        root.addView(progress,UiKit.full(c));
        status=UiKit.text(c,"اختر محركًا لبدء القياس.");
        status.setTextColor(Color.BLACK);
        root.addView(status,UiKit.full(c));
        summary=UiKit.text(c,"لا توجد قياسات محفوظة لهذا المحرك.");
        summary.setTextColor(Color.BLACK);
        root.addView(summary,UiKit.full(c));

        HorizontalScrollView horizontal=new HorizontalScrollView(c);
        table=new TableLayout(c);
        table.setStretchAllColumns(false);
        table.setMinimumWidth(UiKit.dp(c,780));
        horizontal.addView(table,new android.widget.FrameLayout.LayoutParams(-2,-2));
        root.addView(horizontal,UiKit.full(c));
        report=UiKit.button(c,"📄 تقرير المحرك المحدد PDF");
        root.addView(report,UiKit.full(c));

        create.setOnClickListener(v->createEngine());
        delete.setOnClickListener(v->deleteSelected());
        measure.setOnClickListener(v->start());
        report.setOnClickListener(v->exportPdf());
        engineMenu.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){
                if(!capturing)refreshSelection();
            }
            public void onNothingSelected(android.widget.AdapterView<?> p){if(!capturing)refreshSelection();}
        });
        reload(-1);
    }

    public View view(){return view;}
    private static String label(EmissionSessionStore.Engine e){return e==null?"":e.name;}
    private EmissionSessionStore.Engine selected(){
        int i=engineMenu.getSelectedItemPosition();
        return i>=0&&i<engines.size()?engines.get(i):null;
    }
    private void reload(long preferredId){
        engines.clear();
        engines.addAll(store.engines());
        List<String> names=new ArrayList<>();
        for(EmissionSessionStore.Engine e:engines)names.add(e.name);
        if(names.isEmpty())names.add("لا توجد محركات — اضغط استحداث");
        engineMenu.setAdapter(UiKit.lightOptions(context,names.toArray(new String[0])));
        int choice=0;
        for(int i=0;i<engines.size();i++)if(engines.get(i).id==preferredId){choice=i;break;}
        engineMenu.setSelection(choice);
        refreshSelection();
    }
    private void createEngine(){
        if(capturing)return;
        EditText input=UiKit.input(context,"");
        input.setSingleLine(true);input.setHint("اسم المحرك أو المولد");
        new AlertDialog.Builder(context).setTitle("استحداث محرك جديد")
            .setView(input).setNegativeButton("إلغاء",(dialog,which)->{})
            .setPositiveButton("تأكيد",(dialog,which)->{
                String name=input.getText().toString().trim();
                if(name.isEmpty()){host.showToast("يجب كتابة اسم المحرك");return;}
                try{
                    long id=store.create(name);reload(id);
                    host.showToast("تم إضافة المحرك: "+name);
                }catch(Exception e){host.showToast("تعذر الإضافة: الاسم مكرر أو غير صالح");}
            }).show();
    }
    private void deleteSelected(){
        if(capturing)return;
        EmissionSessionStore.Engine e=selected();if(e==null)return;
        new AlertDialog.Builder(context).setTitle("حذف المحرك وقياساته")
            .setMessage("سيُحذف «"+e.name+"» وجميع نتائج القياس الخاصة به نهائيًا.")
            .setNegativeButton("إلغاء",(d,w)->{})
            .setPositiveButton("حذف",(d,w)->{
                store.delete(e.id);reload(-1);host.showToast("تم حذف المحرك ونتائجه");
            }).show();
    }
    private void refreshSelection(){
        EmissionSessionStore.Engine e=selected();
        boolean has=e!=null;
        measure.setEnabled(has&&!capturing);
        delete.setEnabled(has&&!capturing);
        create.setEnabled(!capturing);
        engineMenu.setEnabled(!capturing);
        includeParticles.setEnabled(!capturing);
        JSONObject latest=has?store.latest(e.id):null;
        report.setEnabled(latest!=null&&!capturing);
        if(!capturing){
            status.setText(has?"المحرك المحدد: "+e.name+" — جاهز للقياس":"استحدث محركًا أولًا.");
            render(latest,has?store.count(e.id):0);
        }
    }
    private void start(){
        EmissionSessionStore.Engine e=selected();
        if(capturing||e==null)return;
        JSONObject latest=host.latestDiagnostics();
        if(latest==null||latest.optJSONObject("mq")==null){
            host.showToast("يلزم الاتصال بالروبوت لبدء قياس انبعاثات");return;
        }
        capturing=true;captureEngineId=e.id;captureName=e.name;
        capturePM=includeParticles.isChecked();
        startClock=SystemClock.elapsedRealtime();startedAtMs=System.currentTimeMillis();
        sums.clear();counts.clear();scanCycles.clear();frames=0;
        progress.setProgress(0);
        measure.setEnabled(false);delete.setEnabled(false);create.setEnabled(false);
        engineMenu.setEnabled(false);includeParticles.setEnabled(false);report.setEnabled(false);
        status.setText("بدأ تسجيل مؤشرات انبعاثات "+captureName);
        tick=()->{
            if(!capturing)return;
            sample();
            long elapsed=Math.max(0,SystemClock.elapsedRealtime()-startClock);
            progress.setProgress((int)Math.min(60,elapsed/1000));
            status.setText(String.format(Locale.US,
                "قياس %s • %d/60 ثانية • %d إطارات بيانات صحيحة",
                captureName,Math.min(60,elapsed/1000),frames));
            if(elapsed>=DURATION_MS)finish(elapsed);
            else handler.postDelayed(tick,1000);
        };
        handler.post(tick);
    }
    public void refresh(){
        // The independent Handler drives capture even when the user switches tabs.
        if(!capturing)refreshSelection();
    }
    private void sample(){
        JSONObject top=host.latestDiagnostics();
        JSONObject mq=top==null?null:top.optJSONObject("mq");
        if(mq==null)return;
        int got=0;
        for(String sensor:new String[]{"MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"}){
            JSONObject row=mq.optJSONObject(sensor.toLowerCase(Locale.ROOT));
            if(row==null||!row.optBoolean("ok",row.optBoolean("valid",false))||
               !row.optBoolean("powered",true)||
               row.optLong("last_ok_age_ms",Long.MAX_VALUE)>3000)continue;
            long cycle=row.optLong("scan_cycle",-1);
            double rs=row.optDouble("rs",Double.NaN);
            if(cycle<0||!Double.isFinite(rs)||rs<=0)continue;
            Long previous=scanCycles.get(sensor);
            if(previous!=null&&previous==cycle)continue;
            scanCycles.put(sensor,cycle);
            sums.put(sensor,sums.containsKey(sensor)?sums.get(sensor)+rs:rs);
            counts.put(sensor,counts.containsKey(sensor)?counts.get(sensor)+1:1);
            got++;
        }
        if(got>0)frames++;
    }
    private void finish(long elapsed){
        if(!capturing)return;
        capturing=false;
        handler.removeCallbacks(tick);
        try{
            if(frames<10){
                status.setText("لم ينجح القياس: بيانات MQ الحديثة غير كافية، ولم تُحفظ نتيجة جديدة.");
                refreshSelection();return;
            }
            Map<String,Double> avg=new LinkedHashMap<>();
            for(Map.Entry<String,Double> e:sums.entrySet()){
                int n=counts.containsKey(e.getKey())?counts.get(e.getKey()):0;
                if(n>0)avg.put(e.getKey(),e.getValue()/n);
            }
            JSONObject result=EmissionResearchEngine.complete(captureEngineId,captureName,
                startedAtMs,elapsed,capturePM,frames,avg,counts);
            store.addMeasurement(captureEngineId,result);
            refreshSelection();
            status.setText("اكتمل تسجيل قياس "+captureName+" • "+frames+
                " إطارات • التحليل الكمي للملوثات معلّق لحين اعتماد المحرك والمراجع.");
        }catch(Exception failure){
            refreshSelection();
            status.setText("حدث خلل أثناء حفظ القياس ولم تُعتمد نتيجة جديدة.");
            host.showToast("تعذر حفظ بيانات القياس");
        }
    }
    private TextView cell(String text,boolean header,boolean exceeded){
        TextView view=UiKit.text(context,text==null?"—":text);
        view.setTextSize(12);view.setGravity(Gravity.CENTER);
        view.setPadding(UiKit.dp(context,6),UiKit.dp(context,11),
            UiKit.dp(context,6),UiKit.dp(context,11));
        view.setTextColor(exceeded?Color.WHITE:Color.BLACK);
        view.setBackground(UiKit.outline(context,
            exceeded?0xFFC62828:header?0xFFFFBE32:Color.WHITE,0xFF8795A5));
        if(header||exceeded)view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }
    private void render(JSONObject latest,int historyCount){
        table.removeAllViews();
        TableRow head=new TableRow(context);
        for(String name:new String[]{"الملوث","التركيز","حد IFC","الحالة","إشارة MQ البحثية"})
            head.addView(cell(name,true,false),new TableRow.LayoutParams(0,-2,1f));
        table.addView(head);
        if(latest==null){
            summary.setText("لا توجد قياسات محفوظة للمحرك المحدد.");
            return;
        }
        summary.setText("عدد الجلسات: "+historyCount+" • آخر تسجيل: "+
            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault())
                .format(new java.util.Date(latest.optLong("measured_at_ms",0)))+
            " • "+latest.optLong("frames",0)+" إطار");
        JSONArray list=latest.optJSONArray("pollutants");
        if(list==null)return;
        for(int i=0;i<list.length();i++){
            JSONObject row=list.optJSONObject(i);if(row==null)continue;
            boolean exceeded=row.optBoolean("ifc_comparable",false)&&
                row.optBoolean("exceeded",false)&&!row.isNull("value_ppm");
            String value=row.isNull("value_ppm")?"غير متاح":row.optString("value_ppm","—");
            String limit=row.isNull("limit")?"لم يحدد":row.optString("limit","—");
            String state="MQ_SIGNAL_ONLY".equals(row.optString("status"))?
                "إشارة بحثية فقط":"غير معاير";
            String signal=row.has("mq_average_rs")?
                row.optString("mq_sensor")+" Rs="+
                String.format(Locale.US,"%.4g",row.optDouble("mq_average_rs")):
                "غير متاح";
            TableRow item=new TableRow(context);
            item.addView(cell(row.optString("label"),false,false),new TableRow.LayoutParams(0,-2,1f));
            item.addView(cell(value,false,exceeded),new TableRow.LayoutParams(0,-2,1f));
            item.addView(cell(limit,false,false),new TableRow.LayoutParams(0,-2,1f));
            item.addView(cell(state,false,false),new TableRow.LayoutParams(0,-2,1f));
            item.addView(cell(signal,false,false),new TableRow.LayoutParams(0,-2,1f));
            table.addView(item);
        }
    }
    private void exportPdf(){
        EmissionSessionStore.Engine e=selected();
        if(e==null||capturing)return;
        JSONObject latest=store.latest(e.id);if(latest==null)return;
        report.setEnabled(false);
        host.io().execute(()->{
            try{
                EmissionReportPdf.save(context,e.name,latest,store.count(e.id));
                view.post(()->{report.setEnabled(true);
                    host.showToast("حُفظ تقرير المحرك المحدد في Downloads/HazRobTank");});
            }catch(Exception problem){
                view.post(()->{report.setEnabled(true);
                    host.showToast("تعذر إنشاء تقرير الانبعاثات");});
            }
        });
    }
}
