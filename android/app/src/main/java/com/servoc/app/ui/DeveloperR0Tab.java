package com.servoc.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.calibration.ManualR0Calibration;
import com.servoc.app.calibration.FirmwareR0Contract;
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
    private boolean automaticActive=false;
    private volatile boolean disposed=false;
    private final Runnable autoTick=new Runnable(){
        @Override public void run(){
            if(!automaticActive||disposed)return;
            host.io().execute(()->{
                try{
                    JSONObject response=host.robotApi().calibrationStatus();
                    FirmwareR0Contract.Progress progress=FirmwareR0Contract.progress(response);
                    scroll.post(()->displayCalibration(progress,response));
                }catch(Exception ex){
                    scroll.post(()->{
                        if(disposed||!automaticActive)return;
                        autoProgress.setText("تعذر تحديث معايرة الروبوت؛ إعادة المحاولة بعد ثانيتين. "+
                            ex.getClass().getSimpleName());
                        clock.postDelayed(autoTick,2000);
                    });
                }
            });
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
        automatic.addView(UiKit.title(c,"معايرة R0 في ESP32 • تسخين 300 ثانية ثم قياس 60 ثانية"),UiKit.full(c));
        automatic.addView(UiKit.small(c,
            "ينفذ فيرموير V4 المعايرة على ESP32 نفسه: تشغيل حساسات MQ الثمانية، "+
            "تسخين 300 ثانية ثم جمع جميع الدورات الصالحة خلال 60 ثانية، "+
            "بحد أدنى 10 قراءات لكل حساس. لا تُنقل قيم R0 إلى التطبيق إلا بعد حفظها "+
            "والتحقق منها في ذاكرة الروبوت. استرجاع القيم متاح بعد إعادة فتح التطبيق. "+
            "يجب استخدام هواء نظيف فعلاً؛ لا توفر هذه المعايرة شهادة دقة PPM."),UiKit.full(c));
        autoProgress=UiKit.small(c,"جاهز • ضع الروبوت في هواء نظيف ثم ابدأ");
        automatic.addView(autoProgress,UiKit.full(c));
        LinearLayout autoActions=UiKit.hbox(c);
        autoStart=UiKit.button(c,"▶ بدء المعايرة التلقائية");
        autoStart.setOnClickListener(v->startAutomatic());
        autoActions.addView(autoStart,UiKit.weight(2));
        Button sync=UiKit.button(c,"استيراد آخر R0 محفوظة");
        sync.setOnClickListener(v->syncFirmwareCalibration());
        autoActions.addView(sync,UiKit.weight(1.4f));
        autoCancel=UiKit.button(c,"إلغاء");
        autoCancel.setEnabled(false);autoCancel.setOnClickListener(v->cancelAutomatic());
        autoActions.addView(autoCancel,UiKit.weight(.8f));
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
        if(automaticActive){host.showToast("أكمل أو ألغِ المعايرة التلقائية أولاً");return true;}
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
        if(disposed||automaticActive||calibrationChangeBlocked())return;
        autoStart.setEnabled(false);
        autoProgress.setText("جارٍ بدء معايرة ESP32...");
        host.io().execute(()->{
            try{
                host.robotApi().mqPower(true);
                JSONObject started=host.robotApi().calibrationStart(300,60);
                FirmwareR0Contract.Progress p=FirmwareR0Contract.progress(started);
                if(!p.active())throw new IllegalStateException("Firmware calibration did not start");
                scroll.post(()->{
                    if(disposed)return;
                    automaticActive=true;autoCancel.setEnabled(true);
                    scroll.setKeepScreenOn(true);displayCalibration(p,started);
                });
            }catch(Exception ex){
                scroll.post(()->{
                    if(disposed)return;
                    autoStart.setEnabled(true);autoCancel.setEnabled(false);
                    autoProgress.setText("تعذر بدء المعايرة: "+ex.getMessage());
                });
            }
        });
    }

    /** Explicit recovery action after Activity destruction, reconnect or phone-side save failure. */
    private void syncFirmwareCalibration(){
        if(disposed||automaticActive||calibrationChangeBlocked())return;
        automaticActive=true;autoStart.setEnabled(false);autoCancel.setEnabled(true);
        autoProgress.setText("جارٍ قراءة حالة R0 من ESP32...");
        clock.post(autoTick);
    }

    private void displayCalibration(FirmwareR0Contract.Progress p,JSONObject response){
        if(disposed||!automaticActive)return;
        if("ready".equals(p.state)){
            try{
                Map<String,Double> values=FirmwareR0Contract.committedValues(response);
                String deviceId=response.optString("device_id","");
                if(deviceId.isEmpty())throw new IllegalStateException("Missing ESP32 device identity");
                if(!ManualR0Calibration.saveFirmware(host.preferences(),values,deviceId))
                    throw new IllegalStateException("Phone could not durably save firmware R0");
                finishAutomatic("تمت مزامنة 8 قيم R0 المعتمدة من ESP32 • الجهاز: "+deviceId);
                reloadFields();host.refreshAllTabs();
            }catch(Exception ex){
                finishAutomatic("حُفظت R0 على الروبوت، لكن لم تُعتمد على الهاتف: "+
                    ex.getMessage()+" • استخدم زر استيراد آخر R0 محفوظة لإعادة المحاولة");
            }
            return;
        }
        if("error".equals(p.state)||"cancelled".equals(p.state)||"idle".equals(p.state)){
            finishAutomatic("حالة المعايرة: "+p.state+
                (p.error.isEmpty()?"":" • "+p.error)+" • لم تتغير R0 المحفوظة في الهاتف");
            return;
        }
        StringBuilder counts=new StringBuilder();
        for(String key:keys){
            if(counts.length()>0)counts.append("  ");
            counts.append(key.toUpperCase(Locale.US)).append(": ").append(p.counts.get(key));
        }
        String stage="warmup".equals(p.state)?"تسخين":"جمع قراءات";
        long total="warmup".equals(p.state)?p.warmupMs:p.durationMs;
        autoProgress.setText(stage+" • "+(p.elapsedMs/1000)+" / "+(total/1000)+" ثانية\n"+
            "القراءات الصالحة: "+counts);
        clock.postDelayed(autoTick,1000);
    }

    private void finishAutomatic(String message){
        automaticActive=false;
        clock.removeCallbacks(autoTick);
        scroll.setKeepScreenOn(false);
        autoStart.setEnabled(true);autoCancel.setEnabled(false);
        autoProgress.setText(message);
    }

    private void cancelAutomatic(){
        if(!automaticActive||disposed)return;
        autoCancel.setEnabled(false);
        autoProgress.setText("جارٍ إرسال إلغاء المعايرة إلى ESP32...");
        host.io().execute(()->{
            try{
                JSONObject result=host.robotApi().calibrationCancel();
                FirmwareR0Contract.Progress p=FirmwareR0Contract.progress(result);
                scroll.post(()->{
                    if(disposed)return;
                    finishAutomatic("أُرسِل أمر الإلغاء؛ حالة الروبوت: "+p.state);
                });
            }catch(Exception ex){
                scroll.post(()->{
                    if(disposed)return;
                    autoProgress.setText("لم يتأكد إلغاء المعايرة: "+ex.getMessage());
                    autoCancel.setEnabled(true);clock.postDelayed(autoTick,1500);
                });
            }
        });
    }

    /** Stop screen polling without issuing a false cancellation to ESP32. */
    public void release(){
        disposed=true;automaticActive=false;
        clock.removeCallbacks(autoTick);
        scroll.setKeepScreenOn(false);
    }

}
