package com.servoc.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import java.util.ArrayList;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.SiteSummaryExportActivity;
import com.servoc.app.field.FieldSessionRepository;
import com.servoc.app.field.FieldCycleRepository;
import com.servoc.app.measurement.FieldCycleController;

import java.util.List;

/** User-only site and field-session workspace. Developer/lab captures are intentionally absent. */
public final class FieldSessionsTab {
    private final HazRobHost host;
    private final FieldSessionRepository repo;
    private final FieldCycleRepository cycles;
    private final FieldCycleController cycleDriver;
    private final ScrollView scroll;
    private final LinearLayout sites,sessions,reportSessionList;
    private final TextView reportScope;
    private final Spinner siteDropdown,sessionDropdown;
    private boolean selectingScope=false,refreshing=false,refreshQueued=false;
    private volatile boolean disposed=false;
    private final Handler main=new Handler(Looper.getMainLooper());
    private String selectedSessionId="";
    // IDs are bound to each Spinner adapter; a delayed selection never queries a changed DB cursor.
    private final List<String> siteOptionIds=new ArrayList<>(),sessionOptionIds=new ArrayList<>();
    // Reusing unchanged adapters prevents posted Spinner events from triggering a reload loop.
    private final List<String> boundSiteIds=new ArrayList<>(),boundSessionIds=new ArrayList<>(),boundSiteLabels=new ArrayList<>(),boundSessionLabels=new ArrayList<>();
    private final TextView state,selectedSiteSummary;
    private final Button siteReportPdf,siteReportXlsx,deleteSite;
    private final Button beginMeasurement,retryMeasurement,approveMeasurement,deleteMeasurement;
    private final CheckBox gasCheck,particleCheck,physicalCheck;
    private final TextView cycleStatus,failureDetails;
    private String latestCycleId="";
    private boolean selectedCanBegin=false,selectedHasApprovedMeasurement=false;
    private boolean selectedHasReviewMeasurement=false;
    private boolean relayRecoverySafetyFault=false;
    private String selectedSiteId="";

