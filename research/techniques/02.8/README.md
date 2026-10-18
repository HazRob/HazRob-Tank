# 02.8 Provenance schema — HazRob Tank research prototype

Minimal structured provenance record for each raw sample batch or derived feature. Required: schema version, record/device/session/sensor IDs, UTC timestamp, firmware and calibration versions, heater phase, measurement kind and units, sample count and ordered processing steps. Optional: environment, parent record IDs and protocol identifier. Canonical JSON SHA-256 digest supports detecting changes to metadata content but **does not authenticate source data** and is not a cryptographic signature. For traceable derived features, parent data hashes, raw-data checksums, algorithm versions, acquisition time interval, gas protocol, ambient conditions and QA flags must be added to the production schema.

Critical: record metadata is not proof that physical measurements were correctly calibrated, nor proof of gas identity. UTC clock synchronization, sensor channel mapping, BME280 timing, MQ7/MQ9 heater state and session split must be checked. This prototype is not integrated into APK/firmware/ChemCore production.

14 unit cases authored; clean checkout execution pending. Run `cd research/techniques/02.8 && python -m unittest -v`. Status PROTOTYPE.
