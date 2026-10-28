package com.servoc.app.measurement;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.servoc.app.field.FieldCycleRepository;
import com.servoc.app.ui.HazRobHost;

import org.json.JSONObject;

/**
 * V5 operator-owned field measurement driver. The main Handler owns the timer;
 * all networking and SQLite work use HazRobHost.io(). Never sleep a worker for
 * 120 seconds and never infer relay success from an HTTP 200 alone.
 *
 * The V4 firmware remains unchanged. A field sample interval is a documented
 * phone-controlled capture from real /diag+/data snapshots, not an invented
 * firmware /measure READY event. Any lost telemetry fails closed.
 */
public final class FieldCycleController {
    public interface Observer {
        void changed(String state,String message,String cycleId);
        void refreshed();
    }
    private static final long WARMUP_MS=60000L,SAMPLING_MS=60000L,STALE_MS=6000L;
    private final HazRobHost host;
    private final FieldCycleRepository repo;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Observer observer;
    private volatile boolean disposed=false;
    private String cycleId="",deviceId="";
    private boolean gas=false,busy=false;private volatile boolean stopping=false;
    private long finalizeRequestedMs=0;
    private long lastFreshMs=0;
    private JSONObject lastSnapshot;
    private final Runnable tick=new Runnable(){public void run(){onTick();}};

    public FieldCycleController(HazRobHost host,Observer observer){
        if(host==null||observer==null)throw new IllegalArgumentException("host and observer required");
        this.host=host;this.observer=observer;repo=new FieldCycleRepository(host.database());
    }

    /** The controller is per Activity. Never imply an unfinished physical run completed on destruction. */
    public void release(){
        disposed=true;main.removeCallbacks(tick);
        MeasurementSessionRegistry.endFieldOperation();
        if(!cycleId.isEmpty()&&busy){
            final String id=cycleId,reason="ACTIVITY_DESTROYED_BEFORE_COMPLETION";
            final boolean turnOff=gas;
            try{host.io().execute(()->failWorker(id,reason,turnOff));}
            catch(Exception ex){android.util.Log.e("TankFieldCycle","Cannot cancel active cycle",ex);}
        }
    }
    public boolean busy(){return busy;}
    public String currentCycleId(){return cycleId;}

    public void begin(String sessionId,FieldCycleRepository.Options modes){
        if(disposed||busy)return;
        if(modes==null)throw new IllegalArgumentException("measurement modes required");
        if(!MeasurementSessionRegistry.reserveFieldOperation()){
            message("BLOCKED","هناك قياس أو تحليل نشط؛ أكمله أولاً","");
            return;
        }
        busy=true;stopping=false;gas=modes.gas;
        message("ARMING","جارٍ تجهيز دورة قياس مستقلة...",cycleId);
        host.io().execute(()->{
            FieldCycleRepository.Cycle row=null;
            try{
                if(MeasurementSessionRegistry.chemCoreCaptureRequested()||
                    MeasurementSessionRegistry.fieldCaptureRequested()||
                    !MeasurementSessionRegistry.activeSessionId().isEmpty())
                    throw new IllegalStateException("يوجد قياس أو تجربة نشطة");
                JSONObject before=host.robotApi().snapshot(0,0);
                if(modes.particles&&!particlesAvailable(before))
                    throw new IllegalStateException("حساس الجزيئات غير متاح؛ ألغِ اختيار الجزيئات");
                String id=identity(before);
                if(id.isEmpty())throw new IllegalStateException("تعذر التحقق من هوية ESP32");
                row=repo.begin(sessionId,modes);
                if(modes.gas)host.robotApi().mqPower(true);
                JSONObject verified=host.robotApi().snapshot(0,0);
                if(!id.equals(identity(verified)))throw new IllegalStateException("تغيرت هوية الروبوت");
                if(modes.gas&&!relayOn(verified))
                    throw new IllegalStateException("لم يؤكد ESP32 تشغيل ريلاي MQ");
                long at=SystemClock.elapsedRealtime();
                boolean saved=modes.gas?repo.relayConfirmed(row.id,at,id):
                    repo.startedWithoutGas(row.id,at,id);
                if(!saved)throw new IllegalStateException("تعذر توثيق بداية الإحماء");
                final String cid=row.id;
                main.post(()->{
                    if(disposed)return;
                    cycleId=cid;deviceId=id;gas=modes.gas;busy=true;
                    lastSnapshot=verified;lastFreshMs=SystemClock.elapsedRealtime();
                    message("WARMUP","إحماء الحساسات/استقرار التشغيل • 60 ثانية",cid);
                    main.removeCallbacks(tick);main.post(tick);
                    observer.refreshed();
                });
            }catch(Exception ex){
                String failedId=row==null?"":row.id;
                failWorker(failedId,ex.getMessage(),modes.gas);
                main.post(()->end("FAILED","تعذر بدء القياس: "+ex.getMessage(),failedId));
            }
        });
    }

