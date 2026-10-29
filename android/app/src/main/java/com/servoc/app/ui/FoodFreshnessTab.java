package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.hazrob.tank.foodfreshness.FoodFreshnessEngine;
import com.hazrob.tank.foodfreshness.FoodFreshnessPdf;
import com.hazrob.tank.foodfreshness.FoodReferenceCatalog;
import com.hazrob.tank.foodfreshness.meat.BeefResearchEngine;
import com.hazrob.tank.foodfreshness.meat.BeefResearchPdf;
import com.hazrob.tank.foodfreshness.meat.BeefResearchCatalog;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Independent single-operation FOOD headspace tab.
 * Only consumes the already-fetched Tank telemetry snapshot from HazRobHost.
 * No networking, heater commands, gas engine calls, persisted history or food
 * labels in chemical candidate identification.
 */
public final class FoodFreshnessTab {
    private static final long STUDY_CAPTURE_MS=180000;
    private static final long MIN_ACCEPT_INTERVAL_MS=950;
    private static final int[] COLORS={0xFF1F6FEB,0xFFE85D04,0xFF279653,0xFF8758A5,
        0xFFD14D72,0xFF0A8795,0xFF955F32,0xFF505A69};
    private final Context context;
    private final HazRobHost host;
    private final ScrollView view;
    private final Spinner foodChoice, modeChoice;
    private final TextView protocolHint, comparisonTitle;
    private String operationMode="MEAT";
    private final Button start,finish,export;
    private final TextView status,results;
    private final ProgressBar progress;
    private final PatternGraph dayGraph,signalGraph;
    private final LinkedHashMap<String,Double> sum=new LinkedHashMap<>();
    private final HashMap<String,Integer> counts=new HashMap<>();
    private final HashMap<String,Long> lastCycle=new HashMap<>();
    private final HashMap<String,List<PlotPoint>> signalHistory=new HashMap<>();
    private final List<String> foodLabels=new ArrayList<>();
    private boolean capturing=false;
    private long started=0,lastAccept=0,frozenElapsed=0;
    private int snapshots=0;
    private double tempSum=0,humiditySum=0,finalTemperature=Double.NaN,finalHumidity=Double.NaN;
    private int tempCount=0,humidityCount=0;
    private String reportId="";
    private FoodFreshnessEngine.Result completed;
    private BeefResearchEngine.Result meatCompleted;
    private Map<String,Double> frozenReadings;

