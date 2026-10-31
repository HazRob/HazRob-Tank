package com.servoc.app.report;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import java.io.OutputStream;

/**
 * V7 R04 SITE REPORT: owner-facing VISUAL DESIGN ONLY.
 *
 * This Android PdfDocument writer renders the visual architecture of the
 * twelve distinct report sections without inventing a site, measurement,
 * material identity, gas concentration, limit or hazard status.
 *
 * Deliberately NOT connected to SiteSummaryReportExporter.writePdf.
 * Source pagination, archived G/F/P values and real chart bindings must
 * be introduced only in the later implementation phase.
 *
 * No test/QA execution was requested for this design checkpoint.
 */
public final class V7SiteVisualPdfWriter {
    private V7SiteVisualPdfWriter() {}

    private static final float MM = V7ReportLayoutKit.mm(1f);
    private static final String DASH = "-";
    private static final String[] PAGE_KEYS = {
        "S01_EXECUTIVE", "S02_SESSIONS", "S03_SITE_COMPARISONS",
        "S04_MATERIAL_144", "S05_MATERIAL_CATALOG",
        "S06_ALL_CANDIDATES", "S07_EXPOSURE_REFERENCES",
        "S08_PHYSICAL_15", "S09_PARTICLE_EVIDENCE",
        "S10_RECURRENCE", "S11_CYCLE_AUDIT", "S12_SCOPE_AND_SOURCES"
    };
    private static final String[] PHYSICAL = {
        "Temperature / الحرارة °C", "Humidity / الرطوبة %RH",
        "Pressure / الضغط hPa", "Altitude / الارتفاع m",
        "Noise / الضوضاء dBA", "Vibration / الاهتزاز",
        "Ionizing Radiation / الإشعاع", "UV / الأشعة فوق البنفسجية",
        "Wind Speed / سرعة الرياح", "Wind Direction / اتجاه الرياح",
        "Illuminance / الإضاءة lux", "Luminous Flux / التدفق lm",
        "PM1 / الجسيمات", "PM2.5 / الجسيمات", "PM10 / الجسيمات"
    };
    private static final float[] CANDIDATE_WIDTHS_MM = {
        35, 58, 40, 29, 55, 70
    };
    private static final String[] CANDIDATE_HEADERS = {
        "GHS / التصنيف", "المادة / CAS / UID", "الترتيب / الدليل",
        "PPM", "مرجع التعرض", "PPM / المرجع"
    };

    private static RectF mmRect(float x, float y, float width, float height) {
        return new RectF(x*MM, y*MM, (x+width)*MM, (y+height)*MM);
    }

    private static void text(Canvas c, String value, float x, float y,
                             float width, float height, boolean rtl, boolean bold) {
        V7ReportLayoutKit.text(c, value, mmRect(x,y,width,height),
            rtl,bold,12,V7ReportLayoutKit.INK);
    }

    private static void title(Canvas c, String value) {
        text(c,value,6,28,284,12,true,true);
    }

    private static void header(Canvas c, Context context, int page, int total) {
        V7ReportLayoutKit.drawHeader(c,context,true,
            "التقرير التجميعي للموقع", "Approved Site Comparison | Design Only", DASH);
        V7ReportLayoutKit.drawFooter(c,true,page,total);
    }

    private static void headerCell(Canvas c,float x,float y,float w,float h,String name) {
        V7ReportLayoutKit.tableHeader(c,mmRect(x,y,w,h),name,true);
    }

    private static void valueCell(Canvas c,float x,float y,float w,float h,String value) {
        V7ReportLayoutKit.tableValue(c,mmRect(x,y,w,h),value,false);
    }