    /** Recover abandoned reservations following an OS process restart. */
    public void recoverInterrupted(){
        if(disposed||busy)return;
        host.io().execute(()->{
            try{
                int n=repo.recoverAbandoned();
                if(n>0)main.post(()->{
                    if(!disposed){
                        message("RECOVERED","تم وسم "+n+" دورة منقطعة بأنها غير صالحة للاعتماد","");observer.refreshed();
                    }
                });
            }catch(Exception e){
                android.util.Log.e("TankFieldCycle","Recovery failed",e);
            }
        });
    }

    /** The predecessor remains immutable; retry always creates a new numbered attempt. */
    public void retry(String predecessor){
        if(disposed||busy)return;
        if(!MeasurementSessionRegistry.reserveFieldOperation()){
            message("BLOCKED","هناك قياس أو تحليل نشط؛ أكمله أولاً",predecessor);
            return;
        }
        busy=true;
        host.io().execute(()->{
            FieldCycleRepository.Cycle row=null;
            try{
                if(MeasurementSessionRegistry.fieldCaptureRequested()||
                    !MeasurementSessionRegistry.activeSessionId().isEmpty())
                    throw new IllegalStateException("يوجد قياس نشط");
                row=repo.retry(predecessor);
                final FieldCycleRepository.Cycle next=row;
                main.post(()->{
                    if(disposed)return;
                    busy=false;
                    startReserved(next);
                });
            }catch(Exception ex){
                // If retry itself failed, old review evidence remains unchanged.
                final String reason=ex.getMessage();
                if(row!=null)repo.fail(row.id,reason);
                main.post(()->end("FAILED","تعذر إعادة القياس: "+reason,predecessor));
            }
        });
    }

    private void startReserved(FieldCycleRepository.Cycle reserved){
        if(disposed||busy)return;
        busy=true;stopping=false;gas=reserved.modes.gas;
        final String cid=reserved.id;
        host.io().execute(()->{
            try{
                JSONObject before=host.robotApi().snapshot(0,0);
                if(reserved.modes.particles&&!particlesAvailable(before))
                    throw new IllegalStateException("حساس الجزيئات غير متاح");
                String id=identity(before);
                if(id.isEmpty())throw new IllegalStateException("هوية الروبوت غير متاحة");
                if(gas)host.robotApi().mqPower(true);
                JSONObject verified=host.robotApi().snapshot(0,0);
                if(!id.equals(identity(verified))||(gas&&!relayOn(verified)))
                    throw new IllegalStateException("تأكيد الريلاي/الجهاز فشل");
                long at=SystemClock.elapsedRealtime();
                if(!(gas?repo.relayConfirmed(cid,at,id):repo.startedWithoutGas(cid,at,id)))
                    throw new IllegalStateException("تعذر تسجيل بداية المحاولة");
                main.post(()->{
                    if(disposed)return;
                    cycleId=cid;deviceId=id;lastSnapshot=verified;
                    lastFreshMs=SystemClock.elapsedRealtime();
                    message("WARMUP","المحاولة التالية • الإحماء 60 ثانية",cid);
                    main.post(tick);observer.refreshed();
                });
            }catch(Exception ex){
                failWorker(cid,ex.getMessage(),gas);
                main.post(()->end("FAILED","فشلت إعادة القياس: "+ex.getMessage(),cid));
            }
        });
    }

