package com.servoc.app.report;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import com.servoc.app.R;

/**
 * Shared V7 report-design renderer. Presentation only; no sensor conversion,
 * exposure threshold, classification, ChemCore gate or freshness inference.
 *
 * IMPORTANT: This component is not yet wired into R01-R06 production writers.
 * It is intentionally additive to preserve current app behavior while layout
 * signoffs, real-source data contracts and Android-native QA are pending.
 */
public final class V7ReportLayoutKit {
    private V7ReportLayoutKit() {}
    private static volatile Context applicationContext;

    /** Bind only the Android application context; never store an Activity. */
    public static void bindContext(Context context) {
        if(context!=null)applicationContext=context.getApplicationContext();
    }

    public static final int INK=0xFF111111;
    public static final int AMBER=0xFFFFBE00;
    public static final int GRAY=0xFFEBEBEB;
    public static final int WHITE=Color.WHITE;
    public static final int BLACK=Color.BLACK;
    public static final int EXCEEDED_RED=0xFFC62828;
    public static final int A4_PORTRAIT_W=595, A4_PORTRAIT_H=842;
    public static final int A4_LANDSCAPE_W=842, A4_LANDSCAPE_H=595;
    public static final float MIN_BODY_PT=12f;

    public static float mm(float v){return v*72f/25.4f;}
    public static float pageMargin(){return mm(5);}
    public static String missing(String value){
        return value==null || value.trim().isEmpty() ? "-" : value;
    }
    public static String missing(double value){
        return Double.isFinite(value) ? Double.toString(value) : "-";
    }

