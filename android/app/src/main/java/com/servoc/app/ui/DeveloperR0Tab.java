package com.servoc.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.calibration.ManualR0Calibration;
import com.servoc.app.calibration.TankAutoR0Math;
import com.servoc.app.chemcore.library.ChemCoreLibraryManager;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** HR069-R1: developer-owned local editable clean-air R0, deliberately separate from ChemCore Stable. */
public final class DeveloperR0Tab {
    private final Context context;
    private final HazRobHost host;
    private final ScrollView scroll;
    private final String[] keys=ManualR0Calibration.keys();
    private final EditText[] entry=new EditText[keys.length];
    private final TextView[] info=new TextView[keys.length];
    private final TextView summary,autoProgress;
    private final Button autoStart,autoCancel;
    private final Handler clock=new Handler(Looper.getMainLooper());
    private TankAutoR0Math autoSampler;
    private boolean automaticActive=false,awaitingSave=false;
    private volatile boolean disposed=false;
    private AlertDialog automaticSaveDialog;
    private long automaticStartMs;
    private final long[] lastCycles=new long[TankAutoR0Math.SENSOR_COUNT];
    private Map<String,Double> proposedAutomatic;
    private final Runnable autoTick=new Runnable(){
        @Override public void run(){
            if(!automaticActive)return;
            long elapsed=Math.max(0,SystemClock.elapsedRealtime()-automaticStartMs);
            captureAutomaticSamples(elapsed);
            if(elapsed>=TankAutoR0Math.TOTAL_MS){completeAutomatic();return;}
            int window=TankAutoR0Math.captureWindow(elapsed);
            String stage;
            if(elapsed<300000L)stage="تسخين أولي";
            else if(window>=0)stage="القياس "+(window+1)+" من 3";
            else stage="مهلة تشغيل مستمر";
            autoProgress.setText("المعايرة التلقائية • "+stage+" • الدقيقة "+
                String.format(Locale.US,"%.1f / 20",elapsed/60000.0));
            clock.postDelayed(this,250);
        }
    };

