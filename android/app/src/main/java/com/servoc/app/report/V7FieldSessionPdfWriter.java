package com.servoc.app.report;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import com.servoc.app.chemical.GhsPictograms;
import com.servoc.app.exposure.ExposureRepository;
import com.servoc.app.data.ExperimentsDb;
import org.json.JSONObject;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * V7 field-session report renderer, backed solely by an immutable, verified,
 * approved archive contract.  NO ChemCore scoring, R0 calibration, candidate
 * inference, source correction, exposure gating or other scientific math here.
 *
 * Stage 1: layout-only visualization. All charts are deliberately empty until
 * the source-series renderer is integrated; do not replace the existing
 * production SmartFieldSessionPdfWriter prematurely.
 */
public final class V7FieldSessionPdfWriter {
    private V7FieldSessionPdfWriter(){}
    private static final float MM=72f/25.4f, X=5f*MM, Y=27f*MM;
    private static final float BODY_W=287f*MM, BODY_H=163f*MM;
    private static final String DASH="-";
    private static final float[] COLUMNS_MM={35f,58f,40f,29f,55f,70f};
    private static final String[] CANDIDATE_HEADERS={
        "GHS / السمية","المادة / CAS / UID",
        "الترتيب والدليل","PPM","الحد المرجعي","المقارنة PPM / Limit"
    };
    private static final String[] PHYSICAL={
        "Temperature / الحرارة °C","Humidity / الرطوبة %RH",
        "Pressure / الضغط hPa","Altitude / الارتفاع m",
        "Noise / الضوضاء dBA","Vibration / الاهتزاز",
        "Ionizing Radiation / الإشعاع","UV / الأشعة فوق البنفسجية",
        "Wind Speed / سرعة الرياح","Wind Direction / اتجاه الرياح",
        "Illuminance / الاستضاءة lux","Luminous Flux / التدفق الضوئي lm",
        "PM1 / جسيمات","PM2.5 / جسيمات","PM10 / جسيمات"
    };

