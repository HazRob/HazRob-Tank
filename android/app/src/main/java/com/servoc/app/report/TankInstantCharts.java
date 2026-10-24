package com.servoc.app.report;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import com.servoc.app.hardware.HazRobHardwareProfile;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Evidence-backed STATIC plots for one in-memory MQ operation; no invented timeline. */
public final class TankInstantCharts {
    public static final String[] LABELS={
        "مقاومة الحساسات Rs (kΩ)","نسب Rs/R0 — بصمة MQ","درجات مطابقة المرشحين"
    };
    private TankInstantCharts(){}
    private static final class Value {
        final String label;final double value;
        Value(String label,double value){this.label=label;this.value=value;}
    }
    public static boolean available(int type,JSONObject captured,JSONObject analysis){
        return !values(type,captured,analysis).isEmpty();
    }
    private static List<Value> values(int type,JSONObject captured,JSONObject analysis){
        List<Value> out=new ArrayList<>();
        if(type<0||type>=LABELS.length)return out;
        if(type==0||type==1){
            JSONObject mq=captured==null?null:captured.optJSONObject("mq");
            for(HazRobHardwareProfile.Channel ch:HazRobHardwareProfile.MQ){
                JSONObject q=mq==null?null:mq.optJSONObject(ch.key);
                if(q==null||!q.optBoolean("ok",q.optBoolean("valid",false)))continue;
                double v=q.optDouble(type==0?"rs":"ratio",Double.NaN);
                if(Double.isFinite(v)&&v>0)out.add(new Value(ch.key.toUpperCase(Locale.US),v));
            }
        }else{
            JSONArray list=analysis==null?null:analysis.optJSONArray("candidates");
            if(list!=null)for(int i=0;i<Math.min(8,list.length());i++){
                JSONObject candidate=list.optJSONObject(i);if(candidate==null)continue;
                double v=candidate.optDouble("candidate_score_pct",Double.NaN);
                if(!Double.isFinite(v))v=candidate.optDouble("posterior",Double.NaN)*100;
                if(!Double.isFinite(v)||v<0)continue;
                out.add(new Value("#"+(i+1)+" "+candidate.optString("label",""),v));
            }
        }
        return out;
    }
    public static Bitmap render(int type,JSONObject captured,JSONObject analysis){
        List<Value> rows=values(type,captured,analysis);
        if(rows.isEmpty())throw new IllegalArgumentException("No observed evidence for requested chart");
        int w=1100,h=630;
        Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);canvas.drawColor(Color.WHITE);
        Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);ink.setColor(0xFF18283C);ink.setTextSize(25);
        ink.setTypeface(android.graphics.Typeface.create("sans-serif",1));
        drawRtl(canvas,LABELS[type],28,ink,w-55);
        ink.setTypeface(android.graphics.Typeface.DEFAULT);
        ink.setTextSize(18);ink.setColor(0xFF5B6571);
        drawRtl(canvas,"قياسات أصلية أو درجات ترشيح حسابية • ليست سلسلة زمنية",64,ink,w-55);
        double max=0;for(Value v:rows)max=Math.max(max,v.value);
        if(type==2)max=Math.max(100,max);
        if(max<=0)max=1;
        float top=112,span=440f/rows.size();
        Paint bar=new Paint(Paint.ANTI_ALIAS_FLAG);bar.setColor(0xFF176B9A);
        for(int i=0;i<rows.size();i++){
            Value r=rows.get(i);float y=top+i*span;
            bar.setColor(i%2==0?0xFF176B9A:0xFF3F87AC);
            canvas.drawRoundRect(170,y+8,170+(float)(700*r.value/max),y+span-12,7,7,bar);
            ink.setColor(0xFF15202B);ink.setTextSize(20);
            String label=r.label;
            if(label.length()>16)label=label.substring(0,15)+"…";
            canvas.drawText(label,15,y+span*.65f,ink);
            canvas.drawText(String.format(Locale.US,"%.2f%s",r.value,type==2?"%":""),890,y+span*.65f,ink);
        }
        ink.setColor(0xFF6B7280);ink.setTextSize(17);
        drawRtl(canvas,"المصدر: عملية قياس واحدة مكتملة • دون نقاط تقديرية مضافة",602,ink,w-55);
        return bitmap;
    }
    private static void drawRtl(Canvas canvas,String value,int baseline,Paint p,int right){
        TextPaint tp=new TextPaint(p);StaticLayout layout=StaticLayout.Builder.obtain(
            value,0,value.length(),tp,right-40)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).build();
        canvas.save();canvas.translate(40,baseline-layout.getLineBaseline(0));layout.draw(canvas);canvas.restore();
    }
}
