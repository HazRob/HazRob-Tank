#!/usr/bin/env python3
"""Fail closed until Tank V4 is genuinely buildable without the SERVOC repository."""
from pathlib import Path
import sys

root = Path(__file__).resolve().parent.parent
required = [
    "settings.gradle",
    "app/build.gradle",
    "app/src/main/AndroidManifest.xml",
    "gradlew",
]
missing = [path for path in required if not (root / path).is_file()]
if missing:
    print("TANK V4 NOT SELF-CONTAINED: missing essential Android project files:", file=sys.stderr)
    for path in missing:
        print(" - " + path, file=sys.stderr)
    print("Do not remove private-source access or claim a standalone APK until these are present.", file=sys.stderr)
    sys.exit(2)
print("PASS: essential standalone Android project paths exist; run Gradle to verify completeness")
