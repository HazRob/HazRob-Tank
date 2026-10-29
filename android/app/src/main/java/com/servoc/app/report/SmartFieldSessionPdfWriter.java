package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;

import com.servoc.app.ReleaseAudit;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemcore.ChemCoreDecisionRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.data.SessionSeries;
import com.servoc.app.field.FieldCycleRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Compact evidence-driven Field Session PDF: 3 columns x 2 rows = 6 charts per analytical page. */
public final class SmartFieldSessionPdfWriter {
    private SmartFieldSessionPdfWriter(){}
    public static final int PAGE_WIDTH=842,PAGE_HEIGHT=595,MIN_PAGES=4,MAX_PAGES=5,CHARTS_PER_PAGE=6; // Base pages; V5 cycle annex paginates independently.
    private static final float MARGIN=28.35f;
    private static final int ROWS_PER_CYCLE_PAGE=15;

    static final class Evidence {
        final String sessionId;final SessionSeries.Series series;final JSONObject decision;final int recordCount;
        Evidence(String sid,SessionSeries.Series s,JSONObject d,int records){sessionId=sid;series=s;decision=d;recordCount=records;}
        boolean seriesOk(){return series!=null&&series.usable();} boolean chemOk(){return decision!=null;}
        int samples(){return series==null?0:series.samples.size();} long durationMs(){return series==null?0:series.durationMs();}
        String outcome(){return decision==null?"—":decision.optString("outcome","—");} String topCandidate(){return SmartFieldSessionPdfWriter.topCandidate(decision);}
        double posterior(){return topPosterior(decision);} int validMq(){return SmartFieldSessionPdfWriter.validMq(decision);}
    }
    private static final class Consensus {final String label;final int count,total;final double meanPosterior;Consensus(String l,int c,int t,double p){label=l;count=c;total=t;meanPosterior=p;}}

    public static void writePdf(OutputStream out,ExperimentsDb db,String fieldSessionId,ChemicalRepository chemicals)throws Exception{
        if(out==null)throw new IllegalArgumentException("output required");
        FieldSessionReportExporter.Bundle b=FieldSessionReportExporter.resolve(db,fieldSessionId);
        if(b.analyticalContract==null)throw new IllegalStateException("SmartReport analytical contract missing");
        SmartReportAnalyticalContract contract=b.analyticalContract;
        List<Evidence> all=evidence(db,b.measurementSessionIds);Evidence primary=primaryFromBundle(b,contract,all);Consensus cs=consensus(all);
        SessionSeries.Series s=contract.series();JSONObject q=contract.decision();
        ReportIntelligence.ReportPlan plan=ReportIntelligence.plan(s,q,b.measurementSessionIds.size());JSONObject hazard=CandidateHazardContext.evaluate(db,q);
        boolean crossSession=all.size()>1;
        int basePages=crossSession?5:4;
        int cyclePages=Math.max(1,(b.cycleLedger.size()+ROWS_PER_CYCLE_PAGE-1)/ROWS_PER_CYCLE_PAGE);
        // Source-only V6 pages are appended, never replacing original V5
        // report sections or changing an approved cycle's stored SHA.
        JSONObject v6ReadOnly=V6FieldSessionResearchAppendix.analyzeVerified(db,b);
        JSONArray v6Cycles=v6ReadOnly.optJSONArray("cycles");
        int v6Count=v6Cycles==null?0:v6Cycles.length();
        int v6Pages=(v6Count+2)/3;
        int total=basePages+cyclePages+v6Pages;
        PdfDocument pdf=new PdfDocument();
        PdfDocument.Page p=page(pdf,1);executiveOverview(p.getCanvas(),b,all,primary,plan,hazard,cs);chrome(p.getCanvas(),1,total,b,plan);pdf.finishPage(p);
        p=page(pdf,2);sensorSix(p.getCanvas(),primary);chrome(p.getCanvas(),2,total,b,plan);pdf.finishPage(p);
        p=page(pdf,3);chemCoreSix(p.getCanvas(),primary);chrome(p.getCanvas(),3,total,b,plan);pdf.finishPage(p);
        p=page(pdf,4);contextSix(p.getCanvas(),primary,plan);chrome(p.getCanvas(),4,total,b,plan);pdf.finishPage(p);
        if(crossSession){p=page(pdf,5);crossSessionAndMethodology(p.getCanvas(),b,all,primary,plan,chemicals,cs);chrome(p.getCanvas(),5,total,b,plan);pdf.finishPage(p);}
        for(int chunk=0;chunk<cyclePages;chunk++){
            int pageNo=basePages+chunk+1;
            p=page(pdf,pageNo);
            cycleLedgerPage(p.getCanvas(),b,chunk);
            chrome(p.getCanvas(),pageNo,total,b,plan);
            pdf.finishPage(p);
        }
        for(int chunk=0;chunk<v6Pages;chunk++){
            int pageNo=basePages+cyclePages+chunk+1;
            p=page(pdf,pageNo);
            v6ResearchAppendixPage(p.getCanvas(),b,v6ReadOnly,chunk);
            chrome(p.getCanvas(),pageNo,total,b,plan);
            pdf.finishPage(p);
        }
        pdf.writeTo(out);pdf.close();
    }

