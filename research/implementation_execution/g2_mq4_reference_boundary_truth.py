"""Replay SHA-pinned external MQ4 reference labels; never qualify Tank G2/G3.

A compact day receipt is replayed from the original ZIP locally. CI verifies
its pinned bytes and the existing forward-test receipt, but not the raw ZIP.
"""
import argparse
import csv
import hashlib
import io
import json
import math
import zipfile
from collections import defaultdict
from datetime import datetime
from decimal import Decimal, InvalidOperation
from pathlib import Path

ZIP_SHA = '7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9'
CSV_SHA = '3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d'
MEMBER = 'raw/MQ-ACQ-2026-06_umn_10ppm_data_csv.csv'
DAYS = [f'2021-07-{n:02d}' for n in range(21, 32)]
TEST = DAYS[4:]
BANDS = (Decimal('0.05'), Decimal('0.10'), Decimal('0.25'))
COLUMNS = ('datetime','licor_ch4','licor_h2o','t','rh','2600_1','2600_2',
           '2600_3','2602_1','2602_2','2602_3','2611C_1','2611C_2',
           '2611C_3','2611E_1','2611E_2','2611E_3','MQ4_1','MQ4_2','MQ4_3')
SCHEMA = 'hazrob.g2.mq4-reference-boundary-truth.v1'
RECEIPT_SHA = '23dec6b78cb40fe0f7883e436f544f4590caf1c4c9f6ced89a22592fd10fd872'
HERE = Path(__file__).resolve().parent
RECEIPT = HERE / 'G2_MQ4_REFERENCE_BOUNDARY_TRUTH_2026-11-02.json'
FORWARD = HERE / 'G2_MQ4_FORWARD_4PPM_2026-11-01.json'


def need(ok, code):
    if not ok:
        raise ValueError('MQ4_REFERENCE_BOUNDARY_' + code)


def strict_json(raw):
    def unique(pairs):
        out = {}
        for k, v in pairs:
            need(k not in out, 'DUPLICATE_KEY')
            out[k] = v
        return out
    def reject(_):
        raise ValueError('MQ4_REFERENCE_BOUNDARY_NONFINITE')
    def number(x):
        value = float(x)
        need(math.isfinite(value), 'NONFINITE')
        return value
    return json.loads(raw, object_pairs_hook=unique, parse_constant=reject,
                      parse_float=number)


def encode(obj):
    return (json.dumps(obj, ensure_ascii=False, sort_keys=True,
                       separators=(',', ':'), allow_nan=False)+'\n').encode()


def analyze_csv(raw, *, pinned=True):
    if pinned:
        need(hashlib.sha256(raw).hexdigest() == CSV_SHA, 'CSV_SHA')
    try:
        reader = csv.DictReader(io.StringIO(raw.decode('utf-8-sig'), newline=''),
                                strict=True)
        need(tuple(reader.fieldnames or ()) == COLUMNS, 'HEADER')
        days, previous = defaultdict(list), None
        for row in reader:
            need(None not in row and set(row) == set(COLUMNS) and
                 all(isinstance(v,str) and v.strip() for v in row.values()), 'CELLS')
            t = datetime.strptime(row['datetime'], '%Y-%m-%d %H:%M:%S')
            need(t.strftime('%Y-%m-%d %H:%M:%S') == row['datetime'] and
                 t.second == 0 and (previous is None or t > previous), 'TIME')
            previous = t
            v = Decimal(row['licor_ch4']) / Decimal(1000)
            need(v.is_finite() and v > 0, 'REFERENCE')
            days[t.date().isoformat()].append(v)
    except (UnicodeError, csv.Error, InvalidOperation, OverflowError) as exc:
        raise ValueError('MQ4_REFERENCE_BOUNDARY_CSV') from exc
    need(bool(days), 'EMPTY')
    result = {}
    for day, values in sorted(days.items()):
        pos = sum(v >= 4 for v in values)
        # Each [below, above] counts labels potentially changed by +/- delta.
        # Upper boundary is EXCLUSIVE: v == 4+delta stays positive after -delta.
        bands = [[sum(4-d <= v < 4 for v in values),
                  sum(4 <= v < 4+d for v in values)] for d in BANDS]
        result[day] = [len(values), pos, bands]
    return result