    private static void executive(Canvas c) {
        title(c,"ملخص الموقع والمشاهدات / Site Executive Summary");
        String[][] details = {
            {"الموقع / Site name",DASH},
            {"Site ID / معرف الموقع",DASH},
            {"Original capture UTC / فترة القياس",DASH},
            {"Source status / الحالة المؤرشفة",DASH}
        };
        for(int i=0;i<details.length;i++){
            float y=43+i*13f;
            headerCell(c,166,y,126,12,details[i][0]);
            valueCell(c,5,y,161,12,details[i][1]);
        }
        String[] kpis = {
            "Approved field sessions / الجلسات",
            "Measurement sessions / القياسات",
            "Approved cycles / الدورات",
            "Gas / الغاز",
            "Physical / الفيزيائي",
            "Particles / الجسيمات",
            "Engine outcomes / النتائج",
            "Source quality / جودة الدليل"
        };
        float startY=99f, width=69f, height=35f, spacing=3.67f;
        for(int i=0;i<kpis.length;i++){
            int row=i/4,col=i%4;
            float x=5+col*(width+spacing),y=startY+row*(height+4);
            headerCell(c,x,y,width,15,kpis[i]);
            valueCell(c,x,y+15,width,20,DASH);
        }
        text(c,"GHS / Occupational references: -",7,181,283,8,false,false);
    }

    private static void sessions(Canvas c) {
        title(c,"مقارنة جميع الجلسات / Complete Session Matrix");
        float[] widths={58,48,48,27,31,36,39};
        String[] heads={"Session ID","Capture start UTC","Capture end UTC",
            "G/F/P","Cycles","Source tier","Engine status"};
        float x=5;
        for(int i=0;i<widths.length;i++){
            headerCell(c,x,43,widths[i],18,heads[i]);
            x+=widths[i];
        }
        // 3 large design rows demonstrate whole-ID wrapping at >=12pt.
        // NOT a three-session production cap.
        for(int row=0;row<3;row++){
            x=5;
            for(float w:widths){
                valueCell(c,x,61+row*35,w,35,DASH);
                x+=w;
            }
        }
        text(c,"التصميم يتمدد لكل الجلسات المؤرشفة، بلا حد top-N أو قص المعرفات.",
             7,176,282,12,true,false);
    }

    private static void comparisons(Canvas c) {
        RectF[] plots=V7FieldChartLayout.fourEqual();
        String[] names={
            "التسلسل الزمني / Session Timeline",
            "الجلسات × G/F/P / Measurement Family Matrix",
            "حالات التحليل / Engine Outcome by Session",
            "الأدلة المرجعية / Candidate Evidence by Session"
        };
        String[] x={"Capture time UTC","Field session","Field session","Field session"};
        String[] y={"Session ID","G / F / P","Engine status","Evidence level"};
        for(int i=0;i<plots.length;i++)
            V7ReportLayoutKit.blankChart(c,plots[i],names[i],x[i],y[i]);
    }

    private static void material144(Canvas c) {
        title(c,"شبكة تكرار المواد 144 / Complete Material Recurrence");
        // 12x12 = 144 visible source slots; no values or colored evidence.
        final float left=5f, top=43f, cw=14.3f, ch=11.7f;
        for(int r=0;r<12;r++){
            for(int col=0;col<12;col++){
                valueCell(c,left+col*cw,top+r*ch,cw,ch,DASH);
            }
        }
        float noteX=181f;
        headerCell(c,noteX,43,111,18,"التفسير / Legend");
        String[] legend={
            "Index != concentration", "Source-qualified recurrence: -",
            "Observed sessions: -", "Reference classification: -",
            "No inferred chemical identity",
            "No simulated heatmap values"
        };
        for(int i=0;i<legend.length;i++)
            valueCell(c,noteX,61+i*20,111,20,legend[i]);
        text(c,"المربعات 144 محفوظة للتصميم؛ الألوان والنتائج لن تظهر دون مصدر مؤهل.",
            5,184,287,9,true,false);
    }