    public DeveloperR0Tab(Context c,HazRobHost h){
        context=c;host=h;
        ManualR0Calibration.load(h.preferences());
        scroll=new ScrollView(c);scroll.setFillViewport(true);
        LinearLayout root=UiKit.vbox(c);
        root.setPadding(UiKit.dp(c,10),UiKit.dp(c,8),UiKit.dp(c,10),UiKit.dp(c,20));
        root.setBackgroundColor(0xFFF7F8FA);scroll.addView(root);

        root.addView(UiKit.title(c,"معايرة R0 — ثمانية حساسات"),UiKit.full(c));
        TextView instructions=UiKit.small(c,
            "هذه معايرة هواء نظيف خاصة بروبوتك عند اتصال الحساسات؛ يمكن تعديل قيم R0 المحفوظة يدويًا دون إنترنت. وحدة Rs وR0 الحالية هي كيلو أوم (kΩ) وفق افتراض RL = 10 kΩ في الفيرموير، وليست أوم. " +
            "تحقق من قيمة مقاومة الحمل RL الفعلية على كل لوحة MQ قبل اعتماد القيمة؛ 1000 Ω = 1 kΩ. أدخل القيمة بوحدة kΩ بعد ثبات تغذية الحساسات وإحمائها. " +
            "يتيح الوضع التلقائي حساب خط أساس تشغيلي ميداني من قراءات Rs في الهواء النظيف؛ أما الإدخال اليدوي فيبقى خيارًا مستقلًا. R0 وحدها لا تحقق معايرة PPM مرجعية. " +
            "الحفظ معطل أثناء التسجيل/التحليل النشط حتى لا تتغير المعايرة وسط الجلسة. " +
            "حفظ القيم يؤثر في التحليلات الجديدة فقط؛ جلسات القياس المحفوظة لا تُعدّل.");
        instructions.setTextColor(0xFF855300);root.addView(instructions,UiKit.full(c));

        LinearLayout automatic=UiKit.card(c);
        automatic.addView(UiKit.title(c,"المعايرة التلقائية للمستخدم • 20 دقيقة"),UiKit.full(c));
        automatic.addView(UiKit.small(c,
            "5 دقائق تسخين ← 3 دقائق قياس ← 3 دقائق مهلة دون إطفاء ← 3 دقائق قياس ← 3 دقائق مهلة ← 3 دقائق قياس. "+
            "يحسب التطبيق المتوسط الحسابي لقراءات Rs في كل فترة، ثم متوسط الفترات الثلاث لتحديد R0 الميدانية لكل حساس؛ دون تمديد زمني أو اختبار استقرار معقد. "+
            "لا تُحفظ أي قيمة إلا بعد تأكيدك في النافذة النهائية."),UiKit.full(c));
        autoProgress=UiKit.small(c,"جاهز • ضع الروبوت في هواء نظيف ثم ابدأ");
        automatic.addView(autoProgress,UiKit.full(c));
        LinearLayout autoActions=UiKit.hbox(c);
        autoStart=UiKit.button(c,"▶ بدء المعايرة التلقائية");
        autoStart.setOnClickListener(v->startAutomatic());
        autoActions.addView(autoStart,UiKit.weight(2));
        autoCancel=UiKit.button(c,"إلغاء");
        autoCancel.setEnabled(false);autoCancel.setOnClickListener(v->cancelAutomatic());
        autoActions.addView(autoCancel,UiKit.weight(1));
        automatic.addView(autoActions,UiKit.full(c));root.addView(automatic,UiKit.full(c));
        for(int i=0;i<keys.length;i++){
            final int index=i;
            LinearLayout card=UiKit.card(c);
            card.addView(UiKit.title(c,keys[i].toUpperCase(Locale.US)),UiKit.full(c));
            entry[i]=UiKit.input(c,"");
            entry[i].setSingleLine(true);
            entry[i].setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
            card.addView(entry[i],UiKit.full(c));
            info[i]=UiKit.small(c,"");info[i].setTextColor(0xFF4B5563);
            card.addView(info[i],UiKit.full(c));
            LinearLayout actions=UiKit.hbox(c);
            Button save=UiKit.button(c,"حفظ R0");
            save.setOnClickListener(v->saveOne(index));
            actions.addView(save,UiKit.weight(1));
            Button reset=UiKit.button(c,"استعادة الأساس");
            reset.setOnClickListener(v->new AlertDialog.Builder(c)
                .setTitle("استعادة R0 السابقة")
                .setMessage("هل تريد حذف القيمة اليدوية لـ"+keys[index].toUpperCase(Locale.US)+" واستعادة خط أساس الهواء النظيف المعتمد؟")
                .setNegativeButton("إلغاء",null)
                .setPositiveButton("استعادة",(d,w)->resetOne(index)).show());
            actions.addView(reset,UiKit.weight(1));
            card.addView(actions,UiKit.full(c));root.addView(card,UiKit.full(c));
        }
        Button saveAll=UiKit.button(c,"حفظ جميع قيم R0 الثماني");
        saveAll.setOnClickListener(v->saveAll());root.addView(saveAll,UiKit.full(c));
        summary=UiKit.small(c,"");summary.setTextColor(0xFF4B5563);
        root.addView(summary,UiKit.full(c));
        reloadFields();refresh();
    }

    public View view(){return scroll;}

