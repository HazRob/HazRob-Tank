package com.servoc.app.ui;

import android.content.Context;
import android.os.Looper;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.servoc.app.chemcore.ChemCoreAnalysisPresenter;
import com.servoc.app.chemcore.TankPpmEstimate;
import com.servoc.app.chemcore.ChemCoreDecisionRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.SessionSeries;
import com.servoc.app.field.FieldSessionRepository;
import com.servoc.app.SiteSummaryExportActivity;
import java.util.ArrayList;
import com.servoc.app.report.AnalyticalCharts;
import com.servoc.app.report.TankChartEligibility;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/** HazRob 0.10.2 / HR069 smart operator report workspace. */
public final class ReportsTab {
    private static final int BG=0xFFF7F8FA,CARD=0xFFFFFFFF,CARD2=0xFFEDF0F2,TEXT=0xFF15202B,MUTED=0xFF46515E,GOOD=0xFF167443,WARN=0xFF995500,BAD=0xFFBC2424,INFO=0xFF13539C;
    private static final String[] CHARTS={
            "استجابة MQ / Rs-R0","البصمة الكيميائية","البصمة الرادارية Radar","المرشحون",
            "خريطة المرشحين Scatter","خريطة المرشحين متعددة المتغيرات","بواقي المرشحين Residual",
            "فحص 144","توزيع فحص 144 Donut","توزيع مستويات الدليل M0-M4","التعرض والاشتعال",
            "مصفوفة MQ × البيئة","جودة إشارة MQ / SNR","حركية الاستجابة t50-t90",
            "خريطة الاستجابة المستمرة AUC","ملف الجودة التحليلية Radar","الحرارة","الرطوبة","الضغط","الصوت"
    };

    private final HazRobHost host;
    private final ScrollView scroll;
    private final LinearLayout root,list;
    private final TextView archiveCard,sessionCard,decisionCard,qualityCard,safetyCard,candidateCard,chartStatus,technical;
    private final Button pdf,xlsx,technicalToggle,openChart;
    private final Spinner chartSelector,siteSelector,fieldSelector;
    private final List<FieldSessionRepository.Site> scopeSites=new ArrayList<>();
    private final List<FieldSessionRepository.FieldSession> scopeSessions=new ArrayList<>();
    private final List<String> boundSiteIds=new ArrayList<>(),boundSiteLabels=new ArrayList<>();
    private final List<String> boundSessionIds=new ArrayList<>(),boundSessionLabels=new ArrayList<>();
    private boolean rebindingScope=false;
    private final List<Integer> availableCharts=new ArrayList<>();
    private volatile long chartGeneration=0;
    private volatile boolean released=false;
    private volatile long reportGeneration=0;
    private String chosenSiteId="",chosenSessionId="";
    private final ImageView chartView;
    private final ChemCoreDecisionRepository decisions;
    private final FieldSessionRepository fieldSessions;
    private long selectedId=-1;
    private boolean technicalVisible=false;

