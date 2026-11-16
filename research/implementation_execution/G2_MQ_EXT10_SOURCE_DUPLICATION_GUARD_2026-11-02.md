# MQ-EXT-10: fail-closed duplicate-source and directory-entry guard (2026-11-02)

This change is restricted to the research branch and the optional source-only ethanol parser. The published C1-C6 labels describe **liquid solution volume fractions**, not measured ethanol headspace ppm.

## Integrity risk and mitigation

The previous parser detected duplicate (concentration, repetition) identifiers but could accept byte-identical 3330-row measurements under different identifiers, potentially allowing duplicated source data to be mistaken for independent measurements. It also silently ignored nested directories when scanning the optional raw-root folder.

The scanner now rejects SHA256-identical measurement files across any distinct names using `MQ_EXT10_DUPLICATE_MEASUREMENT_BYTES`, and rejects nonregular, symlinked or nested entries with `MQ_EXT10_NONREGULAR_OR_NESTED_SOURCE_ENTRY`. These checks are **necessary, not sufficient**: distinct bytes do not prove independent physical exposures; an original archive with nested data must be flattened in a traceable staging directory before scanning, not silently ignored.

Three deterministic synthetic tests cover cross-concentration duplication, cross-repetition duplication and a nested directory. The existing source-only CI test discovery automatically includes these cases. No original Mendeley raw files were acquired; the existing no-raw receipt remains unchanged. No G2/G3 scientific qualification, gas ppm calibration, or Stable changes are implied.

## Reproduce

```bash
python -m unittest discover -s research/implementation_execution -p 'test_g2_mq_ext10_ethanol_preflight.py' -v
python research/implementation_execution/g2_mq_ext10_ethanol_preflight.py
```

The blocker remains independently grouped, blinded Tank exposures with traceable gas concentration references and authenticated raw-source provenance.
