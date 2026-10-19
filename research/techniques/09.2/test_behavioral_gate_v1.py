import unittest
from gas_mapping_v1 import grid_map
class BehavioralGate(unittest.TestCase):
    def test_basic(self): self.assertEqual(grid_map([(0,0,2)])[0]["mean_reading"],2)
    def test_bin_average(self): self.assertEqual(grid_map([(0,0,2),(.5,.5,4)])[0]["mean_reading"],3)
    def test_count(self): self.assertEqual(grid_map([(0,0,2),(.5,.5,4)])[0]["n"],2)
    def test_negative_floor(self): self.assertEqual(grid_map([(-.1,-1.1,2)])[0]["cell"],[-1,-2])
    def test_two_cells(self): self.assertEqual(len(grid_map([(0,0,1),(1,0,2)])),2)
    def test_custom_cell(self): self.assertEqual(grid_map([(1,1,2)],cell_size=2)[0]["cell"],[0,0])
    def test_order(self): self.assertEqual(grid_map([(1,0,2),(0,0,1)])[0]["cell"],[0,0])
    def test_empty(self): self.assertEqual(grid_map([]),[])
    def test_zero_size(self):
        with self.assertRaises(ValueError): grid_map([(0,0,1)],cell_size=0)
    def test_bool_size(self):
        with self.assertRaises(ValueError): grid_map([(0,0,1)],cell_size=True)
    def test_nan_size(self):
        with self.assertRaises(ValueError): grid_map([(0,0,1)],cell_size=float("nan"))
    def test_bad_shape(self):
        with self.assertRaises(ValueError): grid_map([(0,0)])
    def test_nan_reading(self):
        with self.assertRaises(ValueError): grid_map([(0,0,float("nan"))])
    def test_bool_coordinate(self):
        with self.assertRaises(ValueError): grid_map([(True,0,1)])
if __name__=="__main__":unittest.main()
