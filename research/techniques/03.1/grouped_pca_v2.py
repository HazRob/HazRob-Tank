"""03.1: fit PCA on source training groups, transform disjoint held-out rows only."""
from math import isfinite
from pca_v1 import fit_pca, transform_pca
def grouped_pca_fit(train_rows,group_ids,sensor_ids,*,source_id,n_components=2):
    xs=list(train_rows);groups=list(group_ids);ids=tuple(sensor_ids)
    if not source_id or len(xs)<3 or len(groups)!=len(xs) or not ids or len(set(ids))!=len(ids):
        raise ValueError("missing source identity, channels or training groups")
    if any(not isinstance(g,str) or not g.strip() for g in groups):
        raise ValueError("invalid train group")
    fitted=fit_pca(xs,n_components)
    if fitted["features"]!=len(ids):raise ValueError("sensor count mismatch")
    return {"fitted":fitted,"train_groups":tuple(sorted(set(groups))),
            "source_id":source_id,"sensor_ids":ids,
            "loadings":fitted["components"],"approved_ppm":False}
def transform_grouped_pca(model, rows, group_ids, *,source_id,sensor_ids):
    xs=list(rows);groups=list(group_ids)
    if not xs or len(xs)!=len(groups) or any(not g for g in groups):
        raise ValueError("heldout groups required")
    if source_id!=model["source_id"] or tuple(sensor_ids)!=model["sensor_ids"]:
        raise ValueError("sensor/source identity mismatch")
    if set(groups)&set(model["train_groups"]):
        raise ValueError("train/heldout experimental group overlap")
    return {"scores":transform_pca(xs,model["fitted"]),
            "explained_variance_ratio":model["fitted"]["explained_variance_ratio"],
            "ppm_approved":False}
