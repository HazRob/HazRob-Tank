#!/usr/bin/env python3
"""Stage the pinned ChemCore Stable 1.0.0-hr069r1 offline payload for Tank V6.

Run ONLY against a temporary, overlaid pinned SERVOC-Android checkout.
Preserve chemical contents, evidence tiers, source revision and SHA semantics.
No APK is built, no remote snapshot is published and no M4 is promoted.
"""
from pathlib import Path
import argparse

p=argparse.ArgumentParser()
p.add_argument("source",type=Path)
args=p.parse_args()
root=args.source.resolve()
generator=root/"tools/build_hr069_embedded_stable.py"
release=root/"app/src/main/java/com/servoc/app/ReleaseAudit.java"
gradle=root/"app/build.gradle"
for f in (generator,release,gradle):
    if not f.is_file():raise SystemExit("Missing V6 Stable dependency: "+str(f))
assert 'public static final String VERSION="6.0";' in release.read_text(encoding="utf-8"),"Wrong app release identity"
assert "SERVO_DRIVE=true" in release.read_text(encoding="utf-8"),"Wrong vehicle source"
g=gradle.read_text(encoding="utf-8")
assert "applicationId 'com.hazrob.tank'" in g and "versionName '6.0'" in g,"Wrong independent package"

s=generator.read_text(encoding="utf-8")
for bound in ("min","max"):
    expected=f'"{bound}_app_version":"6.0"'
    original=f'"{bound}_app_version":"0.10.2"'
    prior=f'"{bound}_app_version":"5.0"'
    if expected not in s:
        matches=[x for x in (original,prior) if x in s]
        if len(matches)!=1:raise SystemExit("Unexpected generator "+bound+" compatibility")
        s=s.replace(matches[0],expected)
for contract in ('"library_version":LIBRARY_VERSION',
                 '"payload_sha256":hashlib.sha256(raw).hexdigest()',
                 '"calibration_count":0',
                 'root["content_counts"]["response_priors"]!=49'):
    if contract not in s:raise SystemExit("Stable evidence contract changed: "+contract)
if generator.read_text(encoding="utf-8")!=s:
    generator.write_text(s,encoding="utf-8")
print("V6 offline Stable source prepared: pinned 1.0.0-hr069r1, app 6.0, 652 identities, 49 research priors, 0 M4.")