    static List<Evidence> evidence(ExperimentsDb db,List<String> ids){List<Evidence> out=new ArrayList<>();ChemCoreDecisionRepository decisions=new ChemCoreDecisionRepository(db);if(ids==null)return out;for(String sid:ids){if(sid==null||sid.trim().isEmpty())continue;String id=sid.trim();out.add(new Evidence(id,SessionSeries.byId(db,id),decisions.latestPayloadForSession(id),ExperimentQueries.bySessionId(db,id).size()));}return out;}
    private static Evidence primaryFromBundle(FieldSessionReportExporter.Bundle b,SmartReportAnalyticalContract contract,List<Evidence> all){String id=b.primaryMeasurementSessionId==null?"":b.primaryMeasurementSessionId;int records=0;for(Evidence e:all)if(id.equals(e.sessionId)){records=e.recordCount;break;}return new Evidence(id,contract.series(),contract.decision(),records);}
    private static Consensus consensus(List<Evidence> all){Map<String,Integer> n=new LinkedHashMap<>();Map<String,Double> sum=new LinkedHashMap<>();int total=0;for(Evidence e:all){if(!e.chemOk())continue;String k=e.topCandidate();if(k.isEmpty()||"—".equals(k))continue;total++;n.put(k,n.containsKey(k)?n.get(k)+1:1);double p=e.posterior();sum.put(k,(sum.containsKey(k)?sum.get(k):0)+(Double.isFinite(p)?p:0));}String best="—";int bn=0;double mean=Double.NaN;for(String k:n.keySet())if(n.get(k)>bn){best=k;bn=n.get(k);mean=sum.get(k)/Math.max(1,bn);}return new Consensus(best,bn,total,mean);}

