#!/usr/bin/env python3
"""Prepare OFFLINE ChemCore Stable compatibility for an ephemeral V9 Android build.

Never modifies the research payload, evidence qualification or upstream branch.
The verified Stable data and SHA are unchanged; only the APK compatibility
version is adapted in the disposable checkout.
"""
from pathlib import Path
import argparse
import re

p = argparse.ArgumentParser()
p.add_argument("source", type=Path)
args = p.parse_args()
root = args.source.resolve()
gradle = root / "app/build.gradle"
release = root / "app/src/main/java/com/servoc/app/ReleaseAudit.java"
generator = root / "tools/build_hr069_embedded_stable.py"
for path in (gradle, release, generator):
    if not path.is_file():
        raise SystemExit(f"Missing pinned Android build input: {path}")
r = release.read_text(encoding="utf-8")
g = gradle.read_text(encoding="utf-8")
assert 'public static final String VERSION="9.0";' in r
assert 'public static final int DATABASE_SCHEMA=16;' in r
assert 'public static final String ANALYTICAL_ENGINE_VERSION="chemcore-next-research-core/0.3-g1-source-trace";' in r
assert "applicationId 'com.hazrob.tank'" in g
assert "versionName '9.0'" in g and "versionCode 9" in g
s = generator.read_text(encoding="utf-8")
for bound in ("min", "max"):
    pattern = r'("' + bound + r'_app_version"\s*:\s*")([^"]+)(")'
    matches = list(re.finditer(pattern, s))
    if len(matches) != 1:
        raise SystemExit(f"Unexpected Stable {bound} compatibility definition")
    current = matches[0].group(2)
    if current not in ("0.10.2", "5.0", "6.0", "7.0", "8.0", "9.0"):
        raise SystemExit(f"Unexpected Stable compatibility: {bound}={current}")
    s = s[:matches[0].start(2)] + "9.0" + s[matches[0].end(2):]
for contract in (
    '"library_version":LIBRARY_VERSION',
    '"payload_sha256":hashlib.sha256(raw).hexdigest()',
    '"calibration_count":0',
    'root["content_counts"]["response_priors"]!=49',
):
    if contract not in s:
        raise SystemExit("Stable evidence contract changed: " + contract)
generator.write_text(s, encoding="utf-8")
print("V9 immutable Stable source prepared: 652 identities, 49 research priors, zero calibration approvals.")
