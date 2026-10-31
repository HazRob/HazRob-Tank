package com.servoc.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.calibration.FirmwareR0Contract;
import com.servoc.app.calibration.ManualR0Calibration;
import com.servoc.app.chemcore.library.ChemCoreLibraryManager;
import com.servoc.app.data.MqDeviceR0LedgerV14;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * V8 clean-room operator R0 UI. No DeveloperTab ownership, no duplicate calibration
 * state, no firmware write on page opening or navigation.
 * ESP32 V6 owns acquisition/NVS; approval is explicit and verified by read-back.
 */
public final class OperatorR0CalibrationScreen {
    private enum Phase { IDLE, BUSY, ACTIVE, REVIEW, ERROR, PAUSED }
    private final Context context;
    private final HazRobHost host;
    private final ScrollView scroll;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final String[] keys=ManualR0Calibration.keys();
    private final EditText[] manual;
    private final TextView[] sensorInfo;
    private final Button start,readSaved,approve,retry,cancel;
    private final TextView state,progress,reviewInfo;
    private Phase phase=Phase.IDLE;
    private boolean disposed=false,visible=false;
    private int generation=0,transientFailures=0;
    private JSONObject staged;
    private String stagedDevice="";
    private final Runnable poll=new Runnable(){
        @Override public void run(){readDeviceStatus(true);}
    };

    public OperatorR0CalibrationScreen(Context context,HazRobHost host){
        this.context=context;this.host=host;
        manual=new EditText[keys.length];sensorInfo=new TextView[keys.length];
        scroll=new ScrollView(context);scroll.setFillViewport(true);
        LinearLayout root=UiKit.vbox(context);
        root.setBackgroundColor(0xFFF7F8FA);
        root.setPadding(UiKit.dp(context,10),UiKit.dp(context,8),
            UiKit.dp(context,10),UiKit.dp(context,20));
        scroll.addView(root);

        root.addView(UiKit.title(context,"معايرة R0 التشغيلية — 8 حساسات"),UiKit.full(context));
        TextView warning=UiKit.small(context,
            "المعايرة في هواء نظيف: 180 ثانية تسخين + 60 ثانية تسجيل Rs. " +
            "لا تُحسب R0 من منحنيات المصنع، ولا تثبت تركيز الغاز أو نقاء الهواء. " +
            "قيم الهاتف المحفوظة لا تتغير قبل اعتماد نتائج ESP32 والتحقق من حفظها.");
        root.addView(warning,UiKit.full(context));

        LinearLayout auto=UiKit.card(context);
        auto.addView(UiKit.title(context,"المعايرة التلقائية على ESP32"),UiKit.full(context));
        state=UiKit.text(context,"الحالة: غير مستعلم عنها");
        progress=UiKit.text(context,"لم تبدأ المعايرة");
        reviewInfo=UiKit.small(context,"");
        state.setTextColor(0xFF17324A);
        progress.setTextColor(0xFF46515E);
        reviewInfo.setTextColor(0xFF17324A);
        auto.addView(state,UiKit.full(context));
        auto.addView(progress,UiKit.full(context));
        auto.addView(reviewInfo,UiKit.full(context));
        start=UiKit.button(context,"▶ بدء معايرة جديدة: 3 دقائق + دقيقة");
        start.setOnClickListener(v->requestStart());
        auto.addView(start,UiKit.full(context));
        readSaved=UiKit.button(context,"قراءة حالة R0 واستيراد المعايرة المحفوظة");
        UiKit.secondary(readSaved);
        readSaved.setOnClickListener(v->readDeviceStatus(false));
        auto.addView(readSaved,UiKit.full(context));
        approve=UiKit.button(context,"✓ اعتماد القيم بعد مراجعتها");
        approve.setOnClickListener(v->requestApproval());
        auto.addView(approve,UiKit.full(context));
        LinearLayout row=UiKit.hbox(context);
        retry=UiKit.button(context,"إعادة المحاولة / استئناف");
        UiKit.secondary(retry);
        retry.setOnClickListener(v->readDeviceStatus(false));
        row.addView(retry,UiKit.weight(1));
        cancel=UiKit.button(context,"إلغاء المعايرة أو المراجعة");
        UiKit.danger(cancel);
        cancel.setOnClickListener(v->requestCancel());
        row.addView(cancel,UiKit.weight(1));
        auto.addView(row,UiKit.full(context));
        root.addView(auto,UiKit.full(context));

        LinearLayout manualGroup=UiKit.card(context);
        manualGroup.addView(UiKit.title(context,"القيم المحلية لكل حساس (kΩ)"),UiKit.full(context));
        manualGroup.addView(UiKit.small(context,
            "الحفظ اليدوي مستقل عن معايرة ESP32 ولا يعدِّل NVS. " +
            "لا تدخل قيمة افتراضية أو من منحنى المصنع باعتبارها معايرة هواء نظيف. " +
            "لا تُعدل الجلسات السابقة."),UiKit.full(context));
        for(int i=0;i<keys.length;i++){
            final int index=i;
            LinearLayout card=UiKit.card(context);
            card.addView(UiKit.title(context,keys[i].toUpperCase(Locale.US)),UiKit.full(context));
            manual[i]=UiKit.input(context,"");
            manual[i].setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
            card.addView(manual[i],UiKit.full(context));
            sensorInfo[i]=UiKit.small(context,"");
            card.addView(sensorInfo[i],UiKit.full(context));
            LinearLayout buttons=UiKit.hbox(context);
            Button save=UiKit.button(context,"حفظ قيمة الهاتف");
            save.setOnClickListener(v->saveManual(index));
            buttons.addView(save,UiKit.weight(1));
            Button clear=UiKit.button(context,"مسح قيمة الهاتف");
            UiKit.secondary(clear);
            clear.setOnClickListener(v->confirmClear(index));
            buttons.addView(clear,UiKit.weight(1));
            card.addView(buttons,UiKit.full(context));
            manualGroup.addView(card,UiKit.full(context));
        }
        Button all=UiKit.button(context,"حفظ القيم اليدوية الثماني في الهاتف");
        all.setOnClickListener(v->saveAllManual());
        manualGroup.addView(all,UiKit.full(context));
        root.addView(manualGroup,UiKit.full(context));
        reloadManual();renderControls();
    }