    public ReportsTab(Context c,HazRobHost host){
        this.host=host;decisions=new ChemCoreDecisionRepository(host.database());fieldSessions=new FieldSessionRepository(host.database());
        scroll=new ScrollView(c);scroll.setFillViewport(true);root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,12));root.setBackgroundColor(BG);scroll.addView(root);
        root.addView(UiKit.title(c,"التقارير الذكية • HazRob Tank"),UiKit.full(c));
        TextView scopeHint=UiKit.small(c,"نطاق التقرير: اختر الموقع، ثم جميع جلساته أو جلسة واحدة. لا يجمع التطبيق جلسات مختارة في تقرير جزئي.");
        scopeHint.setTextColor(MUTED);root.addView(scopeHint,UiKit.full(c));
        root.addView(UiKit.small(c,"الموقع"),UiKit.full(c));
        siteSelector=new Spinner(c);UiKit.outlinedSpinner(siteSelector);root.addView(siteSelector,UiKit.full(c));
        root.addView(UiKit.small(c,"الجلسة / نطاق التقرير"),UiKit.full(c));
        fieldSelector=new Spinner(c);UiKit.outlinedSpinner(fieldSelector);root.addView(fieldSelector,UiKit.full(c));
        siteSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                if(rebindingScope||position<0||position>=scopeSites.size()
                        ||position!=siteSelector.getSelectedItemPosition())return;
                String next=scopeSites.get(position).id;
                if(next.equals(chosenSiteId))return;
                chosenSiteId=next;chosenSessionId="";
                refresh();
            }
            public void onNothingSelected(AdapterView<?> parent){}
        });
        fieldSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                if(rebindingScope||position!=fieldSelector.getSelectedItemPosition())return;
                String next=position>0&&position<=scopeSessions.size()?scopeSessions.get(position-1).id:"";
                if(next.equals(chosenSessionId))return;
                // A selected Field Session also scopes the history cards and
                // charts, not just the PDF/XLSX export labels.
                chosenSessionId=next;updateExportButtonLabels();refresh();
            }
            public void onNothingSelected(AdapterView<?> parent){}
        });


        archiveCard=card(c,"السجل • لا توجد قياسات محفوظة");root.addView(archiveCard,UiKit.full(c));
        LinearLayout r1=UiKit.hbox(c);sessionCard=kpi(c,"الجلسة\n—");decisionCard=kpi(c,"القرار\n—");r1.addView(sessionCard,UiKit.weight(1));r1.addView(decisionCard,UiKit.weight(1));root.addView(r1,UiKit.full(c));
        LinearLayout r2=UiKit.hbox(c);qualityCard=kpi(c,"الجودة\n—");safetyCard=kpi(c,"السلامة\n—");r2.addView(qualityCard,UiKit.weight(1));r2.addView(safetyCard,UiKit.weight(1));root.addView(r2,UiKit.full(c));
        candidateCard=card(c,"المرشحون • —");candidateCard.setTextIsSelectable(true);root.addView(candidateCard,UiKit.full(c));

        LinearLayout actions=UiKit.hbox(c);pdf=UiKit.button(c,"تصدير PDF");pdf.setOnClickListener(v->exportScope("pdf"));actions.addView(pdf,UiKit.weight(1));xlsx=UiKit.button(c,"تصدير Excel");xlsx.setOnClickListener(v->exportScope("xlsx"));actions.addView(xlsx,UiKit.weight(1));root.addView(actions,UiKit.full(c));

        TextView chartTitle=UiKit.small(c,"الرسوم التحليلية • البيانات المسجلة فقط • لا توجد نقاط مصطنعة");chartTitle.setTextColor(MUTED);root.addView(chartTitle,UiKit.full(c));
        chartSelector=new Spinner(c);UiKit.outlinedSpinner(chartSelector);root.addView(chartSelector,UiKit.full(c));
        chartStatus=card(c,"المخطط • اختر جلسة محفوظة");root.addView(chartStatus,UiKit.full(c));
        chartView=new ImageView(c);chartView.setAdjustViewBounds(true);chartView.setScaleType(ImageView.ScaleType.FIT_CENTER);chartView.setMinimumHeight(UiKit.dp(c,220));chartView.setBackgroundColor(CARD2);chartView.setVisibility(View.GONE);root.addView(chartView,UiKit.full(c));
        chartView.setContentDescription("مخطط البيانات المحفوظة، اضغط لفتحه بحجم كبير");
        chartView.setOnClickListener(v->showChartDialog());
        openChart=UiKit.button(c,"🔍 عرض المخطط بحجم كبير");
        openChart.setEnabled(false);openChart.setOnClickListener(v->showChartDialog());
        root.addView(openChart,UiKit.full(c));
        chartSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){renderSelectedChart(pos);}public void onNothingSelected(AdapterView<?> p){}});

        technical=card(c,"—");technical.setTextIsSelectable(true);technical.setVisibility(View.GONE);
        technicalToggle=UiKit.button(c,"إظهار الأدلة التقنية");technicalToggle.setOnClickListener(v->{technicalVisible=!technicalVisible;technical.setVisibility(technicalVisible?View.VISIBLE:View.GONE);technicalToggle.setText(technicalVisible?"إخفاء الأدلة التقنية":"إظهار الأدلة التقنية");});root.addView(technicalToggle,UiKit.full(c));
        root.addView(technical,UiKit.full(c));

        TextView history=UiKit.small(c,"القياسات المحفوظة");history.setTextColor(MUTED);root.addView(history,UiKit.full(c));list=UiKit.vbox(c);root.addView(list,UiKit.full(c));setEnabled(false);
    }

    public View view(){return scroll;}
    /** Prevent late chart callbacks from drawing into a destroyed Activity. */
    public void release(){released=true;reportGeneration++;chartGeneration++;chartView.setImageDrawable(null);chartView.setVisibility(View.GONE);openChart.setEnabled(false);}
    public void selectScope(String siteId,String sessionId){
        chosenSiteId=siteId==null?"":siteId;
        chosenSessionId=sessionId==null?"":sessionId;
        refresh();
    }
    /**
     * Reuse adapters whenever site/session rows are unchanged. Spinner callbacks can run
     * after an adapter change: all selections use stable UUIDs, not previous row indices.
     */
    private static final class ScopeData {
        final String requestedSite,requestedSession;
        final List<FieldSessionRepository.Site> sites;
        final List<FieldSessionRepository.FieldSession> sessions;
        final List<ExperimentQueries.Record> records;
        ScopeData(String site,String session,List<FieldSessionRepository.Site> sites,
                  List<FieldSessionRepository.FieldSession> sessions,
                  List<ExperimentQueries.Record> records){
            requestedSite=site;requestedSession=session;this.sites=sites;
            this.sessions=sessions;this.records=records;
        }
    }

    /** Pure UI update from background-loaded SQLite results. */
    private void refreshSiteScope(ScopeData data){
        rebindingScope=true;
        try{
            List<FieldSessionRepository.Site> sites=data.sites;
            scopeSites.clear();scopeSites.addAll(sites);
            List<String> ids=new ArrayList<>(),labels=new ArrayList<>();
            int selected=0;
            for(int i=0;i<sites.size();i++){
                FieldSessionRepository.Site site=sites.get(i);
                ids.add(site.id);labels.add(site.name);
                if(site.id.equals(chosenSiteId))selected=i;
            }
            if(sites.isEmpty()){
                chosenSiteId="";chosenSessionId="";labels.add("لا توجد مواقع");
            }else if(!sites.get(selected).id.equals(chosenSiteId)){
                chosenSiteId=sites.get(selected).id;chosenSessionId="";
            }
            if(siteSelector.getAdapter()==null||!boundSiteIds.equals(ids)||!boundSiteLabels.equals(labels)){
                siteSelector.setAdapter(UiKit.lightOptions(scroll.getContext(),labels.toArray(new String[0])));
                boundSiteIds.clear();boundSiteIds.addAll(ids);
                boundSiteLabels.clear();boundSiteLabels.addAll(labels);
            }
            siteSelector.setEnabled(!sites.isEmpty());
            if(siteSelector.getSelectedItemPosition()!=selected)siteSelector.setSelection(selected);
            refreshSessionScope(data.sessions);
        }finally{rebindingScope=false;}
    }
    private void refreshSessionScope(List<FieldSessionRepository.FieldSession> sessions){
        rebindingScope=true;
        try{
            scopeSessions.clear();scopeSessions.addAll(sessions);
            List<String> ids=new ArrayList<>(),labels=new ArrayList<>();
            labels.add("الجلسات المعتمدة للموقع • تقرير الموقع");
            int selected=0;
            for(int i=0;i<sessions.size();i++){
                FieldSessionRepository.FieldSession f=sessions.get(i);
                ids.add(f.id);labels.add(f.name+" • معتمدة");
                if(f.id.equals(chosenSessionId))selected=i+1;
            }
            chosenSessionId=selected>0?sessions.get(selected-1).id:"";
            if(fieldSelector.getAdapter()==null||!boundSessionIds.equals(ids)||!boundSessionLabels.equals(labels)){
                fieldSelector.setAdapter(UiKit.lightOptions(scroll.getContext(),labels.toArray(new String[0])));
                boundSessionIds.clear();boundSessionIds.addAll(ids);
                boundSessionLabels.clear();boundSessionLabels.addAll(labels);
            }
            if(fieldSelector.getSelectedItemPosition()!=selected)fieldSelector.setSelection(selected);
            fieldSelector.setEnabled(!chosenSiteId.isEmpty());
            updateExportButtonLabels();
        }finally{rebindingScope=false;}
    }
    private void updateExportButtonLabels(){
        if(pdf==null||xlsx==null)return;
        boolean hasSite=!chosenSiteId.isEmpty()&&!scopeSessions.isEmpty();
        pdf.setEnabled(hasSite);xlsx.setEnabled(hasSite);
        pdf.setText(chosenSessionId.isEmpty()?"PDF الذكي للموقع":"PDF الذكي للجلسة");
        xlsx.setText(chosenSessionId.isEmpty()?"Excel الذكي للموقع":"Excel الذكي للجلسة");
    }
    private void exportScope(String format){
        if(chosenSiteId.isEmpty()){host.showToast("اختر موقعًا أولًا");return;}
        if(chosenSessionId.isEmpty())SiteSummaryExportActivity.start(scroll.getContext(),chosenSiteId,format);
        else host.requestFieldSessionExport(format,chosenSessionId);
    }


    public void refresh(){
        if(released)return;
        if(Looper.myLooper()!=Looper.getMainLooper()){scroll.post(this::refresh);return;}
        final long request=++reportGeneration;
        final String siteId=chosenSiteId,sessionId=chosenSessionId;
        try{
            host.io().execute(()->{
                try{
                    List<FieldSessionRepository.Site> sites=fieldSessions.sites();
                    String effectiveSite=siteId;boolean found=false;
                    for(FieldSessionRepository.Site site:sites){
                        if(site.id.equals(effectiveSite)){found=true;break;}
                    }
                    if(!found)effectiveSite=sites.isEmpty()?"":sites.get(0).id;
                    List<FieldSessionRepository.FieldSession> sessions=effectiveSite.isEmpty()
                            ?new ArrayList<>():fieldSessions.approvedSessionsForSite(effectiveSite);
                    // Scope the query BEFORE the 100-row limit. A missing or
                    // stale selected Field Session means "all approved sessions
                    // for this site" after the spinner is re-bound.
                    String effectiveSession="";
                    for(FieldSessionRepository.FieldSession item:sessions)
                        if(item.id.equals(sessionId)){effectiveSession=sessionId;break;}
                    List<ExperimentQueries.Record> records=effectiveSite.isEmpty()
                        ?new ArrayList<>()
                        :ExperimentQueries.approvedFieldReportsForScope(
                            host.database(),effectiveSite,effectiveSession,100);
                    ScopeData data=new ScopeData(siteId,sessionId,sites,sessions,records);
                    scroll.post(()->{
                        if(released||reportGeneration!=request)return;
                        if(!chosenSiteId.equals(siteId)||!chosenSessionId.equals(sessionId)){
                            refresh();return;
                        }
                        refreshSiteScope(data);
                        renderHistory(data.records);
                    });
                }catch(RuntimeException ex){
                    android.util.Log.e("TankReports","Cannot load approved reports",ex);
                    scroll.post(()->{
                        if(released||reportGeneration!=request)return;
                        archiveCard.setText("تعذر تحميل التقارير؛ تحقق من قاعدة البيانات وأعد المحاولة.");
                        pdf.setEnabled(false);xlsx.setEnabled(false);
                        chartGeneration++;chartView.setImageDrawable(null);
                        chartView.setVisibility(View.GONE);openChart.setEnabled(false);
                    });
                }
            });
        }catch(RuntimeException ex){
            android.util.Log.e("TankReports","Cannot schedule reports refresh",ex);
            archiveCard.setText("تعذر بدء تحميل التقارير.");
        }
    }

    private void renderHistory(List<ExperimentQueries.Record> rows){
        list.removeAllViews();
        archiveCard.setText("التقارير المعتمدة • "+rows.size()+" قياس معتمد (الأحدث أولاً)");
        if(rows.isEmpty()){
            chartGeneration++;selectedId=-1;setEnabled(false);
            sessionCard.setText("الجلسة\n—");decisionCard.setText("القرار\n—");
            qualityCard.setText("الجودة\n—");safetyCard.setText("السلامة\n—");
            candidateCard.setText("المرشحون • —");technical.setText("لا توجد بيانات تقرير.");
            availableCharts.clear();chartSelector.setAdapter(null);chartView.setImageDrawable(null);
            chartView.setVisibility(View.GONE);openChart.setEnabled(false);
            chartStatus.setText("المخطط • لا توجد جلسة محفوظة");
            return;
        }
        if(selectedId<0)selectedId=rows.get(0).id;
        boolean found=false;
        for(ExperimentQueries.Record row:rows){
            if(row.id==selectedId)found=true;
            Button button=UiKit.button(list.getContext(),(row.id==selectedId?"● ":"")+"#"+row.id+
                    "   "+safe(row.ts)+"   "+safe(row.protocol));
            button.setOnClickListener(v->select(row.id));
            list.addView(button,UiKit.full(list.getContext()));
        }
        if(!found)selectedId=rows.get(0).id;
        showSelected();
    }

    private void select(long id){selectedId=id;refresh();}

    private void showSelected(){
        if(released)return;
        final long id=selectedId,request=++reportGeneration;
        try{
            host.io().execute(()->{
                try{
                    ExperimentQueries.Record row=ExperimentQueries.byId(host.database(),id);
                    if(row==null){
                        scroll.post(()->{if(!released&&reportGeneration==request)setEnabled(false);});
                        return;
                    }
                    String sid=row.sessionId==null?"":row.sessionId.trim();
                    JSONObject decision=sid.isEmpty()?null:decisions.latestPayloadForSession(sid);
                    SessionSeries.Series series=SessionSeries.forExperiment(host.database(),row);
                    String fieldId=sid.isEmpty()?"":fieldSessions.fieldSessionIdForMeasurement(sid);
                    scroll.post(()->{
                        if(released||reportGeneration!=request||selectedId!=id)return;
                        showSelectedReady(row,decision,series,fieldId);
                    });
                }catch(RuntimeException ex){
                    android.util.Log.e("TankReports","Cannot load selected analysis",ex);
                    scroll.post(()->{
                        if(released||reportGeneration!=request)return;
                        setEnabled(false);chartStatus.setText("تعذر تحميل بيانات القياس المحدد");
                    });
                }
            });
        }catch(RuntimeException ex){
            android.util.Log.e("TankReports","Cannot schedule selected report",ex);
            chartStatus.setText("تعذر عرض التقرير المحدد");
        }
    }

    /** UI-only rendering of a saved, immutable ChemCore snapshot. */
    private void showSelectedReady(ExperimentQueries.Record r,JSONObject cc,SessionSeries.Series ss,
                                   String fieldSessionId){
        setEnabled(true);
        JSONObject d=json(r.payload),m=d.optJSONObject("measurement"),e=d.optJSONObject("env"),
                relay=d.optJSONObject("relay"),a=json(r.analytics);
        String sid=r.sessionId==null?"":r.sessionId.trim();
        boolean fieldReport=!fieldSessionId.isEmpty();updateExportButtonLabels();
        boolean seriesOk=ss!=null&&ss.usable(),mqOn=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);String state=m==null?"—":m.optString("state","—");
        sessionCard.setText("الجلسة #"+r.id+"\n"+(sid.isEmpty()?"بدون Session ID":sid)+"\n"+state+(seriesOk?" • "+ss.samples.size()+" عينة":" • لا سلسلة زمنية"));sessionCard.setTextColor(seriesOk?GOOD:WARN);

        if(cc==null){decisionCard.setText("القرار\nلا توجد نتيجة ChemCore");decisionCard.setTextColor(WARN);qualityCard.setText("الجودة\n—");safetyCard.setText("السلامة\nمحجوبة");candidateCard.setText("المرشحون • لا توجد نتيجة مرتبطة");}
        else{
            String outcome=cc.optString("outcome","—"),tier=cc.optString("evidence_tier","—"),chemical=cc.optString("chemical","—");double post=cc.optDouble("top_posterior",Double.NaN),margin=cc.optDouble("posterior_separation_margin",Double.NaN),ood=cc.optDouble("ood_score",Double.NaN),entropy=cc.optDouble("normalized_entropy",Double.NaN);TankPpmEstimate.Result ppmResult=TankPpmEstimate.from(cc);
            decisionCard.setText("القرار • "+outcome+"\n"+chemical+" • "+pct(post)+"\n"+tier+(ppmResult.available()?" • "+fmt(ppmResult.ppm)+" ppm • "+ppmResult.qualityAr():" • لا توجد نتيجة PPM رقمية"));decisionCard.setTextColor("M4_HAZROB_VALIDATED".equals(tier)?GOOD:INFO);
            JSONObject q=first(cc.optJSONObject("measurement_quality"),cc.optJSONObject("lab_quality"));int valid=q==null?-1:q.optInt("valid_mq_channels",q.optInt("valid_channels",-1));String qstate=q==null?"—":q.optString("state",q.optString("overall_state",q.optBoolean("capture_ready",false)?"READY":"—"));qualityCard.setText("الجودة • "+qstate+"\nMQ "+(valid>=0?valid+"/8":"—")+" • OOD "+fmt(ood)+"\nEntropy "+fmt(entropy)+" • Δ "+pct(margin));qualityCard.setTextColor(valid>=6?GOOD:WARN);
            JSONObject gx=cc.optJSONObject("gas_exposure_ppm");if(gx==null||!gx.optBoolean("input_gate_passed",false)){safetyCard.setText("السلامة\nالتفسير الكمي محجوب\n"+(gx==null?"لا توجد نتيجة M4":"بوابة القياس لم تجتز"));safetyCard.setTextColor(WARN);}else{double hp=gx.optDouble("hazard_priority_percent",Double.NaN),lfl=gx.optDouble("percent_lfl",Double.NaN);safetyCard.setText("السلامة • "+gx.optString("hazard_symbols","")+gx.optString("toxicity_state",gx.optString("state","—"))+"\nأولوية "+pct100(hp)+" • LFL "+pct100(lfl));safetyCard.setTextColor((Double.isFinite(hp)&&hp>=100)||(Double.isFinite(lfl)&&lfl>=100)?BAD:WARN);}
            candidateCard.setText(candidateSummary(cc));
        }

        StringBuilder t=new StringBuilder("المرجع والتتبّع\n");t.append("Experiment #").append(r.id).append(" • UTC ").append(safe(r.ts)).append("\nProtocol: ").append(safe(r.protocol)).append("\nSession: ").append(sid.isEmpty()?"—":sid).append("\nField Session: ").append(fieldReport?fieldSessionId:"—").append("\nMQ Power: ").append(mqOn?"ON":"OFF");if(relay!=null&&mqOn)t.append(" • ").append(relay.optLong("power_ms",0)).append(" ms");if(e!=null&&e.optBoolean("ok",false))t.append("\nENV: ").append(fmt(e.optDouble("temperature",Double.NaN))).append(" °C • ").append(fmt(e.optDouble("humidity",Double.NaN))).append(" %RH • ").append(fmt(e.optDouble("pressure",Double.NaN))).append(" hPa");t.append("\nStored analytical rows: ").append(a.optJSONArray("results")==null?0:a.optJSONArray("results").length());if(cc!=null){t.append("\n\n").append(ChemCoreAnalysisPresenter.summary(cc));t.append("\n\n").append(ChemCoreAnalysisPresenter.equationsAndResiduals(cc,6));}technical.setText(t.toString());refreshAvailableCharts(ss,cc);
    }

    /** Only expose diagrams backed by the selected record's actual persisted evidence. */
    private void refreshAvailableCharts(SessionSeries.Series series,JSONObject chemcore){
        if(released)return;
        chartGeneration++; // invalidate any chart drawing for the previous record
        availableCharts.clear();List<String> labels=new ArrayList<>();
        for(int i=0;i<CHARTS.length;i++){
            if(TankChartEligibility.hasData(i,series,chemcore)){
                availableCharts.add(i);labels.add(CHARTS[i]);
            }
        }
        chartView.setImageDrawable(null);chartView.setVisibility(View.GONE);openChart.setEnabled(false);
        if(labels.isEmpty()){
            labels.add("لا توجد رسوم ذات بيانات صالحة");
            chartStatus.setText("لا توجد رسوم لهذا القياس • أنشئ جلسة تتضمن سلسلة زمنية أو تحليل ChemCore محفوظًا");
            chartStatus.setTextColor(WARN);
        }else chartStatus.setText("اختر رسمًا من "+labels.size()+" رسوم متاحة مرتبطة بالقياس المحدد");
        ArrayAdapter<String> adapter=UiKit.lightOptions(scroll.getContext(),labels.toArray(new String[0]));
        chartSelector.setAdapter(adapter);chartSelector.setEnabled(!availableCharts.isEmpty());
        if(!availableCharts.isEmpty())renderSelectedChart(0);
    }

    private void renderSelectedChart(int selectedIndex){
        if(released)return;
        if(selectedIndex<0||selectedIndex>=availableCharts.size()){chartGeneration++;chartView.setImageDrawable(null);chartView.setVisibility(View.GONE);openChart.setEnabled(false);chartStatus.setText("المخطط غير متاح لهذه الجلسة");return;}
        final int pos=availableCharts.get(selectedIndex);
        final long experimentId=selectedId,request=++chartGeneration;
        chartStatus.setText("جارٍ إعداد الرسم • "+CHARTS[pos]);openChart.setEnabled(false);
        // DB reconstruction and rasterisation are potentially expensive: never do them on UI.
        host.io().execute(()->{
            try{
                if(released||chartGeneration!=request)return;
                ExperimentQueries.Record r=ExperimentQueries.byId(host.database(),experimentId);
                if(r==null){
                    scroll.post(()->{
                        if(released||chartGeneration!=request)return;
                        chartView.setImageDrawable(null);chartView.setVisibility(View.GONE);
                        openChart.setEnabled(false);
                        chartStatus.setText("تعذر العثور على جلسة المخطط المحفوظة");
                        chartStatus.setTextColor(WARN);
                    });
                    return;
                }
                SessionSeries.Series ss=SessionSeries.forExperiment(host.database(),r);
                String sid=r.sessionId==null?"":r.sessionId.trim();
                JSONObject cc=sid.isEmpty()?null:decisions.latestPayloadForSession(sid);
                boolean needsChem=pos>=3&&pos<=10;
                boolean ready=TankChartEligibility.hasData(pos,ss,cc);
                if(!ready){
                    scroll.post(()->{if(released||chartGeneration!=request)return;chartView.setImageDrawable(null);chartView.setVisibility(View.GONE);openChart.setEnabled(false);chartStatus.setText("بيانات الرسم غير متوفرة لهذه الجلسة");});
                    return;
                }
                if(released||chartGeneration!=request)return;
                Bitmap b=Bitmap.createBitmap(1200,650,Bitmap.Config.ARGB_8888);
                Canvas canvas=new Canvas(b);canvas.drawColor(Color.WHITE);RectF box=new RectF(12,12,1188,638);
        switch(pos){
            case 0:AnalyticalCharts.mqRsR0(canvas,box,ss);break;case 1:AnalyticalCharts.fingerprint(canvas,box,ss);break;case 2:AnalyticalCharts.fingerprintRadar(canvas,box,ss);break;
            case 3:AnalyticalCharts.candidates(canvas,box,cc);break;case 4:AnalyticalCharts.candidateScatter(canvas,box,cc);break;case 5:AnalyticalCharts.candidateBubbleMap(canvas,box,cc);break;case 6:AnalyticalCharts.candidateResiduals(canvas,box,cc);break;case 7:AnalyticalCharts.screening144(canvas,box,cc);break;case 8:AnalyticalCharts.screeningDonut(canvas,box,cc);break;case 9:AnalyticalCharts.evidenceTiers(canvas,box,cc);break;case 10:AnalyticalCharts.exposureRatios(canvas,box,cc);break;
            case 11:AnalyticalCharts.mqEnvironmentMatrix(canvas,box,ss);break;case 12:AnalyticalCharts.mqSignalQuality(canvas,box,ss);break;case 13:AnalyticalCharts.mqResponseKinetics(canvas,box,ss);break;case 14:AnalyticalCharts.mqPersistenceMap(canvas,box,ss);break;case 15:AnalyticalCharts.analyticalQualityRadar(canvas,box,ss);break;case 16:AnalyticalCharts.temperature(canvas,box,ss);break;case 17:AnalyticalCharts.humidity(canvas,box,ss);break;case 18:AnalyticalCharts.pressure(canvas,box,ss);break;case 19:AnalyticalCharts.sound(canvas,box,ss);break;
            default:AnalyticalCharts.candidates(canvas,box,cc);
        }
                scroll.post(()->{
                    if(released||chartGeneration!=request||selectedId!=experimentId){b.recycle();return;}
                    chartView.setImageBitmap(b);chartView.setVisibility(View.VISIBLE);openChart.setEnabled(true);
                    chartStatus.setText("● "+CHARTS[pos]+" • "+(needsChem?"ChemCore محفوظ":"سلسلة زمنية محفوظة"));
                    chartStatus.setTextColor(GOOD);
                });
            }catch(Exception failure){
                scroll.post(()->{if(released||chartGeneration!=request)return;chartView.setImageDrawable(null);chartView.setVisibility(View.GONE);openChart.setEnabled(false);chartStatus.setText("تعذر رسم المخطط • "+failure.getClass().getSimpleName());chartStatus.setTextColor(WARN);});
            }
        });
    }

    /** A real enlarged chart view; never opens an empty/fabricated graph. */
    private void showChartDialog(){
        if(chartView.getDrawable()==null||chartView.getVisibility()!=View.VISIBLE){
            host.showToast("لا توجد بيانات مخطط متاحة للعرض");
            return;
        }
        Context c=scroll.getContext();
        ImageView expanded=new ImageView(c);
        expanded.setImageDrawable(chartView.getDrawable());
        expanded.setAdjustViewBounds(true);
        expanded.setScaleType(ImageView.ScaleType.FIT_CENTER);
        expanded.setMinimumHeight(UiKit.dp(c,310));
        expanded.setPadding(UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4));
        expanded.setBackgroundColor(Color.WHITE);
        new AlertDialog.Builder(c)
            .setTitle(String.valueOf(chartSelector.getSelectedItem()))
            .setView(expanded)
            .setPositiveButton("إغلاق",null)
            .show();
    }

    private TextView card(Context c,String text){TextView t=UiKit.text(c,text);t.setTextColor(TEXT);t.setBackgroundColor(CARD);t.setPadding(UiKit.dp(c,10),UiKit.dp(c,9),UiKit.dp(c,10),UiKit.dp(c,9));return t;}
    private TextView kpi(Context c,String text){TextView t=card(c,text);t.setTextSize(14);t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);return t;}
    private static JSONObject first(JSONObject a,JSONObject b){return a!=null?a:b;}
    private static String candidateSummary(JSONObject p){JSONArray a=p==null?null:p.optJSONArray("candidates");if(a==null||a.length()==0)return "المرشحون • لا توجد فرضيات قابلة للحل";StringBuilder s=new StringBuilder("المرشحون الأعلى • ").append(a.length());int n=Math.min(6,a.length());for(int i=0;i<n;i++){JSONObject q=a.optJSONObject(i);if(q==null)continue;s.append("\n#").append(q.optInt("rank",i+1)).append(" ").append(q.optString("label","—")).append(" • ").append(pct(q.optDouble("posterior",Double.NaN))).append(" • residual ").append(fmt(q.optDouble("residual_rms",Double.NaN))).append(" • ").append(q.optString("evidence_tier","—"));}if(a.length()>n)s.append("\n+").append(a.length()-n).append(" مرشح إضافي في نتائج التحليل");return s.toString();}
    private void setEnabled(boolean v){updateExportButtonLabels();chartSelector.setEnabled(v);}
    private static JSONObject json(String s){try{return s==null?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    private static String safe(String s){return s==null?"":s;}
    private static String fmt(double v){return Double.isFinite(v)?String.format(Locale.US,"%.2f",v):"—";}
    private static String pct(double v){if(!Double.isFinite(v))return"—";return String.format(Locale.US,"%.1f%%",v<=1.000001?v*100:v);}
    private static String pct100(double v){return Double.isFinite(v)?String.format(Locale.US,"%.1f%%",v):"—";}
}