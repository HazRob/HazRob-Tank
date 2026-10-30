# HazRob Tank ChemCore V6 — N09B Unified chemical master, hazards, and technique evidence (V13)
Date: 2026-10-30. Independent Tank branch: `feature/tank-v6-implementation`.

## Core invariant
One canonical `chemicals.uid` per material. The 144 historical `legacy_id` records belong inside the unified material master and are NOT a second application picker. The former list is a retained research-priority/provenance subset. Source evidence, scientific risk reference tables, observations and session histories use the SAME canonical identity. Never use a source curve's 0-based legacy ID as a SQLite UID.

The `v11_unified_chemical_catalog` SQL VIEW name is retained for Android API compatibility and extended to 82 columns in SQLite schema **V13**; it is not a duplicate table. Selection and search use `ChemicalRepository.search` / `byUid` / `scientificProfile(uid)`.

## Verified snapshot, not chemical safety approval
Android API31 integration [GitHub Actions #40994115598](https://github.com/HazRob/HazRob-Tank/actions/runs/40994115598) passed with 39 assertions:
- `sqlite_version=13`; `materials=734`; `unified_chemical_menu_rows=734`; `unique_master_uids=734`; `original_144_members=144`
- `unified_view_columns=82`; source-qualified LFL materials `213`; UFL materials `179`
- Official reference limits `1432` (OSHA Z1 `638`; NIOSH IDLH `402`; others including NIOSH LFL/UFL `392`). Limits are not material identities and may have multiple jurisdictions, duration and units
- `chemical_risk_profiles=734`; source-qualified natural ranges initially `1` (oxygen humid/dry air contextual range); unsourced natural boundaries deliberately **NULL**
- Prior to the additional EPA radon seed, curated source-qualified hazard classifications `7`; no attempt to infer all 734 labels
- `physical_tank_g3=0`, `chemcore_m4_g3_validation_count=0`, `certified_gas_alerts=false`.

## Schema / source distinctions
- The material view includes original identity, CAS/PubChem, names/classification/formula and `original_144_member`.
- Natural ranges `natural_min`, `natural_max`, `natural_environment_type`, `natural_range_unit`, `natural_range_source_id`: source- and context-qualified. Values may not be generalized to occupied rooms or substituted as occupational limits.
- Occupational `recommended_limit_niosh`, `osha_pel_twa_ppm`, `niosh_idlh_ppm` are distinct. Early warning fraction `early_exposure_fraction` defaults 0.9 only for suitable occupational reference checks. Instantaneous reading at an 8h TWA value is a *potential location hazard* requiring independent verification, not proof of regulatory TWA exceedance.
- Hazard boundaries `danger_lower_limit`, `danger_upper_limit`, `danger_limit_unit` are separate from reference concentration and normal-air values. Oxygen below 19.5% / above 23.5% is tied to OSHA confined-space scope, not universal outdoor background.
- Flammable limits `lfl_vol_pct`, `ufl_vol_pct`, `lfl_source_id`, `ufl_source_id`, `flammable_precaution_10pct_lfl_vol`: NIOSH NPG numeric LFL/UEL in volume percentage where source-qualified. Do not trigger on 90% LFL. A mixture's flammability cannot be represented by a single pure compound's LFL.
- Positive-only source evidence flags `is_oxidizer`, `is_asphyxiant`, `is_irritant`, `is_harmful`, `is_corrosive`, `is_radioactive`, `is_toxic`, `is_carcinogen`, `is_flammable`, `is_explosive`, `is_sensitizer`, `is_reproductive_toxic`, `is_environmental_hazard`, `is_neurotoxic`, `is_mutagen`, `is_organ_toxic`. If no verified category record, these return `NULL`, **not 0 = safe**.
- Evidence descriptors `iarc_group`, `ghs_h_codes`, `ghs_pictograms`, `health_effects`, `health_effect_severity`, `exposure_routes`, `target_organs`, `incompatibilities`, `sds_url`, `radioisotope`; physical numeric properties `flash_point_c`, `autoignition_c`, `boiling_point_c`, `vapor_density_air`, `dust_mec_g_m3`, `explosion_pressure_kpa`.
- ChemCore metadata `chemcore_fingerprint_artifact_count`, `chemcore_legacy_fingerprint_count`, `chemcore_source_fingerprint_count`, `chemcore_source_curve_count`, `chemcore_local_calibration_curve_count`, `chemcore_logarithmic_model_count`, `chemcore_technique_evidence_count`, `chemcore_g1_evidence_count`, `chemcore_g2_evidence_count`, `chemcore_m4_g3_validation_count`, `chemcore_m4_status`. Counts refer to different evidence families; no evidence counts are a calibrated accuracy score.
- Linked source tables `chemical_hazard_classifications`, `chemical_hazard_properties`, `chemical_background_ranges`, `chemical_hazard_boundaries`, `exposure_limits`, `chemcore_technique_evidence` preserve multiple qualifications and original sources without duplicating material rows.

## Verified sample evidence (not complete population)
1. Benzene, CAS 71-43-2: OSHA 29 CFR 1910.1028 supports cancer, flammability, irritation, blood/CNS effects; OSHA technical appendix supports flash point -11°C, autoignition ~580°C, boiling 80.1°C, vapor density 2.7. WHO/IARC benzene carcinogenicity Group 1; separately coded source records.
2. Oxygen, CAS 7782-44-7: oxidizer (oxygen can strongly promote combustion), **not a flammable fuel**. Hazard from oxygen deficiency/enrichment uses distinct lower/upper bounds and requires capable oxygen instrument.
3. Methane, CAS 74-82-8: simple oxygen displacement/asphyxiation information in OSHA/NIOSH oil-and-gas tank-gauging alert. Source LFL/UFL stored independently; no inferred safe level.
4. Radon, CAS 10043-92-2: EPA radioactive gaseous hazard and lung cancer risk. Requires a dedicated radon/radiation measurement instrument. MQ sensors do not establish radioactivity.

## Source references
- [NIOSH Pocket Guide Introduction](https://www.cdc.gov/niosh/npg/pgintrod.html) — LEL/UEL physical properties, exposure routes and health effects.
- [OSHA Hazard Communication](https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.1200) — health / physical hazard classification.
- [OSHA Benzene](https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.1028) and [Substance technical annex](https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.1028AppB).
- [IARC classified agents](https://monographs.iarc.who.int/agents-classified-by-the-iarc/).
- [PubChem oxygen CID977](https://pubchem.ncbi.nlm.nih.gov/compound/977).
- [EPA radon](https://www.epa.gov/radiation/frequent-questions-radiation-sources).
- [NIOSH/OSHA Tank Gauging Alert](https://www.cdc.gov/niosh/docs/2016-108/pdfs/2016-108.pdf).

## Gaps / prohibitions
Only 213/734 source-qualified LFL and 179/734 UFL currently exist; most GHS health-hazard classes lack source-qualified material-specific records and remain UNKNOWN. None of those absences is a negative safety finding. Existing manufacturer/literature MQ source curves and fingerprints are research evidence, not physically calibrated Tank ppm or a radon/oxygen detector. Additional GHS/SDS/IARC imports must be CAS-matched, versioned, attributed, and reviewed; no inferred labels for entire families. Tank G3 remains 0/80.

## Migration and rollback
SQLite V13 upgrades V12 without rekeying `chemicals.uid`, rewriting sessions, deleting evidence, or replacing exposure source limits. The universal catalogue is a view. Unique source class/property/technique evidence indices avoid duplicates on repeated startup. For scientific claims beyond the included sample, source qualification is mandatory.
