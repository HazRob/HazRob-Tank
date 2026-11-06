# HazRob Tank V9 — PCA9685 / unified I²C draft allocation (discussion only)

Date: 2026-11-06
Status: PROPOSED — NOT APPROVED FOR FIRMWARE IMPLEMENTATION.
Branch: feature/tank-v9-implementation

## I²C bus topology
- Controller ESP32 GPIO21 = SDA; GPIO22 = SCL; pull-ups at 3.3 V, not 5 V. All module boards must be checked for onboard pull-ups / logic levels and harness length.
- PCA9685: 0x40 (A0..A5 default low).
- ADS1115 #1: 0x48; ADS1115 #2: 0x49 (existing 8 MQ channels unchanged).
- ADS1115 #3: proposed 0x4A (ADDR to SDA) for 4 more single-ended analog channels; ADC inputs MUST remain within actual VDD and GND.
- MPU-6050: 0x68 (AD0 low); 0x69 if AD0 high.
- BME/BMP280: 0x76 or 0x77 after migration from dedicated GPIO13/14 bus. User earlier said BMS; confirm intended board.
- Replaced Lux sensor: hardware model pending. Existing pictured LDR/LM393 is digital threshold only, NOT quantitative lux.

## Owner architecture decision — 2026-11-06

**Approved hardware topology:** Keep both continuous-rotation track drive servos on independent ESP32 LEDC signals: LEFT GPIO16, RIGHT GPIO17, as in the V8 firmware source. Give the larger drive servos a properly sized separate power rail. Do not move their signal pins to PCA9685. This separation is motivated by reliability and control isolation, *not* by the claim that PCA9685 cannot work with separately powered servos.

**Firmware status:** Architectural decision documented; NO source-code modifications or physical acceptance tests have yet been performed.

## Revised PCA9685 PWM channel map (proposed)

| CH | Name | Duty/command type | Requirement status |
|---|---|---|---|
| 0 | gripper_servo | servo motion | proposed |
| 1 | arm_axis_A | servo motion | proposed |
| 2 | arm_axis_B | servo motion | proposed |
| 3 | arm_base_rotation | servo motion | proposed |
| 4 | suction | ON/OFF external power stage | proposed |
| 5 | pump | ON/OFF external power stage | proposed |
| 6 | fan | ON/OFF external power stage | proposed |
| 7 | extinguisher | ON/OFF power stage and independent actuation safety | proposed |
| 8 | night_light | ON/OFF external power stage | proposed |
| 9 | arm_light | ON/OFF external power stage | proposed |
| 10 | warning_light | ON/OFF external power stage | proposed |
| 11 | future_light_4 | reserved; not yet assigned a device | reserved |
| 12 | active_buzzer | ON/OFF envelope; not independent audio pitch | proposed |
| 13 | spare_1 | reserved | reserved |
| 14 | spare_2 | reserved | reserved |
| 15 | spare_3 | reserved | reserved |

Count on PCA9685: 4 arm/gripper servos + 4 accessories + 4 lights (one future) + 1 buzzer + 3 spare = 16. Two large drive servos remain on ESP32 GPIO16/17 and are excluded from the PCA channel count.

## Essential constraints
1. PCA9685 is a logic/PWM control chip; its 3-pin servo breakout channels are not high-current motor drivers. Suction/pump/fan/extinguisher/lights/buzzer need load-appropriate transistors, MOSFET drivers, relays or full motor bridges, protected from inductive flyback as appropriate.
2. All 16 PWM channels share a single prescale frequency. At ~50Hz for servos, a *passive* buzzer cannot have independently adjustable acoustic pitch from PCA9685 alone; an *active* buzzer can produce distinct temporal alert cadences using full ON/OFF and its internal oscillator. Model, operating voltage, current, active/passive type require confirmation.
3. Servo V+ power supply must be appropriately sized and electrically separated from PCA logic VCC; provide a common signal ground where designed, avoid routing servo currents through thin logic conductors. Validate brownouts and bus fault recovery.
4. Keep drive PWM on ESP32 GPIO16/17. Preserve 500ms drive-command watchdog, transport-loss neutral STOP and E-stop. Independently test drive-servo power cuts, GPIO boot behavior, ESP32 brownouts, and the fact that neutral pulses can only stop a calibrated continuous-rotation servo. PCA bus faults must not silently alter drive commands; arm safety and powered actuators require their own fail-safe power strategy.
5. Do not claim V9 code is implemented or compiled. Await owner confirmation, especially number/kinds of arm motors, buzzer type, channel 13, exact current draws and power electronics.

Sources:
- NXP PCA9685 product and data sheet: https://www.nxp.com/products/power-drivers/lighting-driver-and-controller-ics/led-drivers/16-channel-12-bit-pwm-fm-plus-ic-bus-led-driver%3APCA9685
- MPU6050 addresses: https://www.mathworks.com/help/matlab/supportpkg/arduinoio.mpu6050-system-object.html

## Owner clarification — 2026-11-06: arm actuator type

The owner confirmed all currently discussed arm/gripper/base actuators are **servos**, not bare DC motors. The photographed micro servo is labeled TowerPro SG90 9g; manufacturer lists the standard SG90 analog as a nominal 4.8V servo compatible with a regulated 5V supply. The photographed label by itself does not establish whether it is the standard position-limited SG90 (~180 degrees) or a continuous-rotation SG90 variant. Verify actual mechanical range before implementing axis limits or speed controls. Keep revised arm channels CH0–CH3 as provisional servo signals, pending confirmation of which exact axes use SG90 and which use other servos.

SG90 servo power: servo red -> regulated 5V rail on PCA V+, brown -> common GND, orange/yellow -> respective PWM signal; PCA VCC -> 3.3V ESP32 logic rail. Power the servo rail independently of the ESP32 logic output, sized for aggregate peak/stall current. References: https://towerpro.com.tw/product/sg90-analog/ ; https://towerpro.com.tw/product/sg90-360-degree-continuous-rotation-servo/ ; https://learn.adafruit.com/16-channel-pwm-servo-driver/pinouts
