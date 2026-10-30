# Cam-tank — HazRob Tank V6 independent camera firmware (candidate)

**Status:** UNRELEASED camera-controller Arduino sketch for AI-Thinker ESP32-CAM. Version `Cam-tank 0.1.4-v6-candidate`. This is independent from the original HazRob-CAM source archived in `firmware/upstream/`.

## Open in Arduino IDE
Extract the separate `HazRob-Tank-V6-Firmware-Arduino.zip`; open `Cam-tank/Cam-tank.ino` in Arduino IDE. Select **AI Thinker ESP32-CAM**, enable PSRAM, install Espressif `esp32` Arduino core (verified version recorded by CI), and configure the correct serial/USB flashing adapter. This ZIP is a sketch project, **not** an Arduino library for “Add ZIP Library”.

## Network and camera behavior
- Wi-Fi **STA only** with SSID `HazRob`, static IP `192.168.4.2`, gateway `192.168.4.1`.
- Local HTTP status on port 80; MJPEG stream on port 81 at `/stream`; port 80 `/stream` redirects to 81. Local playback takes priority over remote video.
- AI-Thinker camera sensor pinout, QVGA 320×240, RGB565 capture, software JPEG encoding, PSRAM required. LED flash on GPIO4.
- **Do not assert remote WSS video works on this configuration without hardware proof.** Existing camera gate explicitly requires the controller's `remote/status` report to include `internet=true` and `napt=true`. Tank firmware built using Arduino-ESP32 2.0.17 currently marks NAPT unavailable, so camera remote publishing is expected to remain disabled in that configuration. Local video is independent of NAPT.
- The existing Cloud/WSS paths are inherited, and endpoint/credentials must be checked before deployment; no production access token belongs in the Arduino sketch ZIP.

## Acceptance
A passing Arduino compile verifies source correctness **only**. Real PSRAM initialization, frame rate, Wi-Fi station connection, flashlight power, signal quality and ESP32/Main controller compatibility still require bench testing. All V6 candidate firmware and Android products are pre-release until the documented release gates are closed.

## Existing implementation details

## Source ancestry: HazRob ESP32-CAM 0.10.1

Hardware target: AI-Thinker-style ESP32-CAM.

Inherited upstream functionality documented here for compatibility reference; this is now a Cam-tank V6 candidate.

## Network contract

- Station SSID: `HazRob`
- Static IP: `192.168.4.2`
- Gateway/main ESP32: `192.168.4.1`
- Wi-Fi mode: STA-only
- The camera never creates a SoftAP
- Wi-Fi sleep and library auto-reconnect are disabled; reconnect is controlled by the firmware

## Local camera service

- HTTP status/control: port 80
- MJPEG stream: port 81, `/stream`
- `GET /stream` on port 80 redirects to the port-81 stream
- Local viewing has priority over remote publishing

The local stream uses:

- AI-Thinker pin mapping
- XCLK 20 MHz
- QVGA 320×240
- RGB565 capture
- software `frame2jpg`
- JPEG quality 35
- one PSRAM framebuffer
- 160 KiB JPEG buffer
- structural JPEG validation before transmission

The local MJPEG task is given higher scheduling priority than the camera control loop, while still yielding between frames. TCP no-delay is enabled for the local viewer.

## Remote video

Remote video publishes JPEG frames through authenticated WSS to `hazrob-video-relay.onrender.com`.

Remote publishing is enabled only when the main controller reports REMOTE mode, Internet readiness and NAPT, and the camera has a valid TLS wall clock. A local MJPEG client pauses remote publishing.

The trusted ISRG Root X1 CA used by the camera is the same validated root used by the main controller. Remote video uses bounded reconnect backoff and adaptive frame pacing from 55 ms to 120 ms. The remote video worker remains alive after first creation so stopping and restarting remote publishing cannot lose the task during a transition.

## Flash

GPIO4 controls the onboard flash LED.

Supported mode values are `off`, `auto`, and `on`. In this firmware `auto` is retained as a protocol state; automatic luminance-based switching is not implemented.

## Build target

Upstream recommended Arduino-ESP32 core: 3.3.11 (the V6 CI must verify each independent sketch).

Manual board target: AI Thinker ESP32-CAM / `esp32cam`.