    private static PdfDocument.Page page(PdfDocument pdf,int n){return pdf.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH,PAGE_HEIGHT,n).create());}
    private static RectF[] grid6(){RectF[] r=new RectF[6];float left=34,top=68,gx=10,gy=12,w=(PAGE_WIDTH-68-2*gx)/3f,h=(PAGE_HEIGHT-68-38-gy)/2f;for(int row=0;row<2;row++)for(int col=0;col<3;col++){int i=row*3+col;float x=left+col*(w+gx),y=top+row*(h+gy);r[i]=new RectF(x,y,x+w,y+h);}return r;}

    private static void executiveOverview(Canvas c,FieldSessionReportExporter.Bundle b,List<Evidence> all,Evidence primary,ReportIntelligence.ReportPlan plan,JSONObject hazard,Consensus cs){
        title(c,"الملخص التنفيذي • "+plan.modeAr,42);float y=74;String[][] rows={{"الموقع",b.site.name},{"الجلسة",b.fieldSession.name},{"الحالة",b.fieldSession.status},{"جلسات القياس",String.valueOf(all.size())},{"مرجع التحليل",primary==null?"—":shorten(primary.sessionId,32)}};for(String[] r:rows){labelValue(c,r[0],r[1],y);y+=22;}
        JSONObject q=primary==null?null:primary.decision;metric(c,420,72,178,70,"المرشح الأعلى",q==null?"—":topCandidate(q),q==null?"—":q.optString("evidence_tier","—"),0xFFFFF3E0);metric(c,610,72,190,70,"ثبات الجلسة",cs.count+" / "+cs.total,shorten(cs.label,24)+" • "+pct(cs.meanPosterior),0xFFE8F5E9);metric(c,420,152,178,70,"الجودة",primary==null?"—":(primary.validMq()<0?"MQ —":"MQ "+primary.validMq()+"/8"),"Posterior "+pct(plan.topPosterior)+" • OOD "+fmt(plan.oodScore),0xFFF4F6F8);metric(c,610,152,190,70,"مصفوفة 144",plan.panelCandidateSupported+" دعم","Evaluated "+plan.panelEvaluated+"/"+plan.panelTarget,0xFFE0F7FA);
        text(c,"جلسات القياس الفنية",42,212,11,Color.BLACK,true);String[] h={"Session","Samples","Duration","Outcome","Top candidate","Post.","MQ"};float[] x={42,248,310,380,490,655,735};for(int i=0;i<h.length;i++)text(c,h[i],x[i],236,7.5f,Color.DKGRAY,true);float yy=258;int shown=0;for(Evidence e:all){if(shown++>=7)break;boolean p=primary!=null&&e.sessionId.equals(primary.sessionId);if(p)box(c,36,yy-14,765,23,0xFFE3F2FD);text(c,(p?"★ ":"")+shorten(e.sessionId,29),x[0],yy,7.2f,Color.BLACK,p);text(c,String.valueOf(e.samples()),x[1],yy,7.2f,Color.BLACK,false);text(c,String.format(Locale.US,"%.1fs",e.durationMs()/1000.0),x[2],yy,7.2f,Color.BLACK,false);text(c,shorten(e.outcome(),15),x[3],yy,7.2f,Color.BLACK,false);text(c,shorten(e.topCandidate(),20),x[4],yy,7.2f,Color.BLACK,false);text(c,pct(e.posterior()),x[5],yy,7.2f,Color.BLACK,false);text(c,e.validMq()<0?"—":e.validMq()+"/8",x[6],yy,7.2f,Color.BLACK,false);yy+=25;}
        if(all.size()>7)text(c,"+ "+(all.size()-7)+" جلسات إضافية في Excel التفصيلي",42,yy+4,8,Color.DKGRAY,false);
        if(hazard!=null&&hazard.optJSONArray("candidates")!=null&&hazard.optJSONArray("candidates").length()>0)CandidateHazardCharts.executiveStrip(c,new RectF(42,442,800,526),hazard);
        text(c,"Screening لا يساوي هوية مؤكدة أو PPM؛ M4 وحده يسمح بالقياس الكمي المهني.",42,548,8,Color.DKGRAY,false);
    }

    private static void sensorSix(Canvas c,Evidence e){title(c,"الحساسات والبصمة • 6 مخططات",42);RectF[] r=grid6();if(e==null||!e.seriesOk()){for(RectF x:r)emptyBox(c,x,"السلسلة الزمنية غير متاحة");return;}SessionSeries.Series s=e.series;AnalyticalCharts.mqRsR0(c,r[0],s);AnalyticalCharts.fingerprint(c,r[1],s);SmartReportAdvancedCharts.mqCorrelationMatrix(c,r[2],s);AnalyticalCharts.fingerprintRadar(c,r[3],s);AnalyticalCharts.mqResponseKinetics(c,r[4],s);AnalyticalCharts.mqPersistenceMap(c,r[5],s);}

    private static void chemCoreSix(Canvas c,Evidence e){title(c,"ChemCore والاستدلال • 6 مخططات",42);RectF[] r=grid6();JSONObject q=e==null?null:e.decision;if(q==null){for(RectF x:r)emptyBox(c,x,"لا توجد نتيجة ChemCore");return;}AnalyticalCharts.candidates(c,r[0],q);AnalyticalCharts.candidateScatter(c,r[1],q);SmartReportAdvancedCharts.curveConsistency(c,r[2],q);AnalyticalCharts.candidateResiduals(c,r[3],q);if(MolecularReportCharts.hasQsprEvidence(q))MolecularReportCharts.qsprEvidence(c,r[4],q);else AnalyticalCharts.evidenceTiers(c,r[4],q);SmartReportCharts.decisionQuality(c,r[5],q);}

    private static void contextSix(Canvas c,Evidence e,ReportIntelligence.ReportPlan plan){title(c,"مصفوفة 144 • البيئة • السلامة • 6 مخططات",42);RectF[] r=grid6();JSONObject q=e==null?null:e.decision;SessionSeries.Series s=e==null?null:e.series;if(q==null){for(RectF x:r)emptyBox(c,x,"لا توجد نتيجة ChemCore");return;}SmartReportCharts.candidateEvidenceMatrix(c,r[0],q);SmartReportCharts.screening144ReliabilityMatrix(c,r[1],q);SmartReportCharts.top144SessionSupport(c,r[2],q);AnalyticalCharts.screeningDonut(c,r[3],q);if(s!=null&&s.usable())AnalyticalCharts.mqEnvironmentMatrix(c,r[4],s);else SmartReportCharts.evidenceBreadth(c,r[4],q);if(plan.ppmReportable)AnalyticalCharts.exposureRatios(c,r[5],q);else if(s!=null&&s.usable())AnalyticalCharts.temperature(c,r[5],s);else AnalyticalCharts.candidateBubbleMap(c,r[5],q);}

    private static void crossSessionAndMethodology(Canvas c,FieldSessionReportExporter.Bundle b,List<Evidence> all,Evidence primary,ReportIntelligence.ReportPlan plan,ChemicalRepository chemicals,Consensus cs){
        title(c,"الثبات بين الجلسات • المنهجية والتتبعية",42);candidateStability(c,new RectF(36,72,405,270),all);posteriorStability(c,new RectF(430,72,806,270),all);
        float y=304;text(c,"المرشح الأكثر تكرارًا: "+shorten(cs.label,28)+" • "+cs.count+"/"+cs.total+" • متوسط posterior "+pct(cs.meanPosterior),42,y,8.5f,Color.BLACK,true);y+=25;JSONObject q=primary==null?null:primary.decision,lib=q==null?null:q.optJSONObject("library");String[][] rows={{"Site ID",b.site.id},{"Field Session",b.fieldSession.id},{"Primary measurement",primary==null?"—":primary.sessionId},{"Model",q==null?"—":q.optString("model_version","—")},{"Feature schema",q==null?"—":q.optString("feature_schema","—")},{"Sampling profile",q==null?"—":q.optString("sampling_profile_id","—")},{"Library version",lib==null?"—":lib.optString("version","—")},{"Library SHA-256",lib==null?"—":shorten(lib.optString("sha256","—"),48)},{"Knowledge base",chemicals==null?"—":String.valueOf(chemicals.count())}};for(String[] z:rows){labelValue(c,z[0],z[1],y);y+=20;}
        text(c,"كل Measurement Session مستقلة؛ لا يتم دمج السلاسل الزمنية. ثبات المرشح يصف الاتساق ولا يثبت الهوية. Tracking غير مدرج في التقرير.",42,548,7.8f,Color.DKGRAY,false);
    }

    /** Full sequence-numbered audit register. Chart pages always use approved evidence only. */
    private static void cycleLedgerPage(Canvas c,FieldSessionReportExporter.Bundle b,int chunk){
        title(c,"تفاصيل دورات القياس واعتمادها • "+(chunk+1),42);
        text(c,"الموقع: "+shorten(b.site.name,42)+" • الجلسة: "+shorten(b.fieldSession.name,42),
            36,67,9,Color.BLACK,true);
        int start=chunk*ROWS_PER_CYCLE_PAGE;
        int end=Math.min(b.cycleLedger.size(),start+ROWS_PER_CYCLE_PAGE);
        String[] labels={"#","المحاولة","الحالة","الفئات","البدء UTC","اعتماد UTC","Measurement ID","SHA"};
        float[] xs={32,67,134,240,300,458,610,761};
        for(int i=0;i<labels.length;i++)text(c,labels[i],xs[i],94,7.5f,Color.DKGRAY,true);
        float y=120;
        for(int i=start;i<end;i++){
            FieldCycleRepository.Cycle v=b.cycleLedger.get(i);
            if((i-start)%2==0)box(c,26,y-15,786,24,0xFFF2F5F7);
            String opts=(v.modes.gas?"G":"")+(v.modes.particles?"P":"")+
                (v.modes.physical?"F":"");
            text(c,Integer.toString(v.sequence),xs[0],y,7,Color.BLACK,false);
            text(c,Integer.toString(v.attempt),xs[1],y,7,Color.BLACK,false);
            text(c,shorten(v.state,14),xs[2],y,7,
                v.approved()?0xFF21633A:0xFFA75B21,true);
            text(c,opts,xs[3],y,7,Color.BLACK,false);
            text(c,shorten(v.started,22),xs[4],y,6.7f,Color.BLACK,false);
            text(c,shorten(v.approved,22),xs[5],y,6.7f,Color.BLACK,false);
            text(c,shorten(v.measurementId,23),xs[6],y,6.7f,Color.BLACK,false);
            text(c,shorten(v.evidenceSha,10),xs[7],y,6.7f,Color.DKGRAY,false);
            y+=27;
        }
        if(start==end)text(c,"لا توجد دورات مسجلة.",42,145,10,Color.DKGRAY,false);
        text(c,"G=Gas • P=Particles • F=Physical. أسماء المراجع ومعرفات SHA الكاملة وحالات الحذف في Excel.",36,539,7.8f,Color.DKGRAY,false);
        text(c,"القياسات المعتمدة فقط تدخل المخططات والتحليل؛ المحاولات الأخرى أدلة تدقيق لا نتائج كيميائية.",36,554,7.8f,Color.DKGRAY,false);
    }


    /** No reportable gas identity, ppm, occupational warning or invented scan:
     * a compact human-readable appendix in the existing approved PDF. */
    private static void v6ResearchAppendixPage(Canvas c,
                  FieldSessionReportExporter.Bundle b,JSONObject evidence,int chunk){
        title(c,"ChemCore Tank V6 • دليل بحثي للغازات / UNKNOWN",42);
        text(c,"READ ONLY • G3=0 • PPM NOT REPORTABLE • NO AUTOMATIC GAS IDENTIFICATION",
            36,65,8,0xFFC62828,true);
        text(c,"الأدلة من telemetry_samples المعتمدة فقط؛ جميع قرارات V5 الأصلية محفوظة",
            36,84,8,Color.DKGRAY,false);
        JSONArray cycles=evidence.optJSONArray("cycles");
        int len=cycles==null?0:cycles.length();
        int start=chunk*3,end=Math.min(start+3,len);
        float y=108;
        for(int i=start;i<end;i++){
            JSONObject q=cycles.optJSONObject(i);
            if(q==null)continue;
            box(c,30,y-13,784,22,0xFFEDF2F6);
            text(c,"#"+(i+1)+" "+shorten(q.optString("measurement_session_id",""),37),
                38,y,8.1f,Color.BLACK,true);
            text(c,"Source SHA "+shorten(q.optString("research_source_sha256",""),22),
                480,y,7.2f,Color.DKGRAY,false);
            y+=20;
            text(c,"Original rows "+q.optInt("original_archived_telemetry_rows",0)+
                " / MQ V6 evaluated "+q.optInt("independently_evaluated_sensor_models",0)+
                " / identity UNKNOWN",38,y,7.5f,0xFF8B2B24,false);
            y+=17;
            JSONArray sensor=q.optJSONArray("sensors");
            if(sensor!=null)for(int j=0;j<sensor.length();j++){
                JSONObject s=sensor.optJSONObject(j);
                if(s==null)continue;
                String state=s.optString("state",""),
                    model=s.optString("sensor_model","—");
                String raw="Rs —",r0="R0 —";
                if(s.has("selected_original_rs"))
                    raw="Rs "+fmt(s.optDouble("selected_original_rs",Double.NaN));
                if(s.has("selected_original_r0")&&!s.isNull("selected_original_r0"))
                    r0="R0 "+fmt(s.optDouble("selected_original_r0",Double.NaN));
                text(c,model+" • scans "+s.optInt("distinct_original_scan_cycles",0)+
                    " • "+raw+" "+r0+" • "+shorten(state,34),
                    43,y,6.9f,Color.DKGRAY,false);
                y+=10;
            }
            y+=12;
        }
        text(c,"لا يتم خلط قنوات بأوقات مختلفة أو تحويل فرضيات المصدر إلى إنذار أو قيمة ppm.",
            36,546,7.6f,Color.DKGRAY,false);
    }

    private static void candidateStability(Canvas c,RectF r,List<Evidence> all){box(c,r.left,r.top,r.width(),r.height(),0xFFF8FAFC);text(c,"تكرار المرشح الأعلى",r.left+12,r.top+22,10,Color.BLACK,true);Map<String,Integer> m=new LinkedHashMap<>();for(Evidence e:all){if(!e.chemOk())continue;String k=e.topCandidate();if(k.isEmpty()||"—".equals(k))continue;m.put(k,m.containsKey(k)?m.get(k)+1:1);}int max=1;for(Integer n:m.values())max=Math.max(max,n);float y=r.top+50;int shown=0;for(Map.Entry<String,Integer> q:m.entrySet()){if(shown++>=5)break;float w=(r.width()-145)*(q.getValue()/(float)max);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xFF1565C0);c.drawRect(r.left+130,y-11,r.left+130+w,y+3,p);text(c,shorten(q.getKey(),18),r.left+10,y,7.5f,Color.DKGRAY,false);text(c,String.valueOf(q.getValue()),r.right-18,y,7.5f,Color.BLACK,true,Paint.Align.RIGHT);y+=25;}}
    private static void posteriorStability(Canvas c,RectF r,List<Evidence> all){box(c,r.left,r.top,r.width(),r.height(),0xFFF8FAFC);text(c,"Posterior عبر الجلسات",r.left+12,r.top+22,10,Color.BLACK,true);float base=r.bottom-30,left=r.left+32,right=r.right-18;Paint ax=new Paint(Paint.ANTI_ALIAS_FLAG);ax.setColor(0xFFB0BEC5);c.drawLine(left,base,right,base,ax);int n=0;for(Evidence e:all)if(Double.isFinite(e.posterior()))n++;if(n==0)return;float step=(right-left)/Math.max(1,n-1);int i=0;for(Evidence e:all){double p=e.posterior();if(!Double.isFinite(p))continue;float x=left+i++*step,y=base-(float)Math.max(0,Math.min(1,p))*(r.height()-60);Paint dot=new Paint(Paint.ANTI_ALIAS_FLAG);dot.setColor(0xFFC62828);dot.setStyle(Paint.Style.FILL);c.drawCircle(x,y,5,dot);}}

    private static String topCandidate(JSONObject q){if(q==null)return"—";String c=q.optString("chemical","").trim();if(!c.isEmpty()&&!"—".equals(c))return c;JSONArray a=q.optJSONArray("candidates");if(a!=null&&a.length()>0){JSONObject x=a.optJSONObject(0);if(x!=null){String v=x.optString("label","").trim();if(!v.isEmpty())return v;}}return"—";}
    private static double topPosterior(JSONObject q){if(q==null)return Double.NaN;double p=q.optDouble("top_posterior",Double.NaN);if(Double.isFinite(p))return p;JSONArray a=q.optJSONArray("candidates");if(a!=null&&a.length()>0){JSONObject x=a.optJSONObject(0);if(x!=null)return x.optDouble("posterior",Double.NaN);}return Double.NaN;}
    private static int validMq(JSONObject q){if(q==null)return-1;JSONObject x=q.optJSONObject("measurement_quality");if(x==null)x=q.optJSONObject("lab_quality");return x==null?-1:x.optInt("valid_mq_channels",x.optInt("valid_channels",-1));}
    private static void metric(Canvas c,float x,float y,float w,float h,String label,String value,String note,int color){box(c,x,y,w,h,color);text(c,label,x+10,y+18,8.2f,Color.DKGRAY,true);text(c,shorten(value,26),x+10,y+41,11,Color.BLACK,true);text(c,shorten(note,34),x+10,y+59,7.1f,Color.DKGRAY,false);}
    private static void title(Canvas c,String s,float y){text(c,s,42,y,18,Color.BLACK,true);} private static void labelValue(Canvas c,String k,String v,float y){text(c,k,42,y,8.3f,Color.DKGRAY,true);text(c,shorten(v,62),180,y,8.8f,Color.BLACK,false);} private static void box(Canvas c,float x,float y,float w,float h,int color){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(x,y,x+w,y+h),7,7,p);} private static void emptyBox(Canvas c,RectF r,String m){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xFFF3F3F3);c.drawRect(r,p);text(c,m,r.centerX(),r.centerY(),9,Color.DKGRAY,false,Paint.Align.CENTER);}
    private static void chrome(Canvas c,int page,int total,FieldSessionReportExporter.Bundle b,ReportIntelligence.ReportPlan plan){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xFFB0B0B0);p.setStrokeWidth(.7f);c.drawLine(MARGIN,32,PAGE_WIDTH-MARGIN,32,p);c.drawLine(MARGIN,PAGE_HEIGHT-28,PAGE_WIDTH-MARGIN,PAGE_HEIGHT-28,p);text(c,"HazRob "+ReleaseAudit.VERSION+" • HR069",MARGIN,23,10,Color.BLACK,true);text(c,b.site.name+" • "+b.fieldSession.name+" • "+plan.modeAr,PAGE_WIDTH/2f,23,9.5f,Color.BLACK,true,Paint.Align.CENTER);text(c,"صفحة "+page+" من "+total,MARGIN,PAGE_HEIGHT-10,8.5f,Color.DKGRAY,false);text(c,ReleaseAudit.PROJECT_PREPARERS,PAGE_WIDTH-MARGIN,PAGE_HEIGHT-10,8.5f,Color.DKGRAY,false,Paint.Align.RIGHT);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){text(c,s,x,y,size,color,bold,Paint.Align.LEFT);} private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setFakeBoldText(bold);p.setTextAlign(align);c.drawText(s==null?"":s,x,y,p);} private static String pct(double v){return Double.isFinite(v)?String.format(Locale.US,"%.1f%%",v<=1.000001?v*100:v):"—";} private static String fmt(double v){return Double.isFinite(v)?String.format(Locale.US,"%.3f",v):"—";} private static String shorten(String s,int n){String v=s==null?"":s;return v.length()<=n?v:v.substring(0,Math.max(1,n-1))+"…";}
}