    private static void materialCatalog(Canvas c) {
        title(c,"فهرس المواد 144 / Full Source Material Catalog");
        String[] heads={"الموقع/الفهرس","Chemical UID","CAS / الاسم","مرجع المصدر","التكرار الموثق"};
        float[] widths={44,48,91,59,45};
        float x=5;
        for(int i=0;i<widths.length;i++){
            headerCell(c,x,43,widths[i],20,heads[i]);
            x+=widths[i];
        }
        // Only empty sample cells. Later source pagination must preserve all
        // documented source records rather than synthesizing 144 identities.
        for(int i=0;i<4;i++){
            x=5;
            for(float width:widths){valueCell(c,x,63+i*26,width,26,DASH);x+=width;}
        }
        text(c,"جميع المواد الحقيقية ومعرفاتها ومراجعها في صفحات متابعة ديناميكية.",
             5,174,287,15,true,false);
    }

    private static void candidates(Canvas c) {
        title(c,"جميع المرشحين مع المراجع / All Candidates, GHS & OEL");
        float cursor=5;
        for(int i=0;i<CANDIDATE_WIDTHS_MM.length;i++){
            float w=CANDIDATE_WIDTHS_MM[i];
            float x=297-cursor-w;
            headerCell(c,x,43,w,18,CANDIDATE_HEADERS[i]);
            cursor+=w;
        }
        // Blank source/candidate placeholders only; no rank is invented.
        for(int row=0;row<3;row++){
            cursor=5;
            for(int i=0;i<CANDIDATE_WIDTHS_MM.length;i++){
                float w=CANDIDATE_WIDTHS_MM[i],x=297-cursor-w;
                RectF slot=mmRect(x,61+row*38,w,38);
                if(i==5)V7FieldChartLayout.blankPpmMicro(c,slot);
                else V7ReportLayoutKit.tableValue(c,slot,DASH,false);
                cursor+=w;
            }
        }
        text(c,"رموز GHS مرجعية للمادة ولا تثبت أن العينة احتوتها.",
            5,179,287,11,true,false);
    }

    private static void exposureReferences(Canvas c) {
        title(c,"المراجع المهنية المتعددة / Occupational Limit Annex");
        headerCell(c,5,43,287,14,"SOURCE REFERENCE ONLY / لا يُستنتج تجاوز TWA أو STEL من لقطة");
        for(int i=0;i<3;i++){
            float y=60+i*40f;
            headerCell(c,5,y,287,11,"Candidate / Source / Authority / Type");
            valueCell(c,5,y+11,287,28,
                "Chemical UID: -   Authority: -   Jurisdiction: -   Type: -\n"+
                "Value: -   Unit: -   Duration: -   Revision: -   SDS/source: -");
        }
        text(c,"يجري التمدد لكل مراجع المصدر دون دمج سلطات أو فترات غير متكافئة.",
            5,184,287,10,true,false);
    }

    private static void physical(Canvas c) {
        title(c,"مصفوفة المؤشرات الفيزيائية / All Fifteen Metrics");
        for(int i=0;i<PHYSICAL.length;i++){
            int col=i/5,row=i%5;
            float x=5+col*95.67f,y=45+row*26f;
            headerCell(c,x,y,95.67f,12,PHYSICAL[i]);
            valueCell(c,x,y+12,95.67f,14,DASH);
        }
        text(c,"قراءات كل جلسة ووحداتها وحالة المصدر على صفحات متابعة دون اختزال.",
            5,181,287,10,true,false);
    }

    private static void particles(Canvas c) {
        RectF[] plots=V7FieldChartLayout.fourEqual();
        String[] names={
            "PM1 / الجسيمات الدقيقة عبر الجلسات",
            "PM2.5 / الجسيمات الدقيقة عبر الجلسات",
            "PM10 / الجسيمات الدقيقة عبر الجلسات",
            "Particle source coverage / اكتمال المصدر"
        };
        for(int i=0;i<plots.length;i++)
            V7ReportLayoutKit.blankChart(c,plots[i],names[i],
                i==3?"Field session":"Recorded UTC time",
                i==3?"Source status":"µg/m³");
    }

