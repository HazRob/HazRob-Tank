# EXCLUSIVE SCOPE: HazRob Tank ChemCore
Never resume this roadmap in HazRob-Lab or the original HazRob repository. Current owner: HazRob/HazRob-Tank. Historical evidence links in HazRob-Lab are references only, not Tank implementation.

# ChemCore — resume checkpoint
Date 2026-10-18 | Roadmap 1.0 | Repo HazRob/HazRob-Tank

## NEXT
01.1 ADC-to-Rs conversion audit, status NOT_STARTED under the new roadmap. Legacy results exist but have not been independently audited for these gates.

## Exact next actions
1. Read research/MASTER_PLAN.md and research/STATUS_LEDGER.md.
2. Inspect latest connected firmware and ChemCore measurement code for ADS1115 and ESP32 ADC scaling, RL, R0, reference voltages, attenuations, divider topology, units, clipping.
3. Verify real MQ board wiring and sensor-specific heater modes; derive and test Rs equations.
4. Create dated experiment log, code tests, decision, update ledger and this checkpoint with last commit and unresolved issues.

## Legacy pointers
references/tech12_nested_subset_validation_2026-10-18.md; references/tech13_evidence_fusion_prototype_2026-10-18.md; references/tech14_real_data_generalization_audit_2026-10-18.md; references/extracted_analytical_equations_batch01_2026-10-18.md.

## Restrictions
No production ChemCore/APK/firmware modifications. No claim of empirical validation from unit tests alone. Do not merge HazRob Tank with original HazRob.

## Prompt for a new conversation
استأنف خارطة ChemCore البحثية من HazRob/HazRob-Tank. اقرأ research/RESUME.md ثم research/MASTER_PLAN.md وresearch/STATUS_LEDGER.md. نفذ البند التالي بعد مراجعة الأدلة السابقة، ووثق الشيفرة والاختبارات والقرار وحدّث نقطة الاستئناف. لا تعدل الإصدارات الإنتاجية.