    public void approve(String id){
        if(disposed||busy)return;busy=true;
        host.io().execute(()->{
            boolean ok=false;
            try{ok=repo.approve(id);}catch(Exception ex){
                android.util.Log.e("TankFieldCycle","Cannot approve cycle",ex);
            }
            final boolean approved=ok;
            main.post(()->end(approved?"APPROVED":"FAILED",
                approved?"تم اعتماد دورة القياس وحفظها":"تعذر الاعتماد: لم تُثبت دورة مكتملة صالحة",id));
        });
    }

    /** Deletes only unapproved terminal attempts. Approved evidence is immutable. */
    public void delete(String cycleId){
        if(disposed||busy)return;
        busy=true;
        host.io().execute(()->{
            boolean deleted=false;
            try{deleted=repo.deleteUnapproved(cycleId);}
            catch(Exception ex){android.util.Log.e("TankFieldCycle","Deletion refused",ex);}
            final boolean success=deleted;
            main.post(()->end(success?"DELETED":"DELETE_REFUSED",
                success?"حُذفت بيانات المحاولة غير المعتمدة مع بقاء رقمها في السجل":
                    "لا يُسمح بحذف قياس معتمد أو مرتبط بمصدر علمي",cycleId));
        });
    }

    private void onTick(){
        if(disposed||!busy||cycleId.isEmpty())return;
        JSONObject current=host.latestDiagnostics();
        long now=SystemClock.elapsedRealtime();
        if(current!=null&&current!=lastSnapshot){
            if(!identity(current).equals(deviceId)){
                stopWithFailure("تغيرت هوية الروبوت أثناء القياس");return;
            }
            lastSnapshot=current;lastFreshMs=now;
        }
        if(lastFreshMs==0||now-lastFreshMs>STALE_MS){
            stopWithFailure("توقفت بيانات الروبوت أو فقد الاتصال");return;
        }
        if(gas&&!relayOn(current)){
            stopWithFailure("انقطع ريلاي MQ أثناء الدورة");return;
        }
        JSONObject m=current==null?null:current.optJSONObject("measurement");
        if(m!=null&&("error".equals(m.optString("state",""))||
            "warmup".equals(m.optString("state",""))||
            "measuring".equals(m.optString("state","")))){
            stopWithFailure("قياس فيرموير آخر نشط أو فاشل");return;
        }
        host.io().execute(()->{
            try{
                FieldCycleRepository.Timing timing=repo.timing(cycleId);
                if(timing==null)throw new IllegalStateException("فقد سجل الدورة");
                long at=SystemClock.elapsedRealtime();
                if(FieldCycleRepository.WARMUP.equals(timing.state)){
                    long left=Math.max(0,WARMUP_MS-(at-timing.warmupStart));
                    main.post(()->message("WARMUP","الإحماء • المتبقي "+seconds(left)+" ثانية",cycleId));
                    if(left==0){
                        // Wait for first real poll and the DB link; a request alone
                        // is not equivalent to a stored measurement.
                        if(!MeasurementSessionRegistry.fieldCaptureRequested()){
                            if(!MeasurementSessionRegistry.requestFieldCapture())
                                throw new IllegalStateException("مسجل القياسات مشغول");
                        }
                        FieldCycleRepository.Cycle linked=repo.byId(cycleId);
                        String active=MeasurementSessionRegistry.activeSessionId();
                        if(linked!=null&&!linked.measurementId.isEmpty()&&linked.measurementId.equals(active)){
                            if(!repo.samplingStarted(cycleId,SystemClock.elapsedRealtime()))
                                throw new IllegalStateException("فشل توثيق بدء أخذ العينات");
                        }else if(at-timing.warmupStart>WARMUP_MS+12000L){
                            throw new IllegalStateException("لم يبدأ التسجيل الفعلي خلال 12 ثانية");
                        }
                        main.post(()->message("SAMPLING_WAIT",
                            "بانتظار أول عينة فعلية وربط التسجيل بالجلسة",cycleId));
                    }
                }else if(FieldCycleRepository.SAMPLING.equals(timing.state)){
                    long left=Math.max(0,SAMPLING_MS-(at-timing.sampleStart));
                    main.post(()->message("SAMPLING","قياس • المتبقي "+seconds(left)+" ثانية",cycleId));
                    if(left==0&&!stopping){
                        stopping=true;finalizeRequestedMs=at;
                        if(!MeasurementSessionRegistry.requestFieldStop("ready"))
                            throw new IllegalStateException("تعذر إنهاء مسجل القياسات");
                        main.post(()->message("FINALIZING","يُحفظ سجل العينات ويُفحص",cycleId));
                    }
                    if(stopping){
                        // Stop is completed by the telemetry poll, not by this timer.
                        if(!MeasurementSessionRegistry.fieldCaptureRequested()){
                            if(repo.complete(cycleId,deviceId)){
                                if(gas)try{host.robotApi().mqPower(false);}
                                    catch(Exception ignored){}
                                main.post(()->end("REVIEW_READY",
                                    "اكتملت الدورة؛ اختر اعتماد القياس أو إعادته",cycleId));
                                return;
                            }
                            throw new IllegalStateException("البيانات الفعلية لا تحقق شروط الاعتماد");
                        }
                        if(at-finalizeRequestedMs>12000L)
                            throw new IllegalStateException("تعذر إنهاء حفظ العينات خلال 12 ثانية");
                    }
                }else if(FieldCycleRepository.REVIEW.equals(timing.state)){
                    main.post(()->end("REVIEW_READY","اكتملت الدورة؛ اختر اعتماد أو إعادة القياس",cycleId));
                    return;
                }else if(FieldCycleRepository.ARMED.equals(timing.state)){
                    throw new IllegalStateException("الدورة ليست في حالة إحماء موثقة");
                }
                main.postDelayed(tick,1000);
            }catch(Exception ex){
                main.post(()->stopWithFailure(ex.getMessage()));
            }
        });
    }