    private static void recurrence(Canvas c) {
        RectF[] plots=V7FieldChartLayout.fourEqual();
        String[] names={
            "تكرار المرشح / Candidate Recurrence",
            "الجلسة × المستشعر / Sensor-Session Response",
            "تباين أدلة الجلسات / Evidence Variation",
            "المقارنة حسب صلاحية الدليل / Source Tier by Session"
        };
        String[] x={"Chemical UID","MQ Sensor","Field session","Field session"};
        String[] y={"Approved sessions","Engine response","Evidence state","Evidence tier"};
        for(int i=0;i<plots.length;i++)
            V7ReportLayoutKit.blankChart(c,plots[i],names[i],x[i],y[i]);
    }

    private static void cycleAudit(Canvas c) {
        title(c,"الدورات والأدلة / Complete Cycle Audit Ledger");
        for(int i=0;i<3;i++){
            float y=43+i*46f;
            headerCell(c,5,y,287,13,
                "Field ID: -  |  Cycle: -  |  Attempt: -  |  G/P/F: -  |  State: -");
            valueCell(c,5,y+13,287,33,
                "Start UTC: -    Complete UTC: -    Approved UTC: -\n"+
                "Measurement ID: -    Device ID: -\nSHA-256: -");
        }
        text(c,"المحاولات غير المعتمدة تظهر كأحداث تدقيق لا كقراءات مقبولة.",
            5,184,287,11,true,false);
    }

    private static void scopeAndSources(Canvas c) {
        title(c,"نطاق الاستنتاجات والمراجع / Scientific Scope & Sources");
        String[][] entries={
            {"OBSERVED / مرصود","-"},
            {"DERIVED / مشتق موثق","-"},
            {"REFERENCE / مرجع","-"},
            {"RESEARCH / تقدير بحثي","-"},
            {"QUALIFIED / مؤهل بالدليل","-"},
            {"UNAVAILABLE / غير متاح","-"},
            {"Analytical engine / المحرك","-"},
            {"Archive and SHA source / الأرشيف","-"},
            {"Applicable standards / المعايير","-"}
        };
        for(int i=0;i<entries.length;i++){
            float y=43+i*15;
            headerCell(c,167,y,125,15,entries[i][0]);
            valueCell(c,5,y,162,15,entries[i][1]);
        }
        text(c,"G3 validation is not established by this blank visual design.",
            5,181,287,12,false,true);
    }

    /**
     * One empty, 12-section first-pass visual proof from Android PdfDocument.
     * Its fixed 12-page count is NOT the production row/page cap. Real records
     * and variable-height Arabic text require source-driven pagination later.
     */
    public static void writeBlankDesign(Context context,OutputStream out) throws Exception {
        if(out==null)throw new IllegalArgumentException("output stream required");
        PdfDocument pdf=new PdfDocument();
        try {
            for(int i=0;i<PAGE_KEYS.length;i++){
                PdfDocument.Page page=V7ReportLayoutKit.open(pdf,i+1,true);
                Canvas c=page.getCanvas();
                c.drawColor(Color.WHITE);
                header(c,context,i+1,PAGE_KEYS.length);
                switch(i){
                    case 0: executive(c); break;
                    case 1: sessions(c); break;
                    case 2: comparisons(c); break;
                    case 3: material144(c); break;
                    case 4: materialCatalog(c); break;
                    case 5: candidates(c); break;
                    case 6: exposureReferences(c); break;
                    case 7: physical(c); break;
                    case 8: particles(c); break;
                    case 9: recurrence(c); break;
                    case 10: cycleAudit(c); break;
                    case 11: scopeAndSources(c); break;
                    default: throw new IllegalStateException("unmapped report section");
                }
                pdf.finishPage(page);
            }
            pdf.writeTo(out);
        } finally { pdf.close(); }
    }
}