    public View view(){return scroll;}

    /** Showing a Settings subpage is never a calibration start or NVS write. */
    public void onShown(){
        if(disposed)return;
        visible=true;
        reloadManual();
        readDeviceStatus(false);
    }

    /** Navigation hides/cancels ONLY Android polling. Firmware calibration keeps running. */
    public void onHidden(){
        if(disposed)return;
        visible=false;
        generation++;
        main.removeCallbacks(poll);
        scroll.setKeepScreenOn(false);
        if(phase==Phase.ACTIVE||phase==Phase.BUSY){
            phase=Phase.PAUSED;
            state.setText("متابعة المعايرة متوقفة على الهاتف؛ ESP32 قد يكون مستمرًا. " +
                "افتح الصفحة مجددًا لقراءة حالته.");
        }
        renderControls();
    }

    public void release(){
        disposed=true;visible=false;generation++;
        main.removeCallbacksAndMessages(null);
        scroll.setKeepScreenOn(false);
    }

    public void refresh(){
        if(disposed)return;
        JSONObject diagnostics=host.latestDiagnostics();
        JSONObject mq=diagnostics==null?null:diagnostics.optJSONObject("mq");
        for(int i=0;i<keys.length;i++){
            String key=keys[i];
            JSONObject q=mq==null?null:mq.optJSONObject(key);
            double rs=q==null?Double.NaN:q.optDouble("rs",Double.NaN);
            double r0=ManualR0Calibration.get(key);
            sensorInfo[i].setText("Rs: "+number(rs)+" kΩ • R0 الهاتف: "+number(r0)+
                " kΩ\nالمصدر: "+ManualR0Calibration.getSource(key)+
                " • عينات معتمدة: "+ManualR0Calibration.getSamples(key)+
                "\nآخر اعتماد: "+ManualR0Calibration.getUtc(key));
        }
    }

    private static String number(double value){
        return Double.isFinite(value)&&value>0?
            String.format(Locale.US,"%.5f",value):"—";
    }

    private void reloadManual(){
        for(int i=0;i<keys.length;i++){
            double val=ManualR0Calibration.get(keys[i]);
            manual[i].setText(Double.isFinite(val)&&val>0?
                String.format(Locale.US,"%.8f",val).replaceAll("0+$","").replaceAll("\\.$",""):"");
        }
        refresh();
    }

