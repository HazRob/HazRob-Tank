package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * A4 Landscape R03 chart geometry with exact owner-approved printable boxes.
 * Immutable presentation helpers only; no curve interpolation, sensor averaging,
 * chemical attribution, certified ppm or exposure decisions.
 */
public final class V7FieldChartLayout {
    private V7FieldChartLayout(){}
    public static final float MM=72f/25.4f;
    public static final float LEFT_X=5f, RIGHT_X=150.5f;
    public static final float TOP_Y=27f, BOTTOM_Y=110.5f;
    public static final float CHART_W=141.5f, CHART_H=79.5f;
    public static final float FULL_W=287f;
    public static final float ADVANCED_Y=27f, ADVANCED_H=95f;
    public static final float COMP_Y=126f, COMP_H=64f;
    private static final String[] TITLES={
        "R0/Rs عبر الزمن | Sensor Response",
        "مصفوفة القنوات والزمن | Sensor-Time Map",
        "بصمة MQ | Sensor Fingerprint",
        "المقارنة بالمرجع | Observed vs Reference"
    };

    public static RectF[] fourEqual(){
        return new RectF[]{
            mmRect(RIGHT_X,TOP_Y,CHART_W,CHART_H),
            mmRect(LEFT_X,TOP_Y,CHART_W,CHART_H),
            mmRect(RIGHT_X,BOTTOM_Y,CHART_W,CHART_H),
            mmRect(LEFT_X,BOTTOM_Y,CHART_W,CHART_H)
        };
    }
    public static RectF mmRect(float x,float y,float w,float h){
        return new RectF(x*MM,y*MM,(x+w)*MM,(y+h)*MM);
    }
    public static RectF advanced3D(){
        return mmRect(5f,ADVANCED_Y,FULL_W,ADVANCED_H);
    }
    public static RectF compensationTemperatureHumidity(){
        return mmRect(RIGHT_X,COMP_Y,CHART_W,COMP_H);
    }
    public static RectF compensationBeforeAfter(){
        return mmRect(LEFT_X,COMP_Y,CHART_W,COMP_H);
    }
    /**
     * Every chart remains visible with blank axes even when the source
     * series is absent. The R0/Rs chart is only a slot, not a fabricated
     * history of a one-shot measurement.
     */
    public static void drawFirstFour(Canvas c){
        RectF[] four=fourEqual();
        for(int i=0;i<four.length;i++) {
            String x=i==0?"Recorded Time":i==1?"Recorded Time":
                i==2?"MQ Sensor":"MQ Sensor";
            String y=i==0?"Rs/R0":i==1?"Channel":i==2?
                "Normalized response":"Reference response";
            V7ReportLayoutKit.blankChart(c,four[i],TITLES[i],x,y);
        }
    }
    public static void drawAdvancedPage(Canvas c){
        V7ReportLayoutKit.blankChart(c,advanced3D(),
            "تحليل 3D/4D | Multidimensional Fingerprint",
            "Source Feature Axis","Source Feature Axis");
        V7ReportLayoutKit.blankChart(c,compensationTemperatureHumidity(),
            "تأثير الحرارة والرطوبة | T/RH Effect",
            "Recorded Time","T / RH");
        V7ReportLayoutKit.blankChart(c,compensationBeforeAfter(),
            "قبل وبعد التعويض | Before/After Correction",
            "Source Observation","Corrected feature");
    }
    /**
     * Candidate micro-chart (70mm reference column). Only empty
     * tracks with printed dash placeholders are drawn; no imaginary
     * ppm/limit, no numerical ticks and no red/green risk interpretation.
     */
    public static void blankPpmMicro(Canvas c,RectF r){
        Paint thin=new Paint(Paint.ANTI_ALIAS_FLAG);
        thin.setColor(V7ReportLayoutKit.INK);
        thin.setStyle(Paint.Style.STROKE);thin.setStrokeWidth(.85f);
        c.drawRect(r,thin);
        float x1=r.left+6f,x2=r.right-8f;
        float yy1=r.top+r.height()*.38f,yy2=r.top+r.height()*.70f;
        Paint track=new Paint(Paint.ANTI_ALIAS_FLAG);
        track.setColor(0xFFC5C5C5);track.setStrokeWidth(1.2f);
        c.drawLine(x1+22,yy1,x2,yy1,track);
        c.drawLine(x1+22,yy2,x2,yy2,track);
        V7ReportLayoutKit.text(c,"PPM",
            new RectF(x1,r.top+4,x1+42,yy1),false,true,12,V7ReportLayoutKit.INK);
        V7ReportLayoutKit.text(c,"Limit",
            new RectF(x1,yy1+4,x1+42,yy2),false,true,12,V7ReportLayoutKit.INK);
        V7ReportLayoutKit.text(c,"-",
            new RectF(x2-23,yy1-18,x2,yy1),false,false,12,Color.BLACK);
        V7ReportLayoutKit.text(c,"-",
            new RectF(x2-23,yy2-18,x2,yy2),false,false,12,Color.BLACK);
    }
    /** Data-source, SDS and pictogram mapping remain with the caller. */
}
