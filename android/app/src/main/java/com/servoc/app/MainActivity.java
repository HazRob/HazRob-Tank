package com.servoc.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.servoc.app.camera.CameraApi;
import com.servoc.app.auth.TankModePolicy;
import com.servoc.app.calibration.ManualR0Calibration;
import com.servoc.app.chemcore.ChemCoreMolecularSeedImporter;
import com.servoc.app.chemcore.ChemCoreQsprRuntimeRegistry;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemical.ChemicalSeedBootstrap;
import com.servoc.app.communication.RobotApi;
import com.servoc.app.communication.RobotLinkService;
import com.servoc.app.communication.WifiRoute;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.exposure.ExposureSeedBootstrap;
import com.servoc.app.measurement.GasAlertCoordinator;
import com.servoc.app.field.HazRobUserLibrary;
import com.servoc.app.field.FieldSessionRepository;
import com.servoc.app.field.HazRobStorageBootstrap;
import com.servoc.app.measurement.LiveGasStateEngine;
import com.servoc.app.measurement.PhoneAlertTone;
import com.servoc.app.measurement.SensorHealthEngine;
import com.servoc.app.measurement.TelemetryRecorder;
import com.servoc.app.navigation.MotionHistory;
import com.servoc.app.report.ReferenceBankExporter;
import com.servoc.app.report.ReportExporter;
import com.servoc.app.report.TankPhysicalSnapshotPdf;
import com.servoc.app.report.TankInstantReportPdf;
import com.servoc.app.report.FieldSessionReportExporter;
import com.servoc.app.ui.ControlTab;
import com.servoc.app.ui.DeveloperTab;
import com.servoc.app.ui.HazRobHost;
import com.servoc.app.ui.MeasurementsTab;
import com.servoc.app.ui.FieldSessionsTab;
import com.servoc.app.ui.ReportsTab;
import com.servoc.app.ui.SettingsTab;
import com.servoc.app.ui.StatusTab;
import com.servoc.app.ui.UiKit;

