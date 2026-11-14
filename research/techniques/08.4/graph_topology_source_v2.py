"""08.4: deterministic normalized graph message aggregation, not a trained GNN.
Topology must be supplied and preserved, not inferred from heldout gas labels.
"""
from math import isfinite

def graph_message_aggregate(features, adjacency, *, sensor_ids,source_id,graph_source_id):
    x=list(features);n=len(x)
    if not source_id or not graph_source_id or n<2 or len(sensor_ids)!=n or len(set(sensor_ids))!=n:
        raise ValueError("complete source and unique graph node identity required")
    if len(adjacency)!=n or any(len(row)!=n for row in adjacency):
        raise ValueError("adjacency dimensions mismatch")
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) for v in x):
        raise ValueError("nonfinite node features")
    for i,row in enumerate(adjacency):
        for j,v in enumerate(row):
            if isinstance(v,bool) or not isinstance(v,(int,float)) or not isfinite(v) or v<0 or v>1:
                raise ValueError("edge weights must be finite in [0,1]")
            if abs(v-adjacency[j][i])>1e-12:raise ValueError("undirected topology symmetry required")
        if row[i]!=1:raise ValueError("self loop required")
    y=[sum(adjacency[i][j]*x[j] for j in range(n))/sum(adjacency[i]) for i in range(n)]
    return {"node_aggregate":y,"sensor_ids":list(sensor_ids),"graph_source_id":graph_source_id,
            "trained_GNN":False,"gas_identity_verified":False,"Tank_G3":False}
