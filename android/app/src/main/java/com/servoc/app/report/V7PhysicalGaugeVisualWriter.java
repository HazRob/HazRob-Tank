package com.servoc.app.report;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import com.servoc.app.measurements.PhysicalCatalog;
import java.io.OutputStream;

/**
 * R02 native VISUAL DESIGN of fifteen neutral gauge slots.
 *
 * No needles, numeric ticks, green/red risk bands, thresholds, stats or
 * trend curves are invented. A source-aware production gauge must receive
 * the original measurement unit, calibration and engine-owned status.
 * NOT currently connected to TankPhysicalSnapshotPdf.write.
 */
public final class V7PhysicalGaugeVisualWriter {
    private V7PhysicalGaugeVisualWriter(){}
    private static final float MM=V7ReportLayoutKit.mm(1f);
    private static RectF mmRect(float x,float y,float w,float h){
        return new RectF(x*MM,y*MM,(x+w)*MM,(y+h)*MM);
    }
    private static void label(Canvas c,String text,RectF r,boolean rtl,boolean bold){
        V7ReportLayoutKit.text(c,text,r,rtl,bold,12,V7ReportLayoutKit.INK);
    }
    private static void gauge(Canvas c,int index,float x,float y){
        RectF frame=mmRect(x,y,96,111);
        Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setColor(V7ReportLayoutKit.BLACK);
        stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeWidth(.7f);
        c.drawRect(frame,stroke);
        V7ReportLayoutKit.tableHeader(c,mmRect(x,y,96,17),
            PhysicalCatalog.NAMES_AR[index],true);
        label(c,PhysicalCatalog.NAMES[index],mmRect(x+3,y+18,90,12),
            false,true);

        // A neutral, uncalibrated gauge scale is only an empty visual slot.
        // Do not draw a needle or risk bands because no qualifying range exists.
        Paint arc=new Paint(Paint.ANTI_ALIAS_FLAG);
        arc.setStyle(Paint.Style.STROKE);
        arc.setColor(0xFF999999);arc.setStrokeWidth(4f);
        c.drawArc(mmRect(x+18,y+34,60,46),205f,130f,false,arc);
        label(c,"-",mmRect(x+38,y+57,20,12),false,true);
        label(c,PhysicalCatalog.UNITS[index],
            mmRect(x+20,y+78,56,13),false,false);
        label(c,"Source: -  |  Valid limit: -",
            mmRect(x+3,y+96,90,12),false,false);
    }
    /** One visual slot per source catalog indicator; no page count cap. */
    public static void writeBlankDesign(Context ctx,OutputStream output)
            throws Exception {
        if(output==null)throw new IllegalArgumentException("output required");
        int count=PhysicalCatalog.COUNT;
        int pages=(count+3)/4;
        PdfDocument pdf=new PdfDocument();
        try{
            for(int page=0;page<pages;page++){
                PdfDocument.Page p=V7ReportLayoutKit.open(pdf,page+1,false);
                Canvas c=p.getCanvas();c.drawColor(Color.WHITE);
                V7ReportLayoutKit.drawHeader(c,ctx,false,
                    "القياسات الفيزيائية — العدادات الوصفية",
                    "Physical Metrics | Neutral Gauge Design","-");
                for(int slot=0;slot<4;slot++){
                    int idx=page*4+slot;
                    if(idx>=count)break;
                    gauge(c,idx,5+(slot%2)*104,35+(slot/2)*119);
                }
                V7ReportLayoutKit.drawFooter(c,false,page+1,pages);
                pdf.finishPage(p);
            }
            pdf.writeTo(output);
        }finally{pdf.close();}
    }
}
