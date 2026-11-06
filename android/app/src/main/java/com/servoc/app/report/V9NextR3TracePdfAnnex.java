package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.RectF;
import com.hazrob.tank.chemcore.next.NextAnalysisTrace;
import com.servoc.app.chemcore.nextintegration.NextDecisionStore;
import java.util.List;

/** Append-only V9 field evidence annex; renders the ORIGINAL stored R3 trace.
 * No inference, fitted ppm, GHS identification, source re-calibration or alarms.
 */
final class V9NextR3TracePdfAnnex {
  private V9NextR3TracePdfAnnex(){}
  static final int ROWS_PER_PAGE=12;
  static int pages(NextDecisionStore.Receipt receipt){
    return receipt==null||receipt.executionTrace==null?0:
        (receipt.executionTrace.size()+ROWS_PER_PAGE-1)/ROWS_PER_PAGE;
  }
  private static Paint p(int color,float size,boolean bold){
    Paint x=new Paint(Paint.ANTI_ALIAS_FLAG);
    x.setColor(color);x.setTextSize(size);x.setFakeBoldText(bold);
    return x;
  }
  private static String shortText(String s,int len){
    if(s==null||s.isEmpty())return "—";
    return s.length()<=len?s:s.substring(0,Math.max(0,len-1))+"…";
  }
  private static void label(Canvas c,String s,float x,float y,float size,
                            int color,boolean bold){
    c.drawText(shortText(s,130),x,y,p(color,size,bold));
  }
  static void draw(Canvas canvas,NextDecisionStore.Receipt receipt,int offset){
    if(canvas==null||receipt==null||receipt.executionTrace==null||
       receipt.traceSha256==null)
      throw new IllegalArgumentException("verified original R3 field trace required");
    List<NextAnalysisTrace.Event> events=receipt.executionTrace;
    if(offset<0||offset>=events.size()||offset%ROWS_PER_PAGE!=0)
      throw new IllegalArgumentException("invalid trace page range");
    final int text=0xFF263238,muted=0xFF5F6C7B;
    label(canvas,"ChemCore Next R3 • ORIGINAL SOURCE TECHNIQUE TRACE",
        34,52,13,Color.BLACK,true);
    label(canvas,"Research-only / no certified identity, ppm, alarm or G3",
        34,69,8,0xFFC62828,true);
    label(canvas,"Decision "+shortText(receipt.decisionUid,44)+
        "   Snapshot "+shortText(receipt.snapshotSha256,25),
        34,84,7.8f,muted,false);
    label(canvas,"Trace SHA256 "+receipt.traceSha256+
        "   Events "+events.size(),
        34,98,7.5f,muted,false);
    for(int i=offset;i<Math.min(events.size(),offset+ROWS_PER_PAGE);i++){
      NextAnalysisTrace.Event e=events.get(i);
      float y=111+(i-offset)*37f;
      Paint background=p(i%2==0?0xFFF3F6FA:Color.WHITE,1,false);
      canvas.drawRoundRect(new RectF(32,y,806,y+35),4,4,background);
      label(canvas,"#"+(i+1)+" "+e.stage+" / "+e.status+" • "+shortText(e.sourceAssetId,40),
          39,y+11,8.2f,text,true);
      label(canvas,"Source root "+shortText(e.sourceRoot,42)+
          " • Sensor "+shortText(e.sensorUid,29)+
          " • UID "+(e.chemicalUid==null?"UNKNOWN":e.chemicalUid),
          39,y+22,7.5f,muted,false);
      label(canvas,"Reason: "+shortText(e.reason,110),39,y+32,7.1f,muted,false);
    }
  }
}
