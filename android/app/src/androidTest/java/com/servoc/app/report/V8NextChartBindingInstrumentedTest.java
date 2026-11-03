package com.servoc.app.report;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.hazrob.tank.chemcore.next.ChemCoreNext;
import com.hazrob.tank.chemcore.next.CoreContracts;
import com.servoc.app.chemcore.nextintegration.NextDecisionStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/** Source-only fixture: test the exact PDF/UI renderer with immutable Next
 * receipts and independently varied original MQ evidence. No Tank G3 claims.
 */
@RunWith(AndroidJUnit4.class)
public final class V8NextChartBindingInstrumentedTest {
    private static final String STABLE=
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static NextDecisionStore.Receipt receipt(
        String session,double rs,int sourceRoots)throws Exception{
        List<CoreContracts.Reading> readings=List.of(
            new CoreContracts.Reading("QA-DEV:MQ2",120,1.2,rs,10.0,null,
                CoreContracts.CalibrationReference.AIR_RS_MEAN,
                "QA_FIXTURE_ONLY",CoreContracts.SampleQuality.VALID),
            new CoreContracts.Reading("QA-DEV:MQ4",340,2.8,7.0,14.0,null,
                CoreContracts.CalibrationReference.AIR_RS_MEAN,
                "QA_FIXTURE_ONLY",CoreContracts.SampleQuality.VALID));
        CoreContracts.Snapshot snapshot=new CoreContracts.Snapshot(
            "TEST-NEXT-"+session,"QA-DEV",CoreContracts.Mode.FIELD_SESSION,
            70000L,24.0,48.0,true,readings,STABLE);
        List<CoreContracts.EvidenceNode> evidence=new ArrayList<>();
        List<String> roots=new ArrayList<>();
        for(int i=0;i<sourceRoots;i++){
            String root="STUDY-ROOT-"+i;
            roots.add(root);
            evidence.add(new CoreContracts.EvidenceNode(root,
                "SOURCE_FINGERPRINT","QA-DEV:MQ2",324L,
                CoreContracts.EvidencePolarity.POSITIVE,"QA TEST ONLY"));
        }
        List<CoreContracts.Candidate> candidates=roots.isEmpty()?List.of():
            List.of(new CoreContracts.Candidate(324L,"QA study roots",roots));
        CoreContracts.DecisionStatus state=roots.isEmpty()?
            CoreContracts.DecisionStatus.UNKNOWN:
            CoreContracts.DecisionStatus.RESEARCH_CANDIDATES;
        String sha=ChemCoreNext.snapshotHash(snapshot);
        CoreContracts.Result result=new CoreContracts.Result(
            "Next QA Result",snapshot,sha,state,evidence,candidates,List.of());
        Constructor<NextDecisionStore.Receipt> ctor=
            NextDecisionStore.Receipt.class.getDeclaredConstructor(
                String.class,String.class,String.class,String.class,
                String.class,CoreContracts.DecisionStatus.class,long.class,
                CoreContracts.Snapshot.class,CoreContracts.Result.class);
        ctor.setAccessible(true);
        return ctor.newInstance("NEXT-QA-"+session,session,snapshot.uid,
            sha,"Next QA Result",state,1700000000000L,snapshot,result);
    }
    private static Bitmap drawn(NextDecisionStore.Receipt source,int key){
        Bitmap b=Bitmap.createBitmap(1200,650,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);c.drawColor(Color.WHITE);
        V8NextChartRenderer.draw(c,new RectF(12,12,1188,638),source,null,key);
        return b;
    }
    private static int difference(Bitmap first,Bitmap second){
        int changed=0;
        for(int y=35;y<610;y+=3)
            for(int x=55;x<1100;x+=3)
                if(first.getPixel(x,y)!=second.getPixel(x,y))changed++;
        return changed;
    }
    @Test public void originalRsChangesOnlyItsSourceGraph()throws Exception{
        var a=receipt("TEST-01",4.0,2);
        var b=receipt("TEST-01",9.5,2);
        for(int id:new int[]{V8NextChartRenderer.RS_SOURCE,
                              V8NextChartRenderer.NORMALIZED_MQ}){
            Bitmap x=drawn(a,id),y=drawn(b,id);
            try{assertTrue("Next original MQ graph must change for changed Rs",
                           difference(x,y)>25);}
            finally{x.recycle();y.recycle();}
        }
        Bitmap first=drawn(a,V8NextChartRenderer.SOURCE_ROOTS);
        Bitmap second=drawn(b,V8NextChartRenderer.SOURCE_ROOTS);
        try{assertEquals("Source root evidence does NOT change with raw Rs alone",
                         0,difference(first,second));}
        finally{first.recycle();second.recycle();}
    }
    @Test public void independentRootsChangeCandidateSupportGraph()throws Exception{
        var a=receipt("TEST-01",4.0,1);
        var b=receipt("TEST-01",4.0,3);
        Bitmap x=drawn(a,V8NextChartRenderer.SOURCE_ROOTS);
        Bitmap y=drawn(b,V8NextChartRenderer.SOURCE_ROOTS);
        try{assertTrue("Evidence roots must change root graph",difference(x,y)>25);}
        finally{x.recycle();y.recycle();}
        assertEquals(12,V8NextChartRenderer.nextChartLabels().length);
    }
    @Test public void wrongMeasurementCannotBorrowNextDecision()throws Exception{
        var a=receipt("TEST-01",4.0,1);
        V8NextChartRenderer.requireSameSession(a,"TEST-01");
        try{V8NextChartRenderer.requireSameSession(a,"OTHER-SESSION");
            fail("Source lineage mismatch accepted");}
        catch(IllegalStateException expected){
            assertTrue(expected.getMessage().contains("SESSION_MISMATCH"));
        }
    }
}
