#!/usr/bin/env python3
"""Prepare the pinned original ChemCore snapshot builder for independent Tank V4.

Build preparation only: this script neither compiles Android nor promotes research
models to validated M4 calibrations. It preserves the existing snapshot and SHA256
integrity generation, but makes the embedded manifest compatible with Tank V4.
"""
from pathlib import Path
import argparse

p=argparse.ArgumentParser()
p.add_argument("source",type=Path,help="pinned Android original checkout, after Tank overlays")
args=p.parse_args()
root=args.source.resolve()
generator=root/"tools/build_hr069_embedded_stable.py"
release=root/"app/src/main/java/com/servoc/app/ReleaseAudit.java"
appbuild=root/"app/build.gradle"

if not generator.is_file() or not release.is_file() or not appbuild.is_file():
    raise SystemExit("Missing pinned original Android build inputs")
version='4.0'
release_text=release.read_text(encoding='utf-8')
assert f'public static final String VERSION="{version}";' in release_text, "Tank V4 ReleaseAudit VERSION mismatch"
assert "SERVO_DRIVE=true" in release_text, "Wrong non-Tank hardware release audit"
build_text=appbuild.read_text(encoding='utf-8')
assert "versionName '4.0'" in build_text, "Tank Android versionName is not V4"

s=generator.read_text(encoding="utf-8")
for kind in ("min","max"):
    old=f'"{kind}_app_version":"0.10.2"'
    new=f'"{kind}_app_version":"{version}"'
    if old in s:
        s=s.replace(old,new)
    elif new not in s:
        raise SystemExit(f"Unsupported embedded Stable generator {kind} app contract")
assert '"library_version":LIBRARY_VERSION' in s, "Do not modify ChemCore Stable identity"
assert '"payload_sha256":hashlib.sha256(raw).hexdigest()' in s, "ChemCore checksum missing"
assert '"calibration_count":0' in s, "Only pre-release non-M4 Stable is authorized"
generator.write_text(s,encoding="utf-8")
print("Tank V4 embedded Stable compatibility: app 4.0; SHA256 and no-M4 policy preserved")
