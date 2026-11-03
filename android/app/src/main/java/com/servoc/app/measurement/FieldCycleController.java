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
    public static final String RELAY_RECOVERY_FAULT_KEY="v8_field_recovery_relay_fault";
    public static final String RELAY_EXPECTED_DEVICE_KEY="v8_field_relay_expected_device_id";
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
        if(!cycleId.isEmpty()&&busy){
            final String id=cycleId,reason="ACTIVITY_DESTROYED_BEFORE_COMPLETION";
            final boolean turnOff=gas;
            try{host.io().execute(()->{
                try{failWorker(id,reason,turnOff);}
                finally{MeasurementSessionRegistry.endFieldOperation();}
            });}
            catch(Exception ex){
                android.util.Log.e("TankFieldCycle","Cannot cancel active cycle",ex);
                MeasurementSessionRegistry.requestFieldStop("error");
                if(turnOff){
                    // No worker means no source-qualified OFF evidence. Preserve
                    // the lock for the next process rather than forgetting it.
                    try{host.database().putMeta(RELAY_RECOVERY_FAULT_KEY,
                        "MQ_RELAY_RELEASE_WORKER_UNAVAILABLE: "+ex.getClass().getSimpleName());}
                    catch(Exception dbFailure){
                        android.util.Log.e("TankFieldCycle",
                            "Cannot persist shutdown safety fault",dbFailure);
                    }
                }
            }
        }else if(!busy){
            MeasurementSessionRegistry.endFieldOperation();
        }
        // If busy during ARMING, there is no cycleId yet. The arming worker
        // owns cleanup and must release the reservation AFTER relay power-off.
    }
    public boolean busy(){return busy;}
    public String currentCycleId(){return cycleId;}

    public void begin(String sessionId,FieldCycleRepository.Options modes){
        if(disposed||busy)return;
        if(!host.database().getMeta(RELAY_RECOVERY_FAULT_KEY,"").isEmpty()||
           !host.database().getMeta(RELAY_EXPECTED_DEVICE_KEY,"").isEmpty()){
            message("SAFETY_FAULT",
                "تعذر تأكيد إطفاء ريلاي MQ بعد انقطاع سابق؛ أعد الاتصال وأعد فتح الجلسات للتحقق من حالة OFF","");return;
        }
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
                if(modes.gas){
                    // Persist the intended physical controller BEFORE sending
                    // ON, so crash recovery cannot accept another robot OFF.
                    host.database().putMeta(RELAY_EXPECTED_DEVICE_KEY,id);
                    host.robotApi().mqPower(true);
                }
                JSONObject verified=host.robotApi().snapshot(0,0);
                if(!id.equals(identity(verified)))throw new IllegalStateException("تغيرت هوية الروبوت");
                if(modes.gas&&!relayOn(verified))
                    throw new IllegalStateException("لم يؤكد ESP32 تشغيل ريلاي MQ");
                long at=SystemClock.elapsedRealtime();
                boolean saved=modes.gas?repo.relayConfirmed(row.id,at,id):
                    repo.startedWithoutGas(row.id,at,id);
                if(!saved)throw new IllegalStateException("تعذر توثيق بداية الإحماء");
                final String cid=row.id;
                if(disposed){
                    failWorker(cid,"ACTIVITY_DESTROYED_DURING_ARMING",modes.gas);
                    MeasurementSessionRegistry.endFieldOperation();
                    return;
                }
                main.post(()->{
                    if(disposed){
                        host.io().execute(()->{
                            try{failWorker(cid,"ACTIVITY_DESTROYED_BEFORE_TIMER",modes.gas);}
                            finally{MeasurementSessionRegistry.endFieldOperation();}
                        });
                        return;
                    }
                    cycleId=cid;deviceId=id;gas=modes.gas;busy=true;
                    lastSnapshot=verified;lastFreshMs=SystemClock.elapsedRealtime();
                    message("WARMUP","إحماء الحساسات/استقرار التشغيل • 60 ثانية",cid);
                    main.removeCallbacks(tick);main.post(tick);
                    observer.refreshed();
                });
            }catch(Exception ex){
                String failedId=row==null?"":row.id;
                failWorker(failedId,ex.getMessage(),modes.gas);
                if(disposed)MeasurementSessionRegistry.endFieldOperation();
                else main.post(()->end("FAILED","تعذر بدء القياس: "+ex.getMessage(),failedId));
            }
        });
    }

    /** Recover abandoned reservations following an OS process restart. */
    public void recoverInterrupted(){
        if(disposed||busy)return;
        host.io().execute(()->{
            try{
                int n=repo.recoverAbandoned();
                boolean previousSafetyFault=!host.database().getMeta(
                    RELAY_RECOVERY_FAULT_KEY,"").isEmpty();
                String expectedDevice=host.database().getMeta(RELAY_EXPECTED_DEVICE_KEY,"");
                boolean relaySafetyFault=false;
                if(n>0||previousSafetyFault||!expectedDevice.isEmpty()){
                    try{
                        host.robotApi().mqPower(false);
                        JSONObject off=host.robotApi().snapshot(0,0);
                        if(!relayOffAcknowledged(off)||identity(off).isEmpty())
                            throw new IllegalStateException("MQ_RELAY_OFF_NOT_ACKNOWLEDGED");
                        if(!expectedDevice.isEmpty()&&!expectedDevice.equals(identity(off)))
                            throw new IllegalStateException("MQ_RELAY_OFF_ACK_WRONG_DEVICE");
                        host.database().putMeta(RELAY_RECOVERY_FAULT_KEY,"");
                        host.database().putMeta(RELAY_EXPECTED_DEVICE_KEY,"");
                    }catch(Exception safetyFailure){
                        relaySafetyFault=true;
                        android.util.Log.e("TankFieldCycle",
                            "Interrupted field run: unable to confirm safe MQ relay OFF",safetyFailure);
                        host.database().putMeta(RELAY_RECOVERY_FAULT_KEY,
                            safetyFailure.getClass().getSimpleName()+":"+
                            safetyFailure.getMessage());
                    }
                }
                final boolean unsafeRelay=relaySafetyFault;
                if(n>0||previousSafetyFault||!expectedDevice.isEmpty())main.post(()->{
                    if(!disposed){
                        message(unsafeRelay?"SAFETY_FAULT":"RECOVERED",
                            unsafeRelay?
                                "استعادة جلسة منقطعة: لم يُؤكد إطفاء ريلاي MQ؛ افصل طاقة الحساسات بأمان":
                                previousSafetyFault||!expectedDevice.isEmpty()?
                                    "تم تأكيد إطفاء ريلاي MQ للروبوت المسجل وإزالة قفل السلامة":
                                    "تم وسم "+n+" دورة منقطعة بأنها غير صالحة للاعتماد","");
                        observer.refreshed();
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
        if(!host.database().getMeta(RELAY_RECOVERY_FAULT_KEY,"").isEmpty()||
           !host.database().getMeta(RELAY_EXPECTED_DEVICE_KEY,"").isEmpty()){
            message("SAFETY_FAULT",
                "إعادة القياس محظورة حتى تأكيد إطفاء ريلاي MQ بعد الانقطاع السابق","");return;
        }
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
                    if(disposed){
                        host.io().execute(()->{
                            try{failWorker(next.id,"ACTIVITY_DESTROYED_DURING_RETRY",next.modes.gas);}
                            finally{MeasurementSessionRegistry.endFieldOperation();}
                        });
                        return;
                    }
                    busy=false;
                    startReserved(next);
                });
            }catch(Exception ex){
                // If retry itself failed, old review evidence remains unchanged.
                final String reason=ex.getMessage();
                if(row!=null)repo.fail(row.id,reason);
                if(disposed)MeasurementSessionRegistry.endFieldOperation();
                else main.post(()->end("FAILED","تعذر إعادة القياس: "+reason,predecessor));
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
                if(gas){
                    host.database().putMeta(RELAY_EXPECTED_DEVICE_KEY,id);
                    host.robotApi().mqPower(true);
                }
                JSONObject verified=host.robotApi().snapshot(0,0);
                if(!id.equals(identity(verified))||(gas&&!relayOn(verified)))
                    throw new IllegalStateException("تأكيد الريلاي/الجهاز فشل");
                long at=SystemClock.elapsedRealtime();
                if(!(gas?repo.relayConfirmed(cid,at,id):repo.startedWithoutGas(cid,at,id)))
                    throw new IllegalStateException("تعذر تسجيل بداية المحاولة");
                if(disposed){
                    failWorker(cid,"ACTIVITY_DESTROYED_DURING_RETRY_ARMING",gas);
                    MeasurementSessionRegistry.endFieldOperation();
                    return;
                }
                main.post(()->{
                    if(disposed){
                        host.io().execute(()->{
                            try{failWorker(cid,"ACTIVITY_DESTROYED_BEFORE_RETRY_TIMER",gas);}
                            finally{MeasurementSessionRegistry.endFieldOperation();}
                        });
                        return;
                    }
                    cycleId=cid;deviceId=id;lastSnapshot=verified;
                    lastFreshMs=SystemClock.elapsedRealtime();
                    message("WARMUP","المحاولة التالية • الإحماء 60 ثانية",cid);
                    main.post(tick);observer.refreshed();
                });
            }catch(Exception ex){
                failWorker(cid,ex.getMessage(),gas);
                if(disposed)MeasurementSessionRegistry.endFieldOperation();
                else main.post(()->end("FAILED","فشلت إعادة القياس: "+ex.getMessage(),cid));
            }
        });
    }

    public void approve(String id){
        if(disposed||busy)return;
        if(!host.database().getMeta(RELAY_RECOVERY_FAULT_KEY,"").isEmpty()||
           !host.database().getMeta(RELAY_EXPECTED_DEVICE_KEY,"").isEmpty()){
            message("SAFETY_FAULT",
                "الاعتماد محظور: إطفاء ريلاي MQ غير مؤكد بعد انقطاع سابق","");return;
        }
        busy=true;
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
                        FieldCycleRepository.Cycle linked=repo.byId(cycleId);
                        if(linked==null||!FieldCycleRepository.WARMUP.equals(linked.state)||
                           !deviceId.equals(linked.deviceId))
                            throw new IllegalStateException("فقد ملكية جلسة القياس أو هوية ESP32");
                        if(!MeasurementSessionRegistry.fieldCaptureRequested()){
                            if(!MeasurementSessionRegistry.requestFieldCapture(linked.fieldSessionId))
                                throw new IllegalStateException("مسجل القياسات مشغول");
                        }else if(!linked.fieldSessionId.equals(
                            MeasurementSessionRegistry.fieldCaptureOwnerSessionId())){
                            throw new IllegalStateException("اختلاف الجلسة المالكة لتسجيل العينات");
                        }
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
                                // Source archive plus immutable Next decision are
                                // sealed BEFORE requesting MQ power-off. Require
                                // physical ESP32 feedback before review approval.
                                if(gas){
                                    try{
                                        host.robotApi().mqPower(false);
                                        JSONObject after=host.robotApi().snapshot(0,0);
                                        if(!deviceId.equals(identity(after))||!relayOffAcknowledged(after))
                                            throw new IllegalStateException(
                                                "MQ_RELAY_SHUTDOWN_ACK_MISSING_OR_STILL_ON");
                                        host.database().putMeta(RELAY_EXPECTED_DEVICE_KEY,"");
                                    }catch(Exception off){
                                        String fault="MQ_RELAY_SHUTDOWN_UNCONFIRMED: "+
                                            off.getClass().getSimpleName()+": "+off.getMessage();
                                        // Seal the safety lock alongside the archived
                                        // failure. New scans remain blocked until the
                                        // device positively acknowledges relay OFF.
                                        host.database().putMeta(RELAY_RECOVERY_FAULT_KEY,fault);
                                        boolean isolated=repo.markUnsafeReview(cycleId,fault);
                                        android.util.Log.e("TankFieldCycle",
                                            "Cannot acknowledge MQ relay shutdown",off);
                                        final String notice=isolated?
                                            "حُفظت البيانات لكن تعذر تأكيد إطفاء ريلاي MQ؛ "+
                                            "الدورة غير قابلة للاعتماد. افصل طاقة الحساسات بأمان":
                                            "خطر: تعذر تأكيد إطفاء MQ أو حظر الاعتماد؛ "+
                                            "أوقف طاقة الحساسات يدويًا وراجع النظام";
                                        main.post(()->end("FAILED",notice,cycleId));
                                        return;
                                    }
                                }
                                main.post(()->end("REVIEW_READY",
                                    "اكتملت الدورة وحُفظ قرار Next وتأكد إطفاء MQ؛ اختر الاعتماد أو الإعادة",cycleId));
                                return;
                            }
                            // Distinguish missing Next scientific receipt
                            // from insufficient raw scans / telemetry quality.
                            FieldCycleRepository.Cycle failedLink=repo.byId(cycleId);
                            String fid=failedLink==null?"":failedLink.measurementId;
                            String nextFailure=fid.isEmpty()?"":host.database().getMeta(
                                "v8_field_next_failure_"+fid,"");
                            if(!nextFailure.isEmpty())
                                throw new IllegalStateException(
                                    "FIELD_NEXT_DECISION_NOT_PERSISTED: "+nextFailure);
                            throw new IllegalStateException(
                                "FIELD_COMPLETION_GATE_FAILED: duration, raw cycles, mode or Next receipt");
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
        // Keep the controller locked while the worker persists failure and
        // obtains a positive OFF acknowledgement from the physical device.
        main.removeCallbacks(tick);
        try{
            host.io().execute(()->{
                boolean unsafeRelay=failWorker(cid,reason,turnOff);
                final String message=unsafeRelay?
                    reason+" • خطر: لم يُؤكد إطفاء ريلاي MQ؛ أوقف الطاقة بأمان":
                    reason;
                main.post(()->end(unsafeRelay?"SAFETY_FAULT":"FAILED",message,cid));
            });
        }catch(RuntimeException unavailable){
            android.util.Log.e("TankFieldCycle","Cannot schedule safe measurement abort",unavailable);
            if(turnOff){
                try{host.database().putMeta(RELAY_RECOVERY_FAULT_KEY,
                    "MQ_RELAY_ABORT_WORKER_UNAVAILABLE: "+unavailable.getClass().getSimpleName());}
                catch(Exception dbFailure){android.util.Log.e("TankFieldCycle",
                    "Unable to retain relay safety lock",dbFailure);}
            }
            MeasurementSessionRegistry.requestFieldStop("error");
            end(turnOff?"SAFETY_FAULT":"FAILED",
                turnOff?"تعذر جدولة إيقاف القياس؛ حالة ريلاي MQ غير مؤكدة، افصل الطاقة بأمان":
                    "تعذر إنهاء دورة القياس",cid);
        }
    }
    /** Fail closed even when cancellation or Activity destruction interrupted
     * an MQ capture. A successful HTTP power-off command is NOT device ACK.
     * Persist the unsafe state for the next Activity/process and prevent a new
     * measurement or approval until recoverInterrupted verifies OFF.
     * Return true when physical OFF remains unconfirmed.
     */
    private boolean failWorker(String id,String reason,boolean turnOff){
        if(MeasurementSessionRegistry.fieldCaptureRequested()){
            if(MeasurementSessionRegistry.activeSessionId().isEmpty())
                MeasurementSessionRegistry.clearFieldCapture();
            else MeasurementSessionRegistry.requestFieldStop("error");
        }
        if(id!=null&&!id.isEmpty())try{repo.fail(id,reason);}catch(Exception failure){
            android.util.Log.e("TankFieldCycle","Unable to persist failed field cycle",failure);
        }
        if(!turnOff)return false;
        try{
            host.robotApi().mqPower(false);
            JSONObject off=host.robotApi().snapshot(0,0);
            String acknowledgedDevice=identity(off);
            if(acknowledgedDevice.isEmpty()||!relayOffAcknowledged(off))
                throw new IllegalStateException("MQ_RELAY_OFF_ACK_ABSENT");
            // If a cycle has a recorded device, a different ESP32 cannot
            // acknowledge this cycle's relay shutdown.
            FieldCycleRepository.Cycle original=
                id==null||id.isEmpty()?null:repo.byId(id);
            if(original!=null&&!original.deviceId.isEmpty()&&
                !original.deviceId.equals(acknowledgedDevice))
                throw new IllegalStateException("MQ_RELAY_OFF_ACK_WRONG_DEVICE");
            String expected=host.database().getMeta(RELAY_EXPECTED_DEVICE_KEY,"");
            if(!expected.isEmpty()&&!expected.equals(acknowledgedDevice))
                throw new IllegalStateException("MQ_RELAY_OFF_ACK_WRONG_DEVICE");
            host.database().putMeta(RELAY_EXPECTED_DEVICE_KEY,"");
            return false;
        }catch(Exception fault){
            String detail="MQ_RELAY_SHUTDOWN_UNCONFIRMED: "+
                fault.getClass().getSimpleName()+": "+fault.getMessage();
            android.util.Log.e("TankFieldCycle",
                "MQ relay not confirmed OFF while failing field measurement",fault);
            host.database().putMeta(RELAY_RECOVERY_FAULT_KEY,detail);
            return true;
        }
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
    /** Confirm actual firmware feedback rather than treating a missing relay
     * object or an invalid value as OFF. Either 'on' or 'mask' may be present
     * on an older firmware, but all fields provided must explicitly agree.
     * No OFF acknowledgement means the measurement remains unapprovable.
     */
    private static boolean relayOffAcknowledged(JSONObject d){
        JSONObject r=d==null?null:d.optJSONObject("relay");
        if(r==null||(!r.has("on")&&!r.has("mask")))return false;
        if(r.has("on")){
            Object state=r.opt("on");
            if(!(state instanceof Boolean)||((Boolean)state))return false;
        }
        if(r.has("mask")){
            Object mask=r.opt("mask");
            if(!(mask instanceof Number)||!Double.isFinite(((Number)mask).doubleValue())||
               ((Number)mask).doubleValue()!=0.0)return false;
        }
        return true;
    }
    /** An ON acknowledgement must be just as explicit as OFF. A mismatched
     * boolean/mask pair, cached malformed JSON or missing field cannot start
     * a 120-second acquisition against an unconfirmed heater state.
     */
    private static boolean relayOn(JSONObject d){
        JSONObject r=d==null?null:d.optJSONObject("relay");
        if(r==null||(!r.has("on")&&!r.has("mask")))return false;
        if(r.has("on")){
            Object state=r.opt("on");
            if(!(state instanceof Boolean)||!((Boolean)state))return false;
        }
        if(r.has("mask")){
            Object mask=r.opt("mask");
            if(!(mask instanceof Number)||!Double.isFinite(((Number)mask).doubleValue())||
               ((Number)mask).doubleValue()!=1.0)return false;
        }
        return true;
    }
    private static String identity(JSONObject d){return d==null?"":d.optString("device_id","").trim();}
    /** Reject nonexistent PM rather than saving a fictitious zero concentration. */
    private static boolean particlesAvailable(JSONObject d){
        JSONObject pm=d==null?null:d.optJSONObject("particles");
        return pm!=null&&pm.optBoolean("installed",false)&&pm.optBoolean("ok",false);
    }
}