    private double parse(int index){
        try{
            String raw=entry[index].getText().toString().trim().replace(',','.');
            double v=Double.parseDouble(raw);
            return Double.isFinite(v)&&v>0?v:Double.NaN;
        }catch(Exception e){return Double.NaN;}
    }
    private boolean calibrationChangeBlocked(){
        if(automaticActive||awaitingSave){host.showToast("أكمل أو ألغِ المعايرة التلقائية أولاً");return true;}
        if(ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics())){
            host.showToast("لا يمكن تغيير R0 أثناء القياس أو التحليل؛ أنهِ الجلسة أولًا");
            return true;
        }
        return false;
    }
    private void saveOne(int index){
        if(calibrationChangeBlocked())return;
        double value=parse(index);
        if(!Double.isFinite(value)){host.showToast("قيمة R0 غير صالحة — أدخل رقمًا موجبًا");return;}
        if(!ManualR0Calibration.save(host.preferences(),keys[index],value)){
            host.showToast("تعذّر حفظ R0 في الهاتف");return;
        }
        reloadFields();host.refreshAllTabs();
        host.showToast("تم تحديث R0 لـ "+keys[index].toUpperCase(Locale.US)+" — يسري على القياسات الجديدة");
    }
    private void saveAll(){
        if(calibrationChangeBlocked())return;
        Map<String,Double> values=new LinkedHashMap<>();
        for(int i=0;i<keys.length;i++){
            double v=parse(i);
            if(!Double.isFinite(v)){host.showToast("قيمة R0 غير صالحة للحساس "+keys[i]);entry[i].requestFocus();return;}
            values.put(keys[i],v);
        }
        if(!ManualR0Calibration.saveAll(host.preferences(),values)){
            host.showToast("فشل الحفظ — لم تتغير أي قيمة من R0");return;
        }
        reloadFields();host.refreshAllTabs();
        host.showToast("تم حفظ قيم R0 الثماني مع سجل مصدر وتوقيت معايرة واحد");
    }
    private void resetOne(int index){
        if(calibrationChangeBlocked())return;
        if(!ManualR0Calibration.reset(host.preferences(),keys[index])){
            host.showToast("تعذّر استعادة خط أساس R0");return;
        }
        reloadFields();host.refreshAllTabs();
        host.showToast("استعيد خط أساس الهواء النظيف لـ "+keys[index].toUpperCase(Locale.US));
    }
    private void reloadFields(){
        for(int i=0;i<keys.length;i++){
            double v=ManualR0Calibration.get(keys[i]);
            entry[i].setText(Double.isFinite(v)?String.format(Locale.US,"%.10f",v).replaceAll("0+$","").replaceAll("\\.$",""):"");
        }
        refresh();
    }
    public void refresh(){
        JSONObject d=host.latestDiagnostics();
        JSONObject mq=d==null?null:d.optJSONObject("mq");
        for(int i=0;i<keys.length;i++){
            String k=keys[i];JSONObject sample=mq==null?null:mq.optJSONObject(k);
            double rs=sample==null?Double.NaN:sample.optDouble("rs",Double.NaN);
            String rsLabel=Double.isFinite(rs)&&rs>0?String.format(Locale.US,"%.5f kΩ",rs):"غير متاح";
            info[i].setText("Rs اللحظية: "+rsLabel+" • المصدر: "+ManualR0Calibration.getSource(k)+
                "\nآخر حفظ: "+ManualR0Calibration.getUtc(k)+" • جلسة: "+ManualR0Calibration.getSession(k));
        }
        summary.setText("R0 الحالية محفوظة على الهاتف ضمن إعدادات الجهاز ولا تُستبدل بواسطة ChemCore Stable. " +
            "الإدخال اليدوي لا يغيّر تقارير جلسات قديمة، ولا يثبت معايرة الغاز المرجعي.");
    }

    private void startAutomatic(){
        if(disposed||automaticActive||awaitingSave||calibrationChangeBlocked())return;
        JSONObject current=host.latestDiagnostics();
        if(current==null||current.optJSONObject("mq")==null){
            host.showToast("اتصل بروبوت Tank قبل بدء معايرة R0");return;
        }
        autoStart.setEnabled(false);
        autoProgress.setText("جارٍ تشغيل تغذية الحساسات الثمانية...");
        host.io().execute(()->{
            try{
                host.robotApi().mqPower(true);
                scroll.post(()->{
                    if(disposed)return; // Do not restart acquisition after Activity destruction.
                    autoSampler=new TankAutoR0Math();
                    java.util.Arrays.fill(lastCycles,-1L);
                    automaticStartMs=SystemClock.elapsedRealtime();
                    automaticActive=true;
                    scroll.setKeepScreenOn(true); // Keep operator-visible 20-minute acquisition active.
                    autoCancel.setEnabled(true);
                    autoProgress.setText("بدأ التسخين الأولي • حساسات MQ قيد التشغيل");
                    clock.post(autoTick);
                });
            }catch(Exception ex){
                scroll.post(()->{if(disposed)return;autoStart.setEnabled(true);autoProgress.setText("تعذر تشغيل حساسات MQ");});
            }
        });
    }
    private void captureAutomaticSamples(long elapsed){
        int window=TankAutoR0Math.captureWindow(elapsed);
        if(window<0||autoSampler==null)return;
        JSONObject d=host.latestDiagnostics(),mq=d==null?null:d.optJSONObject("mq");
        if(mq==null)return;
        for(int i=0;i<TankAutoR0Math.SENSOR_COUNT;i++){
            JSONObject sample=mq.optJSONObject(TankAutoR0Math.sensorKey(i));
            if(sample==null||!sample.optBoolean("ok",sample.optBoolean("valid",false)))continue;
            double rs=sample.optDouble("rs",Double.NaN);
            if(!Double.isFinite(rs)||rs<=0)continue;
            // ESP32-Tank emits a unique scan_cycle per 8-channel ADC scan.
            // Count each real sensor sample once; never turn a stale UI snapshot
            // into hundreds of artificial samples during a 3-minute capture.
            long cycle=sample.optLong("scan_cycle",-1L);
            if(cycle<=0||cycle<=lastCycles[i])continue;
            lastCycles[i]=cycle;
            autoSampler.accept(i,window,rs);
        }
    }
    private void cancelAutomatic(){
        automaticActive=false;awaitingSave=false;
        scroll.setKeepScreenOn(false);
        proposedAutomatic=null;clock.removeCallbacks(autoTick);
        autoStart.setEnabled(true);autoCancel.setEnabled(false);
        autoProgress.setText("أُلغيت المعايرة • لم تتغير أي قيمة R0");
    }
    private void completeAutomatic(){
        automaticActive=false;clock.removeCallbacks(autoTick);
        scroll.setKeepScreenOn(false);
        autoCancel.setEnabled(false);autoStart.setEnabled(true);
        proposedAutomatic=autoSampler==null?null:autoSampler.validProposals();
        if(proposedAutomatic==null||proposedAutomatic.isEmpty()){
            autoProgress.setText("انتهى الوقت • لم تتوفر قراءات صالحة لحساب R0");
            host.showToast("لم يتم حفظ أي قيمة R0");
            return;
        }
        awaitingSave=true;
        StringBuilder content=new StringBuilder("انتهت المعايرة التلقائية (20 دقيقة). جميع القيم بالكيلو أوم kΩ.\n\n");
        for(String key:keys){
            double oldValue=ManualR0Calibration.get(key);
            Double proposed=proposedAutomatic.get(key);
            content.append(key.toUpperCase(Locale.US)).append("  ");
            content.append(Double.isFinite(oldValue)?String.format(Locale.US,"%.3f",oldValue):"—");
            content.append(" ← ");
            content.append(proposed==null?"غير متاحة (تبقى السابقة)":String.format(Locale.US,"%.3f",proposed));
            content.append("\n");
        }
        content.append("\nسيعتمد التطبيق وChemCore القيم الجديدة في التحليلات القادمة فقط.");
        autoProgress.setText("اكتملت معايرة "+proposedAutomatic.size()+" حساسات • بانتظار موافقة الحفظ");
        automaticSaveDialog=new AlertDialog.Builder(context)
            .setTitle("حفظ واعتماد قيم R0")
            .setMessage(content.toString())
            .setNegativeButton("إلغاء",(dialog,which)->{
                awaitingSave=false;proposedAutomatic=null;
                autoProgress.setText("لم تُحفظ نتائج الدورة • قيم R0 السابقة باقية");
            })
            .setPositiveButton("حفظ واعتماد",(dialog,which)->saveAutomatic())
            .setCancelable(false)
            .show();
    }
    /** Cancel orphaned timers and dialogs before the host Activity is destroyed. */
    public void release(){
        disposed=true;
        automaticActive=false;
        awaitingSave=false;
        proposedAutomatic=null;
        clock.removeCallbacks(autoTick);
        scroll.setKeepScreenOn(false);
        if(automaticSaveDialog!=null){
            automaticSaveDialog.dismiss();
            automaticSaveDialog=null;
        }
    }

    private void saveAutomatic(){
        if(!awaitingSave||proposedAutomatic==null)return;
        Map<String,Double> valuesToSave=proposedAutomatic;
        boolean ok=ManualR0Calibration.saveAutomatic(host.preferences(),valuesToSave);
        awaitingSave=false;proposedAutomatic=null;
        if(!ok){autoProgress.setText("تعذر حفظ R0 • القيم السابقة باقية");host.showToast("فشل حفظ نتائج المعايرة");return;}
        autoProgress.setText("تم اعتماد "+valuesToSave.size()+" قيم R0 ميدانية • تسري على القياسات الجديدة");
        reloadFields();host.refreshAllTabs();
        host.showToast("تم تحديث R0 في التطبيق وChemCore للقياسات القادمة");
    }

}
