"""Fail on duplicate technique accounting or promotion without Tank evidence."""
import json
from pathlib import Path

def verify(root):
    p=json.loads((root/'PROGRESS_80.json').read_text())
    q=json.loads((root/'NEXT_ACTIONS_80.json').read_text())
    ids=[t['id'] for t in p['techniques']]
    assert len(ids)==len(set(ids))==80
    e=p['external_reference_progress']
    groups=[e['technique_ids'],e['blocked_techniques'],e['analytical_only_techniques']]
    flat=[x for g in groups for x in g]
    assert len(flat)==len(set(flat))==80 and set(flat)==set(ids)
    assert [len(g) for g in groups]==[49,29,2]
    assert e['reviewed_G2_techniques']==80
    assert e['techniques_with_first_real_external_array_evidence']==len(groups[0])
    assert e['reviewed_total_breakdown']==dict(external_numeric_or_metadata=49,scientifically_blocked=29,analytical_only=2)
    assert all(t['empirical_gate']=='NOT_VALIDATED' for t in p['techniques'])
    assert e['scientific_Tank_G3_accepted_count']==0
    assert e['unique_exposures_across_all_G2_batches']==sum(e['unique_measured_exposure_records_by_source'].values())==14238
    assert e['total_independent_source_collections']==len(e['unique_measured_exposure_records_by_source'])==4
    tasks=q['tasks'];assert len(tasks)==80 and {t['id'] for t in tasks}==set(ids)
    assert all(sum(t['next_batch']==b for t in tasks)==10 for b in range(1,9))
    assert all(t['state']=='PLANNED_NOT_IMPLEMENTED_BY_THIS_PLAN' for t in tasks)
    for n in (7,8):
        fn='uci1081_g2_batch07_2026-10-20.json' if n==7 else 'g2_batch08_2026-10-20.json'
        r=json.loads((root/'benchmarks/results'/fn).read_text())
        assert len(set(r['reviewed_ids']))==10
        assert set(r['external_numeric_ids'])|set(r['blocked'])==set(r['reviewed_ids'])
        assert not set(r['external_numeric_ids'])&set(r['blocked'])
        assert r['Tank_G3_accepted']==0
        for id in r['reviewed_ids']:
            t=next(t for t in p['techniques'] if t['id']==id)
            assert t['g2_review_batch']==n and t['g2_review_report']==f'research/G2_REAL_DATA_BATCH{n:02d}_AR.md'
    print('PASS: 80 unique G2 reviews; 49 numeric,29 blocked,2 theoretical; 80 next tasks; Tank G3=0; 14238 unique exposures.')
if __name__=='__main__':verify(Path(__file__).resolve().parents[1])
