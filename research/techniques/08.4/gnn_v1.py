"""Research-only graph neighborhood mean aggregation primitive."""
from math import isfinite
def aggregate(features,edges,include_self=True):
    x=[list(row) for row in features]
    if not x or not x[0]:raise ValueError("nonempty node features required")
    n=len(x);d=len(x[0])
    if any(len(row)!=d or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in row) for row in x):raise ValueError("finite aligned features required")
    neighbors=[set([i]) if include_self else set() for i in range(n)]
    for edge in edges:
        if not isinstance(edge,(list,tuple)) or len(edge)!=2 or any(not isinstance(i,int) or isinstance(i,bool) or not 0<=i<n for i in edge):raise ValueError("invalid edge")
        a,b=edge;neighbors[a].add(b);neighbors[b].add(a)
    return [[sum(x[i][j] for i in sorted(s))/len(s) for j in range(d)] if s else [0.]*d for s in neighbors]
