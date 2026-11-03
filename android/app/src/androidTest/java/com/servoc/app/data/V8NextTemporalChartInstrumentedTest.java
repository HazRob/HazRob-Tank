package com.servoc.app.data;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.servoc.app.report.V8NextChartRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Verifies V8 Next PDF graph pixels are derived from original archived MQ
 * samples, and duplicate firmware scan_cycle IDs cannot inflate coverage.
 * Inputs are Android12 CI TEST fixtures, never field readings or source curves.
 */
@RunWith(AndroidJUnit4.class)
public final class V8NextTemporalChartInstrumentedTest {
    private static SessionSeries.Series fixture(double slope,boolean duplicateCycles,
                                                boolean valid) throws Exception {
        List<SessionSeries.Sample> rows=new ArrayList<>();
        for(int n=0;n<35;n++){
            JSONObject mq=new JSONObject();
            for(String key:new String[]{"mq2","mq3","mq4","mq5",
                                        "mq7","mq8","mq9","mq135"}){
                double initial=75+(key.length()*17.5);
                JSONObject q=new JSONObject().put("ok",valid)
                    .put("powered",true)
                    .put("scan_cycle",duplicateCycles?1:n)
                    .put("rs",initial*Math.exp(slope*n));
                mq.put(key,q);
            }
            JSONObject env=new JSONObject().put("temperature",25.3)
                .put("humidity",54.0);
            rows.add(new SessionSeries.Sample(
                100000+n*1500,env.toString(),mq.toString(),"{}"));
        }
        return new SessionSeries.Series(
            "CI-V8-NEXT-ARCHIVED-ONE-SESSION","start","end",rows);
    }
    private static Bitmap draw(SessionSeries.Series series,boolean coverage){
        Bitmap bmp=Bitmap.createBitmap(295,265,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(bmp);
        c.drawColor(Color.WHITE);
        if(coverage)V8NextChartRenderer.drawActualMqCoverage(
            c,new RectF(7,7,288,258),series);
        else V8NextChartRenderer.drawActualMqKinetics(
            c,new RectF(7,7,288,258),series);
        return bmp;
    }
    private static int differing(Bitmap a,Bitmap b){
        int n=0;
        for(int y=10;y<250;y+=2)for(int x=10;x<285;x+=2)
            if(a.getPixel(x,y)!=b.getPixel(x,y))n++;
        return n;
    }
    @Test public void original60SecondTemporalSignalChangesPrintedGraph()
        throws Exception {
        Bitmap up=draw(fixture(0.025,false,true),false);
        Bitmap down=draw(fixture(-0.025,false,true),false);
        try {
            assertTrue("MQ signed log-change graph must depend on actual t_ms / Rs",
                differing(up,down)>120);
        }finally{up.recycle();down.recycle();}
    }
    @Test public void duplicateFirmwareCyclesDoNotBecomeExtraValidSamples()
        throws Exception {
        Bitmap real=draw(fixture(0.015,false,true),true);
        Bitmap duplicates=draw(fixture(0.015,true,true),true);
        try {
            assertTrue("duplicate scan_cycle data must have lower sample coverage",
                differing(real,duplicates)>80);
        }finally{real.recycle();duplicates.recycle();}
    }
    @Test public void invalidMqCannotDrawFakeTrend()throws Exception {
        Bitmap bad=draw(fixture(.035,false,false),false);
        Bitmap good=draw(fixture(.035,false,true),false);
        try {
            assertTrue("invalid original MQ must not produce the valid graph",
                differing(bad,good)>200);
        }finally{bad.recycle();good.recycle();}
    }
}
