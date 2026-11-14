"""Reproduce descriptive day-level MQ4 raw signal diagnostics (external source only).

Reads the pinned Minnesota ZIP from the user Library. This is neither a
Tank calibration nor a G2/G3 validation. Stdlib-only, no fitted models.
"""
import argparse
import csv
import hashlib
import io
import json
import math
import zipfile
from collections import Counter, defaultdict
from datetime import datetime
from pathlib import Path

ARCHIVE_SHA256 = '7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9'
MEMBER_SHA256 = '3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d'
MEMBER = 'raw/MQ-ACQ-2026-06_umn_10ppm_data_csv.csv'
SENSORS = ('MQ4_1', 'MQ4_2', 'MQ4_3')
COLUMNS = ('datetime', 'licor_ch4', 'licor_h2o', 't', 'rh',
           '2600_1', '2600_2', '2600_3', '2602_1', '2602_2', '2602_3',
           '2611C_1', '2611C_2', '2611C_3', '2611E_1', '2611E_2',
           '2611E_3', *SENSORS)


def require(ok, code):
    if not ok:
        raise ValueError('MQ4_RAW_DAY_DIAGNOSTIC_' + code)


def pearson(x, y):
    require(len(x) == len(y) and len(x) > 1, 'CORRELATION_SUPPORT')
    mx, my = math.fsum(x)/len(x), math.fsum(y)/len(y)
    xx, yy = math.fsum((v-mx)**2 for v in x), math.fsum((v-my)**2 for v in y)
    require(xx > 0 and yy > 0, 'CONSTANT_SIGNAL')
    return math.fsum((a-mx)*(b-my) for a, b in zip(x, y))/math.sqrt(xx*yy)


def analyze_csv(raw):
    require(hashlib.sha256(raw).hexdigest() == MEMBER_SHA256, 'RAW_MEMBER_SHA256')
    reader = csv.DictReader(io.StringIO(raw.decode('utf-8-sig'), newline=''))
    require(tuple(reader.fieldnames or ()) == COLUMNS, 'HEADER')
    days = defaultdict(list)
    previous = None
    for row in reader:
        require(None not in row and all(v is not None and v.strip() for v in row.values()),
                'CSV_CELLS')
        timestamp = datetime.strptime(row['datetime'], '%Y-%m-%d %H:%M:%S')
        require(timestamp.second == 0 and (previous is None or timestamp > previous),
                'TIME_ORDER')
        previous = timestamp
        values = [float(row[k]) for k in COLUMNS[1:]]
        require(all(math.isfinite(v) for v in values), 'FINITE')
        days[timestamp.date().isoformat()].append((values[0]/1000,
                                                   *[values[COLUMNS.index(k)-1] for k in SENSORS]))
    require(len(days) == 11 and sum(map(len, days.values())) == 14186, 'SOURCE_SCOPE')
    out = {}
    for day, records in sorted(days.items()):
        ref = [x[0] for x in records]
        sensors = {}
        for i, name in enumerate(SENSORS, 1):
            values = [x[i] for x in records]
            counts = Counter(values)
            sensors[name] = {
                'distinct_raw_values': len(counts),
                'distinct_fraction': len(counts)/len(values),
                'largest_exact_value_count': max(counts.values()),
                'pearson_raw_vs_licor_ppm': pearson(values, ref),
            }
        out[day] = {'rows': len(records), 'reference_min_ppm': min(ref),
                    'reference_max_ppm': max(ref), 'sensors': sensors}
    summary = {}
    for name in SENSORS:
        signs = {day: item['sensors'][name]['pearson_raw_vs_licor_ppm']
                 for day, item in out.items()}
        summary[name] = {
            'days_positive_pearson': [d for d, v in signs.items() if v > 0],
            'days_negative_pearson': [d for d, v in signs.items() if v < 0],
            'min_day_pearson': min(signs.values()),
            'max_day_pearson': max(signs.values()),
            'min_day_distinct_fraction': min(x['sensors'][name]['distinct_fraction']
                                             for x in out.values()),
            'max_day_distinct_fraction': max(x['sensors'][name]['distinct_fraction']
                                             for x in out.values()),
        }
    return {'schema': 'hazrob.g2.mq4-raw-day-diagnostics.v1',
            'archive_sha256': ARCHIVE_SHA256, 'member_sha256': MEMBER_SHA256,
            'source': 'external_observational_minnesota_MQ4_not_tank',
            'rows': sum(x['rows'] for x in out.values()), 'calendar_days': len(out),
            'sensor_names': list(SENSORS), 'days': out, 'summary': summary,
            'g2_qualified': False, 'g3_qualified': False,
            'independent_tank_exposures': False, 'traceable_calibration': False,
            'blinded_prospective': False,
            'limitations': ('Raw MQ4 signal and LiCor associations only; raw sensor units '
                            'not asserted to be ohms. Within-day Pearson correlations '
                            'can change sign due to time, environment, sensor response '
                            'or confounding; not causal or independent validation. '
                            'Exact-value cardinality does not prove ADC resolution. '
                            'No gas identity, ppm calibration, safety threshold, '
                            'confidence interval or G2/G3 qualification.')}


def analyze_archive(blob):
    require(hashlib.sha256(blob).hexdigest() == ARCHIVE_SHA256, 'ARCHIVE_SHA256')
    with zipfile.ZipFile(io.BytesIO(blob)) as z:
        names = z.namelist()
        require(len(names) == len(set(names)) and names.count(MEMBER) == 1,
                'ZIP_MEMBERS')
        return analyze_csv(z.read(MEMBER))



def compact(report):
    """Stable 5-decimal source receipt projection; never a scientific promotion."""
    fields = ('schema', 'archive_sha256', 'member_sha256', 'source', 'rows',
              'calendar_days', 'sensor_names')
    out = {k: report[k] for k in fields}
    out['daily_compact'] = {
        day: {'n': item['rows'],
              'ref_min_ppm': round(item['reference_min_ppm'], 4),
              'ref_max_ppm': round(item['reference_max_ppm'], 4),
              'mq4': {name: [sensor['distinct_raw_values'],
                              round(sensor['pearson_raw_vs_licor_ppm'], 5)]
                      for name, sensor in item['sensors'].items()}}
        for day, item in report['days'].items()}
    out['summary'] = {
        name: {key: round(value, 5) if isinstance(value, float) else value
               for key, value in entry.items()}
        for name, entry in report['summary'].items()}
    for k in ('g2_qualified', 'g3_qualified', 'independent_tank_exposures',
              'traceable_calibration', 'blinded_prospective', 'limitations'):
        out[k] = report[k]
    return out

def main():
    p = argparse.ArgumentParser()
    p.add_argument('--archive', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--compact', action='store_true', help='emit committed compact receipt')
    a = p.parse_args()
    report = analyze_archive(a.archive.read_bytes())
    if a.compact:
        report = compact(report)
    a.output.write_text(json.dumps(report, ensure_ascii=False, sort_keys=True,
                                   indent=2, allow_nan=False) + '\n', encoding='utf-8')
    print('Pinned raw MQ4 day diagnostics PASS; external source only; no G2/G3')


if __name__ == '__main__':
    main()
