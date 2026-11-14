"""Negative and positive metadata controls; not evidence of measured gas identity."""
import unittest
from g2_mendeley_mq14_source_probe import inspect_metadata

class Mq14MetadataTests(unittest.TestCase):
    def test_metadata_never_claims_qualification(self):
        x = inspect_metadata({"name":"Test","files":[{"filename":"raw.csv","size":100}]})
        self.assertEqual(x["files_reported"], 1)
        self.assertFalse(x["raw_bytes_verified"])
        self.assertFalse(x["sensor_models_verified"])
        self.assertFalse(x["reference_gas_ppm_verified"])
        self.assertFalse(x["g2_qualified"])
        self.assertFalse(x["g3_tank"])

    def test_missing_file_list_not_invented(self):
        self.assertEqual(inspect_metadata({"files":[]})["files_reported"], 0)

    def test_malformed_file_list_rejected(self):
        with self.assertRaisesRegex(ValueError,"SOURCE_FILES_NOT_LIST"):
            inspect_metadata({"files":"raw.csv"})

    def test_malformed_file_rejected(self):
        with self.assertRaisesRegex(ValueError,"SOURCE_FILE_METADATA_NOT_OBJECT"):
            inspect_metadata({"files":["raw.csv"]})

if __name__=="__main__": unittest.main()
