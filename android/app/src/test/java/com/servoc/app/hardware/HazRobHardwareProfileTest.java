package com.servoc.app.hardware;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Locks the Android hardware identity to the controlled HazRob 0.10.2 / HR069 GPIO/firmware profile. */
public final class HazRobHardwareProfileTest {
    @Test public void profileIdentityMatchesCanonicalContract(){
        assertEquals("tank-doit-v1-2servo-8mq-echo-bme",HazRobHardwareProfile.ID);
    }

    @Test public void driveAndMqAuthorityRemainPinned(){
        assertEquals("dual_continuous_servo",HazRobHardwareProfile.DRIVE_DRIVER);
        assertEquals(16,HazRobHardwareProfile.SERVO_L_GPIO);
        assertEquals(17,HazRobHardwareProfile.SERVO_R_GPIO);
        assertEquals(25,HazRobHardwareProfile.MQ_RELAY_GPIO);
        assertFalse("IR must be absent from Tank profile", java.util.Arrays.stream(HazRobHardwareProfile.class.getDeclaredFields()).anyMatch(f -> f.getName().startsWith("IR_")));
        assertEquals(8,HazRobHardwareProfile.MQ.length);
    }

    @Test public void environmentalBusIsIndependentFromAdsBus(){
        assertEquals(21,HazRobHardwareProfile.I2C_SDA_GPIO);
        assertEquals(22,HazRobHardwareProfile.I2C_SCL_GPIO);
        assertEquals(13,HazRobHardwareProfile.ENV_I2C_SDA_GPIO);
        assertEquals(14,HazRobHardwareProfile.ENV_I2C_SCL_GPIO);
        assertFalse(HazRobHardwareProfile.I2C_SDA_GPIO==HazRobHardwareProfile.ENV_I2C_SDA_GPIO);
        assertFalse(HazRobHardwareProfile.I2C_SCL_GPIO==HazRobHardwareProfile.ENV_I2C_SCL_GPIO);
    }

    @Test public void installedSafetyAndSensorPinsRemainPinned(){
        assertEquals(26,HazRobHardwareProfile.ALERT_GPIO);
        assertEquals(34,HazRobHardwareProfile.LDR_GPIO);
        assertEquals(36,HazRobHardwareProfile.SOUND_GPIO);
        assertEquals(33,HazRobHardwareProfile.BATTERY_ADC_GPIO);
        assertEquals(23,HazRobHardwareProfile.ULTRASONIC_TRIG_GPIO);
        assertEquals(39,HazRobHardwareProfile.ULTRASONIC_ECHO_GPIO);
    }

    @Test public void manufacturerHeaterQualificationMatchesInstalledHardware(){
        assertFalse(HazRobHardwareProfile.CYCLIC_MQ_HEATER_DRIVER_INSTALLED);
        for(String key:new String[]{"mq2","mq3","mq4","mq5","mq8","mq135"})
            assertTrue(key,HazRobHardwareProfile.manufacturerHeaterProtocolValid(key));
        assertFalse(HazRobHardwareProfile.manufacturerHeaterProtocolValid("mq7"));
        assertFalse(HazRobHardwareProfile.manufacturerHeaterProtocolValid("mq9"));
        assertFalse(HazRobHardwareProfile.manufacturerHeaterProtocolValid("mq6"));
    }
}
