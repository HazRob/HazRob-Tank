#!/usr/bin/env python3
"""ChemCore Tank V6: deterministic, research-only source ingestion and simulation.

This module performs algebraic validation of pinned upstream curves, R0-reference
contracts and selected fingerprint spaces. It does not fit or certify a gas detector.
No third-party packages, network access, Android dependencies or real gas standards.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from dataclasses import dataclass
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent
SNAPSHOTS = ROOT / "source_snapshots" / "hr069"
SOURCE_MANIFEST = ROOT / "source_manifest.json"
SENSORS = ("MQ2", "MQ3", "MQ4", "MQ5", "MQ7", "MQ8", "MQ9", "MQ135")
CLEAN_AIR_FACTORS = {"MQ2": 9.83, "MQ3": 60.0, "MQ4": 4.4,
                     "MQ5": 6.5, "MQ7": 27.5, "MQ8": 70.0,
                     "MQ9": 9.6, "MQ135": 3.6}
# Example clean-air ratios from MQUnifiedsensor, not manufacturer-certified
# calibrations of the eight physical Tank modules.
R0_FROM_CLEAN_AIR = "R0_CLEAN_AIR_RATIO"
R0_FROM_REFERENCE_GAS = "R0_REFERENCE_GAS_AT_PPM"
RS_AIR_DIRECT = "RS_AIR_DIRECT_BASELINE"
RATIO_FORWARD = "RS_OVER_R0"
RATIO_INVERSE = "R0_OVER_RS"


class EvidenceError(ValueError):
    """Invalid chemistry, signal or non-interchangeable reference assumptions."""


def require_positive_finite(value: Any, what: str) -> float:
    if isinstance(value, bool):
        raise EvidenceError(f"{what}: boolean is not a measurement")
    try:
        x = float(value)
    except (TypeError, ValueError, OverflowError) as exc:
        raise EvidenceError(f"{what}: not numeric") from exc
    if not math.isfinite(x) or x <= 0:
        raise EvidenceError(f"{what}: must be positive and finite")
    return x


def git_blob_sha1(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\x00" + data).hexdigest()


def verify_source_snapshot(manifest_path: Path = SOURCE_MANIFEST,
                           root: Path = SNAPSHOTS) -> dict[str, Any]:
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    rows = manifest["research_sources"]["copies"]
    if len(rows) != 14 or len({r["filename"] for r in rows}) != len(rows):
        raise EvidenceError("expected fourteen unique canonical source files")
    names = {f.name for f in root.iterdir() if f.is_file()}
    expected = {row["filename"] for row in rows}
    if names != expected:
        raise EvidenceError(f"source-set mismatch: missing={sorted(expected-names)} extra={sorted(names-expected)}")
    results = []
    for row in rows:
        name = row["filename"]
        if "/" in name or "\\" in name or name.startswith("."):
            raise EvidenceError("unsafe source filename")
        digest = git_blob_sha1((root / name).read_bytes())
        if digest != row["git_blob_sha1"]:
            raise EvidenceError(f"modified upstream source blob: {name}")
        results.append({"filename": name, "git_blob_sha1": digest, "verified": True})
    return {"expected": len(rows), "verified": len(results), "files": results}


def resistance_ohm(rl_ohm: float, vc_v: float, vrl_v: float) -> float:
    rl = require_positive_finite(rl_ohm, "RL ohm")
    vc = require_positive_finite(vc_v, "supply V")
    vrl = require_positive_finite(vrl_v, "load voltage V")
    if vrl >= vc:
        raise EvidenceError("ADC load voltage at or above supply")
    rs = rl * (vc / vrl - 1.0)
    return require_positive_finite(rs, "derived Rs ohm")


def r0_ohm_from_clean_air(rs_air_ohm: float, sensor: str,
                          clean_air_factor: float | None = None) -> float:
    if sensor not in CLEAN_AIR_FACTORS:
        raise EvidenceError("sensor is not one of the eight Tank MQ models")
    factor = CLEAN_AIR_FACTORS[sensor] if clean_air_factor is None else clean_air_factor
    return require_positive_finite(rs_air_ohm, "Rs air ohm") / require_positive_finite(factor, "clean air ratio")


def ratio_rs_r0(rs_ohm: float, r0_ohm: float,
                direction: str = RATIO_FORWARD) -> float:
    ratio = require_positive_finite(rs_ohm, "Rs ohm") / require_positive_finite(r0_ohm, "R0 ohm")
    if direction == RATIO_FORWARD:
        return ratio
    if direction == RATIO_INVERSE:
        return 1.0 / ratio
    raise EvidenceError("unknown ratio direction")


def normalized_ratio_for_source(rs_ohm: float, r0_ohm: float,
                                acquisition_r0_definition: str,
                                curve_r0_definition: str,
                                acquisition_ratio_direction: str = RATIO_FORWARD,
                                curve_ratio_direction: str = RATIO_FORWARD) -> float:
    """Fail closed on incompatible R0 definitions. Do not guess a conversion."""
    if not acquisition_r0_definition or not curve_r0_definition:
        raise EvidenceError("R0 definition absent")
    if acquisition_r0_definition not in (R0_FROM_CLEAN_AIR, R0_FROM_REFERENCE_GAS, RS_AIR_DIRECT):
        raise EvidenceError("unknown R0 definition")
    if acquisition_r0_definition != curve_r0_definition:
        raise EvidenceError("INCOMPATIBLE_R0_REFERENCE")
    observed = ratio_rs_r0(rs_ohm, r0_ohm, acquisition_ratio_direction)
    if curve_ratio_direction == acquisition_ratio_direction:
        return observed
    if curve_ratio_direction not in (RATIO_FORWARD, RATIO_INVERSE):
        raise EvidenceError("invalid curve ratio direction")
    return 1.0 / observed


@dataclass(frozen=True)
class Curve:
    index: int
    sensor: str
    gas_label: str
    chemical_legacy_id: int
    a: float
    b: float
    ppm_min: float
    ppm_max: float
    output_scale: float
    source_id: str
    source_git_blob_sha: str
    research_only: bool = True

    def __post_init__(self) -> None:
        if self.sensor not in SENSORS or not self.gas_label:
            raise EvidenceError("invalid sensor/gas curve")
        for key in ("a", "ppm_min", "ppm_max", "output_scale"):
            require_positive_finite(getattr(self, key), key)
        if not math.isfinite(self.b) or abs(self.b) < 1e-12 or self.ppm_max <= self.ppm_min:
            raise EvidenceError("curve slope/domain invalid")

    def forward(self, ppm: float) -> float:
        ppm = require_positive_finite(ppm, "ppm")
        if not self.ppm_min <= ppm <= self.ppm_max:
            raise EvidenceError("PPM_OUTSIDE_SOURCE_DOMAIN")
        ratio = self.a * (ppm / self.output_scale) ** self.b
        return require_positive_finite(ratio, "curve Rs/R0")

    def inverse(self, ratio: float) -> float:
        ratio = require_positive_finite(ratio, "curve Rs/R0")
        try:
            ppm = self.output_scale * (ratio / self.a) ** (1.0 / self.b)
        except (OverflowError, ZeroDivisionError) as exc:
            raise EvidenceError("curve inverse overflow") from exc
        ppm = require_positive_finite(ppm, "inverse ppm")
        if not self.ppm_min * (1 - 1e-10) <= ppm <= self.ppm_max * (1 + 1e-10):
            raise EvidenceError("PPM_OUTSIDE_SOURCE_DOMAIN")
        return ppm


def load_curves(root: Path = SNAPSHOTS) -> list[Curve]:
    raw = (root / "mq_research_curves_43.json").read_bytes()
    data = json.loads(raw)
    if data["curve_count"] != 43 or len(data["curves"]) != 43 or data.get("m4") is not False:
        raise EvidenceError("source curve count or research-only status changed")
    source = data["source"]["id"]
    if data["source"]["commit"] != "e88f876140587a708ee84a4d4d3acbd5720274ee":
        raise EvidenceError("MQDataScience source not pinned")
    sha = git_blob_sha1(raw)
    return [Curve(index=i+1, sensor=r["sensor"], gas_label=r["gas_label"],
                  chemical_legacy_id=int(r["legacy_id"]), a=float(r["ratio_a"]),
                  b=float(r["ratio_b"]), ppm_min=float(r["min_ppm"]),
                  ppm_max=float(r["max_ppm"]), output_scale=float(r.get("output_scale", 1)),
                  source_id=source, source_git_blob_sha=sha)
            for i, r in enumerate(data["curves"])]


def logspaced_domain(curve: Curve, n: int = 9) -> list[float]:
    if n < 2:
        raise EvidenceError("at least two grid points required")
    # Bound the floating-point logspace endpoints exactly to the source domain.
    # Otherwise 10**log10(max) may round a few ULP above max and fail closed.
    lo, hi = math.log10(curve.ppm_min), math.log10(curve.ppm_max)
    grid = [min(curve.ppm_max, max(curve.ppm_min,
            10 ** (lo + (hi - lo) * j / (n-1)))) for j in range(n)]
    grid[0], grid[-1] = curve.ppm_min, curve.ppm_max
    return grid


def validate_curve_roundtrips(curves: list[Curve]) -> dict[str, Any]:
    worst = 0.0
    tests = 0
    for curve in curves:
        for ppm in logspaced_domain(curve):
            back = curve.inverse(curve.forward(ppm))
            error = abs(back - ppm) / ppm
            worst = max(worst, error)
            tests += 1
            if error > 1e-9:
                raise EvidenceError(f"roundtrip failed for curve #{curve.index}")
    return {"source_curves": len(curves), "points_tested": tests,
            "max_relative_roundtrip_error": worst,
            "numerical_self_consistency_only": True,
            "independent_ppm_accuracy_established": False}


def r0_sensitivity_simulation(curves: list[Curve],
                              multipliers: tuple[float, ...] = (0.5, 1.0, 2.0, 10.0)) -> list[dict[str, Any]]:
    """Synthetic: same observed Rs divided by alternative hypothetical R0.
    This is not an empirical evaluation of any physical Tank sensor.
    """
    result = []
    for curve in curves:
        central_ppm = math.sqrt(curve.ppm_min * curve.ppm_max)
        baseline_ratio = curve.forward(central_ppm)
        for mult in multipliers:
            mult = require_positive_finite(mult, "R0 multiplier")
            altered_ratio = baseline_ratio / mult
            try:
                fitted_ppm = curve.inverse(altered_ratio)
                status = "IN_DOMAIN"
                fold = fitted_ppm / central_ppm
            except EvidenceError as exc:
                fitted_ppm = None
                fold = None
                status = str(exc)
            result.append({"curve_id": curve.index, "sensor": curve.sensor,
                           "source_label": curve.gas_label, "baseline_ppm": central_ppm,
                           "assumed_R0_multiplier": mult, "fitted_ppm": fitted_ppm,
                           "ppm_fold": fold, "state": status,
                           "truth_kind": "SYNTHETIC_SAME_SOURCE_COUNTERFACTUAL",
                           "device_validated": False})
    return result


def multi_class_fingerprint_audit(root: Path = SNAPSHOTS) -> dict[str, Any]:
    obj = json.loads((root / "multimodal_mq_transfer_fingerprints_v1.json").read_text())
    ordered = obj["hr069_sensor_order"]
    if len(ordered) != 8 or set(ordered) != set(SENSORS):
        raise EvidenceError("fingerprint channel schema mismatch")
    shared = set(obj["overlap_sensors"])
    missing = set(obj["missing_hr069_channels"])
    if shared & missing or shared | missing != set(SENSORS):
        raise EvidenceError("overlap and unavailable source channels inconsistent")
    if obj["total_samples"] != 6400 or len(obj["records"]) != 4:
        raise EvidenceError("unexpected source class record counts")
    classes = []
    for rec in obj["records"]:
        vals = rec["log10_cross_sensor_geomean_shape_hr069_order"]
        if len(vals) != 8:
            raise EvidenceError("wrong fingerprint vector dimensions")
        for sensor, val in zip(ordered, vals):
            if sensor in missing and val is not None:
                raise EvidenceError("fabricated value on missing source channel")
            if sensor in shared and (val is None or not math.isfinite(float(val))):
                raise EvidenceError("invalid observed overlapping channel")
        classes.append(rec["class"])
    if set(classes) != {"Mixture", "NoGas", "Perfume", "Smoke"}:
        raise EvidenceError("not the original source classes")
    return {"source": obj["source_id"], "class_count": len(classes),
            "source_adc_rows": obj["total_samples"], "physical_exposure_count": None,
            "overlap_channels": sorted(shared), "missing_channels": sorted(missing),
            "classes": sorted(classes), "pure_gas_identification": False,
            "device_validated": False}


def audit(root: Path = SNAPSHOTS, manifest: Path = SOURCE_MANIFEST) -> dict[str, Any]:
    snapshot = verify_source_snapshot(manifest, root)
    curves = load_curves(root)
    roundtrips = validate_curve_roundtrips(curves)
    fingerprints = multi_class_fingerprint_audit(root)
    groups = {sensor: sum(c.sensor == sensor for c in curves) for sensor in SENSORS}
    scaled = [{"curve_id": c.index, "sensor": c.sensor, "label": c.gas_label,
               "output_scale": c.output_scale} for c in curves if c.output_scale != 1]
    fp = json.loads((root / "identification_fingerprint_priors_v1.json").read_text())
    food = json.loads((root / "food_freshness_headspace_fingerprints_v1.json").read_text())
    uci = json.loads((root / "uci_mq135_lowconcentration_transfer_fingerprints_v1.json").read_text())
    if fp["total_numeric_fingerprint_families"] != 13:
        raise EvidenceError("fingerprint family count changed")
    if len(food["sessions"]) != 59 or food.get("quantitative_ppm") is not False:
        raise EvidenceError("headspace semantics changed")
    if len(uci["records"]) != 18 or uci["overlap_count"] != 1:
        raise EvidenceError("UCI transfer source semantics changed")
    sensitivity = r0_sensitivity_simulation(curves)
    return {
        "schema": "chemcore-v6-source-audit/1",
        "mode": "SOURCE_VERIFIED_NUMERICAL_SIMULATION_ONLY",
        "source_integrity": {"expected": snapshot["expected"], "verified": snapshot["verified"]},
        "curve_roundtrips": roundtrips,
        "per_sensor_curve_counts": groups,
        "nonunit_output_scales": scaled,
        "fingerprint_class_audit": fingerprints,
        "derived_source_fingerprint_families": fp["total_numeric_fingerprint_families"],
        "food_headspace_session_count": len(food["sessions"]),
        "UCI1081_derived_mq135_gas_level_records": len(uci["records"]),
        "synthetic_R0_counterfactual_cases": len(sensitivity),
        "synthetic_R0_out_of_domain": sum(e["state"] != "IN_DOMAIN" for e in sensitivity),
        "no_real_Tank_gas_data_used": True,
        "M4_or_safety_certification": False,
    }


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--output", type=Path, default=None)
    ap.add_argument("--counterfactual-output", type=Path, default=None)
    args = ap.parse_args()
    result = audit()
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2, sort_keys=True, ensure_ascii=False)+"\n")
    if args.counterfactual_output:
        args.counterfactual_output.parent.mkdir(parents=True, exist_ok=True)
        cases = r0_sensitivity_simulation(load_curves())
        args.counterfactual_output.write_text(json.dumps(cases, indent=2, sort_keys=True)+"\n")
    print(json.dumps(result, indent=2, sort_keys=True, ensure_ascii=False))


if __name__ == "__main__":
    main()