    public FieldSessionsTab(Context c,HazRobHost host){
        this.host=host;this.repo=new FieldSessionRepository(host.database());
        this.cycles=new FieldCycleRepository(host.database());
        this.cycleDriver=new FieldCycleController(host,new FieldCycleController.Observer(){
            public void changed(String state,String message,String id){
                main.post(()->{if(disposed)return;
                    if("REVIEW_READY".equals(state)&&id!=null&&!id.isEmpty())latestCycleId=id;
                    // A live safety fault must lock controls immediately,
                    // not only after the asynchronous SQLite refresh returns.
                    if("SAFETY_FAULT".equals(state))relayRecoverySafetyFault=true;
                    if("SAFETY_FAULT".equals(state)||"FAILED".equals(state)){
                        failureDetails.setText(("SAFETY_FAULT".equals(state)?
                            "تعذر تأكيد أمان ريلاي MQ: ":"سبب فشل قياس الجلسة: ")+message);
                        failureDetails.setVisibility(View.VISIBLE);
                    }
                    cycleStatus.setText(message);renderCycleButtons(state);
                    if("BLOCKED".equals(state)||"SAFETY_FAULT".equals(state))refresh();
                });
            }
            public void refreshed(){main.post(()->{if(!disposed)refresh();});}
        });
        scroll=new ScrollView(c);scroll.setFillViewport(true);
        LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));root.setBackgroundColor(0xFFF7F8FA);scroll.addView(root);
        root.addView(UiKit.title(c,"المواقع والجلسات"),UiKit.full(c));
        LinearLayout nav=UiKit.hbox(c);
        Button manageButton=UiKit.button(c,"🗂 إدارة المواقع والجلسات");
        Button reportButton=UiKit.button(c,"📄 التقارير المعتمدة");
        nav.addView(manageButton,UiKit.weight(1));nav.addView(reportButton,UiKit.weight(1));
        root.addView(nav,UiKit.full(c));
        LinearLayout managePane=UiKit.vbox(c),reportPane=UiKit.vbox(c);
        root.addView(managePane,UiKit.full(c));root.addView(reportPane,UiKit.full(c));
        reportPane.setVisibility(View.GONE);
        manageButton.setOnClickListener(v->{managePane.setVisibility(View.VISIBLE);reportPane.setVisibility(View.GONE);UiKit.primary(manageButton);UiKit.secondary(reportButton);});
        reportButton.setOnClickListener(v->{managePane.setVisibility(View.GONE);reportPane.setVisibility(View.VISIBLE);UiKit.secondary(manageButton);UiKit.primary(reportButton);refresh();});
        UiKit.primary(manageButton);UiKit.secondary(reportButton);
        reportScope=UiKit.small(c,"اختر موقعًا من تبويب الإدارة لعرض تقاريره المعتمدة");
        reportPane.addView(reportScope,UiKit.full(c));
        reportSessionList=UiKit.vbox(c);

        state=UiKit.text(c,"لا توجد جلسة ميدانية نشطة");state.setBackgroundColor(0xFFEDEFF2);managePane.addView(state,UiKit.full(c));

        LinearLayout actions=UiKit.hbox(c);
        Button addSite=UiKit.button(c,"إضافة موقع");addSite.setOnClickListener(v->ask(c,"اسم الموقع","",name->host.io().execute(()->{
            try{
                final String id=repo.createSite(name,"");
                main.post(()->{selectedSiteId=id;selectedSessionId="";host.showToast("تم إنشاء الموقع");refresh();});
            }catch(Exception e){host.showToast("تعذر إنشاء الموقع");}
        })));actions.addView(addSite,UiKit.weight(1));
        Button addSession=UiKit.button(c,"استحداث جلسة قياس");addSession.setOnClickListener(v->{if(selectedSiteId.isEmpty()){host.showToast("اختر موقعًا أولًا");return;}ask(c,"اسم الجلسة","",name->{
            final String site=selectedSiteId;
            host.io().execute(()->{
                try{
                    final String id=repo.startFieldSession(site,name);
                    main.post(()->{selectedSiteId=site;selectedSessionId=id;host.showToast("بدأت الجلسة الميدانية");refresh();});
                }catch(Exception e){host.showToast("تعذر بدء الجلسة: "+e.getMessage());}
            });
        });});actions.addView(addSession,UiKit.weight(1));
        managePane.addView(actions,UiKit.full(c));

        TextView localNote=UiKit.small(c,"المواقع والجلسات محفوظة محليًا داخل التطبيق. لا تُرفع بيانات جلسات المستخدم إلى المكتبة العلمية المركزية. إدارة المزامنة والنشر محصورة في واجهات المطور.");localNote.setTextColor(0xFF505966);managePane.addView(localNote,UiKit.full(c));
        managePane.addView(UiKit.title(c,"مكتبة المواقع والجلسات"),UiKit.full(c));
        sites=UiKit.vbox(c);sites.setVisibility(View.GONE);
        managePane.addView(UiKit.small(c,"اختر موقعًا من القائمة"),UiKit.full(c));
        siteDropdown=new Spinner(c);UiKit.outlinedSpinner(siteDropdown);managePane.addView(siteDropdown,UiKit.full(c));
        siteDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){
                if(selectingScope||pos!=siteDropdown.getSelectedItemPosition())return;
                if(pos>=0&&pos<siteOptionIds.size()&&!siteOptionIds.get(pos).equals(selectedSiteId)){
                    selectedSiteId=siteOptionIds.get(pos);selectedSessionId="";refresh();
                }
            }
            public void onNothingSelected(AdapterView<?> p){}
        });
        selectedSiteSummary=UiKit.text(c,"اختر موقعًا");selectedSiteSummary.setBackgroundColor(0xFFEDEFF2);managePane.addView(selectedSiteSummary,UiKit.full(c));
        LinearLayout siteExports=UiKit.hbox(c);
        siteReportPdf=UiKit.button(c,"تقرير الموقع PDF");siteReportPdf.setEnabled(false);siteReportPdf.setOnClickListener(v->{if(selectedSiteId.isEmpty()){host.showToast("اختر موقعًا أولًا");return;}SiteSummaryExportActivity.start(scroll.getContext(),selectedSiteId,"pdf");});siteExports.addView(siteReportPdf,UiKit.weight(1));
        siteReportXlsx=UiKit.button(c,"Excel ذكي للموقع");siteReportXlsx.setEnabled(false);siteReportXlsx.setOnClickListener(v->{if(selectedSiteId.isEmpty()){host.showToast("اختر موقعًا أولًا");return;}SiteSummaryExportActivity.start(scroll.getContext(),selectedSiteId,"xlsx");});siteExports.addView(siteReportXlsx,UiKit.weight(1));
        reportPane.addView(siteExports,UiKit.full(c));
        deleteSite=UiKit.button(c,"حذف الموقع الفارغ");deleteSite.setEnabled(false);
        deleteSite.setOnClickListener(v->{if(selectedSiteId.isEmpty())return;
            final String id=selectedSiteId;new AlertDialog.Builder(c)
                .setTitle("حذف الموقع").setMessage("سيُحذف الموقع الفارغ نهائيًا من مكتبة HazRob. يجب حذف جميع جلساته أولًا.")
                .setNegativeButton("إلغاء",null).setPositiveButton("حذف", (d,w)->host.deleteEmptyUserSite(id)).show();
        });managePane.addView(deleteSite,UiKit.full(c));
        managePane.addView(UiKit.small(c,"جلسات الموقع المحدد"),UiKit.full(c));
        sessionDropdown=new Spinner(c);UiKit.outlinedSpinner(sessionDropdown);managePane.addView(sessionDropdown,UiKit.full(c));
        sessionDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){
                if(selectingScope||selectedSiteId.isEmpty()||pos!=sessionDropdown.getSelectedItemPosition())return;
                if(pos>=0&&pos<sessionOptionIds.size()&&!sessionOptionIds.get(pos).equals(selectedSessionId)){
                    selectedSessionId=sessionOptionIds.get(pos);refresh();
                }
            }
            public void onNothingSelected(AdapterView<?> p){}
        });
        LinearLayout cycleCard=UiKit.card(c);
        cycleCard.addView(UiKit.title(c,"قياس خاص بالجلسة المحددة"),UiKit.full(c));
        cycleCard.addView(UiKit.small(c,
            "كل جلسة لها قياس نهائي واحد فقط. يمكنك إعادة المحاولة قبل الاعتماد، "+
            "وتبقى المحاولات المرفوضة في سجل التدقيق. بعد تأكيد الريلاي: "+
            "60 ثانية إحماء ثم 60 ثانية لجمع عينات فعلية. "+
            "زر «اعتماد قياس الجلسة» يعتمد القياس والجلسة معًا."),
            UiKit.full(c));
        LinearLayout families=UiKit.hbox(c);
        gasCheck=measurementCheck(c,"غازات",true);
        particleCheck=measurementCheck(c,"جزيئات",false);
        physicalCheck=measurementCheck(c,"فيزيائية",true);
        families.addView(gasCheck,UiKit.weight(1));
        families.addView(particleCheck,UiKit.weight(1));
        families.addView(physicalCheck,UiKit.weight(1));
        cycleCard.addView(families,UiKit.full(c));
        cycleStatus=UiKit.text(c,"اختر جلسة غير معتمدة لبدء قياسها الوحيد");
        cycleStatus.setTextColor(0xFF17324A);
        cycleCard.addView(cycleStatus,UiKit.full(c));
        failureDetails=UiKit.text(c,"");
        failureDetails.setTextColor(0xFFC62828);
        failureDetails.setVisibility(View.GONE);
        cycleCard.addView(failureDetails,UiKit.full(c));
        LinearLayout cycleActions=UiKit.hbox(c);
        beginMeasurement=UiKit.button(c,"▶ بدء القياس");
        beginMeasurement.setEnabled(false);
        beginMeasurement.setOnClickListener(v->{
            if(selectedSessionId.isEmpty()||selectedHasApprovedMeasurement||!selectedCanBegin)return;
            final String id=selectedSessionId;
            try{
                FieldCycleRepository.Options opts=new FieldCycleRepository.Options(
                    gasCheck.isChecked(),particleCheck.isChecked(),physicalCheck.isChecked());
                cycleDriver.begin(id,opts);
                if(cycleDriver.busy())renderCycleButtons("ARMING");
            }catch(IllegalArgumentException ex){host.showToast("اختر نوع قياس واحدًا على الأقل");}
        });
        cycleActions.addView(beginMeasurement,UiKit.weight(1.1f));
        retryMeasurement=UiKit.button(c,"↻ إعادة القياس");retryMeasurement.setEnabled(false);
        retryMeasurement.setOnClickListener(v->{
            if(latestCycleId.isEmpty())return;
            cycleDriver.retry(latestCycleId);if(cycleDriver.busy())renderCycleButtons("ARMING");
        });
        cycleActions.addView(retryMeasurement,UiKit.weight(1));
        approveMeasurement=UiKit.button(c,"✓ اعتماد قياس الجلسة");approveMeasurement.setEnabled(false);
        approveMeasurement.setOnClickListener(v->{
            if(latestCycleId.isEmpty()||selectedHasApprovedMeasurement)return;
            final String cycle=latestCycleId;
            new AlertDialog.Builder(c).setTitle("اعتماد قياس الجلسة الوحيد")
                .setMessage("سيتم التحقق من اكتمال القياس وحفظه ثم اعتماد الجلسة تلقائيًا "+
                    "في عملية واحدة. لا يمكن إجراء قياس آخر لهذه الجلسة بعد الاعتماد. "+
                    "هل تريد اعتماد القياس الحالي؟")
                .setNegativeButton("إلغاء",null)
                .setPositiveButton("اعتماد القياس",(d,w)->{
                    if(selectedHasApprovedMeasurement)return;
                    cycleDriver.approve(cycle);
                    if(cycleDriver.busy())renderCycleButtons("ARMING");
                }).show();
        });
        cycleActions.addView(approveMeasurement,UiKit.weight(1));
        cycleCard.addView(cycleActions,UiKit.full(c));
        deleteMeasurement=UiKit.button(c,"حذف المحاولة غير المعتمدة");
        deleteMeasurement.setEnabled(false);
        deleteMeasurement.setOnClickListener(v->{
            if(latestCycleId.isEmpty())return;
            final String id=latestCycleId;
            new AlertDialog.Builder(c).setTitle("حذف قياس غير معتمد")
                .setMessage("سيُحذف محتوى القياس وعيناته ونتائجه غير المعتمدة، "+
                    "مع الإبقاء على رقم المحاولة وسجل الحذف للتتبع. "+
                    "لا يمكن حذف قياس معتمد أو مستخدم كمرجع علمي.")
                .setNegativeButton("إلغاء",null)
                .setPositiveButton("حذف القياس",(d,w)->{
                    cycleDriver.delete(id);
                    if(cycleDriver.busy())renderCycleButtons("DELETING");
                }).show();
        });
        cycleCard.addView(deleteMeasurement,UiKit.full(c));
        managePane.addView(cycleCard,UiKit.full(c));

        sessions=UiKit.vbox(c);managePane.addView(sessions,UiKit.full(c));
        reportPane.addView(UiKit.title(c,"تقارير الجلسات المعتمدة"),UiKit.full(c));
        reportPane.addView(reportSessionList,UiKit.full(c));
        cycleDriver.recoverInterrupted();
    }

    public View view(){return scroll;}

    /** Drop pending screen updates after the owning Activity closes. */
    public void release(){
        disposed=true;
        refreshQueued=false;
        main.removeCallbacksAndMessages(null);
        cycleDriver.release();
    }

    /** Immutable DB read snapshot. All SQLite joins run off the UI thread. */
    private static final class SessionData{
        final FieldSessionRepository.FieldSession active;
        final List<FieldSessionRepository.Site> sites;
        final String siteId;
        final List<FieldSessionRepository.FieldSession> children,approved;
        final java.util.Map<String,Integer> counts;
        final List<FieldCycleRepository.Cycle> cycleRows;
        final String relaySafetyFault;
        SessionData(FieldSessionRepository.FieldSession a,List<FieldSessionRepository.Site> ss,
                String id,List<FieldSessionRepository.FieldSession> c,
                List<FieldSessionRepository.FieldSession> p,java.util.Map<String,Integer> n,
                List<FieldCycleRepository.Cycle> rows,String relayFault){
            active=a;sites=ss;siteId=id;children=c;approved=p;counts=n;cycleRows=rows;
            relaySafetyFault=relayFault==null?"":relayFault;
        }
    }

    public void refresh(){
        if(disposed)return;
        if(Looper.myLooper()!=Looper.getMainLooper()){
            main.post(this::refresh);return;
        }
        if(refreshing){refreshQueued=true;return;}
        refreshing=true;
        final String requestedSite=selectedSiteId;
        final String requestedSession=selectedSessionId;
        try{
            host.io().execute(()->{
                if(disposed)return;
                SessionData snapshot=null;RuntimeException failure=null;
                try{
                    repo.verifySessionSchema();
                    FieldSessionRepository.FieldSession active=repo.activeFieldSession();
                    List<FieldSessionRepository.Site> ss=repo.sites();
                    String id=requestedSite;
                    boolean exists=false;
                    for(FieldSessionRepository.Site q:ss)if(q.id.equals(id)){exists=true;break;}
                    if(!exists)id=ss.isEmpty()?"":ss.get(0).id;
                    List<FieldSessionRepository.FieldSession> children=id.isEmpty()
                            ?new ArrayList<>():repo.sessionsForSite(id);
                    List<FieldSessionRepository.FieldSession> approved=id.isEmpty()
                            ?new ArrayList<>():repo.approvedSessionsForSite(id);
                    java.util.Map<String,Integer> counts=id.isEmpty()
                            ?new java.util.HashMap<>():repo.measurementCountsForSite(id);
                    List<FieldCycleRepository.Cycle> rows=requestedSession.isEmpty()
                        ?new ArrayList<>():cycles.forSession(requestedSession);
                    String relayFault=host.database().getMeta(
                        FieldCycleController.RELAY_RECOVERY_FAULT_KEY,"");
                    String pendingDevice=host.database().getMeta(
                        FieldCycleController.RELAY_EXPECTED_DEVICE_KEY,"");
                    if(relayFault.isEmpty()&&!pendingDevice.isEmpty())
                        relayFault="MQ_RELAY_OFF_ACK_PENDING_FOR_DEVICE: "+pendingDevice;
                    snapshot=new SessionData(active,ss,id,children,approved,counts,rows,
                        relayFault);
                }catch(RuntimeException ex){failure=ex;}
                final SessionData result=snapshot;
                final RuntimeException error=failure;
                if(disposed)return;
                main.post(()->{
                    if(disposed){refreshing=false;return;}
                    try{
                        if(error!=null){
                            android.util.Log.e("TankSessions","Unable to load user sessions",error);
                            showSessionLoadError();
                        }else if(!requestedSite.equals(selectedSiteId)||!requestedSession.equals(selectedSessionId)){
                            refreshQueued=true; // A newer site selection supersedes the DB snapshot.
                        }else{
                            renderSessionUi(result);
                            // The first valid session may be selected DURING
                            // rendering. Its cycle rows were queried for the
                            // previous session; do not leave stale approval or
                            // Start buttons enabled during the requery.
                            if(!requestedSession.equals(selectedSessionId)||
                               !requestedSite.equals(selectedSiteId)){
                                latestCycleId="";
                                selectedCanBegin=false;
                                beginMeasurement.setEnabled(false);
                                retryMeasurement.setEnabled(false);
                                approveMeasurement.setEnabled(false);
                                deleteMeasurement.setEnabled(false);
                                gasCheck.setEnabled(false);
                                particleCheck.setEnabled(false);
                                physicalCheck.setEnabled(false);
                                cycleStatus.setText("جارٍ تحميل قياس الجلسة المختارة والتحقق من حالته…");
                                refreshQueued=true;
                            }
                        }
                    }catch(RuntimeException ex){
                        android.util.Log.e("TankSessions","Unable to render user sessions",ex);
                        showSessionLoadError();
                    }finally{
                        refreshing=false;
                        if(refreshQueued){refreshQueued=false;refresh();}
                    }
                });
            });
        }catch(RuntimeException ex){
            refreshing=false;
            android.util.Log.e("TankSessions","Cannot schedule user session load",ex);
            showSessionLoadError();
        }
    }

    /** Preserve local data and keep navigation alive if a migrated DB is not readable. */
    private void showSessionLoadError(){
        siteOptionIds.clear();sessionOptionIds.clear();boundSiteIds.clear();boundSessionIds.clear();boundSiteLabels.clear();boundSessionLabels.clear();selectingScope=false;
        sites.removeAllViews();sessions.removeAllViews();reportSessionList.removeAllViews();
        state.setText("تعذر تحميل المواقع والجلسات. البيانات المحلية لم تُحذف. أعد فتح التطبيق، ثم راجع النسخة الاحتياطية أو سجل التشخيص.");
        state.setTextColor(0xFFC62828);
        selectedSiteSummary.setText("تعذر قراءة بيانات الجلسات");
        reportScope.setText("تقارير الجلسات غير متاحة حتى إصلاح قاعدة البيانات");
        siteReportPdf.setEnabled(false);siteReportXlsx.setEnabled(false);
        deleteSite.setEnabled(false);siteDropdown.setEnabled(false);sessionDropdown.setEnabled(false);
        clearCycleSelection(relayRecoverySafetyFault?
            "حالة ريلاي MQ كانت غير مؤكدة قبل فشل قراءة قاعدة البيانات":"");
        cycleStatus.setText("تعذر التحقق من جلسات القياس؛ جميع أوامر البدء والاعتماد معطلة");
    }
    private void renderSessionUi(SessionData data){
        FieldSessionRepository.FieldSession active=data.active;
        state.setText("تخزين محلي • جلسة واحدة = قياس واحد معتمد"+"\n"+
            (active==null?"لا توجد جلسة ميدانية بانتظار القياس":
                "الجلسة الحالية: "+active.name+
                " • تعتمد تلقائيًا عند اعتماد قياسها الوحيد"));
        state.setTextColor(active==null?0xFF985000:0xFF167443);
        sites.removeAllViews();List<FieldSessionRepository.Site> ss=data.sites;
        if(ss.isEmpty()){siteOptionIds.clear();sessionOptionIds.clear();reportScope.setText("لا توجد مواقع أو جلسات معتمدة");reportSessionList.removeAllViews();selectedSiteId="";selectedSessionId="";selectingScope=true;
            if(siteDropdown.getAdapter()==null||!boundSiteIds.isEmpty()){siteDropdown.setAdapter(UiKit.lightOptions(scroll.getContext(),new String[]{"لا توجد مواقع"}));boundSiteIds.clear();boundSiteLabels.clear();}
            if(sessionDropdown.getAdapter()==null||!boundSessionIds.isEmpty()){sessionDropdown.setAdapter(UiKit.lightOptions(scroll.getContext(),new String[]{"لا توجد جلسات"}));boundSessionIds.clear();boundSessionLabels.clear();}
            selectingScope=false;siteReportPdf.setEnabled(false);siteReportXlsx.setEnabled(false);deleteSite.setEnabled(false);selectedSiteSummary.setText("لا توجد مواقع محفوظة.");sites.addView(UiKit.small(sites.getContext(),"لا توجد مواقع محفوظة."),UiKit.full(sites.getContext()));sessions.removeAllViews();clearCycleSelection(data.relaySafetyFault);return;}
        boolean exists=false;for(FieldSessionRepository.Site s:ss)if(s.id.equals(selectedSiteId))exists=true;if(!exists)selectedSiteId=ss.get(0).id;
        FieldSessionRepository.Site selected=null;int siteIndex=0;
        ArrayList<String> siteNames=new ArrayList<>();siteOptionIds.clear();for(int i=0;i<ss.size();i++){FieldSessionRepository.Site s=ss.get(i);siteNames.add(s.name);siteOptionIds.add(s.id);if(s.id.equals(selectedSiteId)){selected=s;siteIndex=i;}}
        selectingScope=true;
        if(!boundSiteIds.equals(siteOptionIds)||!boundSiteLabels.equals(siteNames)){
            siteDropdown.setAdapter(UiKit.lightOptions(scroll.getContext(),siteNames.toArray(new String[0])));
            boundSiteIds.clear();boundSiteIds.addAll(siteOptionIds);boundSiteLabels.clear();boundSiteLabels.addAll(siteNames);
        }
        if(siteDropdown.getSelectedItemPosition()!=siteIndex)siteDropdown.setSelection(siteIndex);
        siteDropdown.setEnabled(true);selectingScope=false;
        List<FieldSessionRepository.FieldSession> children=data.children;java.util.Map<String,Integer> measureCounts=data.counts;int measurementCount=0;for(Integer count:measureCounts.values())measurementCount+=count;selectedSiteSummary.setText("الموقع: "+(selected==null?selectedSiteId:selected.name)+
            "\nالجلسات: "+children.size()+" • القياسات المعتمدة: "+data.approved.size()+
            " (قياس واحد لكل جلسة)"+
            "\nالمحاولات الفنية المؤرشفة: "+measurementCount+
            " • تقرير الموقع يقارن الجلسات المستقلة دون دمج PPM.");List<FieldSessionRepository.FieldSession> approved=data.approved;boolean hasApproved=!approved.isEmpty();siteReportPdf.setEnabled(hasApproved);siteReportXlsx.setEnabled(hasApproved);deleteSite.setEnabled(children.isEmpty());reportScope.setText("الموقع: "+(selected==null?selectedSiteId:selected.name)+" • جلسات معتمدة: "+approved.size());
        ArrayList<String> names=new ArrayList<>();sessionOptionIds.clear();int chosen=0;boolean found=false;
        for(int i=0;i<children.size();i++){FieldSessionRepository.FieldSession q=children.get(i);names.add(q.name+" • "+q.displayStatus()+(q.active()?" (نشطة)":""));sessionOptionIds.add(q.id);if(q.id.equals(selectedSessionId)){found=true;chosen=i;}}
        if(!found)selectedSessionId=children.isEmpty()?"":children.get(0).id;
        selectingScope=true;
        if(!boundSessionIds.equals(sessionOptionIds)||!boundSessionLabels.equals(names)){
            sessionDropdown.setAdapter(UiKit.lightOptions(scroll.getContext(),children.isEmpty()?new String[]{"لا توجد جلسات"}:names.toArray(new String[0])));
            boundSessionIds.clear();boundSessionIds.addAll(sessionOptionIds);boundSessionLabels.clear();boundSessionLabels.addAll(names);
        }
        if(sessionDropdown.getSelectedItemPosition()!=chosen)sessionDropdown.setSelection(chosen);
        sessionDropdown.setEnabled(!children.isEmpty());selectingScope=false;
        sessions.removeAllViews();if(children.isEmpty()){reportSessionList.removeAllViews();reportSessionList.addView(UiKit.small(scroll.getContext(),"لا توجد جلسات معتمدة لإصدار تقارير."),UiKit.full(scroll.getContext()));sessions.addView(UiKit.small(sessions.getContext(),"لا توجد جلسات تحت هذا الموقع."),UiKit.full(sessions.getContext()));clearCycleSelection(data.relaySafetyFault);return;}
        renderCycleWorkspace(data);
        reportSessionList.removeAllViews();
        if(approved.isEmpty())reportSessionList.addView(UiKit.small(scroll.getContext(),"لا توجد جلسات معتمدة لإصدار تقارير."),UiKit.full(scroll.getContext()));
        for(FieldSessionRepository.FieldSession q:approved){
            Button viewReport=UiKit.button(scroll.getContext(),"📄 "+q.name+" • تقرير الجلسة");
            viewReport.setOnClickListener(v->host.openReportsForSite(q.siteId,q.id));
            reportSessionList.addView(viewReport,UiKit.full(scroll.getContext()));
        }
        for(FieldSessionRepository.FieldSession q:children){if(!q.id.equals(selectedSessionId))continue;String status=q.displayStatus()+(q.active()?" (نشطة)":"");LinearLayout card=UiKit.card(sessions.getContext());TextView t=UiKit.text(sessions.getContext(),q.name+" • "+status+"\n"+q.startedTs+
            " • القياس المعتمد: "+(q.approved()?"1/1":"0/1")+
            " • المحاولات المسجلة: "+(measureCounts.containsKey(q.id)?measureCounts.get(q.id):0));t.setBackgroundColor(0xFFEDEFF2);card.addView(t,UiKit.full(sessions.getContext()));LinearLayout ex=UiKit.hbox(sessions.getContext());Button p=UiKit.button(sessions.getContext(),"تقرير الجلسة PDF • 4–5 صفحات أفقية + ملحق الدورات");p.setEnabled(q.approved());p.setOnClickListener(v->host.requestFieldSessionExport("pdf",q.id));ex.addView(p,UiKit.weight(1.35f));Button x=UiKit.button(sessions.getContext(),"Excel ذكي للجلسة");x.setEnabled(q.approved());x.setOnClickListener(v->host.requestFieldSessionExport("xlsx",q.id));ex.addView(x,UiKit.weight(.65f));card.addView(ex,UiKit.full(sessions.getContext()));
            Button remove=UiKit.button(sessions.getContext(),"حذف الجلسة وبياناتها");remove.setEnabled(!q.active()&&!q.approved());
            remove.setOnClickListener(v->new AlertDialog.Builder(sessions.getContext())
                .setTitle("حذف الجلسة نهائيًا").setMessage("ستُحذف الجلسة «"+q.name+"» وجميع قياساتها وعيناتها ونتائج ChemCore المرتبطة بها من التخزين المحلي للتطبيق وأي أرشيف محلي مرتبط. لا يمكن التراجع عن ذلك. التقارير المصدرة مسبقًا خارج التطبيق لن تُحذف.")
                .setNegativeButton("إلغاء",null).setPositiveButton("حذف نهائي",(d,w)->host.deleteUserFieldSession(q.id)).show());
            card.addView(remove,UiKit.full(sessions.getContext()));sessions.addView(card,UiKit.full(sessions.getContext()));}
    }

    private static CheckBox measurementCheck(Context c,String label,boolean checked){
        CheckBox item=new CheckBox(c);
        item.setText(label);item.setTextColor(0xFF15202B);
        item.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFF146B54));
        item.setBackground(UiKit.outline(c,0xFFFFFFFF,0xFF8291A2));
        item.setPadding(UiKit.dp(c,4),UiKit.dp(c,6),UiKit.dp(c,4),UiKit.dp(c,6));
        item.setChecked(checked);item.setMinHeight(UiKit.dp(c,48));
        return item;
    }

    /** Empty site/session scopes must never reuse the previous selected
     * measurement's cycle ID or action button state. Keep the persistent
     * relay fault visible even when no session is available.
     */
    private void clearCycleSelection(String relayFault){
        latestCycleId="";
        selectedCanBegin=false;
        selectedHasApprovedMeasurement=false;
        selectedHasReviewMeasurement=false;
        relayRecoverySafetyFault=relayFault!=null&&!relayFault.isEmpty();
        cycleStatus.setText(relayRecoverySafetyFault?
            "القياس محظور حتى تأكيد إطفاء ريلاي MQ من الروبوت الأصلي":
            "لا توجد جلسة قياس محددة في الموقع الحالي");
        if(relayRecoverySafetyFault){
            failureDetails.setText("تحذير ريلاي MQ: "+relayFault);
            failureDetails.setVisibility(View.VISIBLE);
        }else{
            failureDetails.setText("");
            failureDetails.setVisibility(View.GONE);
        }
        beginMeasurement.setEnabled(false);
        retryMeasurement.setEnabled(false);
        approveMeasurement.setEnabled(false);
        deleteMeasurement.setEnabled(false);
        gasCheck.setEnabled(false);
        particleCheck.setEnabled(false);
        physicalCheck.setEnabled(false);
    }

    private void renderCycleWorkspace(SessionData data){
        boolean active=false,approved=false;
        for(FieldSessionRepository.FieldSession f:data.children)
            if(f.id.equals(selectedSessionId)){
                active=f.active();approved=f.approved();break;
            }
        FieldCycleRepository.Cycle recent=null;
        boolean hasApprovedCycle=false,hasReview=false;
        for(FieldCycleRepository.Cycle c:data.cycleRows){
            if(recent==null||c.sequence>recent.sequence)recent=c;
            if(c.approved())hasApprovedCycle=true;
            if(c.reviewReady())hasReview=true;
        }
        latestCycleId=recent==null?"":recent.id;
        selectedHasApprovedMeasurement=approved||hasApprovedCycle;
        selectedHasReviewMeasurement=hasReview;
        relayRecoverySafetyFault=!data.relaySafetyFault.isEmpty();
        selectedCanBegin=active&&!selectedHasApprovedMeasurement&&!hasReview&&
            !relayRecoverySafetyFault;
        boolean available=!cycleDriver.busy();
        if(available){
            if(recent!=null&&FieldCycleRepository.FAILED.equals(recent.state)){
                String cause=recent.failReason==null?"":recent.failReason.trim();
                if(cause.isEmpty())cause="لا يوجد تفصيل مخزن لسبب الفشل؛ راجع سجل التشخيص";
                failureDetails.setText("سبب فشل قياس الجلسة: "+cause+
                    "\nلم يُعتمد هذا القياس. تحقق من السبب وأعد المحاولة.");
                failureDetails.setVisibility(View.VISIBLE);
            }else if(recent!=null&&FieldCycleRepository.CANCELLED.equals(recent.state)){
                failureDetails.setText("أُلغيت محاولة القياس دون اعتماد. يمكنك إعادة المحاولة.");
                failureDetails.setVisibility(View.VISIBLE);
            }else{
                failureDetails.setText("");
                failureDetails.setVisibility(View.GONE);
            }
            if(selectedHasApprovedMeasurement)
                cycleStatus.setText("قياس الجلسة معتمد (1/1). الجلسة أُغلقت آليًا، "+
                    "وأصبح تقريرها جاهزًا. افتح جلسة جديدة لقياس جديد.");
            else if(recent==null)
                cycleStatus.setText(active?"جلسة جديدة • قياس واحد بانتظار البدء":
                    "اختر جلسة قياس جديدة");
            else
                cycleStatus.setText("القياس الوحيد للجلسة • المحاولة #"+
                    recent.attempt+" • الحالة "+recent.state+
                    (hasReview?" • اختر الاعتماد أو إعادة المحاولة":"")+
                    (recent.measurementId.isEmpty()?"":" • المصدر "+recent.measurementId));
            if(relayRecoverySafetyFault){
                cycleStatus.setText("تحذير أمان: قياس الجلسة محظور حتى تأكيد إطفاء ريلاي MQ");
                failureDetails.setText("فشل تأكيد حالة ريلاي MQ بعد انقطاع سابق. "+
                    "أعد الاتصال بالروبوت ثم أعد فتح تبويب الجلسات للتحقق من حالة OFF. "+
                    "السبب الفني: "+data.relaySafetyFault);
                failureDetails.setVisibility(View.VISIBLE);
            }
        }
        beginMeasurement.setEnabled(available&&selectedCanBegin&&(
            recent==null||FieldCycleRepository.DELETED.equals(recent.state)||
            FieldCycleRepository.SUPERSEDED.equals(recent.state)));
        boolean review=recent!=null&&recent.reviewReady()&&available&&
            !selectedHasApprovedMeasurement;
        approveMeasurement.setEnabled(review&&active&&!relayRecoverySafetyFault);
        deleteMeasurement.setEnabled(available&&!selectedHasApprovedMeasurement&&
            recent!=null&&(recent.reviewReady()||
            FieldCycleRepository.FAILED.equals(recent.state)||
            FieldCycleRepository.CANCELLED.equals(recent.state)||
            FieldCycleRepository.SUPERSEDED.equals(recent.state)));
        retryMeasurement.setEnabled(available&&active&&!selectedHasApprovedMeasurement&&
            !relayRecoverySafetyFault&&
            recent!=null&&(recent.reviewReady()||
            FieldCycleRepository.FAILED.equals(recent.state)||
            FieldCycleRepository.CANCELLED.equals(recent.state)));
        gasCheck.setEnabled(available&&selectedCanBegin&&!relayRecoverySafetyFault);
        particleCheck.setEnabled(available&&selectedCanBegin&&!relayRecoverySafetyFault);
        physicalCheck.setEnabled(available&&selectedCanBegin&&!relayRecoverySafetyFault);
    }
    private void renderCycleButtons(String stage){
        boolean locked=relayRecoverySafetyFault||"SAFETY_FAULT".equals(stage)||
            "BLOCKED".equals(stage)||"DELETING".equals(stage)||
            cycleDriver.busy()||"ARMING".equals(stage)||
            "WARMUP".equals(stage)||"SAMPLING".equals(stage)||
            "SAMPLING_WAIT".equals(stage)||"FINALIZING".equals(stage);
        boolean ready="REVIEW_READY".equals(stage);
        // A delayed REVIEW_READY callback may belong to a session which the
        // operator has already left. Never enable approval for an unbound row.
        // End-of-cycle refreshed() re-reads the selected session's SQLite rows.
        boolean canApprove=!locked&&!selectedSessionId.isEmpty()&&
            selectedHasReviewMeasurement&&!selectedHasApprovedMeasurement&&
            !relayRecoverySafetyFault&&ready;
        beginMeasurement.setEnabled(!locked&&selectedCanBegin&&
            !relayRecoverySafetyFault&&latestCycleId.isEmpty());
        retryMeasurement.setEnabled(canApprove);
        approveMeasurement.setEnabled(canApprove);
        deleteMeasurement.setEnabled(canApprove);
        gasCheck.setEnabled(!locked&&selectedCanBegin&&!relayRecoverySafetyFault);
        particleCheck.setEnabled(!locked&&selectedCanBegin&&!relayRecoverySafetyFault);
        physicalCheck.setEnabled(!locked&&selectedCanBegin&&!relayRecoverySafetyFault);
    }

    private interface NameResult{void accept(String name);}
    private static void ask(Context c,String title,String initial,NameResult result){EditText e=UiKit.input(c,initial);new AlertDialog.Builder(c).setTitle(title).setView(e).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty())result.accept(n);}).show();}
}