    private boolean mutationBlocked(){
        if(phase==Phase.BUSY||phase==Phase.ACTIVE||phase==Phase.REVIEW||
           phase==Phase.ERROR||phase==Phase.PAUSED){
            host.showToast("أكمل أو ألغِ المعايرة على ESP32 قبل تغيير قيم الهاتف");
            return true;
        }
        if(ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics())){
            host.showToast("أوقف القياس أو التحليل قبل تغيير R0");
            return true;
        }
        return false;
    }

    private void renderControls(){
        boolean idle=phase==Phase.IDLE,error=phase==Phase.ERROR,
            review=phase==Phase.REVIEW,active=phase==Phase.ACTIVE,
            paused=phase==Phase.PAUSED;
        start.setEnabled(idle);
        readSaved.setEnabled(idle||error||paused);
        retry.setEnabled(error||paused);
        approve.setEnabled(review);
        cancel.setEnabled(review||active||error||paused);
        scroll.setKeepScreenOn(visible&&active);
    }

    private void showError(String heading,Exception error,JSONObject response,boolean recoverable){
        android.util.Log.e("TankOperatorR0","R0: "+heading,error);
        phase=Phase.ERROR;
        String message=error.getClass().getSimpleName()+
            (error.getMessage()==null?"":": "+error.getMessage());
        String contract=response==null?"لا توجد استجابة JSON من ESP32":
            "schema="+response.optString("schema","—")+
            " • state="+response.optString("state","—")+
            " • warmup_ms="+response.optInt("warmup_ms",-1)+
            " • duration_ms="+response.optInt("duration_ms",-1)+
            " • windows="+response.optInt("window_count",-1);
        state.setText("تعذر إكمال "+heading);
        progress.setText(message+"\nESP32: "+contract+
            "\nلا تُحفظ قيم جديدة. اقرأ الحالة أو ألغِ العملية، ولا تعتمد R0 قبل التحقق.");
        reviewInfo.setText("");
        renderControls();
    }

    private void showProgress(FirmwareR0Contract.Progress p){
        phase=Phase.ACTIVE;
        state.setText("معايرة ESP32 • "+("warmup".equals(p.state)?"تسخين":
            "sampling".equals(p.state)?"تسجيل Rs":"انتظار"));
        long total="warmup".equals(p.state)?p.warmupMs:p.durationMs;
        progress.setText("المرحلة: "+p.state+
            " • المتبقي "+Math.max(0,(total-p.elapsedMs+999)/1000)+" ثانية\n"+
            "البروتوكول: "+p.protocol+" • المصدر: "+p.r0Definition+
            "\nالقراءات: "+p.counts);
        reviewInfo.setText("");
        renderControls();
        schedulePoll();
    }

    private void schedulePoll(){
        main.removeCallbacks(poll);
        if(!disposed&&visible&&phase==Phase.ACTIVE)main.postDelayed(poll,1000);
    }

    private void showResponse(JSONObject response,FirmwareR0Contract.Progress p){
        transientFailures=0;
        if(p.active()){staged=null;stagedDevice="";showProgress(p);return;}
        if("pending".equals(p.state)||("ready".equals(p.state)&&p.committed)){
            try{
                Map<String,Double> candidates="pending".equals(p.state)?
                    FirmwareR0Contract.pendingValues(response):
                    FirmwareR0Contract.committedValues(response);
                String id=response.optString("device_id","").trim();
                if(id.isEmpty())throw new IllegalStateException("Missing ESP32 device identity");
                staged=new JSONObject(response.toString());
                stagedDevice=id;
                phase=Phase.REVIEW;
                state.setText("pending".equals(p.state)?
                    "مكتملة • قيم معلقة للمراجعة، ولم تُكتب في NVS":
                    "قِيَم R0 محفوظة في NVS ويمكن استيرادها إلى الهاتف");
                StringBuilder b=new StringBuilder("هوية ESP32: "+id+
                    "\nالتعريف: "+p.r0Definition+" • النوافذ: "+p.windowCount+"\n");
                for(String key:keys)
                    b.append(key.toUpperCase(Locale.US)).append(": ")
                     .append(number(candidates.get(key))).append(" kΩ • ")
                     .append(p.counts.get(key)).append(" عينة\n");
                reviewInfo.setText(b.toString());
                progress.setText("راجع الثماني قيم قبل الضغط على «اعتماد». " +
                    "لا تُفسر المعايرة على أنها قياس مرجعي للغاز أو ppm.");
                renderControls();
            }catch(Exception e){showError("مراجعة R0",e,response,false);}
            return;
        }
        staged=null;stagedDevice="";
        phase=Phase.IDLE;
        state.setText("حالة ESP32: "+p.state);
        progress.setText(p.error.isEmpty()?
            "لا توجد معايرة قيد الانتظار. تستطيع بدء معايرة جديدة.":
            "سبب الفشل من ESP32: "+p.error+" • لم تتغير R0 القديمة.");
        reviewInfo.setText("");
        renderControls();
    }

    private void readDeviceStatus(boolean polling){
        if(disposed||!visible)return;
        if(!polling&&phase==Phase.BUSY)return;
        if(polling&&phase!=Phase.ACTIVE)return;
        final int token=++generation;
        if(!polling){phase=Phase.BUSY;state.setText("جارٍ الاستعلام عن ESP32...");renderControls();}
        host.io().execute(()->{
            JSONObject response=null;
            try{
                response=host.robotApi().calibrationStatus();
                FirmwareR0Contract.Progress p=FirmwareR0Contract.progress(response);
                final JSONObject observed=response;
                main.post(()->{
                    if(disposed||!visible||generation!=token)return;
                    showResponse(observed,p);
                });
            }catch(Exception ex){
                final JSONObject observed=response;
                main.post(()->{
                    if(disposed||!visible||generation!=token)return;
                    // Repeated malformed firmware responses cannot loop indefinitely.
                    // A few transient network failures can be retried; schema
                    // violations stop immediately so the operator can inspect/cancel.
                    if(polling&&!(ex instanceof IllegalArgumentException)&&
                        ++transientFailures<=2){
                        progress.setText("خطأ اتصال "+transientFailures+"/2: "+ex.getMessage()+
                            "\nإعادة الاستعلام؛ لا توجد أي كتابة R0.");
                        schedulePoll();
                    }else showError("الاستعلام عن R0",ex,observed,true);
                });
            }
        });
    }

    private void requestStart(){
        if(disposed||!visible||phase!=Phase.IDLE)return;
        if(ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics())){
            host.showToast("أنه القياسات النشطة قبل معايرة R0");return;
        }
        phase=Phase.BUSY;renderControls();
        state.setText("جارٍ التحقق من حالة ESP32 قبل بدء المعايرة...");
        final int token=++generation;
        host.io().execute(()->{
            JSONObject response=null;
            try{
                response=host.robotApi().calibrationStatus();
                FirmwareR0Contract.Progress prior=FirmwareR0Contract.progress(response);
                if(prior.active()||"pending".equals(prior.state)){
                    final JSONObject priorPayload=response;
                    main.post(()->{
                        if(disposed||!visible||generation!=token)return;
                        showResponse(priorPayload,prior);
                    });
                    return;
                }
                host.robotApi().mqPower(true);
                response=host.robotApi().calibrationStart(180,60);
                FirmwareR0Contract.Progress started=FirmwareR0Contract.progress(response);
                if(!started.active())throw new IllegalStateException(
                    "ESP32 did not enter warmup/sampling state");
                final JSONObject initial=response;
                main.post(()->{
                    if(disposed||!visible||generation!=token)return;
                    showResponse(initial,started);
                });
            }catch(Exception ex){
                final JSONObject observed=response;
                main.post(()->{
                    if(disposed||!visible||generation!=token)return;
                    showError("بدء المعايرة",ex,observed,true);
                });
            }
        });
    }

    private void requestApproval(){
        if(disposed||!visible||phase!=Phase.REVIEW||staged==null)return;
        final JSONObject proposal=staged;
        final String device=stagedDevice;
        phase=Phase.BUSY;renderControls();
        state.setText("جارٍ اعتماد القيم والتحقق من NVS...");
        final int token=++generation;
        host.io().execute(()->{
            try{
                FirmwareR0Contract.Progress requestedProgress=FirmwareR0Contract.progress(proposal);
                Map<String,Double> requested="pending".equals(requestedProgress.state)?
                    FirmwareR0Contract.pendingValues(proposal):
                    FirmwareR0Contract.committedValues(proposal);
                JSONObject observed=host.robotApi().calibrationStatus();
                FirmwareR0Contract.Progress current=FirmwareR0Contract.progress(observed);
                if(!device.equals(observed.optString("device_id","").trim()))
                    throw new IllegalStateException("ESP32 device identity changed");
                if("pending".equals(current.state)&&current.pending){
                    observed=host.robotApi().calibrationAccept();
                    current=FirmwareR0Contract.progress(observed);
                }
                if(!"ready".equals(current.state)||!current.committed)
                    throw new IllegalStateException("Firmware R0 was not committed");
                // A fresh status read verifies that the approved values persisted
                // across the accept response and the NVS readback.
                JSONObject verified=host.robotApi().calibrationStatus();
                FirmwareR0Contract.Progress saved=FirmwareR0Contract.progress(verified);
                Map<String,Double> actual=FirmwareR0Contract.committedValues(verified);
                if(!device.equals(verified.optString("device_id","").trim())||
                    !requestedProgress.r0Definition.equals(saved.r0Definition)||
                    requestedProgress.windowCount!=saved.windowCount)
                    throw new IllegalStateException("R0 provenance changed on readback");
                for(String key:keys){
                    double before=requested.get(key),after=actual.get(key);
                    if(Math.abs(before-after)>Math.max(0.00001,before*0.00001))
                        throw new IllegalStateException("NVS value mismatch: "+key);
                }
                if(!ManualR0Calibration.saveFirmware(host.preferences(),actual,device,
                    saved.counts,
                    "DEVICE_CLEAN_AIR_RS_MEAN".equals(saved.r0Definition)?
                        "DEVICE_CLEAN_AIR_RS_MEAN":"LEGACY_MQ_FACTOR_SCALED_R0_NOT_AIR_RS",
                    saved.windowCount))
                    throw new IllegalStateException("Phone R0 storage failed; NVS may be committed");
                String ledger="";
                try{
                    int count=MqDeviceR0LedgerV14.recordFirmwareApproved(host.database(),
                        device,actual,saved.counts);
                    ledger=" • سجل البحث: "+count;
                }catch(Exception ex){
                    android.util.Log.e("TankOperatorR0","R0 scientific ledger failed",ex);
                    ledger=" • تعذر مزامنة سجل البحث؛ بقيت NVS والهاتف محفوظتين";
                }
                final String message="اعتمدت ثماني قيم R0 بعد التحقق من NVS والهاتف • "+
                    device+ledger;
                main.post(()->{
                    if(disposed||generation!=token)return;
                    staged=null;stagedDevice="";phase=Phase.IDLE;
                    state.setText("نجحت معايرة R0");
                    progress.setText(message);
                    reviewInfo.setText("");
                    reloadManual();renderControls();
                    host.refreshAllTabs();
                });
            }catch(Exception ex){
                android.util.Log.e("TankOperatorR0","R0 approval failed",ex);
                main.post(()->{
                    if(disposed||generation!=token)return;
                    // The candidate remains reviewable. A retry checks real ESP32
                    // state first, including a potentially successful NVS commit.
                    phase=Phase.REVIEW;state.setText("لم يكتمل اعتماد R0");
                    progress.setText(ex.getClass().getSimpleName()+": "+ex.getMessage()+
                        "\nقد تكون NVS قد حُفظت؛ استعلم من ESP32 قبل إعادة الاعتماد.");
                    renderControls();
                });
            }
        });
    }

    private void requestCancel(){
        if(disposed||!visible||
            !(phase==Phase.REVIEW||phase==Phase.ACTIVE||
              phase==Phase.ERROR||phase==Phase.PAUSED))return;
        if(phase==Phase.REVIEW&&staged!=null&&
            "ready".equals(staged.optString("state",""))){
            staged=null;stagedDevice="";phase=Phase.IDLE;
            state.setText("أُلغي استيراد قيم R0 إلى الهاتف");
            progress.setText("المعايرة المعتمدة في NVS لم تتغير.");
            reviewInfo.setText("");renderControls();return;
        }
        phase=Phase.BUSY;main.removeCallbacks(poll);renderControls();
        state.setText("جارٍ إلغاء المعايرة المعلقة على ESP32...");
        final int token=++generation;
        host.io().execute(()->{
            try{
                JSONObject response=host.robotApi().calibrationCancel();
                FirmwareR0Contract.Progress p=FirmwareR0Contract.progress(response);
                if(!"cancelled".equals(p.state)||p.pending||p.committed)
                    throw new IllegalStateException("ESP32 did not acknowledge cancellation");
                main.post(()->{
                    if(disposed||generation!=token)return;
                    staged=null;stagedDevice="";phase=Phase.IDLE;
                    state.setText("أُلغيت المعايرة");
                    progress.setText("لم تتغير قيم R0 المعتمدة سابقًا على ESP32 أو الهاتف.");
                    reviewInfo.setText("");renderControls();
                });
            }catch(Exception ex){
                main.post(()->{
                    if(disposed||generation!=token)return;
                    showError("إلغاء المعايرة",ex,null,true);
                });
            }
        });
    }

    private double parsed(int i){
        try{
            double value=Double.parseDouble(manual[i].getText().toString()
                .trim().replace(',','.'));
            return Double.isFinite(value)&&value>0?value:Double.NaN;
        }catch(Exception ex){return Double.NaN;}
    }

    private void saveManual(int index){
        if(mutationBlocked())return;
        double value=parsed(index);
        if(!Double.isFinite(value)){
            host.showToast("أدخل R0 موجبة بوحدة kΩ للحساس "+keys[index]);return;
        }
        Runnable action=()->{
            if(mutationBlocked())return;
            JSONObject diag=host.latestDiagnostics();
            String id=diag==null?"":diag.optString("device_id","");
            if(!ManualR0Calibration.saveForDevice(host.preferences(),keys[index],value,id)){
                host.showToast("فشل حفظ القيمة المحلية");return;
            }
            reloadManual();host.refreshAllTabs();
            host.showToast("حُفظت R0 اليدوية في الهاتف فقط");
        };
        if("firmware_clean_air_r0".equals(ManualR0Calibration.getSource(keys[index]))){
            new AlertDialog.Builder(context)
                .setTitle("تجاوز معايرة ESP32 على الهاتف")
                .setMessage("ستستبدل القيمة المستوردة في الهاتف فقط. لا تتغير NVS. هل تؤكد؟")
                .setNegativeButton("إلغاء",null)
                .setPositiveButton("تأكيد التجاوز",(d,w)->action.run()).show();
        }else action.run();
    }

    private void saveAllManual(){
        if(mutationBlocked())return;
        Map<String,Double> values=new LinkedHashMap<>();
        for(int i=0;i<keys.length;i++){
            double value=parsed(i);
            if(!Double.isFinite(value)){
                manual[i].requestFocus();
                host.showToast("R0 غير صالحة للحساس "+keys[i]);return;
            }
            values.put(keys[i],value);
        }
        Runnable action=()->{
            if(mutationBlocked())return;
            JSONObject diag=host.latestDiagnostics();
            String id=diag==null?"":diag.optString("device_id","");
            if(!ManualR0Calibration.saveAllForDevice(host.preferences(),values,id)){
                host.showToast("فشل الحفظ، ولم تتغير R0");return;
            }
            reloadManual();host.refreshAllTabs();
            host.showToast("حُفظت القيم اليدوية الثماني في الهاتف فقط");
        };
        boolean firmware=false;
        for(String key:keys)
            if("firmware_clean_air_r0".equals(ManualR0Calibration.getSource(key)))
                firmware=true;
        if(firmware)new AlertDialog.Builder(context)
            .setTitle("تجاوز R0 المستوردة")
            .setMessage("سيُستبدل مصدر R0 على الهاتف فقط لجميع الحساسات. هل تؤكد؟")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("تأكيد التجاوز",(d,w)->action.run()).show();
        else action.run();
    }

    private void confirmClear(int index){
        if(mutationBlocked())return;
        new AlertDialog.Builder(context)
            .setTitle("مسح R0 المحلية")
            .setMessage("ستُمسح قيمة الهاتف للحساس "+keys[index]+
                " فقط؛ لن تُمسح معايرة ESP32 من NVS.")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("مسح القيمة المحلية",(d,w)->{
                if(mutationBlocked())return;
                if(!ManualR0Calibration.reset(host.preferences(),keys[index])){
                    host.showToast("تعذر مسح القيمة");return;
                }
                reloadManual();host.refreshAllTabs();
            }).show();
    }
}
