"""Research-only 2D grid binning of sensor readings."""
from math import floor,isfinite
def grid_map(samples,cell_size=1):
    if not isinstance(cell_size,(int,float)) or not isfinite(cell_size) or cell_size<=0:raise ValueError("positive cell size required")
    cells={}
    for row in samples:
        if not isinstance(row,(list,tuple)) or len(row)!=3 or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in row):raise ValueError("(x,y,reading) required")
        x,y,value=row;key=(floor(x/cell_size),floor(y/cell_size))
        cells.setdefault(key,[]).append(value)
    return [{"cell":list(k),"mean_reading":sum(v)/len(v),"n":len(v)} for k,v in sorted(cells.items())]
