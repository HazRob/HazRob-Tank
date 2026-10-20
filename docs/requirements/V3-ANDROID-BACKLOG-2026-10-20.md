# HazRob Tank Android V3 — baseline audit and rolling backlog
Date: 2026-10-20 | State: DRAFT FOR OWNER REVIEW | Scope: independent Android application only

## Evidence and version boundary
- Repo: HazRob/HazRob-Tank, Android working branch develop.
- Last successfully built V2 APK: commit `df92e9ccd2d7d8b69b83fff5515569d298734dc4`, Actions run `39084627573`; debug-signed, not upgrade-stable, no physical device validation.
- Later offline-entry code `ec8861da7a9c8141d4a6554cfb39a8b7cc5acaca` was **not** included in that APK. Workflow run `39086020081` compiled and ran unit tests successfully but failed at persistent signing (`TANK_KEYSTORE_BASE64` missing); no new signed artifact.
- Current develop had simulator files deleted at owner's request, final cleanup commit `4bc3bfe7672423ca96d77d668a6d0ba80dc1ebbe`. **Do not reintroduce Android emulator / screenshot CI** unless requested.
- Source baseline remains `sohpany/SERVOC-Android@6281d3eb7c1c961ce63b0b5e4ae0792874626720` (read-only). Tank overlay is the only place for Tank source changes.
- Categories: MISSING_IN_V2_APK; PARTIAL_IN_V2; PRESENT_UNVERIFIED; PROPOSED_FOR_V3. Unit tests and static contract matching are not on-robot validation.

## Approved / documented gaps identified for V3
| ID | Priority | Area | V2 assessment | Proposed V3 acceptance criterion |
|---|---|---|---|---|
| T3-001 | P0 | Signing and upgrades | MISSING_IN_V2_APK | Configure stable keystore securely via administrator; retain package com.hazrob.tank, increment versionCode, validate signature, install update over same-signed predecessor without data loss, publish standalone signed APK not artifact ZIP. Existing V2 debug key may not support in-place upgrade. |
| T3-002 | P0 | True offline review | MISSING_IN_V2_APK; subsequent code PARTIAL | Offline visible and navigable with no robot, no false connection/telemetry, no nonzero drive or automatic networking. Explicit bug to fix: ConnectionGate writes 'offline' but callback Mode.DIRECT passes 'direct' into EntryActivity/MainActivity; MainActivity then overwrites connection_mode and starts comms. |
| T3-003 | P0 | Motion safety / fail-safe | PRESENT_UNVERIFIED | Verify release-to-stop, E-STOP latch and reset, 500ms watchdog, no motion when disconnected/offline, direct/remote handling. Do not change established safety mechanisms without separate approved risk decision. |
| T3-004 | P1 | Tank-only branding | PARTIAL_IN_V2 | Replace remaining original HazRob/HR069 icon, headings, footers, report filenames and labels with independent HazRob Tank V3 branding while preserving app identity. |
| T3-005 | P1 | Uniform RTL light styling | PARTIAL_IN_V2 | Audit Control/FieldSessions/Reports/Status/Settings/Developer screens for inherited dark palettes, insufficient contrast, crowded buttons and RTL overflow; preserve camera dark viewport when useful. |
| T3-006 | P1 | Field sessions lifecycle | PARTIAL_IN_V2 | Implement 2-pane الجلسات [الإدارة, التقارير]; single accepted acquisition per session; show only جديدة/معتمدة; completion/approval/cancel/retry rules; ensure atomic storage and migration; site cannot be deleted with children. Pure TankAcquisitionPolicy alone is not connected to old FieldSessionsTab repository/UI. |
| T3-007 | P1 | Approved-only reporting | PARTIAL_IN_V2 | Filter field report selectors and site aggregates to APPROVED captures only; disallow report creation from incomplete/unapproved sessions. Old ReportsTab currently lists sessions from sessionsForSite without approval filtering. |
| T3-008 | P1 | Instant analysis vs documented acquisition | PARTIAL_IN_V2 | Distinct lifecycle and persistence; instant capture does not become a site/session; automatically bounded, status-aware; start locked during acquisition; approved field capture must be tied to exactly one site session. |
| T3-009 | P1 | Chemical capabilities and target UX | PARTIAL_IN_V2 | Data-backed columns for availability of ppm, fingerprint, curves and molecular methods; verified chemical family taxonomy; selected target prominence; safe tracking for records without legacyId; no implied validated detection. |
| T3-010 | P1 | Analysis evidence and safety highlighting | PARTIAL_IN_V2 | Show ranked top-ten, evidence tiers and validated ppm only; limit crossing in bold red only where reference/time/calibration comparable; clear styles on disconnection and missing data; distinguish instantaneous vs OSHA TWA/STEL. |
| T3-011 | P1 | Physical snapshot and reference provenance | PARTIAL_IN_V2 | Dedicated instantaneous PDF containing physical measurements displayed, units, time and validity; provenance for calibration and duration; physical reference policy receives real validated arguments, not fixed `false,false,0,false`; no invented dBA/PM. |
| T3-012 | P1 | Smart report UX/templates | PARTIAL_IN_V2 | Owner-approved page layouts, summaries and verified charts, correct PDF/Excel, timestamps, evidence and source; no fake points; align site/session vs instant scopes. |
| T3-013 | P1 | Real ChemCore embedded activation | PRESENT_UNVERIFIED | Confirm 652-identity HR069 stable payload installation and runtime active-library selection, hash and usability; 0 calibration records is not a field-validation claim. Research 80-techniques prototypes must not be represented as production integrated by default. |
| T3-014 | P1 | Network and hardware contract | PRESENT_UNVERIFIED | Hardware verify API4 response JSON, Direct AP, Remote MQTT, camera, MQ relay, 2 ADS, BME, Echo warning-only, dual servos. Do not add IR, sensor-triggered automatic stop, or L298N legacy references. |
| T3-015 | P2 | Particle/PM view | PARTIAL_IN_V2 | Current PM1/PM2.5/PM10 display is a placeholder with disabled reports; add real measured series/reports only after approved PM hardware and calibration, otherwise explicit unavailable state. |
| T3-016 | P2 | Performance, regression and visual acceptance | PARTIAL_IN_V2 | Test chemical lookup responsiveness, navigation, list scrolling, disconnection, colors and data state across all seven top tabs and subviews on real Android device; no emulator dependency. Replace legacy test exclusions with Tank-appropriate assertions. |
| T3-017 | P2 | Developer/operator data isolation | PRESENT_UNVERIFIED | Ensure user local sessions do not publish centrally; developer reference experiments only are sync/upload candidates; audit access, export and update screens. |
| T3-018 | P2 | App install / security behavior | PARTIAL_IN_V2 | Diagnose package conflict and Play Protect warning through signature, manifest permissions and distribution integrity; do not instruct bypassing protection. |
| T3-019 | P2 | Research technique future integration | PROPOSED_FOR_V3 only on owner approval | Treat ChemCore 80-technique research, G1/G2 benchmarks and Tank hardware G3 separately from app stable runtime; integrate only techniques satisfying scientific/physical validation and explicit owner approval. |