    private void stopWithFailure(String reason){
        if(disposed||!busy)return;
        String cid=cycleId;boolean turnOff=gas;
        busy=false;main.removeCallbacks(tick);
        host.io().execute(()->{
            failWorker(cid,reason,turnOff);
            main.post(()->end("FAILED",reason,cid));
        });
    }
    private void failWorker(String id,String reason,boolean turnOff){
        if(MeasurementSessionRegistry.fieldCaptureRequested()){
            if(MeasurementSessionRegistry.activeSessionId().isEmpty())
                MeasurementSessionRegistry.clearFieldCapture();
            else MeasurementSessionRegistry.requestFieldStop("error");
        }
        if(id!=null&&!id.isEmpty())try{repo.fail(id,reason);}catch(Exception ignored){}
        if(turnOff)try{host.robotApi().mqPower(false);}catch(Exception ignored){}
    }
    private void end(String state,String text,String id){
        if(disposed)return;
        busy=false;stopping=false;cycleId="";lastSnapshot=null;lastFreshMs=0;
        MeasurementSessionRegistry.endFieldOperation();
        main.removeCallbacks(tick);
        message(state,text,id);observer.refreshed();
    }
    private void message(String state,String text,String id){
        if(!disposed)observer.changed(state,text,id);
    }
    private static String seconds(long ms){return Long.toString((ms+999)/1000);}
    private static boolean relayOn(JSONObject d){
        JSONObject r=d==null?null:d.optJSONObject("relay");
        return r!=null&&r.optBoolean("on",r.optInt("mask",0)!=0);
    }
    private static String identity(JSONObject d){return d==null?"":d.optString("device_id","").trim();}
    /** Reject nonexistent PM rather than saving a fictitious zero concentration. */
    private static boolean particlesAvailable(JSONObject d){
        JSONObject pm=d==null?null:d.optJSONObject("particles");
        return pm!=null&&pm.optBoolean("installed",false)&&pm.optBoolean("ok",false);
    }
}
