# HazRob Tank firmware version policy

Independent firmware components:
- `ESP32-Tank` — initial source baseline `0.1.0`
- `Cam-tank` — initial source baseline `0.1.0`

These are source-import baselines, **not verified builds or production releases**. Each subsequent code change must increment its component version, update its CHANGELOG, and record build/test status. Semantic versioning: MAJOR.MINOR.PATCH; pre-release suffixes (`-rc.N`) as needed. The original HR069 sources are preserved at `firmware/upstream/hr069-0.10.2/`.

Provenance: `sohpany/SERVOC-Android`, branch `release/hazrob-0.10.2-rc2`, snapshot imported 2026-10-17. Only the Arduino sketch filenames were renamed in the working copies; the code content is unchanged.