## Owner-requested additional requirements (sequential review)
| ID | Priority | Area | V2 assessment | V3 acceptance criterion |
|---|---|---|---|---|
| T3-020 | P1 | Measurements > الفيزيائية visual redesign | MISSING_IN_V2_APK | Replace the conventional four-column physical-measurement TableLayout/TableRow view with a modern responsive RTL grid/list of individual measurement cards; each physical measurement has a clear relevant emoji/icon, readable Arabic title, real live value and unit, source/quality/connection status, and a validated warning visual state. Responsive portrait/landscape with no clipping. Preserve all PhysicalCatalog measurements and diagnostic values including MAX4466 RMS/P-P; distinguish unsupported/uninstalled values from disconnected; never invent measurement values or limit crossings. User to approve final look before implementation. Cross-reference T3-005 (visual style) and T3-011 (physical references/PDF). |
 
## Source-specific findings
- `android/app/src/main/java/com/servoc/app/measurement/TankAcquisitionPolicy.java` has NEW/WARMUP/MEASURING/COMPLETED/CANCELLED/APPROVED but this is a stand-alone policy, not proof of durable session-state integration.
- V2 inherited `FieldSessionsTab` uses active/closed sessions with add/close buttons, not two required management/report panes nor approval status.
- V2 inherited `ReportsTab.refreshSessionScope` enumerates `fieldSessions.sessionsForSite` and labels active/closed, not approved only.
- `MeasurementsTab.PhysicalPane.setPhysical` calls `PhysicalReferencePolicy.evaluate(index,value,true,false,false,0L,false)`: real context/averaging/calibration not passed, so most valid conditional exceedances are not evaluated.
- `MeasurementsTab.buildParticlesLanding` provides dashes, empty-data chart and a disabled PM report; this is correct for absent PM hardware.
- `MainActivity` still has hard-coded header “HazRob Tank v2”; inherited `ReleaseAudit.VERSION` footer and inherited branding in `ConnectionGate`.
- `MainActivity` still contains legacy IR parsing paths in updateGasAlert/onTelemetryUpdated despite Tank without installed IR: audit; never claim a real IR/cliff sensor.
- Device safety concern not to be silently altered: firmware history-reversal on link loss appears in `firmware/ESP32-Tank/CHANGELOG.md`, and lacks documented physical validation.

## Change-control protocol
- This backlog captures previously documented requirements and review findings; it is **not approval to implement or release**.
- Owner-requested item T3-020 recorded after comparing against the exact V2 APK source commit; assign further new requests T3-021+ with affected tab, acceptance test, dependencies and priority.
- No APK release until V3 owner requirements are frozen, unit/integration checks pass, Android actual-device UI is reviewed, hardware safety tests are performed and stable signing is configured.
- No emulator or screenshot workflow reinstatement.
