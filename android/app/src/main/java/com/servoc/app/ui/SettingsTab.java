package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.calibration.ManualR0Calibration;
import com.servoc.app.calibration.MicrophoneDbCalibration;
import com.servoc.app.control.DriveDirectionOption;
import com.servoc.app.control.DriveTuning;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.models.ModelRuntimeRegistry;
import com.servoc.app.models.MqDataScience76Catalog;
import com.servoc.app.chemcore.library.ChemCoreLibraryManager;
import com.servoc.app.chemical.ChemicalReferenceSyncV6;
import com.servoc.app.chemcore.ChemCoreMolecularSeedImporter;
import com.servoc.app.chemcore.ChemCoreQsprRuntimeRegistry;
import com.servoc.app.ReleaseAudit;
import org.json.JSONObject;
import java.util.Locale;

/** Compact operator settings shared by Direct and Remote modes. */
public final class SettingsTab {
    private static final int DRIVE_FIRMWARE_LIMIT=100;
    private final HazRobHost host;private final ScrollView scroll;
    private final FrameLayout workspace;
    private DeveloperR0Tab userCalibration;
    private LinearLayout calibrationScreen;
    private final EditText robot,camera,warm,measure,micOffset,turnSpeed,pivotSpeed;


    private final TextView compatibility,cameraState,libraryInfo,analysisUpdateInfo,micCalibrationInfo;
    private final ChemCoreLibraryManager libraryManager;

