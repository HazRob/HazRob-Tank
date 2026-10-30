package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Fifteen original stroke icons for the V7 physical/particulate PDF catalog.
 * Pure Canvas geometry: independent of fallback glyph fonts and emoji.
 * Same order as PhysicalCatalog and no scientific implications.
 */
public final class V7PhysicalIcons {
    private V7PhysicalIcons(){}
    private static final int INK=0xFF171717;
    private static final int PALE=0xFFF7F7F7;

    private static Paint stroke(float w){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(INK);p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w);p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        return p;
    }
    private static Paint fill(){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(INK);p.setStyle(Paint.Style.FILL);return p;
    }
    private static void line(Canvas c,Paint p,float x1,float y1,float x2,float y2){
        c.drawLine(x1,y1,x2,y2,p);
    }
    private static void poly(Canvas c,Paint p,float...xy){
        Path a=new Path();a.moveTo(xy[0],xy[1]);
        for(int k=2;k<xy.length;k+=2)a.lineTo(xy[k],xy[k+1]);
        c.drawPath(a,p);
    }
    private static void droplet(Canvas c,Paint p,float cx,float cy,float r){
        Path a=new Path();a.moveTo(cx,cy-r*1.4f);
        a.cubicTo(cx-r*.52f,cy-r*.55f,cx-r,cy-r*.3f,cx-r,cy+r*.45f);
        a.cubicTo(cx-r,cy+r*1.6f,cx+r,cy+r*1.6f,cx+r,cy+r*.45f);
        a.cubicTo(cx+r,cy-r*.3f,cx+r*.52f,cy-r*.55f,cx,cy-r*1.4f);
        a.close();c.drawPath(a,p);
    }
    private static void sun(Canvas c,Paint p,float cx,float cy,float r){
        c.drawCircle(cx,cy,r,p);
        for(int i=0;i<8;i++){
            double t=i*Math.PI/4;
            float dx=(float)Math.cos(t),dy=(float)Math.sin(t);
            line(c,p,cx+(r+3)*dx,cy+(r+3)*dy,
                cx+(r+6)*dx,cy+(r+6)*dy);
        }
    }
    private static void microDots(Canvas c,Paint p,int size){
        // Particle counts are visual labels only; this is not particle count.
        int count=size==0?3:size==1?5:7;
        for(int i=0;i<count;i++){
            double a=2*Math.PI*i/count;
            float radius= size==0?5.0f:size==1?6.0f:7.0f;
            c.drawCircle(20+(float)Math.cos(a)*radius,
                20+(float)Math.sin(a)*radius,1.7f,p);
        }
        c.drawCircle(20,20,2.2f,p);
    }
    /**
     * Render a vector glyph into a physical measurement table's rightmost
     * 40pt pictogram cell. The surrounding circle is a frame, not a data status.
     */
    public static void draw(Canvas dest,int catalogIndex,float x,float y,float side){
        dest.save();
        dest.translate(x,y);
        dest.scale(side/40f,side/40f);
        Paint bg=new Paint(Paint.ANTI_ALIAS_FLAG);bg.setColor(PALE);
        bg.setStyle(Paint.Style.FILL);dest.drawCircle(20,20,19,bg);
        Paint p=stroke(1.8f);dest.drawCircle(20,20,19,p);
        Paint solid=fill();
        switch(catalogIndex){
            case 0: // temperature: bulb and vertical capillary
                dest.drawRoundRect(new RectF(17.5f,9,22.5f,27),2.5f,2.5f,p);
                dest.drawCircle(20,29,5,p);
                line(dest,p,20,17,20,31);
                break;
            case 1: // humidity: water droplet
                droplet(dest,p,20,20,9);break;
            case 2: // pressure: dial with pointer
                dest.drawArc(new RectF(9,10,31,32),195,150,false,p);
                dest.drawCircle(20,22,2,solid);
                line(dest,p,20,22,27,15);break;
            case 3: // illuminance: radiating sun
                sun(dest,p,20,20,6);break;
            case 4: // luminous flux: light bulb + radiating light
                dest.drawArc(new RectF(11,8,29,28),195,150,false,p);
                line(dest,p,15,25,25,25);
                line(dest,p,16,29,24,29);
                line(dest,p,17,32,23,32);break;
            case 5: // noise: speaker + acoustic waves
                poly(dest,p,9,17,14,17,20,12,20,28,14,23,9,23,9,17);
                dest.drawArc(new RectF(16,14,28,26),-55,110,false,p);
                dest.drawArc(new RectF(15,10,34,30),-54,108,false,p);break;
            case 6: // vibration: waveform
                poly(dest,p,7,21,12,21,15,14,19,28,23,12,27,21,33,21);
                break;
            case 7: // ionizing radiation: black trefoil
                dest.drawCircle(20,20,2.3f,solid);
                for(int a=0;a<3;a++){
                    float angle=120*a-90;
                    dest.drawArc(new RectF(10,10,30,30),angle,52,false,stroke(4.4f));
                }
                break;
            case 8: // UV: sun with little shield
                sun(dest,p,18,17,5);
                poly(dest,p,23,23,30,23,30,29,26.5f,32,23,29,23,23);
                break;
            case 9: microDots(dest,solid,0);break; // PM1
            case 10: microDots(dest,solid,1);break; // PM2.5
            case 11: microDots(dest,solid,2);break; // PM10
            case 12: // wind velocity: moving air lines
                line(dest,p,7,15,25,15);
                dest.drawArc(new RectF(24,10,32,20),-90,270,false,p);
                line(dest,p,7,23,28,23);
                dest.drawArc(new RectF(25,20,33,28),-90,250,false,p);
                line(dest,p,10,30,22,30);break;
            case 13: // wind direction: compass
                dest.drawCircle(20,20,11,p);
                poly(dest,p,20,10,24,24,20,21,16,24,20,10);
                break;
            case 14: // altitude: mountain silhouette
                poly(dest,p,7,29,17,12,24,23,28,16,34,29);
                line(dest,p,7,29,34,29);break;
            default:
                dest.drawCircle(20,20,4,p);break;
        }
        dest.restore();
    }
}