import org.json.JSONObject;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** HazRob 0.10.2 coordinator. Direct and Remote are transports for the same operator workspace. */
public final class MainActivity extends Activity implements HazRobHost {
    private static final int EXPORT_XLSX=8101,EXPORT_PDF=8102,EXPORT_REFERENCE=8103,EXPORT_RAW=8104,USER_LIBRARY_TREE=8105,EXPORT_FIELD_PDF=8106,EXPORT_FIELD_XLSX=8107,EXPORT_PHYSICAL_PDF=8108,EXPORT_INSTANT_PDF=8109;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService workers=Executors.newScheduledThreadPool(7);
    private final AtomicBoolean driveDispatching=new AtomicBoolean(false),driveDirty=new AtomicBoolean(false),telemetryPolling=new AtomicBoolean(false);
    private final MotionHistory motionHistory=new MotionHistory();
    private final SensorHealthEngine sensorHealth=new SensorHealthEngine();
    private final GasAlertCoordinator gasAlertCoordinator=new GasAlertCoordinator();
    private SharedPreferences prefs;private HazRobUserLibrary userLibrary;private RobotApi robotApi;private CameraApi cameraApi;private ExperimentsDb db;private TelemetryRecorder telemetryRecorder;private PhoneAlertTone phoneAlertTone;
    private volatile String pendingPhysicalSnapshot="{}",pendingPhysicalUtc="";
    private volatile String pendingInstantTelemetry="{}",pendingInstantAnalysis="{}",pendingInstantId="";
    private volatile long pendingInstantTime=0;
    private boolean pendingPhysicalDocumentRequest=false;
    private volatile JSONObject latestDiag;private volatile int left=0,right=0;private volatile boolean foreground=false;private volatile String flashMode="off";
    private volatile int trackingTarget=-1;private volatile String trackingName="";
    private volatile int lastGasAlertLevel=GasAlertCoordinator.NONE;private volatile long gasAlertSentAt=0;
    private boolean developerMode=false,entered=false,commsScheduled=false;private FrameLayout page;private TextView globalStatus;private Button[] tabButtons;private int selectedTab=0;private long pendingExperimentId=-1;private String pendingFieldSessionId="";
    private ControlTab controlTab;private FieldSessionsTab fieldSessionsTab;private MeasurementsTab measurementsTab;private ReportsTab reportsTab;private StatusTab statusTab;private SettingsTab settingsTab;private DeveloperTab developerTab;private View[] tabViews;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);HazRobStorageBootstrap.ensure(this);prefs=getSharedPreferences("servoc",MODE_PRIVATE);ManualR0Calibration.load(prefs);userLibrary=new HazRobUserLibrary(this);robotApi=new RobotApi(prefs.getString("robot","192.168.4.1"));cameraApi=new CameraApi(prefs.getString("camera","192.168.4.2"));flashMode="on".equals(prefs.getString("flash_mode","off"))?"on":"off";trackingTarget=prefs.getInt("tracking_id",-1);trackingName=prefs.getString("tracking_name","");db=new ExperimentsDb(this);telemetryRecorder=new TelemetryRecorder(db);try{phoneAlertTone=new PhoneAlertTone();}catch(Exception ignored){phoneAlertTone=null;}loadSensorBaselines();
        String mode=getIntent()==null?null:getIntent().getStringExtra("connection_mode");if(mode!=null&&!mode.trim().isEmpty())prefs.edit().putString("connection_mode",mode).apply();
        RobotApi.setTankOffline(isOffline());
        // SAF may recreate the Activity while the system picker is open.
        // Keep the original captured telemetry immutable until that specific picker returns.
        if(b!=null){
            pendingPhysicalDocumentRequest=b.getBoolean("tank_v3_physical_export_waiting",false);
            pendingPhysicalSnapshot=b.getString("tank_v3_physical_export_snapshot","{}");
            pendingPhysicalUtc=b.getString("tank_v3_physical_export_utc","");
            pendingInstantTelemetry=b.getString("tank_v4_instant_telemetry","{}");
            pendingInstantAnalysis=b.getString("tank_v4_instant_analysis","{}");
            pendingInstantId=b.getString("tank_v4_instant_id","");
            pendingInstantTime=b.getLong("tank_v4_instant_time",0);
        }
        workers.execute(()->{
            try{
                // Verify and activate the bundled knowledge off the Android main thread.
                com.servoc.app.chemcore.library.ChemCoreEmbeddedStableInstaller.ensureInstalled(getApplicationContext());
                ChemicalSeedBootstrap.ensureNiosh(getApplicationContext(),db);
                ExposureSeedBootstrap.ensureOshaZ1(getApplicationContext(),db);
                db.putMeta("tank_v3_chemcore_bootstrap","IMPORTING");
                ChemCoreMolecularSeedImporter.Result seed=ChemCoreMolecularSeedImporter.importBestAvailable(getApplicationContext(),db);
                db.putMeta("tank_v3_chemcore_seed_origin",seed.origin);
                db.putMeta("tank_v3_chemcore_seed_result",seed.message);
                ChemCoreQsprRuntimeRegistry.Status model=ChemCoreQsprRuntimeRegistry.rebuild(db);
                db.putMeta("tank_v3_chemcore_qspr_status",model.summary());
                db.putMeta("tank_v3_chemcore_bootstrap",seed.available?"READY":"PARTIAL_NO_MOLECULAR_SEED");
            }catch(Exception ex){
                db.putMeta("tank_v3_chemcore_bootstrap","ERROR");
                db.putMeta("tank_v3_chemcore_bootstrap_error",ex.getClass().getSimpleName()+": "+String.valueOf(ex.getMessage()));
            }finally{ui.post(this::refreshAllTabs);}
        });
        enter();
    }

    private boolean isOffline(){return TankModePolicy.isOffline(prefs==null?null:prefs.getString("connection_mode","direct"));}
    private void loadSensorBaselines(){for(String key:SensorHealthEngine.INSTALLED){String v=db.getMeta("health_baseline_"+key,"");try{double q=Double.parseDouble(v);if(Double.isFinite(q)&&q>0)sensorHealth.setBaseline(key,q);}catch(Exception ignored){}}}
    private void enter(){entered=true;buildTabs();setContentView(buildShell());showTab(0);if(!isOffline()){scheduleComms();startLinkService();if(foreground)controlTab.startCamera();}else{try{stopService(new Intent(this,RobotLinkService.class));}catch(Exception ignored){}}}
    private void buildTabs(){controlTab=new ControlTab(this,this);fieldSessionsTab=new FieldSessionsTab(this,this);measurementsTab=new MeasurementsTab(this,this);reportsTab=new ReportsTab(this,this);statusTab=new StatusTab(this,this);settingsTab=new SettingsTab(this,this);developerTab=new DeveloperTab(this,this,()->{developerMode=true;refreshAllTabs();});tabViews=new View[]{controlTab.view(),withRefresh(measurementsTab.view(),54),withRefresh(fieldSessionsTab.view(),8),withRefresh(reportsTab.view(),8),withRefresh(statusTab.view(),8),withRefresh(settingsTab.view(),8),withRefresh(developerTab.view(),54)};}
    /** Compact refresh overlay in the upper-left of each non-driving tab's CONTENT; no extra row. */
    private View withRefresh(View content,int topMarginDp){
        FrameLayout frame=new FrameLayout(this);
        frame.addView(content,new FrameLayout.LayoutParams(-1,-1));
        Button refresh=UiKit.button(this,"🔄");
        refresh.setTextSize(18);refresh.setContentDescription("تحديث");
        refresh.setMinWidth(UiKit.dp(this,44));refresh.setMinHeight(UiKit.dp(this,44));
        UiKit.secondary(refresh);
        refresh.setOnClickListener(v->requestManualRefresh());
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(UiKit.dp(this,44),UiKit.dp(this,44),Gravity.TOP|Gravity.LEFT);
        p.leftMargin=UiKit.dp(this,6);p.topMargin=UiKit.dp(this,topMarginDp);
        frame.addView(refresh,p);
        return frame;
    }

    private View buildShell(){
        LinearLayout root=UiKit.vbox(this);
        root.setBackgroundColor(0xFFF7F8FA);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        // No redundant header/footer. Detailed transport and application versions
        // are available exclusively in Status, never above the main navigation.
        globalStatus=UiKit.small(this,"غير متصل");
        HorizontalScrollView tabsScroll=new HorizontalScrollView(this);
        tabsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs=UiKit.hbox(this);
        String[] names={"القيادة","القياسات","الجلسات","التقارير","الحالة","الإعدادات","المطور"};
        tabButtons=new Button[names.length];
        for(int i=0;i<names.length;i++){
            final int x=i;Button bt=UiKit.button(this,names[i]);
            bt.setTextSize(12);bt.setOnClickListener(v->showTab(x));
            tabButtons[i]=bt;
            tabs.addView(bt,new LinearLayout.LayoutParams(UiKit.dp(this,110),UiKit.dp(this,44)));
        }
        tabsScroll.addView(tabs);
        root.addView(tabsScroll,new LinearLayout.LayoutParams(-1,-2));
        page=new FrameLayout(this);
        root.addView(page,new LinearLayout.LayoutParams(-1,0,1f));
        return root;
    }

    private void showTab(int index){
        if(!entered||index<0||index>=tabViews.length)return;
        if(selectedTab==0&&index!=0)controlTab.stopCamera();
        selectedTab=index;page.removeAllViews();page.addView(tabViews[index],new FrameLayout.LayoutParams(-1,-1));for(int i=0;i<tabButtons.length;i++){tabButtons[i].setTextColor(i==index?0xFF90CAF9:Color.DKGRAY);tabButtons[i].setBackgroundColor(i==index?0xFF263238:0xFFE0E0E0);}if(index==6&&!developerTab.isUnlocked())developerTab.requestUnlock();if(index==0&&foreground&&!isOffline())controlTab.startCamera();if(index==2)fieldSessionsTab.refresh();else if(index==3)reportsTab.refresh();else refreshVisibleTab();
    }

    private void queueDriveSend(){
        if(!entered||isOffline())return;
        driveDirty.set(true);
        if(!driveDispatching.compareAndSet(false,true))return;
        workers.execute(()->{
            try{
                do{
                    driveDirty.set(false);
                    int sendLeft=left,sendRight=right;
                    try{robotApi.drive(sendLeft,sendRight);}catch(Exception ignored){}
                }while(entered&&driveDirty.get());
            }finally{
                driveDispatching.set(false);
                if(entered&&driveDirty.get())queueDriveSend();
            }
        });
    }
    private void scheduleComms(){
        if(commsScheduled||isOffline())return;commsScheduled=true;
        // Preserve validated 20 Hz drive scheduling; RobotApi coalesces unchanged commands.
        workers.scheduleAtFixedRate(()->{if(entered&&foreground)queueDriveSend();},0,50,TimeUnit.MILLISECONDS);
        workers.scheduleAtFixedRate(()->pollTelemetry(false),0,150,TimeUnit.MILLISECONDS);
    }
    private void requestManualRefresh(){if(isOffline()){refreshAllTabs();showToast("وضع الاستعراض دون اتصال • لا توجد قراءات حية");return;}workers.execute(()->pollTelemetry(true));}
    private void pollTelemetry(boolean manual){
        if(!entered||isOffline())return;if(!telemetryPolling.compareAndSet(false,true)){if(manual)showToast("التحديث جارٍ");return;}
        try{long now=SystemClock.elapsedRealtime();JSONObject snap=robotApi.snapshot(left,right);latestDiag=snap;sensorHealth.accept(snap,now);updateGasAlert(snap,now);telemetryRecorder.accept(snap,left,right,developerMode&&selectedTab==6);ui.post(this::onTelemetryUpdated);if(manual)showToast("تم تحديث البيانات الحية");}
        catch(Exception e){latestDiag=null;gasAlertCoordinator.reset();lastGasAlertLevel=GasAlertCoordinator.NONE;gasAlertSentAt=0;if(phoneAlertTone!=null)phoneAlertTone.clear();ui.post(this::onTelemetryUpdated);if(manual)showToast("تعذر التحديث: "+e.getClass().getSimpleName());}
        finally{telemetryPolling.set(false);}
    }
    private void updateGasAlert(JSONObject snap,long now){
        if(isOffline())return;
        int warm=Math.max(0,prefs.getInt("warm",60));int level=gasAlertCoordinator.evaluate(snap,sensorHealth,warm,now);
        JSONObject device=snap==null?null:snap.optJSONObject("device"),proximity=device==null?null:device.optJSONObject("proximity");boolean hazard=proximity!=null&&proximity.optBoolean("warning",false);int phoneLevel=Math.max(level,hazard?GasAlertCoordinator.WARNING:GasAlertCoordinator.NONE);if(phoneAlertTone!=null)phoneAlertTone.update(phoneLevel,now);
        boolean changed=level!=lastGasAlertLevel,refresh=level>0&&now-gasAlertSentAt>=1000;if(!changed&&!refresh)return;lastGasAlertLevel=level;gasAlertSentAt=now;try{robotApi.setAlertLevel(level,3000);}catch(Exception ignored){}
    }
    private void onTelemetryUpdated(){if(isOffline()){latestDiag=null;if(globalStatus!=null){globalStatus.setText("دون اتصال");globalStatus.setTextColor(0xFF955000);}refreshVisibleTab();return;}JSONObject d=latestDiag;if(globalStatus==null)return;if(d==null){globalStatus.setText("غير متصل");globalStatus.setTextColor(0xFFFFB74D);}else{JSONObject drv=d.optJSONObject("drive"),device=d.optJSONObject("device"),proximity=device==null?null:device.optJSONObject("proximity");boolean near=proximity!=null&&proximity.optBoolean("warning",false),stale=drv!=null&&drv.optBoolean("stale_stop",false),gas=lastGasAlertLevel>0;globalStatus.setText("متصل"+(stale?" • توقف لانقطاع أوامر القيادة":gas?" • ⚠ غازات":near?" • ⚠ جسم قريب":""));globalStatus.setTextColor(stale?0xFF985400:gas?0xFFD62828:near?0xFFD62828:0xFF167443);}refreshVisibleTab();}
    private void refreshVisibleTab(){if(!entered)return;if(selectedTab==0)controlTab.refresh();else if(selectedTab==1)measurementsTab.refresh();else if(selectedTab==4)statusTab.refresh();else if(selectedTab==5)settingsTab.refresh();else if(selectedTab==6)developerTab.refresh();}

    @Override protected void onResume(){super.onResume();if(!isOffline()&&!"remote".equalsIgnoreCase(prefs.getString("connection_mode","direct")))WifiRoute.bindConnectedWifi(this);foreground=true;if(entered){if(!isOffline()){startLinkService();if(selectedTab==0)controlTab.startCamera();}refreshVisibleTab();}}
    @Override protected void onPause(){
        foreground=false;
        if(entered){
            // HR069-R1 drive dead-man: Android SAF pickers, incoming calls, app switching
            // and focus loss cannot leave a stale nonzero motor command running.
            left=right=0;
            motionHistory.record(0,0,SystemClock.elapsedRealtime());
            if(controlTab!=null){controlTab.resetJoystick();controlTab.stopCamera();}
            if(!isOffline()){queueDriveSend();workers.execute(()->{try{robotApi.motionStop();}catch(Exception ignored){}});}
        }
        super.onPause();
    }
    @Override protected void onSaveInstanceState(Bundle out){
        out.putBoolean("tank_v3_physical_export_waiting",pendingPhysicalDocumentRequest);
        if(pendingPhysicalDocumentRequest){
            out.putString("tank_v3_physical_export_snapshot",pendingPhysicalSnapshot);
            out.putString("tank_v4_instant_telemetry",pendingInstantTelemetry);
            out.putString("tank_v4_instant_analysis",pendingInstantAnalysis);
            out.putString("tank_v4_instant_id",pendingInstantId);
            out.putLong("tank_v4_instant_time",pendingInstantTime);
            out.putString("tank_v3_physical_export_utc",pendingPhysicalUtc);
        }
        super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy(){entered=false;if(developerTab!=null)developerTab.release();if(controlTab!=null)controlTab.releaseCamera();try{if(!isOffline()&&lastGasAlertLevel!=GasAlertCoordinator.NONE)robotApi.setAlertLevel(GasAlertCoordinator.NONE,500);}catch(Exception ignored){}if(phoneAlertTone!=null)try{phoneAlertTone.close();}catch(Exception ignored){}workers.shutdownNow();WifiRoute.unbind(this);if(db!=null)db.close();super.onDestroy();}
    private void startLinkService(){if(isOffline())return;try{RobotLinkService.start(getApplicationContext());}catch(Exception ignored){}}

    @Override public RobotApi robotApi(){return robotApi;}@Override public SharedPreferences preferences(){return prefs;}@Override public ExperimentsDb database(){return db;}@Override public ExecutorService io(){return workers;}@Override public String operatorRole(){return developerMode&&selectedTab==6?"DEVELOPER":"USER";}@Override public JSONObject latestDiagnostics(){return latestDiag;}@Override public void setLatestDiagnostics(JSONObject value){latestDiag=value;}@Override public void showToast(String text){ui.post(()->android.widget.Toast.makeText(this,text,android.widget.Toast.LENGTH_SHORT).show());}
    @Override public void emergencyStop(){left=right=0;motionHistory.record(0,0,SystemClock.elapsedRealtime());if(controlTab!=null)controlTab.resetJoystick();if(isOffline()){showToast("دون اتصال • لم يرسل أمر إلى الروبوت");return;}workers.execute(()->{try{robotApi.emergencyStop();}catch(Exception ignored){}});}
    @Override public void setDriveCommand(int l,int r){if(isOffline()){left=right=0;return;}int nl=Math.max(-100,Math.min(100,l)),nr=Math.max(-100,Math.min(100,r));left=nl;right=nr;motionHistory.record(nl,nr,SystemClock.elapsedRealtime());queueDriveSend();}
    @Override public int driveLeft(){return left;}@Override public int driveRight(){return right;}
    @Override public void setCameraHost(String host){cameraApi.setHost(host);prefs.edit().putString("camera",cameraApi.host()).apply();if(entered&&selectedTab==0&&foreground)controlTab.startCamera();}@Override public String cameraHost(){return cameraApi.host();}@Override public boolean cameraReachable(){return !isOffline()&&cameraApi.ping();}@Override public JSONObject cameraDiagnostics(){return isOffline()?null:cameraApi.status();}
    @Override public String flashMode(){return flashMode;}
    @Override public void setFlashMode(String mode){if(isOffline()){showToast("الفلاش غير متاح دون اتصال");return;}final String requested="on".equals(CameraApi.normalizeMode(mode))?"on":"off";workers.execute(()->{boolean ok=cameraApi.flash(requested);if(ok){flashMode=requested;prefs.edit().putString("flash_mode",requested).apply();if(controlTab!=null)ui.post(controlTab::refresh);showToast("الفلاش: "+flashAr(requested));}else showToast("تعذر إرسال أمر الفلاش");});}
    @Override public void testPhoneAlert(){if(phoneAlertTone==null){showToast("تنبيه الهاتف غير متاح على هذا الجهاز");return;}try{phoneAlertTone.test();showToast("اختبار صوت الهاتف — STREAM_ALARM");}catch(Exception e){showToast("تعذر تشغيل اختبار صوت الهاتف");}}

    @Override public SensorHealthEngine.Snapshot sensorHealth(String key){return sensorHealth.snapshot(key);}
    @Override public LiveGasStateEngine.Snapshot liveGasState(){return gasAlertCoordinator.liveSnapshot();}
    @Override public int captureCleanAirBaseline(){if(!developerMode)return 0;JSONObject d=latestDiag;int n=sensorHealth.captureCleanAirBaseline(d);if(n>0){for(String key:SensorHealthEngine.INSTALLED){double q=sensorHealth.baseline(key);if(Double.isFinite(q)&&q>0)db.putMeta("health_baseline_"+key,Double.toString(q));}db.putMeta("health_baseline_ts",isoNow());}return n;}
    @Override public int evaluateCleanAirDrift(){if(!developerMode)return 0;return sensorHealth.evaluateCleanAirDrift(latestDiag);}
    @Override public String cleanAirBaselineTimestamp(){return db==null?"":db.getMeta("health_baseline_ts","");}
    @Override public String lastMeasurementSessionId(){return telemetryRecorder==null?"":telemetryRecorder.lastCompletedSessionId();}

    @Override public void setTrackingTarget(int chemicalId,String name){trackingTarget=chemicalId;trackingName=chemicalId<0?"":(name==null?"":name);prefs.edit().putInt("tracking_id",trackingTarget).putString("tracking_name",trackingName).apply();}
    @Override public int trackingTargetId(){return trackingTarget;}@Override public String trackingTargetName(){return trackingName;}

    @Override public void requestExport(String format,long experimentId){
        ExperimentQueries.Record r=ExperimentQueries.byId(db,experimentId);
        if(r==null){showToast("السجل غير موجود");return;}
        // Operator PDF/XLSX must NEVER silently fall back to the legacy one-chart exporter.
        // A persisted user measurement belongs to exactly one Field Session when reportable.
        String fs="";
        try{if(r.sessionId!=null&&!r.sessionId.isEmpty())
            fs=new FieldSessionRepository(db).fieldSessionIdForMeasurement(r.sessionId);}
        catch(Exception e){showToast("تعذر العثور على جلسة القياس");return;}
        if(fs!=null&&!fs.isEmpty()){requestFieldSessionExport(format,fs);return;}
        // Developer laboratory experiments have their own scientific export identity;
        // they are not field-session reports and cannot be relabeled as site/session data.
        if(!developerMode||selectedTab!=6||!ExperimentQueries.isDeveloperMeasurementSession(db,r.sessionId)){
            showToast("التقرير الذكي يتطلب جلسة مستخدم ميدانية؛ اختر الموقع والجلسة من تبويب التقارير");
            return;
        }
        pendingExperimentId=experimentId;
        boolean pdf="pdf".equalsIgnoreCase(format);
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(pdf?"application/pdf":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        i.putExtra(Intent.EXTRA_TITLE,String.format(Locale.US,"HazRob_Developer_Experiment_%06d_%s%s",r.id,fileStamp(),pdf?".pdf":".xlsx"));
        startActivityForResult(i,pdf?EXPORT_PDF:EXPORT_XLSX);
    }
    @Override public void requestInstantReportPdf(JSONObject frozen,JSONObject chemcore,String id,long atMs){
        if(frozen==null||chemcore==null||id==null||id.isEmpty()){
            showToast("أكمل عملية القياس والتحليل أولاً");return;
        }
        pendingInstantTelemetry=frozen.toString();
        pendingInstantAnalysis=chemcore.toString();
        pendingInstantId=id;
        pendingInstantTime=atMs;
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE,"HazRob_Tank_Instant_"+fileStamp()+".pdf");
        startActivityForResult(intent,EXPORT_INSTANT_PDF);
    }

    @Override public void requestPhysicalSnapshotPdf(){
        if(pendingPhysicalDocumentRequest){showToast("أكمل حفظ التقرير السابق أو ألغِه أولاً");return;}
        if(isOffline()){showToast("التقرير الفوري يتطلب قراءات حية من الروبوت");return;}
        JSONObject snap=latestDiag;
        if(snap==null){showToast("لا توجد قياسات فيزيائية حية لتقرير PDF");return;}
        pendingPhysicalSnapshot=snap.toString();
        pendingPhysicalUtc=isoNow();
        pendingPhysicalDocumentRequest=true;
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE,"HazRob_Tank_Physical_"+fileStamp()+".pdf");
        try{startActivityForResult(intent,EXPORT_PHYSICAL_PDF);}
        catch(Exception ex){
            pendingPhysicalDocumentRequest=false;
            pendingPhysicalSnapshot="{}";pendingPhysicalUtc="";
            showToast("تعذر فتح نافذة حفظ تقرير PDF");
        }
    }

    @Override public void requestFieldSessionExport(String format,String fieldSessionId){
        String fs=fieldSessionId==null?"":fieldSessionId.trim();
        if(fs.isEmpty()){showToast("الجلسة غير صالحة");return;}
        // Only a lightweight indexed ID lookup is allowed on the Android UI thread.
        // FieldSessionReportExporter.resolve + PDF/XLSX construction execute later on workers.
        FieldSessionRepository.FieldSession selected;
        try{selected=new FieldSessionRepository(db).fieldSessionById(fs);}
        catch(Exception e){showToast("تعذر قراءة الجلسة");return;}
        if(selected==null){showToast("هذه الجلسة غير موجودة");return;}
        if(!selected.approved()){showToast("لا يمكن تصدير تقرير لجلسة غير معتمدة");return;}
        pendingFieldSessionId=fs;
        boolean pdf="pdf".equalsIgnoreCase(format);
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(pdf?"application/pdf":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String safeName=selected.name.replaceAll("[^A-Za-z0-9._-]","_");
        i.putExtra(Intent.EXTRA_TITLE,"HazRob_FieldSession_"+safeName+"_"+fs.replaceAll("[^A-Za-z0-9._-]","_")+"_"+fileStamp()+(pdf?".pdf":".xlsx"));
        startActivityForResult(i,pdf?EXPORT_FIELD_PDF:EXPORT_FIELD_XLSX);
    }

    @Override public void openReportsForSite(String siteId,String fieldSessionId){
        ui.post(()->{if(reportsTab!=null){reportsTab.selectScope(siteId,fieldSessionId);showTab(3);}});
    }
    @Override public void deleteUserFieldSession(String fieldSessionId){
        workers.execute(()->{
            try{
                boolean ok=new FieldSessionRepository(db).deleteFieldSession(fieldSessionId);
                if(!ok){showToast("لا يمكن حذف جلسة نشطة أو غير موجودة");return;}
                boolean synced=true;
                if(userLibrary!=null&&userLibrary.attached()){
                    try{userLibrary.deleteStoredSessionArchive(fieldSessionId);userLibrary.syncUserIndex(db);}
                    catch(Exception e){synced=false;}
                }
                showToast(synced?"تم حذف الجلسة وقياساتها":"حذفت الجلسة محليًا؛ فشلت مزامنة المكتبة، يرجى إعادة المزامنة");
                refreshAllTabs();
            }catch(Exception e){showToast("رفض الحذف لحماية القياسات المرجعية أو تعذر الحذف: "+e.getClass().getSimpleName());}
        });
    }
    @Override public void deleteEmptyUserSite(String siteId){
        workers.execute(()->{
            try{
                boolean ok=new FieldSessionRepository(db).deleteEmptySite(siteId);
                if(!ok){showToast("لا يمكن حذف موقع يحوي جلسات");return;}
                boolean synced=true;
                if(userLibrary!=null&&userLibrary.attached())try{userLibrary.syncUserIndex(db);}catch(Exception e){synced=false;}
                showToast(synced?"تم حذف الموقع الفارغ":"حذف الموقع محليًا؛ يرجى مزامنة المكتبة");
                refreshAllTabs();
            }catch(Exception e){showToast("تعذر حذف الموقع: "+e.getClass().getSimpleName());}
        });
    }

    @Override public void requestRawExport(long experimentId){ExperimentQueries.Record r=ExperimentQueries.byId(db,experimentId);if(r==null){showToast("السجل غير موجود");return;}pendingExperimentId=experimentId;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,String.format(Locale.US,"HazRob_Raw_%06d_%s.json",r.id,fileStamp()));startActivityForResult(i,EXPORT_RAW);}
    @Override public void requestReferenceBankExport(long experimentId){if(!developerMode){showToast("هذه الوظيفة متاحة للمطور فقط");return;}ExperimentQueries.Record r=ExperimentQueries.byId(db,experimentId);if(r==null){showToast("التجربة غير موجودة");return;}pendingExperimentId=experimentId;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,r.referenceName==null||r.referenceName.isEmpty()?ReferenceBankExporter.fileName(r.id,r.ts):r.referenceName);startActivityForResult(i,EXPORT_REFERENCE);}

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==EXPORT_PHYSICAL_PDF&&(resultCode!=RESULT_OK||data==null||data.getData()==null)){
            pendingPhysicalDocumentRequest=false;
            pendingPhysicalSnapshot="{}";pendingPhysicalUtc="";
            return;
        }
        if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        if(requestCode==USER_LIBRARY_TREE){
            try{userLibrary.attach(data.getData());userLibrary.syncUserIndex(db);showToast("تم ربط ومزامنة مكتبة HazRob الدائمة");refreshAllTabs();}catch(Exception e){showToast("تعذر فتح مكتبة HazRob: "+e.getClass().getSimpleName());}
            return;
        }
        if(requestCode==EXPORT_INSTANT_PDF){
            final Uri uri=data.getData();
            final String frozen=pendingInstantTelemetry,analysis=pendingInstantAnalysis,
                opId=pendingInstantId;
            final long capturedAt=pendingInstantTime;
            pendingInstantTelemetry="{}";pendingInstantAnalysis="{}";pendingInstantId="";
            pendingInstantTime=0;
            workers.execute(()->{
                try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
                    if(out==null)throw new IllegalStateException("No PDF destination");
                    TankInstantReportPdf.write(out,new JSONObject(frozen),
                        new JSONObject(analysis),opId,capturedAt);
                    showToast("تم تصدير تقرير العملية الآنية");
                }catch(Exception ex){showToast("تعذر تصدير تقرير القياس الآني: "+ex.getClass().getSimpleName());}
            });
            return;
        }
        if(requestCode==EXPORT_PHYSICAL_PDF){
            if(!pendingPhysicalDocumentRequest){showToast("انتهت صلاحية لقطة القياس قبل الحفظ");return;}
            final Uri uri=data.getData(); // unique frozen snapshot belongs to this SAF result
            final String frozen=pendingPhysicalSnapshot,stamp=pendingPhysicalUtc;
            pendingPhysicalDocumentRequest=false;
            pendingPhysicalSnapshot="{}";pendingPhysicalUtc="";
            workers.execute(()->{
                try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
                    if(out==null)throw new IllegalStateException("output stream");
                    TankPhysicalSnapshotPdf.write(out,new JSONObject(frozen),stamp);
                    showToast("تم تصدير تقرير القياسات الفيزيائية PDF");
                }catch(Exception e){showToast("فشل تقرير القياسات الفيزيائية: "+e.getClass().getSimpleName());}
            });
            return;
        }
        if(requestCode==EXPORT_FIELD_PDF||requestCode==EXPORT_FIELD_XLSX){
            final Uri uri=data.getData();final int req=requestCode;final String fieldId=pendingFieldSessionId;
            workers.execute(()->{
                if(fieldId==null||fieldId.trim().isEmpty()){showToast("تعذر تحديد الجلسة");return;}
                try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
                    if(out==null)throw new IllegalStateException("output stream");
                    ChemicalRepository chemicals=new ChemicalRepository(db);
                    if(req==EXPORT_FIELD_PDF)FieldSessionReportExporter.writePdf(out,db,fieldId,chemicals);else FieldSessionReportExporter.writeXlsx(out,db,fieldId,chemicals);
                    showToast(req==EXPORT_FIELD_PDF?"تم تصدير تقرير الجلسة PDF":"تم تصدير تقرير الجلسة Excel");
                }catch(Exception e){showToast("فشل تصدير الجلسة: "+e.getClass().getSimpleName());}
            });
            return;
        }
        if(requestCode!=EXPORT_PDF&&requestCode!=EXPORT_XLSX&&requestCode!=EXPORT_REFERENCE&&requestCode!=EXPORT_RAW)return;
        final Uri uri=data.getData();final int req=requestCode;final long id=pendingExperimentId;
        workers.execute(()->{ExperimentQueries.Record r=ExperimentQueries.byId(db,id);if(r==null){showToast("تعذر العثور على السجل");return;}try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IllegalStateException("output stream");ChemicalRepository chemicals=new ChemicalRepository(db);if(req==EXPORT_PDF)ReportExporter.writePdf(out,r,chemicals);else if(req==EXPORT_XLSX)ReportExporter.writeXlsx(out,r,chemicals);else if(req==EXPORT_REFERENCE)ReferenceBankExporter.writeJson(out,r);else writeRaw(out,r);showToast(req==EXPORT_PDF?"تم تصدير PDF":req==EXPORT_XLSX?"تم تصدير Excel":req==EXPORT_RAW?"تم تصدير Raw JSON":"تم تصدير سجل البنك المرجعي");}catch(Exception e){showToast("فشل التصدير: "+e.getClass().getSimpleName());}});
    }

    private static void writeRaw(OutputStream out,ExperimentQueries.Record r)throws Exception{JSONObject j=new JSONObject();j.put("id",r.id);j.put("timestamp",r.ts);j.put("protocol",r.protocol);j.put("session_id",r.sessionId);j.put("capture_id",r.captureId);j.put("chemical_uid",r.chemicalUid);j.put("reference_kind",r.referenceKind);if(r.knownPpm!=null)j.put("known_ppm",r.knownPpm);try{j.put("payload",new JSONObject(r.payload));}catch(Exception e){j.put("payload_text",r.payload);}try{j.put("analytics",new JSONObject(r.analytics));}catch(Exception e){j.put("analytics_text",r.analytics);}out.write(j.toString(2).getBytes(StandardCharsets.UTF_8));}
    @Override public boolean userLibraryAttached(){return userLibrary!=null&&userLibrary.attached();}
    @Override public void requestUserLibraryFolder(){startActivityForResult(HazRobUserLibrary.createPickerIntent(),USER_LIBRARY_TREE);}
    @Override public void syncUserLibrary(){if(userLibrary==null||!userLibrary.attached()){showToast("اختر مجلد مكتبة HazRob أولًا");return;}workers.execute(()->{try{userLibrary.syncUserIndex(db);showToast("تمت مزامنة مكتبة المواقع والجلسات");}catch(Exception e){showToast("فشلت مزامنة مكتبة HazRob");}});}
    @Override public void restoreUserLibrary(){if(userLibrary==null||!userLibrary.attached()){showToast("اختر مجلد مكتبة HazRob أولًا");return;}workers.execute(()->{try{HazRobUserLibrary.RestoreSummary r=userLibrary.restoreUserIndex(db);showToast("استعادة المكتبة: "+r.sites+" موقع • "+r.sessions+" جلسة • "+r.measurements+" قياس • "+r.samples+" عينة"+(r.skippedLinks>0?" • "+r.skippedLinks+" رابط غير مستعاد":""));refreshAllTabs();}catch(Exception e){showToast("فشلت استعادة مكتبة HazRob");}});}
    @Override public void refreshAllTabs(){if(Looper.myLooper()!=Looper.getMainLooper()){ui.post(this::refreshAllTabs);return;}if(fieldSessionsTab!=null)fieldSessionsTab.refresh();if(reportsTab!=null)reportsTab.refresh();refreshVisibleTab();}

    private static String flashAr(String m){return"on".equals(m)?"تشغيل":"إيقاف";}
    private static String fileStamp(){SimpleDateFormat f=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US);f.setTimeZone(TimeZone.getDefault());return f.format(new Date());}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
