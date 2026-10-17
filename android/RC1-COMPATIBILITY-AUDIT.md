# Tank Android RC1 — static compatibility audit

Status: **BLOCKED — NOT BUILDABLE; NO APK GENERATED**

## Baselines
Android source: `sohpany/SERVOC-Android@6281d3eb7c1c961ce63b0b5e4ae0792874626720`; Tank firmware: `HazRob/HazRob-Tank@develop`.

## Static checks completed

| Check | Result | Evidence / required follow-up |
|---|---|---|
| Main servo pins | Staged | Tank firmware GPIO16 left / GPIO17 right; Android staged profile updated |
| Direct drive | Interface matches | Android GET `/drive?l=&r=`; firmware exposes `/drive` |
| Manual STOP / E-STOP | Interface matches | `/motion/stop`, `/stop`, `/estop`, `/estop/reset` |
| Telemetry | Endpoints match | `/diag`, `/data`; schema-level end-to-end test pending |
| Measurements and relay | Endpoints match | `/measure?cmd=start/cancel`, `/relay?mask=0/1` |
| IR removal | Partial | Five staged tabs and normalized IR fabrication removed; entire source not imported/audited |
| Obstacle auto-stop | Partial | Settings toggle/API methods removed in staged files; stale settings diagnostic label fixed in commit `ad04392513e4324cccf146346907afe6d7e70eaa`; entire source audit pending |
| Experiments | Not tested | Java file copied without behavioral change; no integration or UI test |
| ChemCore | Not tested | Core and dependencies not imported |
| Reports | Not tested | Sources/dependencies not imported |
| Remote/MQTT | Not tested | Remote client and broker integration not imported/verified |
| Camera | Not tested | Existing camera behavior to remain unchanged; verify after build |
| App package/version | Pending | Define unique Tank applicationId and RC1 version, avoid replacing original app |
| Android compilation | Blocked | Only eight Java files staged; Gradle project and dependencies absent |
| Device/robot tests | Not performed | Need physical hardware for motion polarity, loss of control, Echo warning |

## Release blockers
1. Import **complete** Android source at pinned commit, including Gradle project, Java/Kotlin, resources, assets, ChemCore, manifests, tests, and build workflows. Ensure no source repo changes.
2. Fix compile references to removed L298N/IR fields and auto-stop APIs across *all* files. Retain necessary backward-compatible constants only if not misleading.
3. Set distinct Tank applicationId, display name, and versionCode; verify signing strategy.
4. Run Gradle compilation, unit tests, lint/static scans and APK artifact generation; check logs and artifact checksum.
5. Perform real Direct/Remote, video, drive deadman, Echo warning, MQ/ADS/BME, ChemCore, sessions, reports tests. Do not claim physical verification without hardware.

## Conclusion
Interface inspection is **not** a passing integration test. RC1 cannot be delivered as APK from the current incomplete project. No downloadable APK path has been verified.
