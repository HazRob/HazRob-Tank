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
import com.servoc.app.data.MqDeviceR0LedgerV14;
import com.servoc.app.chemcore.library.ChemCoreLibraryManager;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** V5: user-owned R0 in Settings, deliberately separate from ChemCore Stable. */
public final class DeveloperR0Tab {
    private final Context context;
    private final HazRobHost host;
    private final ScrollView scroll;
    private final String[] keys=ManualR0Calibration.keys();
    private final EditText[] entry=new EditText[keys.length];
    private final TextView[] info=new TextView[keys.length];
    private final TextView summary,autoProgress;
    private final Button autoStart,autoCancel,autoApprove,autoRetry;
    private JSONObject pendingFirmwareResult;
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
            "فيرموير V5 يحسب قيم R0 على ESP32 قبل الاعتماد: تشغيل حساسات MQ الثمانية، "+
            "تسخين 300 ثانية ثم جمع جميع الدورات الصالحة خلال 60 ثانية، "+
            "بحد أدنى 10 قراءات لكل حساس. النتيجة مبدئيًا في RAM ولا تُحفظ في NVS إلا بعد زر الاعتماد. "+
            "يمكن إلغاء نتيجة RAM دون تغيير المعايرة السابقة، واستيراد القيم المعتمدة بعد إعادة فتح التطبيق. "+
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
        autoApprove=UiKit.button(c,"اعتماد R0 في التطبيق");
        autoApprove.setEnabled(false);autoApprove.setOnClickListener(v->acceptFirmwareResult());
        autoActions.addView(autoApprove,UiKit.weight(1.3f));
        autoRetry=UiKit.button(c,"إعادة المعايرة");
        autoRetry.setEnabled(false);autoRetry.setOnClickListener(v->retryAutomatic());
        autoActions.addView(autoRetry,UiKit.weight(1f));
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
            Button reset=UiKit.button(c,"مسح R0 المحلية");
            reset.setOnClickListener(v->new AlertDialog.Builder(c)
                .setTitle("مسح R0 المحلية")
                .setMessage("سيتم مسح R0 للهاتف للحساس "+keys[index].toUpperCase(Locale.US)+
                    " ولن تُستعاد قيمة افتراضية. تبقى معايرة ESP32 محفوظة؛ استخدم «استيراد آخر R0» لاستعادتها.")
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
        if(automaticActive||pendingFirmwareResult!=null){host.showToast("اعتمد أو أعد أو ألغِ نتيجة R0 المعلقة أولاً");return true;}
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
        Runnable persist=()->{
            if(calibrationChangeBlocked())return;
            JSONObject diag=host.latestDiagnostics();
            String controller=diag==null?"":diag.optString("device_id","");
            if(!ManualR0Calibration.saveForDevice(host.preferences(),keys[index],value,controller)){
                host.showToast("تعذّر حفظ R0 في الهاتف");return;
            }
            reloadFields();host.refreshAllTabs();
            host.showToast("تم تحديث R0 لـ "+keys[index].toUpperCase(Locale.US)+"؛ "+(controller.isEmpty()?"لا توجد هوية جهاز متصل لتوثيقها":"مرتبطة بالروبوت "+controller));
        };
        if("firmware_clean_air_r0".equals(ManualR0Calibration.getSource(keys[index]))){
            new AlertDialog.Builder(context)
                .setTitle("تجاوز معايرة ESP32 يدويًا")
                .setMessage("ستتجاوز القيمة اليدوية معايرة R0 المستوردة من ESP32 لهذا الحساس على الهاتف فقط. "+
                    "لا تتغير معايرة ESP32 ولا تصبح قيمة R0 شهادة PPM. هل تؤكد؟")
                .setNegativeButton("إلغاء",null)
                .setPositiveButton("تأكيد التجاوز",(d,w)->persist.run()).show();
        }else persist.run();
    }
    private void saveAll(){
        if(calibrationChangeBlocked())return;
        Map<String,Double> values=new LinkedHashMap<>();
        for(int i=0;i<keys.length;i++){
            double v=parse(i);
            if(!Double.isFinite(v)){host.showToast("قيمة R0 غير صالحة للحساس "+keys[i]);entry[i].requestFocus();return;}
            values.put(keys[i],v);
        }
        Runnable persist=()->{
            if(calibrationChangeBlocked())return;
            JSONObject diag=host.latestDiagnostics();
            String controller=diag==null?"":diag.optString("device_id","");
            if(!ManualR0Calibration.saveAllForDevice(host.preferences(),values,controller)){
                host.showToast("فشل الحفظ — لم تتغير أي قيمة من R0");return;
            }
            reloadFields();host.refreshAllTabs();
            host.showToast("تم حفظ ثماني قيم R0؛ "+(controller.isEmpty()?"غير مرتبطة بهوية جهاز متصل":"مرتبطة بالروبوت "+controller));
        };
        boolean firmwarePresent=false;
        for(String key:keys)if("firmware_clean_air_r0".equals(ManualR0Calibration.getSource(key))){
            firmwarePresent=true;break;
        }
        if(firmwarePresent)new AlertDialog.Builder(context)
            .setTitle("تجاوز معايرة ESP32 لجميع الحساسات")
            .setMessage("ستحل ثماني قيم يدوية محل معايرة ESP32 المستوردة إلى الهاتف فقط، "+
                "مع حفظ مصدرها وتوقيتها. لا تتغير معايرة الروبوت الفعلية. هل تؤكد؟")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("تأكيد التجاوز",(d,w)->persist.run()).show();
        else persist.run();
    }
    private void resetOne(int index){
        if(calibrationChangeBlocked())return;
        if(!ManualR0Calibration.reset(host.preferences(),keys[index])){
            host.showToast("تعذّر استعادة خط أساس R0");return;
        }
        reloadFields();host.refreshAllTabs();
        host.showToast("مُسحت R0 للهاتف للحساس "+keys[index].toUpperCase(Locale.US)+"؛ لم تُستعد أي قيمة افتراضية");
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
                "\nآخر حفظ: "+ManualR0Calibration.getUtc(k)+" • جلسة: "+ManualR0Calibration.getSession(k)+
                " • عينات R0 الموثقة: "+ManualR0Calibration.getSamples(k));
        }
        summary.setText("R0 الحالية محفوظة على الهاتف ضمن إعدادات الجهاز ولا تُستبدل بواسطة ChemCore Stable. " +
            "الإدخال اليدوي لا يغيّر تقارير جلسات قديمة، ولا يثبت معايرة الغاز المرجعي.");
    }

    private void startAutomatic(){
        if(disposed||automaticActive||pendingFirmwareResult!=null||calibrationChangeBlocked())return;
        // Lock manual edits immediately, BEFORE any asynchronous /relay or /r0 request.
        // Otherwise a manual R0 save can race the ESP32 warm-up/commit.
        automaticActive=true;
        autoStart.setEnabled(false);autoApprove.setEnabled(false);
        autoRetry.setEnabled(false);autoCancel.setEnabled(true);
        autoProgress.setText("جارٍ بدء معايرة ESP32...");
        host.io().execute(()->{
            boolean startRequested=false;
            try{
                host.robotApi().mqPower(true);
                startRequested=true;
                JSONObject started=host.robotApi().calibrationStart(300,60);
                FirmwareR0Contract.Progress p=FirmwareR0Contract.progress(started);
                if(!p.active())throw new IllegalStateException("Firmware calibration did not start");
                scroll.post(()->{
                    if(disposed||!automaticActive)return;
                    scroll.setKeepScreenOn(true);displayCalibration(p,started);
                });
            }catch(Exception ex){
                final boolean uncertain=startRequested&&
                        !(ex instanceof UnsupportedOperationException);
                scroll.post(()->{
                    if(disposed||!automaticActive)return;
                    if(uncertain){
                        autoProgress.setText("لم يتأكد بدء المعايرة؛ جارٍ الاستعلام من ESP32 دون "+
                                "السماح بتعديل R0 حتى تتضح الحالة: "+ex.getClass().getSimpleName());
                        clock.post(autoTick);
                    }else finishAutomatic("تعذر بدء المعايرة: "+ex.getMessage());
                });
            }
        });
    }

    /** Explicit recovery action after Activity destruction, reconnect or phone-side save failure. */
    private void syncFirmwareCalibration(){
        if(disposed||automaticActive||pendingFirmwareResult!=null||calibrationChangeBlocked())return;
        Runnable importNow=()->{
            if(disposed||automaticActive||calibrationChangeBlocked())return;
            automaticActive=true;autoStart.setEnabled(false);autoCancel.setEnabled(true);
            autoProgress.setText("جارٍ قراءة حالة R0 من ESP32...");
            clock.post(autoTick);
        };
        boolean override=false;
        for(String key:keys)if("manual_device_r0".equals(ManualR0Calibration.getSource(key))){
            override=true;break;
        }
        if(override)new AlertDialog.Builder(context)
            .setTitle("استبدال قيم R0 اليدوية")
            .setMessage("استيراد معايرة ESP32 سيستبدل القيم اليدوية المحفوظة في الهاتف بالقيم "+
                "الموثقة على الروبوت. لا تتغير جلسات القياس القديمة. هل تريد المتابعة؟")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("استيراد",(d,w)->importNow.run()).show();
        else importNow.run();
    }

    private void displayCalibration(FirmwareR0Contract.Progress p,JSONObject response){
        if(disposed||!automaticActive)return;
        if("pending".equals(p.state)||"ready".equals(p.state)){
            try{
                boolean staged="pending".equals(p.state);
                if(staged)FirmwareR0Contract.pendingValues(response);
                else FirmwareR0Contract.committedValues(response);
                String deviceId=response.optString("device_id","").trim();
                if(deviceId.isEmpty())throw new IllegalStateException("Missing ESP32 device identity");
                pendingFirmwareResult=new JSONObject(response.toString());
                automaticActive=false;clock.removeCallbacks(autoTick);
                scroll.setKeepScreenOn(false);
                autoStart.setEnabled(false);autoApprove.setEnabled(true);
                autoRetry.setEnabled(true);autoCancel.setEnabled(true);
                autoProgress.setText(staged?
                    "اكتملت معايرة 8 حساسات • القيم معلقة في RAM فقط ولم تتغير NVS. "+
                    "اعتماد يحفظ على ESP32 والهاتف؛ إلغاء يترك R0 السابقة دون تغيير.":
                    "آخر R0 محفوظة ومتحقق منها على ESP32. يمكن استيرادها للهاتف دون إعادة "+
                    "كتابة NVS؛ إلغاء الاستيراد لا يغير معايرة الروبوت.");
            }catch(Exception ex){
                pendingFirmwareResult=null;
                finishAutomatic("لا تتوافر ثماني قيم صالحة للمراجعة: "+ex.getMessage());
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
        autoProgress.setText(stage+" • المتبقي "+Math.max(0,(total-p.elapsedMs+999)/1000)+" ثانية من "+(total/1000)+"\n"+
            "القراءات الصالحة: "+counts);
        clock.postDelayed(autoTick,1000);
    }

    /** Commit staged R0 to ESP32 NVS only on explicit user approval. */
    private void acceptFirmwareResult(){
        if(disposed||automaticActive||pendingFirmwareResult==null)return;
        if(ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics())){
            host.showToast("لا يمكن تغيير R0 أثناء قياس أو تحليل نشط");return;
        }
        final JSONObject proposed=pendingFirmwareResult;
        final String expected=proposed.optString("device_id","").trim();
        if(expected.isEmpty()){autoProgress.setText("تعذر اعتماد معايرة بدون هوية ESP32");return;}
        automaticActive=true;autoApprove.setEnabled(false);autoRetry.setEnabled(false);autoCancel.setEnabled(false);
        autoProgress.setText("جارٍ حفظ R0 على ESP32 ثم قراءة NVS للتحقق...");
        host.io().execute(()->{
            try{
                FirmwareR0Contract.Progress proposal=FirmwareR0Contract.progress(proposed);
                Map<String,Double> requested="pending".equals(proposal.state)?
                    FirmwareR0Contract.pendingValues(proposed):
                    FirmwareR0Contract.committedValues(proposed);
                JSONObject committed="pending".equals(proposal.state)?
                    host.robotApi().calibrationAccept():host.robotApi().calibrationStatus();
                // The V5 firmware only reports COMMITTED after NVS read-back.
                FirmwareR0Contract.Progress actualProgress=FirmwareR0Contract.progress(committed);
                Map<String,Double> actual=FirmwareR0Contract.committedValues(committed);
                String deviceId=committed.optString("device_id","").trim();
                if(!expected.equals(deviceId))
                    throw new IllegalStateException("ESP32 identity changed during acceptance");
                for(String sensor:keys)
                    if(Math.abs(requested.get(sensor)-actual.get(sensor))>
                        Math.max(0.00001,requested.get(sensor)*0.00001))
                        throw new IllegalStateException("NVS R0 readback mismatch: "+sensor);
                if(!ManualR0Calibration.saveFirmware(host.preferences(),actual,deviceId,actualProgress.counts))
                    throw new IllegalStateException("Cannot durably save phone R0");
                // The phone/NVS commit is complete. The V14 scientific ledger
                // records this exact approved 8-channel state, but is NOT
                // certification of clean-air purity, extended 3-window
                // calibration or G3 gas ppm. Never relabel an already saved
                // firmware result as a total failure if the ledger fails.
                final String scientificLedgerStatus;
                try{
                    int saved=MqDeviceR0LedgerV14.recordFirmwareApproved(
                        host.database(),deviceId,actual,actualProgress.counts);
                    scientificLedgerStatus=" • مرجع V14: "+saved+" سجلات R0 تشغيلية";
                }catch(Exception ledgerError){
                    android.util.Log.e("TankR0V14","Device R0 ledger sync failed",ledgerError);
                    scientificLedgerStatus=" • تعذر توثيق R0 في سجل V14 (مع بقائها محفوظة على الهاتف وESP32)";
                }
                scroll.post(()->{
                    if(disposed)return;
                    pendingFirmwareResult=null;automaticActive=false;
                    autoApprove.setEnabled(false);autoRetry.setEnabled(false);autoCancel.setEnabled(false);
                    finishAutomatic("تم اعتماد 8 قيم R0 على ESP32 وNVS والهاتف • "+deviceId+
                        scientificLedgerStatus);
                    reloadFields();host.refreshAllTabs();
                });
            }catch(Exception ex){
                scroll.post(()->{
                    if(disposed)return;
                    automaticActive=false;
                    autoApprove.setEnabled(true);autoRetry.setEnabled(true);autoCancel.setEnabled(true);
                    autoProgress.setText("تعذر إتمام اعتماد R0: "+ex.getMessage()+
                        " • تحقق من حالة ESP32 قبل إعادة المحاولة.");
                });
            }
        });
    }

    private void retryAutomatic(){
        if(disposed||automaticActive||pendingFirmwareResult==null)return;
        // V5 firmware start replaces pending RAM candidate; does not change NVS.
        pendingFirmwareResult=null;
        autoApprove.setEnabled(false);autoRetry.setEnabled(false);autoCancel.setEnabled(false);
        autoStart.setEnabled(true);
        startAutomatic();
    }

    private void finishAutomatic(String message){
        automaticActive=false;
        clock.removeCallbacks(autoTick);
        scroll.setKeepScreenOn(false);
        autoStart.setEnabled(true);autoCancel.setEnabled(false);
        autoProgress.setText(message);
    }

    private void cancelAutomatic(){
        if(disposed)return;
        final JSONObject candidate=pendingFirmwareResult;
        if(candidate!=null){
            if(automaticActive)return;
            if(!"pending".equals(candidate.optString("state",""))){
                pendingFirmwareResult=null;
                autoApprove.setEnabled(false);autoRetry.setEnabled(false);
                finishAutomatic("أُلغي استيراد الهاتف؛ NVS المعتمدة مسبقاً لم تتغير.");
                return;
            }
            automaticActive=true;
        }else if(!automaticActive)return;
        autoCancel.setEnabled(false);
        autoProgress.setText("جارٍ إرسال إلغاء R0 إلى ESP32 والتحقق...");
        host.io().execute(()->{
            try{
                JSONObject response=host.robotApi().calibrationCancel();
                FirmwareR0Contract.Progress p=FirmwareR0Contract.progress(response);
                if(!"cancelled".equals(p.state)||p.pending||p.committed)
                    throw new IllegalStateException("ESP32 did not acknowledge cancellation");
                scroll.post(()->{
                    if(disposed)return;
                    pendingFirmwareResult=null;
                    autoApprove.setEnabled(false);autoRetry.setEnabled(false);
                    finishAutomatic("ألغيت القيم المعلقة في ESP32؛ لم يتغير سجل R0 السابق في NVS.");
                });
            }catch(Exception ex){
                scroll.post(()->{
                    if(disposed)return;
                    if(candidate!=null)automaticActive=false;
                    autoProgress.setText("لم يتأكد إلغاء R0: "+ex.getMessage());
                    autoCancel.setEnabled(true);
                    if(candidate==null)clock.postDelayed(autoTick,1500);
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
