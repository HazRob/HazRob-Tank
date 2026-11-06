package com.servoc.app.report;

import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.hazrob.tank.chemcore.next.ChemCoreNext;
import com.hazrob.tank.chemcore.next.CoreContracts;
import com.hazrob.tank.chemcore.next.NextAnalysisTrace;
import com.servoc.app.chemcore.nextintegration.NextDecisionEnvelope;
import com.servoc.app.chemcore.nextintegration.NextDecisionStore;
import com.servoc.app.data.ExperimentsDb;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;

/** Android PDF smoke tests using genuine original archived R3 trace, never
 *  physical Tank gas exposures or authenticated G3 calibration.
 */
@RunWith(AndroidJUnit4.class)
public final class V9NextR3PdfAnnexInstrumentedTest {
  private static final String STABLE="aabbccddaabbccddaabbccddaabbccddaabbccddaabbccddaabbccddaabbccdd";
  private Context ctx;private ExperimentsDb db;
  @Before public void before(){
    ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
    ctx.deleteDatabase("servoc.db");
    db=new ExperimentsDb(ctx);
  }
  @After public void after(){if(db!=null)db.close();ctx.deleteDatabase("servoc.db");}
  private static CoreContracts.Snapshot snapshot(){
    return new CoreContracts.Snapshot("V9R3-PDF-SOURCE-001","simulator-not-tank",
      CoreContracts.Mode.INSTANT,60000L,25.0,40.0,true,List.of(),STABLE);
  }
  private NextDecisionStore.Receipt receipt(){
    var s=snapshot();
    var r=new CoreContracts.Result(ChemCoreNext.VERSION,s,ChemCoreNext.snapshotHash(s),
      CoreContracts.DecisionStatus.UNKNOWN,List.of(),List.of(),
      List.of("NOT_QUALIFIED"));
    var events=List.of(
        new NextAnalysisTrace.Event("INPUT","CAPTURE",STABLE,"",null,
            "HASH_VERIFIED","same original source",null),
        new NextAnalysisTrace.Event("CURVE","MQ2_CURVE","ONE_ROOT","MQ2",100L,
            "ABSTAIN","NO_VERIFIED_SOURCE_R0",null),
        new NextAnalysisTrace.Event("COHERENCE","ROOT","ONE_ROOT","MQ2",null,
            "NO_TWO_CHANNEL_MATCH","insufficient source-qualified sensors",null),
        new NextAnalysisTrace.Event("FINAL","STATUS","","",null,
            "UNKNOWN","research evidence only",null));
    return new NextDecisionStore(db).appendWithTrace("V9R3-PDF-SESSION",100L,s,r,events);
  }
  private int pages(byte[] bytes,String filename)throws Exception{
    assertTrue(bytes.length>1000);
    File tmp=new File(ctx.getCacheDir(),filename);
    try(FileOutputStream o=new FileOutputStream(tmp)){o.write(bytes);}
    try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(tmp,ParcelFileDescriptor.MODE_READ_ONLY);
        PdfRenderer renderer=new PdfRenderer(fd)){
      return renderer.getPageCount();
    }finally{tmp.delete();}
  }
  @Test public void instantR3PdfAppendsAllOriginalEventPages()throws Exception{
    var saved=receipt();
    JSONObject model=NextDecisionEnvelope.fromReceipt(saved);
    model.put("observed_channels",0);
    ByteArrayOutputStream bytes=new ByteArrayOutputStream();
    TankInstantV7PdfWriter.writeNext(bytes,new JSONObject().put("mq",new JSONObject()),
        model,"V9R3-PDF-SESSION",100L);
    assertEquals("one original main PDF plus complete trace annex",2,
        pages(bytes.toByteArray(),"r3-instant-instrumented.pdf"));
  }
  @Test public void fieldR3AnnexRendersOriginalReceiptWithoutSourceReanalysis()throws Exception{
    var saved=receipt();
    assertEquals(1,V9NextR3TracePdfAnnex.pages(saved));
    PdfDocument pdf=new PdfDocument();
    var page=pdf.startPage(new PdfDocument.PageInfo.Builder(842,595,1).create());
    V9NextR3TracePdfAnnex.draw(page.getCanvas(),saved,0);
    pdf.finishPage(page);
    ByteArrayOutputStream out=new ByteArrayOutputStream();
    pdf.writeTo(out);pdf.close();
    assertEquals(1,pages(out.toByteArray(),"r3-field-instrumented.pdf"));
  }
}
