"""Deterministic synthetic source-parser tests; never substitute for raw exposures."""
import json
import tempfile
import unittest
from pathlib import Path
from g2_mq_ext10_ethanol_preflight import (
    inspect_file, parse_ethanol_filename, parse_numeric_rows, run,
)


class MQExt10EthanolPreflightTests(unittest.TestCase):
    def test_source_only_status(self):
        r = run()
        self.assertEqual(r["source_id"], "MQ-EXT-10")
        self.assertEqual(r["files_scanned"], 0)
        self.assertFalse(r["g2_qualified"])
        self.assertFalse(r["g3_qualified"])
        self.assertFalse(r["headspace_ethanol_ppm_measured"])
        self.assertFalse(r["liquid_percent_vv_is_gas_ppm"])

    def test_saved_receipt_byte_replay(self):
        path = Path(__file__).resolve().parent / "G2_MQ_EXT10_ETHANOL_PREFLIGHT_2026-11-02.json"
        encoded = json.dumps(run(), ensure_ascii=False, sort_keys=True,
                             separators=(",", ":"), allow_nan=False) + "\n"
        self.assertEqual(path.read_bytes(), encoded.encode("utf-8"))

    def test_filename_concentration_codes(self):
        self.assertEqual(parse_ethanol_filename("Ea-C1_R01"), ("C1", 1))
        self.assertEqual(parse_ethanol_filename("EaC6R010"), ("C6", 10))

    def test_reject_unknown_concentration(self):
        with self.assertRaisesRegex(ValueError, "INVALID_FILENAME"):
            parse_ethanol_filename("Ea-C7_R01")

    def test_reject_unverified_gas_ppm_in_filename(self):
        with self.assertRaisesRegex(ValueError, "INVALID_FILENAME"):
            parse_ethanol_filename("Ea-10000ppm_R01")

    def test_valid_eight_channel_rows(self):
        d = b"50,25,1,2,3,4,5,6\n51,26,2,3,4,5,6,7\n"
        self.assertEqual(parse_numeric_rows(d, expected_rows=2)["rows"], 2)

    def test_whitespace_and_semicolon(self):
        self.assertEqual(parse_numeric_rows(b"50 25 1 2 3 4 5 6", expected_rows=1)["rows"], 1)
        self.assertEqual(parse_numeric_rows(b"50;25;1;2;3;4;5;6", expected_rows=1)["rows"], 1)

    def test_reject_negative_relative_humidity(self):
        with self.assertRaisesRegex(ValueError, "RELATIVE_HUMIDITY_OUT_OF_RANGE"):
            parse_numeric_rows(b"-0.1,25,1,2,3,4,5,6\n", expected_rows=1)

    def test_reject_relative_humidity_above_100_percent(self):
        with self.assertRaisesRegex(ValueError, "RELATIVE_HUMIDITY_OUT_OF_RANGE"):
            parse_numeric_rows(b"100.1,25,1,2,3,4,5,6\n", expected_rows=1)

    def test_accept_relative_humidity_physical_boundaries(self):
        data = b"0,25,1,2,3,4,5,6\n100,25,1,2,3,4,5,6\n"
        self.assertEqual(parse_numeric_rows(data, expected_rows=2)["rows"], 2)

    def test_reject_temperature_below_absolute_zero(self):
        with self.assertRaisesRegex(ValueError, "TEMPERATURE_BELOW_ABSOLUTE_ZERO"):
            parse_numeric_rows(b"50,-273.16,1,2,3,4,5,6\n", expected_rows=1)

    def test_accept_temperature_at_absolute_zero_boundary(self):
        self.assertEqual(parse_numeric_rows(
            b"50,-273.15,1,2,3,4,5,6\n", expected_rows=1)["rows"], 1)

    def test_reject_delimiter_switch_between_rows(self):
        data = b"50,25,1,2,3,4,5,6\n51;26;2;3;4;5;6;7\n"
        with self.assertRaisesRegex(ValueError, "INCONSISTENT_DELIMITER"):
            parse_numeric_rows(data, expected_rows=2)

    def test_reject_mixed_delimiters_within_row(self):
        with self.assertRaisesRegex(ValueError, "MIXED_DELIMITERS_IN_ROW"):
            parse_numeric_rows(b"50,25;1,2,3,4,5,6\n", expected_rows=1)

    def test_accept_consistent_semicolon_rows(self):
        data = b"50;25;1;2;3;4;5;6\n51;26;2;3;4;5;6;7\n"
        self.assertEqual(parse_numeric_rows(data, expected_rows=2)["rows"], 2)

    def test_reject_rh_above_100_rounded_to_100_in_float(self):
        with self.assertRaisesRegex(ValueError, "RELATIVE_HUMIDITY_OUT_OF_RANGE"):
            parse_numeric_rows(
                b"100.0000000000000000000000000000001,25,1,2,3,4,5,6\n",
                expected_rows=1,
            )

    def test_reject_tiny_negative_rh_rounded_to_negative_zero(self):
        with self.assertRaisesRegex(ValueError, "RELATIVE_HUMIDITY_OUT_OF_RANGE"):
            parse_numeric_rows(b"-1e-99999,25,1,2,3,4,5,6\n", expected_rows=1)

    def test_reject_temperature_just_below_absolute_zero_rounded_up(self):
        with self.assertRaisesRegex(ValueError, "TEMPERATURE_BELOW_ABSOLUTE_ZERO"):
            parse_numeric_rows(
                b"50,-273.1500000000000000000000000000001,1,2,3,4,5,6\n",
                expected_rows=1,
            )

    def test_reject_positive_resistance_float_underflow_explicitly(self):
        with self.assertRaisesRegex(ValueError, "POSITIVE_RESISTANCE_FLOAT_UNDERFLOW"):
            parse_numeric_rows(b"50,25,1e-99999,2,3,4,5,6\n", expected_rows=1)

    def test_accept_high_precision_rh_just_below_100(self):
        self.assertEqual(parse_numeric_rows(
            b"99.9999999999999999999999999999999,25,1,2,3,4,5,6\n",
            expected_rows=1,
        )["rows"], 1)

    def test_accept_small_positive_resistance_representable_in_float(self):
        self.assertEqual(parse_numeric_rows(
            b"50,25,1e-300,2,3,4,5,6\n", expected_rows=1,
        )["rows"], 1)

    def test_reject_nonfinite(self):
        with self.assertRaisesRegex(ValueError, "NONFINITE_ROW"):
            parse_numeric_rows(b"50,25,1,2,3,4,5,nan", expected_rows=1)

    def test_reject_nonpositive_resistance(self):
        with self.assertRaisesRegex(ValueError, "NONPOSITIVE_RESISTANCE"):
            parse_numeric_rows(b"50,25,1,2,0,4,5,6", expected_rows=1)

    def test_reject_wrong_column_count(self):
        with self.assertRaisesRegex(ValueError, "COLUMN_COUNT_NOT_EIGHT"):
            parse_numeric_rows(b"50,25,1,2,3,4,5", expected_rows=1)

    def test_reject_unexpected_row_count(self):
        with self.assertRaisesRegex(ValueError, "ROW_COUNT_MISMATCH"):
            parse_numeric_rows(b"50,25,1,2,3,4,5,6", expected_rows=2)

    def test_reject_header_without_verified_format(self):
        with self.assertRaisesRegex(ValueError, "NONNUMERIC_ROW"):
            parse_numeric_rows(b"RH,T,MQ3,MQ4,MQ6,MQ3,MQ4,MQ6", expected_rows=1)

    def test_synthetic_file_provenance(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "Ea-C1_R01.txt"
            p.write_text("50,25,1,2,3,4,5,6\n" * 3330)
            row = inspect_file(p)
            self.assertEqual(row["rows"], 3330)
            self.assertEqual(row["nominal_liquid_ethanol_percent_vv"], "1")
            self.assertEqual(len(row["sha256"]), 64)
            result = run(Path(d))
            self.assertEqual(result["files_scanned"], 1)
            self.assertEqual(result["rows_scanned"], 3330)
            self.assertIn("INCOMPLETE_REPORTED_ETHANOL_MEASUREMENTS", result["blockers"])
            self.assertFalse(result["g2_qualified"])

    def test_duplicate_repetition_fail_closed(self):
        with tempfile.TemporaryDirectory() as d:
            for name in ("Ea-C1_R01.txt", "EaC1R01.csv"):
                (Path(d) / name).write_text("50,25,1,2,3,4,5,6\n" * 3330)
            with self.assertRaisesRegex(ValueError, "DUPLICATE_CONCENTRATION_REPETITION"):
                run(Path(d))

    def test_reject_duplicate_bytes_across_concentrations(self):
        with tempfile.TemporaryDirectory() as d:
            raw = "50,25,1,2,3,4,5,6\n" * 3330
            for name in ("Ea-C1_R01.txt", "Ea-C2_R01.txt"):
                (Path(d) / name).write_text(raw)
            with self.assertRaisesRegex(ValueError, "DUPLICATE_MEASUREMENT_BYTES"):
                run(Path(d))

    def test_reject_duplicate_bytes_across_repetitions(self):
        with tempfile.TemporaryDirectory() as d:
            raw = "50,25,1,2,3,4,5,6\n" * 3330
            for name in ("Ea-C1_R01.txt", "Ea-C1_R02.txt"):
                (Path(d) / name).write_text(raw)
            with self.assertRaisesRegex(ValueError, "DUPLICATE_MEASUREMENT_BYTES"):
                run(Path(d))

    def test_numeric_trace_digest_normalizes_numeric_notation(self):
        a = parse_numeric_rows(b"50,25,1,2,3,4,5,6\n", expected_rows=1)
        b = parse_numeric_rows(b"5e1;25.0;1.00;2;3;4;5;6\n", expected_rows=1)
        self.assertEqual(a["numeric_trace_sha256"], b["numeric_trace_sha256"])

    def test_reject_numeric_duplicate_with_different_file_bytes(self):
        with tempfile.TemporaryDirectory() as d:
            (Path(d) / "Ea-C1_R01.txt").write_text("50,25,1,2,3,4,5,6\n" * 3330)
            (Path(d) / "Ea-C2_R01.txt").write_text("5e1;25.0;1.00;2;3;4;5;6\n" * 3330)
            with self.assertRaisesRegex(ValueError, "DUPLICATE_MEASUREMENT_NUMERIC_TRACE"):
                run(Path(d))

    def test_long_precision_values_have_distinct_numeric_traces(self):
        a = parse_numeric_rows(
            b"50,25,1.1234567890123456789012345678901,2,3,4,5,6\n",
            expected_rows=1,
        )
        b = parse_numeric_rows(
            b"50,25,1.1234567890123456789012345678902,2,3,4,5,6\n",
            expected_rows=1,
        )
        self.assertNotEqual(a["numeric_trace_sha256"], b["numeric_trace_sha256"])

    def test_exact_numeric_equivalence_with_trailing_zeros(self):
        a = parse_numeric_rows(b"50,25,1.2300,2,3,4,5,6\n", expected_rows=1)
        b = parse_numeric_rows(b"5e1;25;123e-2;2;3;4;5;6\n", expected_rows=1)
        self.assertEqual(a["numeric_trace_sha256"], b["numeric_trace_sha256"])

    def test_reject_unicode_digits(self):
        with self.assertRaisesRegex(ValueError, "INVALID_NUMERIC_TOKEN"):
            parse_numeric_rows("50,25,١,2,3,4,5,6\n".encode("utf-8"), expected_rows=1)

    def test_reject_oversized_numeric_token(self):
        with self.assertRaisesRegex(ValueError, "NUMERIC_TOKEN_TOO_LONG"):
            parse_numeric_rows(("50,25," + "1" * 129 + ",2,3,4,5,6\n").encode(),
                               expected_rows=1)

    def test_distinct_high_precision_files_not_false_duplicates(self):
        with tempfile.TemporaryDirectory() as d:
            for name, value in (
                ("Ea-C1_R01.txt", "1.1234567890123456789012345678901"),
                ("Ea-C2_R01.txt", "1.1234567890123456789012345678902"),
            ):
                (Path(d) / name).write_text(("50,25," + value + ",2,3,4,5,6\n") * 3330)
            result = run(Path(d))
            self.assertEqual(result["files_scanned"], 2)
            self.assertFalse(result["g2_qualified"])

    def test_reject_blank_data_row_instead_of_silent_skip(self):
        with self.assertRaisesRegex(ValueError, "BLANK_DATA_ROW"):
            parse_numeric_rows(b"50,25,1,2,3,4,5,6\n\n51,25,1,2,3,4,5,6\n", expected_rows=2)

    def test_distinct_numeric_measurements_are_not_duplicates(self):
        with tempfile.TemporaryDirectory() as d:
            (Path(d) / "Ea-C1_R01.txt").write_text("50,25,1,2,3,4,5,6\n" * 3330)
            (Path(d) / "Ea-C2_R01.txt").write_text("51,25,1,2,3,4,5,6\n" * 3330)
            self.assertEqual(run(Path(d))["files_scanned"], 2)

    def test_reject_hidden_nested_measurement_directory(self):
        with tempfile.TemporaryDirectory() as d:
            (Path(d) / "Ea-C1_R01.txt").write_text("50,25,1,2,3,4,5,6\n" * 3330)
            (Path(d) / "additional_measurements").mkdir()
            with self.assertRaisesRegex(ValueError, "NONREGULAR_OR_NESTED_SOURCE_ENTRY"):
                run(Path(d))

    def test_reject_unknown_file(self):
        with tempfile.TemporaryDirectory() as d:
            (Path(d) / "README.txt").write_text("50,25,1,2,3,4,5,6\n")
            with self.assertRaisesRegex(ValueError, "INVALID_FILENAME"):
                run(Path(d))


if __name__ == "__main__":
    unittest.main()
