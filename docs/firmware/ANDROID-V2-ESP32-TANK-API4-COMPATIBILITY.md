# Android Tank v2 ↔ ESP32-Tank API4 contract review
Status: static source audit, NOT a hardware validation. Date: 2026-10-18. Branch: develop.

## Sources reviewed
- `firmware/ESP32-Tank/Config.h`: FW `ESP32-Tank 0.1.1-dev`, `API=4`, HW profile `tank-doit-v1-2servo-8mq-echo-bme`.
- `firmware/ESP32-Tank/ESP32-Tank.ino`: Direct HTTP route registrations and network profile guards.
- `firmware/ESP32-Tank/SettingsOps.cpp`: configuration clamping.
- `android/app/src/main/java/com/servoc/app/communication/RobotApi.java`: direct transport commands.
- `android/app/src/main/java/com/servoc/app/communication/RobotProtocolV4.java`: telemetry normalization.

## Direct HTTP compatibility matrix
| Android RobotApi | ESP32-Tank route | Static assessment |
| --- | --- | --- |
| `GET /drive?l=&r=` | `/drive`, checks both args, clamps [-100,100], rejects REMOTE and latched ESTOP | Matched |
| `GET /estop` | `/estop` latches safety and stops | Matched |
| `GET /estop/reset` | `/estop/reset` | Matched |
| `GET /sensor/off` | `/sensor/off` | Matched |
| `GET /heartbeat?arm=0` | `/heartbeat` handles `arm=0` | Matched |
| `GET /relay?mask=0/1` | `/relay` accepts only 0/1 | Matched |
| `GET /measure?cmd=start/cancel` | `/measure` start/cancel, start may return HTTP 409 on precheck failure | Matched |
| `GET /data` | `/data` returns JSON from `dataJson()` | Matched |
| `GET /diag` | `/diag` returns JSON from `diagJson()` | Matched |
| `GET /cfg` | `/cfg` returns settings JSON | Matched |
| `GET /cfg?cam=&speed=&warm=&measure=` | `/cfg` accepts same arguments | Matched |
| `POST /remote/...` | remote management routes exist | Further payload-by-payload validation needed |

## Settings contract
Firmware clamps `speed` to 20–100, `warm` to 0–3600 seconds, `measure` to 5–600 seconds. Android must render returned values rather than assume unbounded requested values were accepted.

## Telemetry and lifecycle
- `RobotProtocolV4.normalize(diag,data,...)` produces `env`, `device`, `relay`, `sound`, `ldr`, `alert`, `adc`, `mq`, `sensor_power`, `measurement`, `drive`, `safety`, `link`.
- Firmware measurement states are normalized as `idle/warmup/measuring/ready/error`, with `duration_s/elapsed_s/remaining_s`, and `valid_channels`.
- A field session should be eligible for approval only after normalized `ready` and validated sample content. The app must not infer completion from a countdown alone.
- Firmware has **one MQ relay**, eight MQ channels across two ADS1115; no PM hardware. Do not show PM values unless provided by a future sensor.
- Echo is warning-only; do not claim automatic obstacle stopping. IR/cliff detection is not part of this Tank profile.
- Two continuous rotation servos on GPIO16/17; watchdog 500 ms; Android direct drive refresh target 150 ms with 250 ms request timeout. Keep drive requests out of UI blocking operations.
- Default boot is Direct AP-only at 192.168.4.1, camera at 192.168.4.2. Remote mode is opt-in.
- `Config.h` servo inversion is explicitly provisional: validate both sides with tracks lifted.

## Release gating and limitations
This review verifies route names and parameter conventions **by reading source**. It does not prove actual response schema values, timing, RF stability, current delivery, ESP32-CAM video quality, or remote MQTT operation. Run Gradle, integration fixtures, and physical testing before calling v2 compatible in operation.