    public static PdfDocument.Page open(PdfDocument pdf,int pageNo,boolean landscape){
        int width=landscape?A4_LANDSCAPE_W:A4_PORTRAIT_W;
        int height=landscape?A4_LANDSCAPE_H:A4_PORTRAIT_H;
        return pdf.startPage(new PdfDocument.PageInfo.Builder(width,height,pageNo).create());
    }
    public static RectF contentBox(boolean landscape){
        float w=landscape?A4_LANDSCAPE_W:A4_PORTRAIT_W;
        float h=landscape?A4_LANDSCAPE_H:A4_PORTRAIT_H;
        return new RectF(mm(5),mm(27),w-mm(5),h-mm(20));
    }
    private static Paint paint(int color,boolean bold,float size){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(Math.max(MIN_BODY_PT,size));
        p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        return p;
    }
    private static void fill(Canvas c,RectF r,int color){
        Paint p=paint(color,false,MIN_BODY_PT);
        p.setStyle(Paint.Style.FILL);c.drawRect(r,p);
    }
    private static void outline(Canvas c,RectF r){
        Paint p=paint(BLACK,false,MIN_BODY_PT);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.70f);c.drawRect(r,p);
    }
    /**
     * Draw shape-aware Arabic/English multiline text. Return height for
     * pagination decisions. Do not truncate content or reduce below 12pt.
     */
    public static int text(Canvas c,String value,RectF box,boolean rtl,
                           boolean bold,float pt,int color){
        if(pt<MIN_BODY_PT)throw new IllegalArgumentException("V7 text below 12pt");
        String s=missing(value);
        TextPaint p=new TextPaint(paint(color,bold,pt));
        StaticLayout layout=StaticLayout.Builder.obtain(s,0,s.length(),p,
            Math.max(1,(int)box.width()))
            .setTextDirection(rtl?TextDirectionHeuristics.FIRSTSTRONG_RTL:
                TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .setAlignment(rtl?Layout.Alignment.ALIGN_NORMAL:
                Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0,1.15f)
            .setIncludePad(false).build();
        c.save();
        c.translate(box.left,box.top);
        layout.draw(c);
        c.restore();
        return layout.getHeight();
    }
    /** Measure before drawing: enables page breaks without clipping or a side effect. */
    public static boolean fits(String value,RectF box,boolean rtl,
                               boolean bold,float pt){
        if(pt<MIN_BODY_PT)throw new IllegalArgumentException("V7 text below 12pt");
        String s=missing(value);
        TextPaint p=new TextPaint(paint(INK,bold,pt));
        StaticLayout layout=StaticLayout.Builder.obtain(s,0,s.length(),p,
            Math.max(1,(int)box.width()))
            .setTextDirection(rtl?TextDirectionHeuristics.FIRSTSTRONG_RTL:
                TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .setLineSpacing(0,1.15f).setIncludePad(false).build();
        return layout.getHeight()<=box.height();
    }
    public static void drawHeader(Canvas c,Context context,boolean landscape,
                                  String titleArabic,String titleEnglish,
                                  String measuredTimestamp){
        float w=landscape?A4_LANDSCAPE_W:A4_PORTRAIT_W;
        RectF band=new RectF(mm(5),mm(5),w-mm(5),mm(23));
        fill(c,band,BLACK);
        float logoSide=mm(15);
        Context resolved=context!=null?context:applicationContext;
        if(resolved!=null){
            Drawable logo=resolved.getDrawable(R.drawable.hazrob_official_report_mark);
            if(logo!=null){
                float right=band.right-mm(1),top=band.top+mm(1.5f);
                logo.setBounds((int)(right-logoSide),(int)top,(int)right,(int)(top+logoSide));
                logo.draw(c);
            }
        }
        float timestampW=mm(38);
        text(c,missing(measuredTimestamp),
            new RectF(band.left+mm(2),band.top+mm(2),
                band.left+timestampW,band.bottom-mm(1)),
            false,false,12,WHITE);
        float titleLeft=band.left+timestampW+mm(3);
        float titleRight=band.right-logoSide-mm(4);
        text(c,missing(titleArabic),new RectF(titleLeft,band.top+mm(1),
            titleRight,band.top+mm(9)),true,true,15,AMBER);
        text(c,missing(titleEnglish),new RectF(titleLeft,band.top+mm(9),
            titleRight,band.bottom-mm(1)),false,true,12,AMBER);
    }
    public static void drawFooter(Canvas c,boolean landscape,int page,int pages){
        float w=landscape?A4_LANDSCAPE_W:A4_PORTRAIT_W;
        float h=landscape?A4_LANDSCAPE_H:A4_PORTRAIT_H;
        RectF band=new RectF(mm(5),h-mm(16),w-mm(5),h-mm(5));
        fill(c,band,GRAY);
        text(c,"صفحة "+page+" من "+pages,new RectF(
            band.left+mm(2),band.top+mm(1),band.left+mm(60),band.bottom),
            true,true,14,BLACK);
        text(c,"HazRob",new RectF(
            band.right-mm(43),band.top+mm(1),band.right-mm(2),band.bottom),
            false,true,14,BLACK);
    }
    public static void tableHeader(Canvas c,RectF r,String heading,boolean rtl){
        fill(c,r,AMBER);outline(c,r);
        text(c,heading,new RectF(r.left+mm(2),r.top+mm(1),
            r.right-mm(2),r.bottom-mm(1)),rtl,true,12,BLACK);
    }
    /** A blank placeholder is a state distinct from a real, measured zero. */
    public static void tableValue(Canvas c,RectF r,String value,boolean rtl){
        fill(c,r,WHITE);outline(c,r);
        text(c,missing(value),new RectF(r.left+mm(2),r.top+mm(1),
            r.right-mm(2),r.bottom-mm(1)),rtl,false,12,BLACK);
    }
    /**
     * Caller must provide the engine's explicit, comparable-reference
     * exceedance flag; the PDF never computes a threshold from raw values.
     */
    public static void measuredValue(Canvas c,RectF r,String value,boolean rtl,
                                     boolean engineReportsExceeded){
        boolean hasReading=value!=null && !value.trim().isEmpty() &&
            !"-".equals(value.trim());
        boolean exceeded=hasReading && engineReportsExceeded;
        fill(c,r,exceeded?EXCEEDED_RED:WHITE);outline(c,r);
        text(c,missing(value),new RectF(r.left+mm(2),r.top+mm(1),
            r.right-mm(2),r.bottom-mm(1)),rtl,exceeded,12,
            exceeded?WHITE:BLACK);
    }
    /** Plot area without fake points, bars, numeric ticks or threshold lines. */
    public static void blankChart(Canvas c,RectF r,String title,String xAxis,String yAxis){
        fill(c,r,WHITE);outline(c,r);
        text(c,title,new RectF(r.left+mm(2),r.top+mm(1),r.right-mm(2),
            r.top+mm(12)),true,true,12,INK);
        RectF area=new RectF(r.left+mm(13),r.top+mm(16),
            r.right-mm(5),r.bottom-mm(13));
        Paint grid=paint(0xFFDEDEDE,false,12);grid.setStrokeWidth(.5f);
        for(int i=1;i<5;i++){
            float x=area.left+i*area.width()/5f;
            c.drawLine(x,area.top,x,area.bottom,grid);
        }
        for(int i=1;i<4;i++){
            float y=area.top+i*area.height()/4f;
            c.drawLine(area.left,y,area.right,y,grid);
        }
        Paint axis=paint(INK,false,12);axis.setStrokeWidth(.75f);
        c.drawLine(area.left,area.top,area.left,area.bottom,axis);
        c.drawLine(area.left,area.bottom,area.right,area.bottom,axis);
        text(c,missing(xAxis),new RectF(area.left+mm(6),area.bottom+mm(1),
            area.right,area.bottom+mm(12)),false,false,12,INK);
        text(c,missing(yAxis),new RectF(r.left+mm(1),area.top,
            area.left-mm(1),area.bottom),false,false,12,INK);
    }
}
