# ChemCore Stable — Android V6 read-only update channel

This directory is the planned canonical **public distribution channel** for signed-off
ChemCore Stable library snapshots consumed by **HazRob Tank V6**.

The Android app uses the **existing Settings > ChemCore updates** action, NOT
a second Settings button. Its pinned URL is:

`https://raw.githubusercontent.com/HazRob/HazRob-Tank/main/updates/chemcore-stable/manifest.json`

## Current publication state

**NOT PUBLISHED YET.** There is deliberately no production `manifest.json`
or `chemcore-stable-snapshot.json` here until a properly validated Stable
snapshot is promoted and both files are published under the `main` branch.
The Android app continues using the embedded verified
`1.0.0-hr069r1` Stable package offline. An HTTP 404 during an update check
is shown as an update-check error; it MUST NOT replace or erase the embedded
library or generate a fabricated update.

## Required source publication pair

- `manifest.json` using `ChemCoreLibraryManifest` schema version 2,
  channel `stable`, matching `source_commit_sha`, `data_revision`,
  `evidence_revision`, `min_app_version`, `max_app_version`,
  `feature_schema_version`, sampling profile compatibility, and SHA-256.
- `chemcore-stable-snapshot.json` matching the declared byte length, hash,
  schema, source commit and counts. The library may contain research priors;
  it **must not** claim Tank G3 or M4 physical calibrations that do not exist.

The GitHub-only updater downloads into temporary storage, validates the
source directory, payload size and SHA-256, checks the Stable schema and
compatibility, then installs atomically with rollback. Updating is not
permitted during a running measurement or analytical session.

## Source-of-truth reminder

- SQLite V14 unified chemical **master catalog** contains 734 distinct
  identities, plus exposure limits, source hazards, natural range coverage
  and per-sensor MQ reference curve links.
- The existing embedded ChemCore Stable **snapshot** contains 652 molecular
  identities + 49 research response priors. The two counts represent
  different layers, **not** duplicate selectable chemical catalogs.
- 0/80 Tank technique G3 physical validations remain: neither layer is a
  certified gas detector or licensed occupational ppm alarm.

## Software acceptance / no APK

The owner requires finishing all Android V6 software modifications and
acceptance tests before producing an APK. The build workflow remains
manual-only and **fail-closed** under
`docs/implementation/V6-ANDROID-SOFTWARE-ACCEPTANCE.json`.

For source-only preflight use
`.github/workflows/chemcore-v6-embedded-stable-source-ci.yml`,
which reconstructs and checks the embedded package but **does not**
publish a remote Stable snapshot or build an APK.
