package com.servoc.app.report;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import java.io.OutputStream;

/**
 * R01 supplementary VISUAL DESIGN, not a production-wired analytical report.
 * The existing TankInstantV7PdfWriter remains the production report entrypoint.
 * GHS/OEL are material/source references, not gas detections or alarms.
 */
public final class V7InstantVisualAnnexWriter {
    private V7InstantVisualAnnexWriter(){}
    private static final float MM=V7ReportLayoutKit.mm(1);

    private static RectF box(float x,float y,float w,float h){
        return new RectF(x*MM,y*MM,(x+w)*MM,(y+h)*MM);
    }
    private static void txt(Canvas c,String s,float x,float y,float w,float h){
        V7ReportLayoutKit.text(c,s,box(x,y,w,h),
            true,false,12,V7ReportLayoutKit.INK);
    }
    private static void head(Canvas c,String s,float x,float y,float w,float h){
        V7ReportLayoutKit.tableHeader(c,box(x,y,w,h),s,true);
    }
    private static void cell(Canvas c,float x,float y,float w,float h){
        V7ReportLayoutKit.tableValue(c,box(x,y,w,h),"-",false);
    }
    private static void pageChrome(Canvas c,Context ctx,int page,int total){
        V7ReportLayoutKit.drawHeader(c,ctx,false,
            "القياس الآني — الملاحق البصرية",
            "Instant Gas | Candidate & Exposure Design","-");
        V7ReportLayoutKit.drawFooter(c,false,page,total);
    }
    private static void candidates(Canvas c){
        txt(c,"المرشحون / Source candidates — not verified identities",
            6,28,198,12);
        // Three demonstration row slots only. This is NOT the eventual
        // candidate-count/page limit; approved source rows paginate dynamically.
        for(int i=0;i<3;i++){
            float y=44+i*66;
            head(c,"Candidate "+(i+1)+" / سجل مرشح",5,y,200,13);
            head(c,"GHS",175,y+13,30,12);
            head(c,"CAS / Chemical UID",115,y+13,60,12);
            head(c,"Evidence / ppm",75,y+13,40,12);
            head(c,"PPM / Limit",5,y+13,70,12);
            cell(c,175,y+25,30,31);
            cell(c,115,y+25,60,31);
            cell(c,75,y+25,40,31);
            V7FieldChartLayout.blankPpmMicro(c,box(5,y+25,70,31));
        }
        txt(c,"تصنيف GHS مرجعي للمادة؛ لا يثبت كشف المادة أو تجاوز حد تعرض مهني.",
            6,247,198,21);
    }
    private static void references(Canvas c){
        txt(c,"مراجع الحدود المهنية / All independent OEL sources",
            6,28,198,13);
        for(int i=0;i<3;i++){
            float y=43+i*49;
            head(c,"OSHA / NIOSH / ACGIH / ... — reference slot",
                5,y,200,12);
            V7ReportLayoutKit.tableValue(c,box(5,y+12,200,34),
                "Chemical UID: -     CAS: -     Source ID: -\n"+
                "Authority: -     Jurisdiction: -     Type: -\n"+
                "Value: -     Unit: -     Duration: -     Revision: -",
                false);
        }
        V7ReportLayoutKit.blankChart(c,box(5,198,200,72),
            "المقارنة الزمنية المشروطة / Engine-supplied exposure comparison",
            "Source-defined averaging duration","ppm / compatible limit");
    }
    public static void writeBlankDesign(Context context,OutputStream out)
            throws Exception{
        if(out==null)throw new IllegalArgumentException("output required");
        PdfDocument pdf=new PdfDocument();
        try{
            for(int i=1;i<=2;i++){
                PdfDocument.Page p=V7ReportLayoutKit.open(pdf,i,false);
                Canvas c=p.getCanvas();c.drawColor(Color.WHITE);
                pageChrome(c,context,i,2);
                if(i==1)candidates(c);else references(c);
                pdf.finishPage(p);
            }
            pdf.writeTo(out);
        }finally{pdf.close();}
    }
}
