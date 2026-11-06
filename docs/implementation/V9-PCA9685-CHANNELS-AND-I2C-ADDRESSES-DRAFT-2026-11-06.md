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

## PCA9685 PWM channel map

| CH | Name | Duty/command type | Requirement status |
|---|---|---|---|
| 0 | left_track_servo | 50Hz continuous-servo neutral/drive | proposed |
| 1 | right_track_servo | 50Hz continuous-servo neutral/drive | proposed |
| 2 | gripper_servo | servo motion | confirm whether actual motor is N20 DC or 360 servo |
| 3 | arm_axis_A | servo motion | proposed |
| 4 | arm_axis_B | servo motion | confirm actual count |
| 5 | arm_base_rotation | servo motion | confirm motor mechanism |
| 6 | suction | ON/OFF external power stage | proposed |
| 7 | pump | ON/OFF external power stage | proposed |
| 8 | fan | ON/OFF external power stage | proposed |
| 9 | extinguisher | ON/OFF external power stage + extra actuation safety | proposed |
| 10 | night_light | ON/OFF external power stage | proposed |
| 11 | arm_light | ON/OFF external power stage | proposed |
| 12 | warning_light | ON/OFF external power stage | proposed |
| 13 | future_light_4 | reserved; user requested four light slots, named three | reserved |
| 14 | active_buzzer | ON/OFF envelope patterns via driver stage | proposed |
| 15 | spare | reserved | reserved |

Count: 2 drive servos + 4 arm/gripper servos + 4 accessories + 4 lights + 1 buzzer + 1 spare = 16 channels.

## Essential constraints
1. PCA9685 is a logic/PWM control chip; its 3-pin servo breakout channels are not high-current motor drivers. Suction/pump/fan/extinguisher/lights/buzzer need load-appropriate transistors, MOSFET drivers, relays or full motor bridges, protected from inductive flyback as appropriate.
2. All 16 PWM channels share a single prescale frequency. At ~50Hz for servos, a *passive* buzzer cannot have independently adjustable acoustic pitch from PCA9685 alone; an *active* buzzer can produce distinct temporal alert cadences using full ON/OFF and its internal oscillator. Model, operating voltage, current, active/passive type require confirmation.
3. Servo V+ power supply must be appropriately sized and electrically separated from PCA logic VCC; provide a common signal ground where designed, avoid routing servo currents through thin logic conductors. Validate brownouts and bus fault recovery.
4. Before transferring drive outputs from ESP32 GPIO16/17 to PCA, preserve drive command watchdog 500ms, E-stop and failsafe neutral. An OE pin may gate outputs but stopping pulses does **not** guarantee neutral/stopped motion of all continuous-rotation servos. For safety-critical motion, independent drive-power disable or appropriate actuator interlock is required based on tested hardware.
5. Do not claim V9 code is implemented or compiled. Await owner confirmation, especially number/kinds of arm motors, buzzer type, channel 13, exact current draws and power electronics.

Sources:
- NXP PCA9685 product and data sheet: https://www.nxp.com/products/power-drivers/lighting-driver-and-controller-ics/led-drivers/16-channel-12-bit-pwm-fm-plus-ic-bus-led-driver%3APCA9685
- MPU6050 addresses: https://www.mathworks.com/help/matlab/supportpkg/arduinoio.mpu6050-system-object.html