    public FoodFreshnessTab(Context c,HazRobHost h){
        context=c;host=h;
        view=new ScrollView(c);view.setFillViewport(true);
        LinearLayout root=UiKit.vbox(c);
        root.setPadding(UiKit.dp(c,10),UiKit.dp(c,10),UiKit.dp(c,10),UiKit.dp(c,22));
        root.setBackgroundColor(0xFFF7F8FA);
        view.addView(root);
        root.addView(UiKit.title(c,"🍎 فحص نضارة الأغذية"),UiKit.full(c));
        root.addView(UiKit.title(c,"طريقة القياس"),UiKit.full(c));
        modeChoice=new Spinner(c);
        modeChoice.setAdapter(UiKit.lightOptions(c,new String[]{"🥩 اللحوم","🍎 الفواكه"}));
        UiKit.outlinedSpinner(modeChoice);
        root.addView(modeChoice,UiKit.full(c));
        TextView note=UiKit.small(c,"قياس آني فردي مستقل عن ChemCore للغازات. نقارن بصمة 8 حساسات MQ مع 59 عينة بحثية. التشابه ليس اختبارًا ميكروبيًا أو شهادة لصلاحية الغذاء.");
        note.setTextColor(0xFF243247);root.addView(note,UiKit.full(c));
        protocolHint=UiKit.small(c,"");
        protocolHint.setTextColor(0xFF172F4A);
        protocolHint.setBackground(UiKit.outline(c,0xFFEDF3F9,0xFFA6B7CB));
        root.addView(protocolHint,UiKit.full(c));
        root.addView(UiKit.title(c,"نوع العينة"),UiKit.full(c));
        foodLabels.add("جميع الأصناف");
        for(String family:FoodFreshnessEngine.foodFamilies())foodLabels.add(family);
        foodChoice=new Spinner(c);
        foodChoice.setAdapter(UiKit.lightOptions(c,foodLabels.toArray(new String[0])));
        UiKit.outlinedSpinner(foodChoice);
        root.addView(foodChoice,UiKit.full(c));
        LinearLayout row=UiKit.hbox(c);
        start=UiKit.button(c,"▶ بدء الفحص");
        finish=UiKit.button(c,"■ إنهاء وتحليل");
        export=UiKit.button(c,"PDF التقرير");
        finish.setEnabled(false);export.setEnabled(false);
        row.addView(start,UiKit.weight(1));
        row.addView(finish,UiKit.weight(1));
        row.addView(export,UiKit.weight(1));
        root.addView(row,UiKit.full(c));
        start.setOnClickListener(v->begin());
        finish.setOnClickListener(v->complete());
        export.setOnClickListener(v->exportPdf());
        progress=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(180);progress.setProgress(0);
        root.addView(progress,UiKit.full(c));
        status=UiKit.text(c,"جاهز لقياس فردي. مدة التسجيل المرجعية 3 دقائق.");
        status.setTextColor(0xFF192535);
        status.setBackground(UiKit.outline(c,Color.WHITE,0xFFA3ABB7));
        root.addView(status,UiKit.full(c));
        results=UiKit.text(c,"لم تبدأ أي عملية فحص.");
        results.setTextColor(0xFF192535);
        results.setTextIsSelectable(true);
        results.setBackground(UiKit.outline(c,Color.WHITE,0xFFA3ABB7));
        root.addView(results,UiKit.full(c));
        root.addView(UiKit.title(c,"مخطط استجابة الحساسات أثناء الفحص"),UiKit.full(c));
        signalGraph=new PatternGraph(c,false);
        root.addView(signalGraph,new LinearLayout.LayoutParams(-1,UiKit.dp(c,208)));
        comparisonTitle=UiKit.title(c,"مخطط مقارنة الدراسة");
        root.addView(comparisonTitle,UiKit.full(c));
        dayGraph=new PatternGraph(c,true);
        root.addView(dayGraph,new LinearLayout.LayoutParams(-1,UiKit.dp(c,208)));
        modeChoice.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> parent,View option,int index,long id){
                if(capturing)return;
                operationMode=index==0?"MEAT":"FRUIT";
                completed=null;meatCompleted=null;frozenReadings=null;reportId="";
                export.setEnabled(false);
                results.setText("ابدأ فحصًا جديدًا بعد اختيار نوع العينة.");
                boolean meat="MEAT".equals(operationMode);
                foodChoice.setVisibility(meat?View.GONE:View.VISIBLE);
                comparisonTitle.setText(meat?
                    "مقارنة الفئات البحثية الأربع للحوم":"التشابه مع أيام دراسة الفواكه");
                protocolHint.setText(meat?
                    "اللحوم: نموذج مستقل للحوم البقرية، مبني على 10,800 قراءة في دراسة Wijaya 2018، بسبعة حساسات متطابقة الطراز. قياس 3 دقائق استكشافي وليس دورة الدراسة الأصلية البالغة 36 ساعة أو فحصًا ميكروبيًا.":
                    "الفواكه: مقارنة بصمة ثمانية حساسات مع 59 مرجعًا؛ زمن اللقطة 180 ثانية وعينات الدراسة متباعدة 24 ساعة. درجة التشابه ليست حكمًا بصلاحية الغذاء.");
                dayGraph.invalidate();
            }
            public void onNothingSelected(android.widget.AdapterView<?> parent){}
        });
        modeChoice.setSelection(0);
    }
    public View view(){return view;}
    private String selectedFamily(){
        int index=foodChoice.getSelectedItemPosition();
        return index<=0?"":foodLabels.get(index);
    }
    private void begin(){
        JSONObject snapshot=host.latestDiagnostics();
        if(snapshot==null||snapshot.optJSONObject("mq")==null){
            host.showToast("يلزم اتصال حي بحساسات MQ لبدء فحص الغذاء");return;
        }
        operationMode=modeChoice.getSelectedItemPosition()==0?"MEAT":"FRUIT";
        capturing=true;completed=null;meatCompleted=null;frozenReadings=null;reportId="";
        sum.clear();counts.clear();lastCycle.clear();signalHistory.clear();
        snapshots=0;started=SystemClock.elapsedRealtime();lastAccept=0;frozenElapsed=0;
        tempSum=humiditySum=0;tempCount=humidityCount=0;
        finalTemperature=finalHumidity=Double.NaN;
        progress.setProgress(0);
        start.setEnabled(false);finish.setEnabled(true);export.setEnabled(false);
        foodChoice.setEnabled(false);modeChoice.setEnabled(false);
        results.setText("جاري تسجيل بصمة الأغذية، لا تغيّر العينة أثناء الفحص.");
        status.setText("القياس جارٍ — 0/180 ثانية");
        signalGraph.invalidate();dayGraph.clear();
        refresh();
    }
    /** Refresh is invoked ONLY by the existing Android telemetry UI refresh. */
    public void refresh(){
        if(!capturing)return;
        long now=SystemClock.elapsedRealtime(),elapsed=Math.max(0,now-started);
        progress.setProgress((int)Math.min(180,elapsed/1000));
        if(elapsed-lastAccept>=MIN_ACCEPT_INTERVAL_MS || lastAccept==0){
            JSONObject snapshot=host.latestDiagnostics();
            JSONObject mq=snapshot==null?null:snapshot.optJSONObject("mq");
            if(mq!=null){
                int accepted=0;
                for(String sensor:FoodReferenceCatalog.SENSOR_ORDER){
                    JSONObject q=mq.optJSONObject(sensor.toLowerCase(Locale.ROOT));
                    if(q==null || !q.optBoolean("ok",q.optBoolean("valid",false)))continue;
                    double rs=q.optDouble("rs",Double.NaN);
                    long cycle=q.optLong("scan_cycle",-1);
                    if(!Double.isFinite(rs)||rs<=0||cycle<0)continue;
                    Long last=lastCycle.get(sensor);
                    if(last!=null&&cycle==last)continue;
                    lastCycle.put(sensor,cycle);
                    sum.put(sensor,sum.getOrDefault(sensor,0.0)+rs);
                    counts.put(sensor,counts.getOrDefault(sensor,0)+1);
                    signalHistory.computeIfAbsent(sensor,k->new ArrayList<>())
                        .add(new PlotPoint(elapsed/1000.0,rs));
                    accepted++;
                }
                if(accepted>0){
                    // The beef source study also recorded T/RH. Use existing
                    // BME280 context, no extra telemetry request to ESP32.
                    JSONObject env=snapshot.optJSONObject("env");
                    if(env!=null&&env.optBoolean("ok",true)){
                        double temp=env.optDouble("t",env.optDouble("temperature",Double.NaN));
                        double rh=env.optDouble("h",env.optDouble("humidity",Double.NaN));
                        if(Double.isFinite(temp)){tempSum+=temp;tempCount++;}
                        if(Double.isFinite(rh)){humiditySum+=rh;humidityCount++;}
                    }
                    snapshots++;signalGraph.invalidate();
                }
            }
            lastAccept=now;
        }
        status.setText(String.format(Locale.US,
            "الفحص جارٍ • %.0f/180 ثانية • %d لقطات • %d حساسات",
            elapsed/1000.0,snapshots,counts.size()));
        if(elapsed>=STUDY_CAPTURE_MS)complete();
    }
    private Map<String,Double> averaged(){
        Map<String,Double> out=new LinkedHashMap<>();
        for(Map.Entry<String,Double> e:sum.entrySet()){
            int n=counts.getOrDefault(e.getKey(),0);
            if(n>0)out.put(e.getKey(),e.getValue()/n);
        }
        return out;
    }
    private void complete(){
        if(!capturing)return;
        capturing=false;
        frozenElapsed=Math.max(0,SystemClock.elapsedRealtime()-started);
        finish.setEnabled(false);start.setEnabled(true);
        foodChoice.setEnabled(true);modeChoice.setEnabled(true);
        frozenReadings=averaged();
        finalTemperature=tempCount>0?tempSum/tempCount:Double.NaN;
        finalHumidity=humidityCount>0?humiditySum/humidityCount:Double.NaN;
        reportId=new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US).format(new Date());
        progress.setProgress((int)Math.min(180,frozenElapsed/1000));
        status.setText(("MEAT".equals(operationMode)?"اللحوم":"الفواكه")+
            " • عملية مكتملة • "+snapshots+" لقطات • "+frozenReadings.size()+
            " حساسات • تقرير واحد "+reportId);
        if("MEAT".equals(operationMode)){
            // Meat source uses seven common MQ models and a four-class beef corpus.
            // It is not a repeat of the fruit headspace protocol/algorithm.
            meatCompleted=BeefResearchEngine.analyze(frozenReadings);
            completed=null;
            export.setEnabled(!meatCompleted.rankedClasses.isEmpty());
            StringBuilder out=new StringBuilder();
            out.append("نتيجة بحثية للحوم البقرية (لا تثبت الصلاحية للاستهلاك)\n")
               .append("المرجع: Wijaya 2018؛ خمس عينات، 10,800 قراءة، حساسات MQ مشتركة: 7.\n")
               .append("الفترة الأصلية: 36 ساعة/عينة؛ عملية الروبوت: ")
               .append(String.format(Locale.US,"%.1f",frozenElapsed/1000.0))
               .append(" ثانية فقط.\n")
               .append("الحرارة المتوسطة: ")
               .append(Double.isFinite(finalTemperature)?String.format(Locale.US,"%.1f °C",finalTemperature):"غير متاحة")
               .append(" • الرطوبة: ")
               .append(Double.isFinite(finalHumidity)?String.format(Locale.US,"%.1f%%",finalHumidity):"غير متاحة")
               .append("\n");
            for(BeefResearchEngine.Match item:meatCompleted.rankedClasses){
                String name;
                switch(item.classCode){
                    case "excellent":name="ممتازة";break;
                    case "good":name="جيدة";break;
                    case "acceptable":name="مقبولة";break;
                    case "spoiled":name="فاسدة (فئة الدراسة)";break;
                    default:name=item.classCode;
                }
                out.append(name).append(" — مطابقة شكلية ")
                   .append(String.format(Locale.US,"%.1f",item.descriptiveSimilarity))
                   .append("/100 (")
                   .append(item.overlap).append(" حساسات)\n");
            }
            out.append("\nأداء الاختبار على عينة بحثية مستقلة: دقة متوازنة ")
               .append(String.format(Locale.US,"%.1f%%",
                     100*BeefResearchCatalog.HELDOUT_BALANCED_ACCURACY))
               .append(" — غير كافية لتصنيف صلاحية الغذاء.\n")
               .append("لا يوجد قياس TVC بكتيري، ولا نقل معايرة مُثبت إلى Tank.");
            if(meatCompleted.rankedClasses.isEmpty())
                out.append("\nلا تتوفر قيم مقاومة صحيحة لإجراء المقارنة.");
            results.setText(out.toString());
        }else{
            List<FoodFreshnessEngine.Reading> inputs=new ArrayList<>();
            for(Map.Entry<String,Double> e:frozenReadings.entrySet())
                inputs.add(new FoodFreshnessEngine.Reading(e.getKey(),e.getValue()));
            completed=FoodFreshnessEngine.analyze(inputs,selectedFamily());
            meatCompleted=null;
            export.setEnabled(!completed.rankedTemplates.isEmpty());
            StringBuilder out=new StringBuilder();
            out.append("مطابقة الفواكه بالمرجع البحثي — ليست حكمًا بصلاحية الغذاء\n")
               .append("العينة: ").append(selectedFamily().isEmpty()?"جميع الأصناف":selectedFamily())
               .append("\nمدة التسجيل: ")
               .append(String.format(Locale.US,"%.1f",frozenElapsed/1000.0))
               .append(" ثانية؛ مرجع اللقطة: 180 ثانية\n");
            int limit=Math.min(8,completed.rankedTemplates.size());
            for(int i=0;i<limit;i++){
                FoodFreshnessEngine.Match m=completed.rankedTemplates.get(i);
                out.append(i+1).append(" — ").append(m.referenceLabel)
                   .append(" | تشابه ")
                   .append(String.format(Locale.US,"%.1f",m.similarityIndex))
                   .append("/100 | ").append(m.overlappingSensors).append(" حساسات\n");
            }
            if(limit==0)out.append("لا توجد بيانات صالحة للمقارنة الحسابية.");
            out.append("\nمرجع الفواكه: Zenodo 10.5281/zenodo.17285312\n")
               .append("لا تثبت النتائج النشاط الميكروبي أو الصلاحية للاستهلاك.");
            results.setText(out.toString());
        }
        dayGraph.invalidate();
    }
    private void exportPdf(){
        if(capturing||frozenReadings==null)return;
        final boolean beef="MEAT".equals(operationMode);
        final FoodFreshnessEngine.Result fruit=completed;
        final BeefResearchEngine.Result meat=meatCompleted;
        if((beef&&meat==null)||(!beef&&fruit==null))return;
        final Map<String,Double> observations=new LinkedHashMap<>(frozenReadings);
        final String id=reportId;final long duration=frozenElapsed;final int count=snapshots;
        final double temp=finalTemperature,rh=finalHumidity;
        export.setEnabled(false);
        host.io().execute(()->{
            try{
                if(beef)BeefResearchPdf.save(context,meat,observations,count,duration,id,temp,rh);
                else FoodFreshnessPdf.save(context,fruit,observations,count,duration,id);
                view.post(()->{
                    export.setEnabled(true);
                    host.showToast("حُفظ تقرير "+(beef?"اللحوم":"الفواكه")+" في Downloads/HazRobTank");
                });
            }catch(Exception e){
                view.post(()->{
                    export.setEnabled(true);
                    host.showToast("تعذر تصدير تقرير الأغذية: "+e.getClass().getSimpleName());
                });
            }
        });
    }
    private static final class PlotPoint {
        final double seconds,value;
        PlotPoint(double t,double v){seconds=t;value=v;}
    }
    private final class PatternGraph extends View {
        final boolean referenceMode;
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        PatternGraph(Context c,boolean dayMode){super(c);referenceMode=dayMode;}
        void clear(){invalidate();}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            float w=getWidth(),h=getHeight();
            canvas.drawColor(Color.WHITE);
            float left=42,top=20,right=w-14,bottom=h-35;
            paint.setColor(0xFFD6DFE9);paint.setStrokeWidth(2f);
            canvas.drawLine(left,top,left,bottom,paint);
            canvas.drawLine(left,bottom,right,bottom,paint);
            paint.setTextSize(11);paint.setColor(0xFF1D2939);
            canvas.drawText(referenceMode?("MEAT".equals(operationMode)?"Beef study class similarity":"Fruit similarity / study day"):"Log relative MQ response / time",
                left,14,paint);
            if(referenceMode){
                if("MEAT".equals(operationMode)){
                    if(meatCompleted==null)return;
                    int n=meatCompleted.rankedClasses.size();
                    for(int i=0;i<n;i++){
                        BeefResearchEngine.Match m=meatCompleted.rankedClasses.get(i);
                        float x=left+8+i*(right-left)/Math.max(1,n);
                        float barHeight=(bottom-top)*(float)(m.descriptiveSimilarity/100.0);
                        paint.setColor(COLORS[i%COLORS.length]);
                        canvas.drawRect(x,bottom-barHeight,x+Math.max(12,(right-left)/n-18),
                                        bottom,paint);
                        paint.setColor(0xFF243247);paint.setTextSize(10);
                        canvas.drawText(m.classCode.substring(0,Math.min(6,m.classCode.length())),
                                        x,bottom+15,paint);
                    }
                    return;
                }
                FoodFreshnessEngine.Result r=completed;
                if(r==null||r.selectedFoodTrend.isEmpty())return;
                int maxDay=1;
                for(FoodFreshnessEngine.Match m:r.selectedFoodTrend)
                    maxDay=Math.max(maxDay,m.studyDay);
                float prevX=Float.NaN,prevY=Float.NaN;
                for(FoodFreshnessEngine.Match m:r.selectedFoodTrend){
                    float x=left+(right-left)*Math.max(0,m.studyDay-1)/Math.max(1,maxDay-1);
                    float y=bottom-(bottom-top)*(float)(m.similarityIndex/100.0);
                    paint.setColor(0xFF1462A5);paint.setStrokeWidth(2.5f);
                    if(Float.isFinite(prevX))canvas.drawLine(prevX,prevY,x,y,paint);
                    canvas.drawCircle(x,y,4,paint);
                    paint.setColor(0xFF263445);paint.setTextSize(11);
                    canvas.drawText("D"+m.studyDay,x-9,bottom+16,paint);
                    prevX=x;prevY=y;
                }
            }else{
                int index=0;
                for(String sensor:FoodReferenceCatalog.SENSOR_ORDER){
                    List<PlotPoint> points=signalHistory.get(sensor);
                    if(points==null||points.size()<2)continue;
                    double base=points.get(0).value;if(!(base>0))continue;
                    paint.setColor(COLORS[index]);paint.setStrokeWidth(2f);
                    float oldX=Float.NaN,oldY=Float.NaN;
                    for(PlotPoint p:points){
                        double z=Math.log10(p.value/base);
                        float x=left+(right-left)*(float)Math.min(1,p.seconds/180.0);
                        float y=bottom-(bottom-top)*(float)Math.max(0,Math.min(1,0.5+z/1.2));
                        if(Float.isFinite(oldX))canvas.drawLine(oldX,oldY,x,y,paint);
                        oldX=x;oldY=y;
                    }
                    paint.setTextSize(10);
                    canvas.drawText(sensor,left+index%4*((right-left)/4),bottom+31-14*(index/4),paint);
                    index++;
                }
            }
        }
    }
}
