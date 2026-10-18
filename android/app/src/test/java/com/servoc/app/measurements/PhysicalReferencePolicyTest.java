package com.servoc.app.measurements;

import org.junit.Test;
import static org.junit.Assert.*;

public class PhysicalReferencePolicyTest {
    @Test public void whoDailyPmRequires24HourAverageAndValidatedInstrument(){
        assertFalse(PhysicalReferencePolicy.evaluate(10,20,true,false,false,0,false).exceeded);
        assertFalse(PhysicalReferencePolicy.evaluate(10,20,true,false,false,86400,false).exceeded);
        assertTrue(PhysicalReferencePolicy.evaluate(10,20,true,false,false,86400,true).exceeded);
        assertFalse(PhysicalReferencePolicy.evaluate(11,45,true,false,false,86400,true).exceeded);
        assertTrue(PhysicalReferencePolicy.evaluate(11,46,true,false,false,86400,true).exceeded);
    }
    @Test public void unvalidatedUvDoesNotTriggerFalseAlarm(){
        assertFalse(PhysicalReferencePolicy.evaluate(8,8,true,false,false,0,false).exceeded);
        assertTrue(PhysicalReferencePolicy.evaluate(8,3,true,true,false,0,false).exceeded);
    }
    @Test public void humidityNeedsIndoorContext(){
        assertFalse(PhysicalReferencePolicy.evaluate(1,80,true,false,false,0,false).exceeded);
        assertTrue(PhysicalReferencePolicy.evaluate(1,65,true,false,true,0,false).exceeded);
    }
    @Test public void uncalibratedNoiseAndMissingMeasurementsRemainUnclassified(){
        assertFalse(PhysicalReferencePolicy.evaluate(5,110,true,false,true,0,false).exceeded);
        assertFalse(PhysicalReferencePolicy.evaluate(10,Double.NaN,true,false,true,86400,true).exceeded);
    }
}
