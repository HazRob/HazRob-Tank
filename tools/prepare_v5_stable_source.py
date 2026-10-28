#!/usr/bin/env python3
"""Prepare an already-overlaid pinned Tank Android checkout for V5 ChemCore Stable.

This is source PREPARATION ONLY. No Gradle, compilation, download, release,
firmware flash, scientific model promotion or APK is performed here.

The same immutable 1.0.0-hr069r1 payload can be embedded in V5, but the
generated manifest compatibility must explicitly match the new V5 APK.
"""
from pathlib import Path
import argparse
import hashlib

parser=argparse.ArgumentParser()
parser.add_argument("source",type=Path,
                    help="Pinned original Android source after all Tank V5 Java overlays")
args=parser.parse_args()
root=args.source.resolve()
generator=root/"tools/build_hr069_embedded_stable.py"
release=root/"app/src/main/java/com/servoc/app/ReleaseAudit.java"
gradle=root/"app/build.gradle"
for path in (generator,release,gradle):
    if not path.is_file():
        raise SystemExit(f"Missing V5 source prerequisite: {path}")

version="5.0"
rel=release.read_text(encoding="utf-8")
if 'public static final String VERSION="5.0";' not in rel:
    raise SystemExit("V5 ReleaseAudit mismatch: refuse to prepare Stable")
if "SERVO_DRIVE=true" not in rel:
    raise SystemExit("Non-Tank hardware audit detected")
build=gradle.read_text(encoding="utf-8")
if "applicationId 'com.hazrob.tank'" not in build or "versionName '5.0'" not in build:
    raise SystemExit("V5 independent applicationId/versionName not configured")

s=generator.read_text(encoding="utf-8")
for bound in ("min","max"):
    target=f'"{bound}_app_version":"5.0"'
    found=False
    for old in ('0.10.2','4.0','5.0'):
        old_literal=f'"{bound}_app_version":"{old}"'
        if old_literal in s:
            if old_literal!=target:
                s=s.replace(old_literal,target)
            found=True
            break
    if not found:
        raise SystemExit(f"Unsupported generator {bound}_app_version expression")
if '"library_version":LIBRARY_VERSION' not in s:
    raise SystemExit("Stable identity expression changed")
if '"payload_sha256":hashlib.sha256(raw).hexdigest()' not in s:
    raise SystemExit("Missing payload checksum contract")
if '"calibration_count":0' not in s:
    raise SystemExit("Cannot embed unverified M4 calibrations")

# Write ONLY the generator source; underlying knowledge and provenance remain unchanged.
if generator.read_text(encoding="utf-8")!=s:
    generator.write_text(s,encoding="utf-8")
print("Tank V5 Stable source prepared: compatibility 5.0 only; payload SHA256"+
      " and zero unvalidated device M4 calibrations preserved; NO BUILD")
