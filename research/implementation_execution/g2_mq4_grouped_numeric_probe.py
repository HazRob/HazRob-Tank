"""Research-only MQ4 raw-channel probe; grouped calendar-day holdouts, no G2/G3 promotion.

Only standard-library operations. The source ZIP is external and SHA256 pinned.
No calibration, physical exposure independence or blinded holdout is inferred.
"""
import argparse
import csv
import hashlib
import io
import json
import math
import statistics
import zipfile
from collections import defaultdict
from datetime import datetime
from pathlib import Path

MEMBER = "raw/MQ-ACQ-2026-06_umn_10ppm_data_csv.csv"
ARCHIVE_SHA256 = "7112a370c7b21445f9e6ebb6d50e08059c352290ee6da23b0b735f5a9583afc9"
MEMBER_SHA256 = "3c27c4f8b8e8aab54b6433acfa41de00890a9eefd19376259a2371f22a48ff2d"
CHANNELS = ("MQ4_1", "MQ4_2", "MQ4_3")


def require(ok, code):
    if not ok:
        raise ValueError("MQ4_GROUPED_PROBE_" + code)


def pearson(xs, ys):
    mx, my = statistics.mean(xs), statistics.mean(ys)
    xx = sum((x - mx)**2 for x in xs)
    yy = sum((y - my)**2 for y in ys)
    return sum((x - mx)*(y - my) for x, y in zip(xs, ys)) / math.sqrt(xx*yy) if xx*yy else None


def probe_csv(raw, expected_member_sha=None):
    if expected_member_sha is not None:
        require(hashlib.sha256(raw).hexdigest() == expected_member_sha, "MEMBER_SHA256")
    try:
        reader = csv.DictReader(io.StringIO(raw.decode("utf-8-sig"), newline=""))
        require(reader.fieldnames is not None and len(set(reader.fieldnames)) == len(reader.fieldnames)
                and all(c in reader.fieldnames for c in ("datetime", "licor_ch4", *CHANNELS)), "COLUMNS")
        grouped = defaultdict(list)
        previous = None
        for row in reader:
            require(None not in row and all(v is not None for v in row.values()), "CSV_SHAPE")
            ts = datetime.strptime(row["datetime"], "%Y-%m-%d %H:%M:%S")
            require(ts.second == 0 and (previous is None or ts > previous), "TIME_ORDER")
            previous = ts
            vals = [float(row[c]) for c in ("licor_ch4", *CHANNELS)]
            require(all(math.isfinite(v) for v in vals), "FINITE")
            grouped[ts.date().isoformat()].append(vals)
    except (UnicodeError, csv.Error, TypeError, OverflowError, KeyError) as ex:
        raise ValueError("MQ4_GROUPED_PROBE_INVALID_CSV") from ex
    require(len(grouped) >= 3 and all(len(rows) >= 3 for rows in grouped.values()), "GROUPS")
    days = sorted(grouped)
    report = {"schema": "hazrob.g2.mq4-grouped-numeric-probe.v1",
              "raw_member_sha256": hashlib.sha256(raw).hexdigest(),
              "rows": sum(map(len, grouped.values())), "days": len(days),
              "day_row_counts": {day: len(grouped[day]) for day in days},
              "split": "leave-one-calendar-day-out; fit OLS on other days only",
              "channels": {}, "g2_qualified": False, "g3_tank_qualified": False,
              "physical_independence_verified": False, "reference_calibration_verified": False,
              "blinded_holdout_verified": False,
              "limitations": "Source-reported methane and raw sensor values only; days are not independently verified physical exposures. No Tank validation, gas identity or calibrated ppm certification."}
    for col, channel in enumerate(CHANNELS, 1):
        allrows = [r for day in days for r in grouped[day]]
        overall_r = pearson([r[col] for r in allrows], [r[0] for r in allrows])
        folds = {}
        squared_model = squared_baseline = 0.0
        for day in days:
            train = [r for d in days if d != day for r in grouped[d]]
            test = grouped[day]
            xs, ys = [r[col] for r in train], [r[0] for r in train]
            mx, my = statistics.mean(xs), statistics.mean(ys)
            sxx = sum((x-mx)**2 for x in xs)
            slope = sum((x-mx)*(y-my) for x, y in zip(xs, ys)) / sxx if sxx else 0.0
            model_sse = sum((r[0] - (my + slope*(r[col]-mx)))**2 for r in test)
            base_sse = sum((r[0] - my)**2 for r in test)
            squared_model += model_sse
            squared_baseline += base_sse
            folds[day] = {"train_rows": len(train), "test_rows": len(test),
                          "within_day_pearson": pearson([r[col] for r in test], [r[0] for r in test]),
                          "rmse_model_ppb": math.sqrt(model_sse/len(test)),
                          "rmse_train_mean_baseline_ppb": math.sqrt(base_sse/len(test))}
        n = len(allrows)
        report["channels"][channel] = {
            "overall_pearson": overall_r,
            "positive_day_correlations": sum(v["within_day_pearson"] is not None and v["within_day_pearson"] > 0 for v in folds.values()),
            "negative_day_correlations": sum(v["within_day_pearson"] is not None and v["within_day_pearson"] < 0 for v in folds.values()),
            "lodo_rmse_ppb": math.sqrt(squared_model/n),
            "lodo_train_mean_baseline_rmse_ppb": math.sqrt(squared_baseline/n),
            "lodo_skill_vs_train_mean": 1-squared_model/squared_baseline if squared_baseline else None,
            "folds": folds}
    return report


def probe_zip(archive, expected_archive_sha=ARCHIVE_SHA256, expected_member_sha=MEMBER_SHA256):
    require(hashlib.sha256(archive).hexdigest() == expected_archive_sha, "ARCHIVE_SHA256")
    with zipfile.ZipFile(io.BytesIO(archive)) as z:
        require(z.namelist().count(MEMBER) == 1, "ZIP_MEMBER")
        raw = z.read(MEMBER)
    return probe_csv(raw, expected_member_sha)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--archive", type=Path, required=True)
    p.add_argument("--output", type=Path, required=True)
    a = p.parse_args()
    result = probe_zip(a.archive.read_bytes())
    a.output.write_text(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2) + "\n", encoding="utf-8")
    print("MQ4 grouped numeric research probe completed; G2/G3 NOT QUALIFIED")


if __name__ == "__main__":
    main()