    private static float mm(float a){return a*MM;}
    private static RectF rect(float x,float y,float w,float h){
        return V7FieldChartLayout.mmRect(x,y,w,h);
    }
    private static String clean(String s){
        return s==null||s.trim().isEmpty()?DASH:s;
    }
    private static String val(int n){return Integer.toString(n);}
    private static void text(Canvas c,String s,RectF box,boolean rtl,boolean bold){
        V7ReportLayoutKit.text(c,clean(s),box,rtl,bold,12,V7ReportLayoutKit.INK);
    }
    private static void title(Canvas c,String s,float y){
        V7ReportLayoutKit.text(c,s,rect(7,y,282,11),true,true,14,V7ReportLayoutKit.INK);
    }
    private static void frame(Canvas c,RectF r){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);p.setColor(0xFF303030);
        p.setStrokeWidth(.7f);c.drawRect(r,p);
    }
    private static void labelValue(Canvas c,float y,String label,String value){
        V7ReportLayoutKit.tableHeader(c,rect(183,y,109,12),label,true);
        V7ReportLayoutKit.tableValue(c,rect(5,y,178,12),value,false);
    }
    private static PdfDocument.Page open(PdfDocument pdf,int page){
        return V7ReportLayoutKit.open(pdf,page,true);
    }
    private static void chrome(Canvas c,Context ctx,int no,int total){
        V7ReportLayoutKit.drawHeader(c,ctx,true,
            "تقرير الجلسة الميدانية المعتمدة",
            "Approved Field Session | Research Evidence",DASH);
        V7ReportLayoutKit.drawFooter(c,true,no,total);
    }
    private static int candidatePages(V7FieldReportSourceSnapshot s){
        int n=s==null?0:s.rawCandidates.size();
        return Math.max(1,(n+3)/4);
    }
    private static int cyclePages(V7FieldReportSourceSnapshot s){
        int n=s==null?0:s.auditCycles.size();
        return Math.max(1,(n+2)/3);
    }
    private static int sourcePages(V7FieldReportSourceSnapshot s){
        int n=s==null?0:s.measurementSessionIds.size();
        return Math.max(1,(n+8)/9);
    }
    private static boolean withPhysical(V7FieldReportSourceSnapshot s){
        return s==null||s.approvedPhysical;
    }
    private static boolean withParticles(V7FieldReportSourceSnapshot s){
        return s==null||s.approvedParticles;
    }
    private static void summary(Canvas c,V7FieldReportSourceSnapshot s){
        title(c,"ملخص الجلسة / Session Executive Overview",28);
        String[][] lines={
            {"الموقع / Site",s==null?DASH:clean(s.siteName)},
            {"معرف الموقع / Site ID",s==null?DASH:clean(s.siteId)},
            {"الجلسة / Field Session",s==null?DASH:clean(s.fieldSessionName)},
            {"معرف الجلسة / Session ID",s==null?DASH:clean(s.fieldSessionId)},
            {"حالة الاعتماد / Approval",s==null?DASH:clean(s.approvalStatus)},
            {"الجلسة التحليلية المصدرية",s==null?DASH:clean(s.primaryMeasurementSessionId)},
            {"جلسات القياس / Measurements",s==null?DASH:val(s.measurementSessionIds.size())},
            {"الدورات المعتمدة / Approved cycles",s==null?DASH:val(s.approvedCycles)},
            {"Gas / Physical / Particles",s==null?DASH:
                (s.approvedGas?"G":"-")+" / "+(s.approvedPhysical?"F":"-")+
                " / "+(s.approvedParticles?"P":"-")}
        };
        float yy=45;
        for(String[] line:lines){labelValue(c,yy,line[0],line[1]);yy+=14;}
        title(c,"تفسير نتائج المصدر / Source Status",174);
        text(c,"تعود المقارنات إلى دورات مؤرشفة ومعتمدة. الترشيح البحثي ليس إثبات هوية غاز أو قياس PPM معتمد.",
            rect(7,184,280,6),true,false);
    }
    private static void candidateTable(Canvas c,V7FieldReportSourceSnapshot s,int page,
            V7FieldSourceReferences refs,Context ctx){
        title(c,"جدول جميع المرشحين / Complete Source Candidates",27);
        float x=5;
        // Six columns in the exact owner-approved order, from RIGHT to LEFT.
        for(int i=0;i<COLUMNS_MM.length;i++){
            float w=COLUMNS_MM[i],left=297-x-w;
            V7ReportLayoutKit.tableHeader(c,rect(left,42,w,18),
                CANDIDATE_HEADERS[i],true);
            x+=w;
        }
        int total=s==null?0:s.rawCandidates.size();
        int begin=page*4,end=Math.min(total,begin+4);
        if(total==0){
            candidateRow(c,60,null,refs,ctx);
        } else {
            for(int i=begin;i<end;i++)
                candidateRow(c,60+(i-begin)*32,s.rawCandidates.get(i),refs,ctx);
        }
    }
    private static String sourceLimitText(V7FieldSourceReferences refs,
             V7FieldReportSourceSnapshot.Candidate candidate){
        if(candidate==null)return DASH;
        ExposureRepository.Limit first=null;
        int n=0;
        for(V7FieldSourceReferences.Ref e:refs.limits){
            if(e.candidate!=candidate)continue;
            if(first==null)first=e.limit;
            n++;
        }
        if(first==null)return DASH;
        String v=first.numeric()?String.valueOf(first.value):DASH;
        return clean(first.authority)+"/"+clean(first.type)+
            "\\n"+v+" "+clean(first.unit)+(n>1?"\\n+"+(n-1)+" refs":"");
    }
    private static void candidateRow(Canvas c,float y,
             V7FieldReportSourceSnapshot.Candidate candidate,
             V7FieldSourceReferences refs,Context ctx){
        String name=candidate==null?DASH:clean(candidate.label);
        String identity=candidate==null?DASH:
            "CAS: "+clean(candidate.cas)+"\nUID: "+clean(candidate.uid);
        String evidence=candidate==null?DASH:
            "S"+candidate.measurementOrdinal+" #"+(candidate.sourceIndex+1)+
                "\n"+clean(candidate.evidenceTier);
        String[] vals={DASH,name+"\n"+identity,evidence,DASH,sourceLimitText(refs,candidate),""};
        float cursor=5;
        for(int i=0;i<COLUMNS_MM.length;i++){
            float w=COLUMNS_MM[i],left=297-cursor-w;
            RectF r=rect(left,y,w,32);
            if(i==5)V7FieldChartLayout.blankPpmMicro(c,r);
            else if(i==0&&candidate!=null&&ctx!=null&&
                    !refs.pictograms(candidate).isEmpty()){
                frame(c,r);
                List<String> codes=refs.pictograms(candidate);
                for(int j=0;j<codes.size();j+=3){
                    int end=Math.min(codes.size(),j+3);
                    GhsPictograms.drawOnPdf(c,ctx,codes.subList(j,end),
                        r.left+mm(2),r.top+mm(2)+(j/3)*mm(10),
                        mm(8),mm(1),3);
                }
            } else V7ReportLayoutKit.tableValue(c,r,vals[i],i!=3);
            cursor+=w;
        }
    }
    private static int referencePages(V7FieldSourceReferences r){
        return Math.max(1,(r.limits.size()+2)/3);
    }
    /** All source references, no derived ratios or threshold classifications. */
    private static void referenceAnnex(Canvas c,V7FieldSourceReferences refs,int page){
        title(c,"مراجع التعرض / All Source Exposure References",28);
        V7ReportLayoutKit.tableHeader(c,rect(5,42,287,10),
            "Occupational reference only - not a Tank exposure determination",false);
        int total=refs.limits.size(),first=page*3,end=Math.min(total,first+3);
        if(total==0){
            V7ReportLayoutKit.tableValue(c,rect(5,54,287,32),DASH,false);
            return;
        }
        for(int i=first;i<end;i++){
            V7FieldSourceReferences.Ref e=refs.limits.get(i);
            ExposureRepository.Limit ref=e.limit;
            float y=54+(i-first)*44;
            String heading="Session #"+e.candidate.measurementOrdinal+
                " / Candidate #"+(e.candidate.sourceIndex+1)+
                " / "+clean(e.candidate.label);
            V7ReportLayoutKit.tableHeader(c,rect(5,y,287,11),heading,false);
            String amount=ref.numeric()?String.valueOf(ref.value):DASH;
            String period=ref.durationMin==null?DASH:String.valueOf(ref.durationMin);
            String label="Authority: "+clean(ref.authority)+
                " / Jurisdiction: "+clean(ref.jurisdiction)+
                " / Type: "+clean(ref.type)+" / "+amount+" "+clean(ref.unit)+
                " / Duration (min): "+period;
            String source="Source ID: "+clean(ref.sourceId)+
                " / Revision: "+clean(ref.revision)+
                " / Notation: "+clean(ref.notation);
            V7ReportLayoutKit.tableValue(c,rect(5,y+11,287,16),label,false);
            V7ReportLayoutKit.tableValue(c,rect(5,y+27,287,17),source,false);
        }
    }

    private static void gallery(Canvas c,String[] t,String[] x,String[] y){
        RectF[] rects=V7FieldChartLayout.fourEqual();
        for(int i=0;i<4;i++)V7ReportLayoutKit.blankChart(
            c,rects[i],t[i],x[i],y[i]);
    }
    private static void secondGallery(Canvas c){
        gallery(c,new String[]{
            "Pearson / ارتباط القنوات","t50/t90 / الاستجابة الزمنية",
            "AUC / المساحة تحت المنحنى","Stability / ثبات النتائج"
        },new String[]{"MQ","Recorded time","MQ","Cycle"},
          new String[]{"Correlation","Response","AUC","Stability"});
    }
    private static void chemGallery(Canvas c){
        gallery(c,new String[]{
            "ChemCore / مسار القرار","تقنيات ومصادر الدليل / Evidence",
            "اتفاق المراجع / Source Agreement","ترتيب المرشحين / Candidate Rank"
        },new String[]{"Stage","Technique","Source","Candidate"},
          new String[]{"Decision","Evidence","Agreement","Rank"});
    }
    private static void sourceGallery(Canvas c){
        gallery(c,new String[]{
            "Source Curves / منحنيات المصدر","Residual / بواقي الملاءمة",
            "Research PPM / التركيز البحثي","Fingerprint Reference / بصمة المصدر"
        },new String[]{"Source","Observation","Observation","MQ"},
          new String[]{"Response","Residual","ppm","Reference"});
    }
    private static void physicalMatrix(Canvas c){
        title(c,"جميع القياسات الفيزيائية / Physical Matrix",28);
        // 3 blocks of 5 cells; every catalog name stays visible even if absent.
        // A complete archived physical reading requires a separate verified
        // cycle-level physical payload, unavailable in current source contract.
        for(int i=0;i<PHYSICAL.length;i++){
            int col=i/5,row=i%5;
            float bx=5+col*95.67f;
            float y=45+row*25.5f;
            V7ReportLayoutKit.tableHeader(c,rect(bx,y,95.67f,12f),
                PHYSICAL[i],false);
            V7ReportLayoutKit.tableValue(c,rect(bx,y+12,95.67f,13.5f),
                DASH,false);
        }
        text(c,"حالات الحدود المهنية لا تُستنتج من Snapshot واحد؛ يلزم المرجع والمدة ووحدة القياس وحالة محرك القياس.",
             rect(8,181,279,10),true,false);
    }
    private static void particleCharts(Canvas c){
        title(c,"الجسيمات / Particle Evidence",28);
        RectF[] g=V7FieldChartLayout.fourEqual();
        String[] t={"PM1 / الزمن","PM2.5 / الزمن","PM10 / الزمن",
            "PM Reference / المرجع"};
        for(int i=0;i<4;i++)V7ReportLayoutKit.blankChart(
            c,g[i],t[i],"Recorded Time","µg/m³");
    }
    private static void cycles(Canvas c,V7FieldReportSourceSnapshot s,int idx){
        title(c,"سجل جميع الدورات / Complete Cycle Provenance",28);
        int n=s==null?0:s.auditCycles.size();
        int st=idx*3,end=Math.min(n,st+3);
        if(n==0){
            V7ReportLayoutKit.tableValue(c,rect(5,49,287,38),DASH,false);
            return;
        }
        for(int i=st;i<end;i++){
            V7FieldReportSourceSnapshot.Cycle r=s.auditCycles.get(i);
            float y=43+(i-st)*46;
            String key="Cycle #"+r.sequence+"  / Attempt "+r.attempt+
                "  / "+clean(r.state)+"  / "+(r.approved?"APPROVED":"NOT APPROVED");
            V7ReportLayoutKit.tableHeader(c,rect(5,y,287,12),key,false);
            String detail="G:"+(r.gas?"Y":"-")+" / F:"+(r.physical?"Y":"-")+
                " / P:"+(r.particles?"Y":"-")+"  |  "+clean(r.startedUtc)+
                "  |  "+clean(r.approvedUtc)+"\nMeasurement ID: "+
                clean(r.measurementId)+"\nSHA-256: "+clean(r.evidenceSha256);
            V7ReportLayoutKit.tableValue(c,rect(5,y+12,287,34),detail,false);
        }
        text(c,"Failed/retired attempts are audit evidence only. Approved measurements remain independent.",
            rect(6,185,285,7),false,false);
    }
    private static void sourceAnnex(Canvas c,V7FieldReportSourceSnapshot s,int idx){
        title(c,"المصادر الأصلية والتتبّع / Source Identity Annex",28);
        if(s==null||s.measurementSessionIds.isEmpty()){
            V7ReportLayoutKit.tableValue(c,rect(5,44,287,17),DASH,false);
        }else{
            int st=idx*9,end=Math.min(s.measurementSessionIds.size(),st+9);
            for(int i=st;i<end;i++){
                V7ReportLayoutKit.tableHeader(c,rect(5,45+15*(i-st),45,15),
                    "Session #"+(i+1),false);
                V7ReportLayoutKit.tableValue(c,rect(50,45+15*(i-st),242,15),
                    clean(s.measurementSessionIds.get(i)),false);
            }
        }
        V7ReportLayoutKit.tableHeader(c,rect(5,181,287,9),
            "Audit status / حالة سلامة المصدر",false);
        // Keep the source contract label only; never reproduce an enormous
        // JSON body inside a 12pt fixed-size text box or silently truncate it.
        text(c,s==null?DASH:clean(s.contractEvidence),
            rect(8,190,277,8),false,false);
    }
    private static void render(Context ctx,OutputStream out,
              V7FieldReportSourceSnapshot source,
              V7FieldSourceReferences references)throws Exception{
        if(out==null)throw new IllegalArgumentException("Output stream required");
        if(references==null)throw new IllegalArgumentException("Source references required");
        int cp=candidatePages(source),rp=referencePages(references);
        int cy=cyclePages(source),sp=sourcePages(source);
        boolean physical=withPhysical(source),pm=withParticles(source);
        int total=1+cp+rp+5+(physical?1:0)+(pm?1:0)+cy+sp;
        PdfDocument pdf=new PdfDocument();
        try {
            for(int p=1;p<=total;p++){
                PdfDocument.Page page=open(pdf,p);
                Canvas canvas=page.getCanvas();canvas.drawColor(Color.WHITE);
                chrome(canvas,ctx,p,total);
                int slot=p-1;
                if(slot==0)summary(canvas,source);
                else {
                    slot--;
                    if(slot<cp)candidateTable(canvas,source,slot,references,ctx);
                    else {
                        slot-=cp;
                        if(slot<rp)referenceAnnex(canvas,references,slot);
                        else {
                            slot-=rp;
                            if(slot==0)V7FieldChartLayout.drawFirstFour(canvas);
                            else if(slot==1)secondGallery(canvas);
                            else if(slot==2)V7FieldChartLayout.drawAdvancedPage(canvas);
                            else if(slot==3)chemGallery(canvas);
                            else if(slot==4)sourceGallery(canvas);
                            else {
                                slot-=5;
                                int phys=physical?1:0,part=pm?1:0;
                                if(physical&&slot==0)physicalMatrix(canvas);
                                else if(pm&&slot==phys)particleCharts(canvas);
                                else {
                                    int rest=slot-phys-part;
                                    if(rest>=0&&rest<cy)cycles(canvas,source,rest);
                                    else sourceAnnex(canvas,source,rest-cy);
                                }
                            }
                        }
                    }
                }
                pdf.finishPage(page);
            }
            pdf.writeTo(out);
        }finally{pdf.close();}
    }
    /** Preview exercises the actual R03 PDF page geometry with no source values. */
    public static void writeBlankDesignFixture(Context ctx,OutputStream out)
            throws Exception{
        render(ctx,out,null,V7FieldSourceReferences.noData());
    }
    /** Approved, source-verified archival data only; no real-time telemetry. */
    public static void writePdf(Context ctx,OutputStream out,ExperimentsDb db,
                                String approvedFieldSessionId)throws Exception{
        V7FieldReportSourceSnapshot source=
            V7FieldReportSourceSnapshot.resolve(db,approvedFieldSessionId);
        V7FieldSourceReferences references=V7FieldSourceReferences.from(db,source);
        render(ctx,out,source,references);
    }
}