def build(archive):
    need(hashlib.sha256(archive).hexdigest() == ZIP_SHA, 'ZIP_SHA')
    try:
        with zipfile.ZipFile(io.BytesIO(archive)) as z:
            names = z.namelist()
            need(len(names) == len(set(names)) and names.count(MEMBER) == 1,
                 'ZIP_MEMBER')
            days = analyze_csv(z.read(MEMBER))
    except (zipfile.BadZipFile, RuntimeError) as exc:
        raise ValueError('MQ4_REFERENCE_BOUNDARY_ZIP') from exc
    need(list(days) == DAYS and sum(x[0] for x in days.values()) == 14186 and
         sum(x[1] for x in days.values()) == 2346 and
         sum(days[d][0] for d in TEST) == 9325 and
         sum(days[d][1] for d in TEST) == 1425, 'SOURCE_SCOPE')
    return {'schema':SCHEMA,'zip_sha256':ZIP_SHA,'csv_sha256':CSV_SHA,
            'source':'external_observational_minnesota_MQ4_not_tank',
            'threshold_ppm':'4.00','bands_ppm':[str(d) for d in BANDS],
            'day_rows_positive_boundary_pairs':days,'test_days':TEST,
            'source_rows':14186,'source_positives':2346,
            'test_rows':9325,'test_positives':1425,
            'g2_qualified':False,'g3_qualified':False,
            'traceable_calibration':False,'independent_tank_exposures':False,
            'threshold_is_safety_limit':False,
            'limitations':'External observational reference only. Bands are hypothetical +/- ppm perturbations, not verified LiCor uncertainty. Original ZIP replayed locally when producing receipt, not in CI. No Tank exposures, safety limits, calibration or G2/G3 approval.'}


def validate_payload(r, forward):
    need(type(r) is dict and type(forward) is dict and r.get('schema') == SCHEMA and
         r.get('zip_sha256') == forward.get('zip_sha256') == ZIP_SHA and
         r.get('csv_sha256') == forward.get('csv_sha256') == CSV_SHA and
         r.get('source') == 'external_observational_minnesota_MQ4_not_tank' and
         r.get('threshold_ppm') == '4.00' and
         r.get('bands_ppm') == [str(x) for x in BANDS], 'SCOPE')
    for flag in ('g2_qualified','g3_qualified','traceable_calibration',
                 'independent_tank_exposures','threshold_is_safety_limit'):
        need(r.get(flag) is False, 'NO_PROMOTION')
    need(forward.get('source_only_not_tank') is True and
         forward.get('g2_qualified') is False and
         forward.get('g3_qualified') is False, 'FORWARD_GUARD')
    days = r.get('day_rows_positive_boundary_pairs')
    need(type(days) is dict and list(days) == DAYS and
         r.get('test_days') == forward.get('test_days') == TEST and
         set(forward.get('test_rows_by_day',{})) == set(TEST) and
         set(forward.get('positive_by_test_day',{})) == set(TEST), 'DAY_SCOPE')
    for day in DAYS:
        x = days[day]
        need(type(x) is list and len(x) == 3 and
             type(x[0]) is int and type(x[1]) is int and
             x[0] > 0 and 0 <= x[1] <= x[0] and
             type(x[2]) is list and len(x[2]) == 3, 'DAY_SHAPE')
        prev_below = prev_above = -1
        for pair in x[2]:
            need(type(pair) is list and len(pair) == 2 and
                 all(type(v) is int for v in pair), 'BAND_TYPE')
            below, above = pair
            need(prev_below <= below <= x[0]-x[1] and
                 prev_above <= above <= x[1] and
                 below + above <= x[0], 'BAND_COUNTS')
            prev_below, prev_above = below, above
        if day in TEST:
            need(x[0] == forward['test_rows_by_day'][day] and
                 x[1] == forward['positive_by_test_day'][day], 'FORWARD_TRUTH')
    need(sum(x[0] for x in days.values()) == r.get('source_rows') == 14186 and
         sum(x[1] for x in days.values()) == r.get('source_positives') == 2346 and
         sum(days[d][0] for d in TEST) == r.get('test_rows') == 9325 and
         sum(days[d][1] for d in TEST) == r.get('test_positives') == 1425,
         'TOTALS')
    return {'source_rows':14186,'source_positives':2346,'test_rows':9325,
            'test_positives':1425,'receipt_sha256':RECEIPT_SHA,
            'zip_replayed_in_ci':False,'g2_qualified':False,'g3_qualified':False}


def audit():
    raw = RECEIPT.read_bytes()
    need(hashlib.sha256(raw).hexdigest() == RECEIPT_SHA, 'RECEIPT_SHA')
    return validate_payload(strict_json(raw), strict_json(FORWARD.read_bytes()))


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--archive', type=Path)
    p.add_argument('--output', type=Path)
    args = p.parse_args()
    data = encode(build(args.archive.read_bytes())) if args.archive else encode(audit())
    if args.output:
        args.output.write_bytes(data)
    else:
        print(data.decode(), end='')


if __name__ == '__main__':
    main()