    public SettingsTab(Context c,HazRobHost host){
        this.host=host;this.libraryManager=new ChemCoreLibraryManager(c);ManualR0Calibration.load(host.preferences());
        workspace=new FrameLayout(c);scroll=new ScrollView(c);scroll.setFillViewport(true);
        workspace.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));root.setBackgroundColor(0xFFF7F8FA);scroll.addView(root);

        LinearLayout net=UiKit.card(c);net.addView(UiKit.title(c,"الشبكة"),UiKit.full(c));
        robot=field(c,net,"ESP32",host.preferences().getString("robot","192.168.4.1"));camera=field(c,net,"ESP32-CAM",host.preferences().getString("camera","192.168.4.2"));
        LinearLayout netButtons=UiKit.hbox(c);Button saveNet=UiKit.button(c,"حفظ");saveNet.setOnClickListener(v->saveNetwork());netButtons.addView(saveNet,UiKit.weight(1));Button testNet=UiKit.button(c,"اختبار");testNet.setOnClickListener(v->testConnections());netButtons.addView(testNet,UiKit.weight(1));net.addView(netButtons,UiKit.full(c));
        cameraState=UiKit.small(c,"CAM —");cameraState.setBackgroundColor(0xFFFFFFFF);net.addView(cameraState,UiKit.full(c));root.addView(net,UiKit.full(c));

        LinearLayout cfg=UiKit.card(c);cfg.addView(UiKit.title(c,"الضبط"),UiKit.full(c));
        warm=field(c,cfg,"التسخين s",String.valueOf(host.preferences().getInt("warm",60)));measure=field(c,cfg,"القياس s",String.valueOf(host.preferences().getInt("measure",30)));
        TextView timingHelp=UiKit.small(c,
            "الأزمنة أعلاه تخص القياس التشغيلي فقط؛ معايرة R0 مستقلة وثابتة: 180s إحماء + 60s قياس.");
        timingHelp.setTextColor(0xFF46515E);
        cfg.addView(timingHelp,UiKit.full(c));
        cfg.addView(UiKit.small(c,"سرعة حركة المركبة تضبط من عمود السرعة في شاشة القيادة: 0–100%، والقيمة الافتراضية عند بدء التطبيق 100%."),UiKit.full(c));
        turnSpeed=field(c,cfg,"سرعة الالتفاف (%)",String.valueOf(host.preferences().getInt(DriveTuning.PREF_TURN_PERCENT,DriveTuning.DEFAULT_TURN_PERCENT)));
        pivotSpeed=field(c,cfg,"سرعة الدوران في المكان (%)",String.valueOf(host.preferences().getInt(DriveTuning.PREF_PIVOT_PERCENT,DriveTuning.DEFAULT_PIVOT_PERCENT)));
        turnSpeed.setInputType(InputType.TYPE_CLASS_NUMBER);pivotSpeed.setInputType(InputType.TYPE_CLASS_NUMBER);
        TextView tuningHelp=UiKit.small(c,"الالتفاف = سرعة الجهة الداخلية عند أقصى انحراف أثناء الحركة. الدوران = سرعة Pivot في المكان. النطاق 0–100%. الافتراضي 40% / 60%. تطبق القيم على العجلة وJoystick والأزرار فقط.");tuningHelp.setTextColor(0xFF13539C);cfg.addView(tuningHelp,UiKit.full(c));
        Button saveDriveTuning=UiKit.button(c,"حفظ سرعات القيادة");saveDriveTuning.setOnClickListener(v->saveDriveTuning());cfg.addView(saveDriveTuning,UiKit.full(c));
        cfg.addView(UiKit.small(c,"منظومة الحركة: V8 تدعم درايفر L298N لأربعة محركات DC عبر 6 منافذ، أو سيرفوَين مستمري الدوران عند اختيار وضعهما في فريموير مستقل. تحقّق من نوع الدرايفر الفعلي في بيانات الاتصال أدناه."),UiKit.full(c));
        TextView reverseHelp=UiKit.small(c,"عكس التوجيه مستقل لكل طريقة: يبدل يمين/يسار فقط ولا يغيّر الأمام/الخلف. المساطر المزدوجة مستثناة وتبقى كل مسطرة مرتبطة بجهتها مباشرة.");reverseHelp.setTextColor(0xFF13539C);cfg.addView(reverseHelp,UiKit.full(c));
        cfg.addView(directionReverseCheck(c,"العجلة: عكس التوجيه يمين/يسار",DriveDirectionOption.PREF_REVERSE_WHEEL),UiKit.full(c));
        cfg.addView(directionReverseCheck(c,"Joystick: عكس التوجيه يمين/يسار",DriveDirectionOption.PREF_REVERSE_JOYSTICK),UiKit.full(c));
        cfg.addView(directionReverseCheck(c,"الأزرار: عكس التوجيه يمين/يسار",DriveDirectionOption.PREF_REVERSE_BUTTONS),UiKit.full(c));
        cfg.addView(directionReverseCheck(c,"عكس الدوران في المكان (Joystick + عجلة + الأزرار؛ باستثناء المساطر)",DriveDirectionOption.PREF_REVERSE_PIVOT_ALL),UiKit.full(c));
        LinearLayout buttons=UiKit.hbox(c);Button sync=UiKit.button(c,"مزامنة");sync.setOnClickListener(v->sync());buttons.addView(sync,UiKit.weight(1));Button apply=UiKit.button(c,"تطبيق");apply.setOnClickListener(v->apply());buttons.addView(apply,UiKit.weight(1));cfg.addView(buttons,UiKit.full(c));root.addView(cfg,UiKit.full(c));

        LinearLayout cleanAir=UiKit.card(c);
        cleanAir.addView(UiKit.title(c,"معايرة R0 — للمستخدم"),UiKit.full(c));
        cleanAir.addView(UiKit.small(c,
            "بروتوكول R0 المعتمد للإصدار السادس: 180 ثانية تسخين ثم 60 ثانية قراءة فعلية في نافذة واحدة (لا ثلاث دورات). "+
            "يعتمد الحفظ على نجاح قنوات MQ الثماني وحالة القبول الموثقة في ESP32؛ "+
            "التحديث اليدوي لا يستعير إثبات معايرة الفيرموير، ولا تُعامل قراءة غير متحققة كهواء نظيف."),
            UiKit.full(c));
        Button openCalibration=UiKit.button(c,"فتح معايرة R0");
        openCalibration.setOnClickListener(v->openUserCalibration());
        cleanAir.addView(openCalibration,UiKit.full(c));
        root.addView(cleanAir,UiKit.full(c));

        LinearLayout mic=UiKit.card(c);mic.addView(UiKit.title(c,"معايرة الميكروفون MAX4466"),UiKit.full(c));
        micOffset=field(c,mic,"تعويض الميكروفون (dB)",String.format(Locale.US,"%.1f",MicrophoneDbCalibration.offsetDb(host.preferences())));
        micOffset.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        TextView micHelp=UiKit.small(c,"أدخل الفرق: قراءة جهاز القياس المعاير − قراءة HazRob الخام. التطبيق يستخدم تعويض نقطة واحدة: القراءة المعوضة = القراءة الخام + التعويض. يجب إبقاء مقبض Gain في نفس الموضع بعد المعايرة.");
        micHelp.setTextColor(0xFF995500);mic.addView(micHelp,UiKit.full(c));
        micCalibrationInfo=UiKit.small(c,"");micCalibrationInfo.setBackgroundColor(0xFFFFFFFF);mic.addView(micCalibrationInfo,UiKit.full(c));
        LinearLayout micButtons=UiKit.hbox(c);Button saveMic=UiKit.button(c,"حفظ التعويض");saveMic.setOnClickListener(v->saveMicrophoneOffset());micButtons.addView(saveMic,UiKit.weight(1));Button clearMic=UiKit.button(c,"مسح التعويض");clearMic.setOnClickListener(v->clearMicrophoneOffset());micButtons.addView(clearMic,UiKit.weight(1));mic.addView(micButtons,UiKit.full(c));root.addView(mic,UiKit.full(c));

        LinearLayout compat=UiKit.card(c);compat.addView(UiKit.title(c,"النظام والتنبيهات"),UiKit.full(c));compatibility=UiKit.text(c,"Firmware —");compat.addView(compatibility,UiKit.full(c));
        LinearLayout alertButtons=UiKit.hbox(c);Button robotAlert=UiKit.button(c,"اختبار تنبيه الروبوت");robotAlert.setOnClickListener(v->testAlert());alertButtons.addView(robotAlert,UiKit.weight(1));Button phoneAlert=UiKit.button(c,"اختبار صوت الهاتف");phoneAlert.setOnClickListener(v->host.testPhoneAlert());alertButtons.addView(phoneAlert,UiKit.weight(1));compat.addView(alertButtons,UiKit.full(c));root.addView(compat,UiKit.full(c));

        LinearLayout science=UiKit.card(c);science.addView(UiKit.title(c,"المكتبة العلمية"),UiKit.full(c));Button info=UiKit.button(c,"الحالة");science.addView(info,UiKit.full(c));libraryInfo=UiKit.small(c,"");libraryInfo.setBackgroundColor(0xFFFFFFFF);libraryInfo.setVisibility(View.GONE);science.addView(libraryInfo,UiKit.full(c));
        info.setOnClickListener(v->{boolean show=libraryInfo.getVisibility()!=View.VISIBLE;libraryInfo.setVisibility(show?View.VISIBLE:View.GONE);if(show)libraryInfo.setText("Chemicals "+host.database().chemicalCount()+"   •   MQDataScience "+MqDataScience76Catalog.COUNT+"   •   Native "+ModelRuntimeRegistry.nativeCount()+"   •   Verified "+ModelRuntimeRegistry.verifiedCount()+"   •   Pending "+ModelRuntimeRegistry.pendingPortCount());});root.addView(science,UiKit.full(c));

        LinearLayout updates=UiKit.card(c);updates.addView(UiKit.title(c,"مزامنة محتويات المكتبة العلمية"),UiKit.full(c));analysisUpdateInfo=UiKit.small(c,"");analysisUpdateInfo.setBackgroundColor(0xFFFFFFFF);updates.addView(analysisUpdateInfo,UiKit.full(c));Button checkUpdate=UiKit.button(c,"الاستعلام عن تحديثات السجلات والإمكانات");checkUpdate.setOnClickListener(v->checkAnalysisUpdate());updates.addView(checkUpdate,UiKit.full(c));root.addView(updates,UiKit.full(c));
        refreshMicrophoneCalibration();
    }

    private CheckBox directionReverseCheck(Context c,String label,String key){
        CheckBox b=new CheckBox(c);
        b.setText(label);b.setTextColor(0xFF15202B);
        b.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFF145C7A));
        b.setBackground(UiKit.outline(c,0xFFF7F8FA,0xFF60758C));
        b.setPadding(UiKit.dp(c,12),UiKit.dp(c,8),UiKit.dp(c,12),UiKit.dp(c,8));
        b.setMinHeight(UiKit.dp(c,48));
        b.setChecked(host.preferences().getBoolean(key,false));
        b.setOnCheckedChangeListener((buttonView,checked)->{
            host.setDriveCommand(0,0);
            host.preferences().edit().putBoolean(key,checked).apply();
            host.refreshAllTabs();
            host.showToast(label+" • "+(checked?"معكوس":"طبيعي"));
        });
        return b;
    }
    private EditText field(Context c,LinearLayout p,String label,String value){TextView l=UiKit.small(c,label);l.setTextColor(0xFF46515E);p.addView(l,UiKit.full(c));EditText e=UiKit.input(c,value);p.addView(e,UiKit.full(c));return e;}
    /**
     * Reuses the same R0 calibration controller from V4 without requiring
     * developer PIN. Create it lazily so settings load cannot fail on a
     * calibration-side constructor error, and give it a full-height page.
     */
    private void openUserCalibration(){
        if(userCalibration==null){
            try{
                userCalibration=new DeveloperR0Tab(scroll.getContext(),host);
                calibrationScreen=UiKit.vbox(scroll.getContext());
                calibrationScreen.setBackgroundColor(0xFFF7F8FA);
                Button back=UiKit.button(scroll.getContext(),"← العودة إلى الإعدادات");
                back.setOnClickListener(v->closeUserCalibration());
                calibrationScreen.addView(back,UiKit.full(scroll.getContext()));
                calibrationScreen.addView(userCalibration.view(),
                    new LinearLayout.LayoutParams(-1,0,1f));
                workspace.addView(calibrationScreen,new FrameLayout.LayoutParams(-1,-1));
            }catch(RuntimeException ex){
                android.util.Log.e("TankSettingsR0","Cannot open user R0 calibration",ex);
                userCalibration=null;calibrationScreen=null;
                host.showToast("تعذر فتح معايرة R0. لم تتغير قيم المعايرة.");
                return;
            }
        }
        scroll.setVisibility(View.GONE);
        calibrationScreen.setVisibility(View.VISIBLE);
        userCalibration.refresh();
    }

    private void closeUserCalibration(){
        if(calibrationScreen!=null)calibrationScreen.setVisibility(View.GONE);
        scroll.setVisibility(View.VISIBLE);
    }

    public void release(){if(userCalibration!=null)userCalibration.release();}
    public View view(){return workspace;}

    public void refresh(){
        refreshMicrophoneCalibration();
        ChemCoreLibraryManager.Status ls=libraryManager.status();analysisUpdateInfo.setText("مكتبة ChemCore • "+(ls.remoteSnapshotActive?"مكتبة Stable مثبتة":"المكتبة المدمجة")+"\nتتوفر بيانات الإصدار والمصدر التفصيلية في تبويب الحالة");
        JSONObject d=host.latestDiagnostics();if(d==null){compatibility.setText("وحدة التحكم • غير متصلة");compatibility.setTextColor(0xFF995500);return;}
        JSONObject adc=d.optJSONObject("adc"),env=d.optJSONObject("env"),ldr=d.optJSONObject("ldr"),alert=d.optJSONObject("alert"),drive=d.optJSONObject("drive");
        String driver=drive==null?HazRobHardwareProfile.DRIVE_DRIVER:drive.optString("driver",HazRobHardwareProfile.DRIVE_DRIVER);boolean envOk=env!=null&&env.optBoolean("ok",false);
        String envStatus=env==null?"—":env.optString("status_code",env.optString("status","NOT READY"));
        String envType=env==null?"—":env.optString("type",env.optString("sensor","—"));
        String envAddr=env==null?"—":env.optString("address","—");
        String envChip=env==null?"—":env.optString("chip_id","—");
        String envAck=env==null?"76:— 77:—":"76:"+(env.optBoolean("ack_0x76",false)?"ACK":"—")+" 77:"+(env.optBoolean("ack_0x77",false)?"ACK":"—");
        String physicalPins=drive==null?"غير معروف":drive.optString("driver","").equals("l298n_four_dc")?
            "L(EN17,IN1=19,IN2=32) • R(EN16,IN1=18,IN2=27)":
            "L servo GPIO16 • R servo GPIO17";
        compatibility.setText("وحدة التحكم • متصلة\nDrive "+driver+" • "+physicalPins+"\nADS bus SDA21/SCL22 • ADS1 "+(adc!=null&&adc.optBoolean("ads1",false)?"OK":"—")+" • ADS2 "+(adc!=null&&adc.optBoolean("ads2",false)?"OK":"—")+"\nENV bus SDA13/SCL14 • "+(envOk?"OK":envStatus)+" • "+envType+" "+envAddr+" chip "+envChip+" • "+envAck+"\nLDR GPIO34 "+(ldr!=null&&ldr.optBoolean("installed",false)?"OK":"—")+"   •   ALERT GPIO"+(alert==null?26:alert.optInt("gpio",26))+" "+(alert!=null?"OK":"—"));compatibility.setTextColor(envOk?0xFF167443:0xFF995500);
    }

    private void saveDriveTuning(){
        int turn=parse(turnSpeed,-1),pivot=parse(pivotSpeed,-1);
        if(turn<0||turn>100||pivot<0||pivot>100){host.showToast("سرعتا الالتفاف والدوران يجب أن تكونا بين 0 و100%");return;}
        host.setDriveCommand(0,0);
        host.preferences().edit().putInt(DriveTuning.PREF_TURN_PERCENT,turn).putInt(DriveTuning.PREF_PIVOT_PERCENT,pivot).apply();
        turnSpeed.setText(String.valueOf(turn));pivotSpeed.setText(String.valueOf(pivot));host.refreshAllTabs();host.showToast("تم حفظ سرعات القيادة • التفاف "+turn+"% • دوران "+pivot+"%");
    }

    private void refreshMicrophoneCalibration(){
        JSONObject d=host.latestDiagnostics(),sound=d==null?null:d.optJSONObject("sound");
        double raw=MicrophoneDbCalibration.rawDbFs(sound),offset=MicrophoneDbCalibration.offsetDb(host.preferences());boolean configured=MicrophoneDbCalibration.configured(host.preferences());
        if(!Double.isFinite(raw)){
            micCalibrationInfo.setText("MAX4466 • لا توجد قراءة RMS صالحة الآن • التعويض "+(configured?String.format(Locale.US,"%+.1f dB",offset):"غير محفوظ"));micCalibrationInfo.setTextColor(0xFF995500);return;
        }
        double corrected=MicrophoneDbCalibration.compensate(raw,offset);
        micCalibrationInfo.setText("HazRob الخام "+String.format(Locale.US,"%.1f dBFS",raw)+" • التعويض "+(configured?String.format(Locale.US,"%+.1f dB",offset):"غير محفوظ")+" • "+(configured?"المعوض "+String.format(Locale.US,"%.1f dB",corrected):"احفظ التعويض لإظهار dB المعوض"));
        micCalibrationInfo.setTextColor(configured?0xFF167443:0xFF995500);
    }

    private void saveMicrophoneOffset(){
        String text=micOffset.getText().toString().trim();double v;try{v=Double.parseDouble(text);}catch(Exception e){host.showToast("قيمة تعويض الميكروفون غير صالحة");return;}
        if(!Double.isFinite(v)||v<MicrophoneDbCalibration.MIN_OFFSET_DB||v>MicrophoneDbCalibration.MAX_OFFSET_DB){host.showToast("تعويض الميكروفون يجب أن يكون بين -160 و +160 dB");return;}
        MicrophoneDbCalibration.save(host.preferences(),v);micOffset.setText(String.format(Locale.US,"%.1f",MicrophoneDbCalibration.offsetDb(host.preferences())));refreshMicrophoneCalibration();host.refreshAllTabs();host.showToast("تم حفظ تعويض الميكروفون");
    }

    private void clearMicrophoneOffset(){MicrophoneDbCalibration.clear(host.preferences());micOffset.setText("0.0");refreshMicrophoneCalibration();host.refreshAllTabs();host.showToast("تم مسح تعويض الميكروفون");}

    private void checkAnalysisUpdate(){
        host.io().execute(()->{
            try{
                boolean active=ChemCoreLibraryManager.measurementOrAnalysisActive(
                    host.latestDiagnostics());
                if(active){host.showToast("أوقف القياس أو التحليل قبل تحديث السجلات");return;}
                ChemicalReferenceSyncV6.Result synced=ChemicalReferenceSyncV6.refresh(
                    host.database(),0,25,active);
                host.database().putMeta("v6_reference_sync_result",synced.summary());
                host.showToast(synced.applied>0?
                    "تم تحديث "+synced.applied+" سجلات: ستظهر الإمكانات الجديدة في جميع القوائم":
                    "السجلات العلمية محدثة • النسخة "+synced.revision);
                // Re-query the live status projection after each successful
                // central update. This does NOT re-qualify any science.
                host.refreshAllTabs();
            }catch(Exception e){
                host.showToast("تعذر الاستعلام عن السجلات المنشورة: "+
                    e.getClass().getSimpleName());
            }
        });
    }

    private void saveNetwork(){host.setDriveCommand(0,0);String rh=robot.getText().toString().trim(),ch=camera.getText().toString().trim();host.robotApi().setRobotHost(rh);host.setCameraHost(ch);camera.setText(host.cameraHost());robot.setText(host.robotApi().getRobotHost());host.preferences().edit().putString("robot",host.robotApi().getRobotHost()).putString("camera",host.cameraHost()).apply();host.showToast("تم حفظ الشبكة");host.refreshAllTabs();}

    private static String cameraDiagnosticText(JSONObject cdiag){
        boolean cameraOk=cdiag.optBoolean("camera_ok",false),wifiOk=cdiag.optBoolean("wifi_connected",true),psram=cdiag.optBoolean("psram",false);
        String err=cdiag.optString("camera_error","—"),mem=cdiag.optString("fb_memory",psram?"psram":"dram"),sensor=cdiag.optString("sensor","UNKNOWN"),ip=cdiag.optString("ip","—");
        int pid=cdiag.optInt("pid",0);String fps=cdiag.has("fps")?String.format(Locale.US,"%.2f",cdiag.optDouble("fps",0)):"—";String rssi=cdiag.has("rssi")?cdiag.optInt("rssi",0)+" dBm":"—";
        return "CAM "+(cameraOk?"OK":"FAIL")+" • Wi-Fi "+(wifiOk?"OK":"NO")+" • IP "+ip+"\nSensor "+sensor+" PID 0x"+String.format(Locale.US,"%04X",pid)+" • ERR "+err+"\nMemory "+mem.toUpperCase(Locale.US)+" • PSRAM "+(psram?"YES":"NO")+" • FPS "+fps+" • RSSI "+rssi;
    }

    private void testConnections(){host.io().execute(()->{boolean robotOk=false;JSONObject robotDiag=null;try{robotDiag=host.robotApi().diagnostics();robotOk=robotDiag!=null;}catch(Exception ignored){}JSONObject cam=null;try{cam=host.cameraDiagnostics();}catch(Exception ignored){}boolean cameraHttp=cam!=null;if(!cameraHttp)try{cameraHttp=host.cameraReachable();}catch(Exception ignored){}final boolean rOk=robotOk,cHttp=cameraHttp;final JSONObject rdiag=robotDiag,cdiag=cam;new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{if(cdiag!=null){boolean actual=cdiag.optBoolean("camera_ok",false);cameraState.setText(cameraDiagnosticText(cdiag));cameraState.setTextColor(actual?0xFF81C784:0xFFFF7043);}else{int clients=rdiag==null?-1:rdiag.optInt("clients",-1);String hint=clients>=0?" • AP clients "+clients:"";cameraState.setText("CAM "+(cHttp?"HTTP OK":"OFFLINE")+hint+"\nإذا كان الهاتف والـCAM فقط متصلين، يفترض أن يكون AP clients ≥ 2. تحقق من إزالة وصلة GPIO0-GND بعد البرمجة ثم أعد التشغيل.");cameraState.setTextColor(cHttp?0xFFFFB74D:0xFFFF7043);}host.showToast("ESP32 "+(rOk?"OK":"OFFLINE")+" • CAM "+(cHttp?"HTTP":"OFFLINE"));});});}
    private void testAlert(){host.io().execute(()->{try{host.robotApi().alertTest();host.showToast("تم إرسال اختبار تنبيه الروبوت");}catch(Exception e){host.showToast("تعذر اختبار تنبيه الروبوت");}});}
    private void sync(){host.io().execute(()->{try{JSONObject x=host.robotApi().config();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->showConfig(x));}catch(Exception e){host.showToast("تعذر قراءة الإعدادات");}});}
    private void apply(){final int w=clamp(parse(warm,60),0,3600),m=clamp(parse(measure,30),5,600);final String cam=host.cameraHost();host.setDriveCommand(0,0);host.io().execute(()->{try{JSONObject x=host.robotApi().setConfig(cam,DRIVE_FIRMWARE_LIMIT,w,m);host.preferences().edit().remove("speed").putInt("warm",w).putInt("measure",m).remove("nl").remove("nr").apply();new android.os.Handler(android.os.Looper.getMainLooper()).post(()->showConfig(x));host.showToast("تم تطبيق إعدادات القياس");}catch(Exception e){host.showToast("فشل تطبيق الضبط");}});}
    private void showConfig(JSONObject x){if(x==null)return;String cam=x.optString("cam",camera.getText().toString());if(cam==null||cam.trim().isEmpty())cam="192.168.4.2";camera.setText(cam);host.setCameraHost(cam);warm.setText(String.valueOf(x.optInt("warm",parse(warm,60))));measure.setText(String.valueOf(x.optInt("measure",parse(measure,30))));}
    private static int parse(EditText e,int def){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception ignored){return def;}}
    private static int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
}
