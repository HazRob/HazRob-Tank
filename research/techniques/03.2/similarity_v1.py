"""Research-only distance/similarity analysis of aligned sensor fingerprints."""
from math import sqrt, isfinite

def _pair(a, b):
    x, y = tuple(a), tuple(b)
    if not x or len(x) != len(y):
        raise ValueError('fingerprint lengths must match and be nonzero')
    if any(isinstance(v, bool) or not isinstance(v, (int, float)) or not isfinite(v) for v in x + y):
        raise ValueError('all features must be finite numbers')
    return x, y

def compare(a, b, *, scales=None):
    """Return cosine, Euclidean, Manhattan, and scaled Euclidean distance."""
    x, y = _pair(a, b)
    if scales is None:
        s = (1.0,) * len(x)
    else:
        s = tuple(scales)
        if len(s) != len(x) or any(isinstance(v, bool) or not isinstance(v, (int, float)) or not isfinite(v) or v <= 0 for v in s):
            raise ValueError('scales must be positive, finite, aligned')
    diff = [v - w for v, w in zip(x, y)]
    nx = sqrt(sum(v*v for v in x))
    ny = sqrt(sum(v*v for v in y))
    cosine = None if nx == 0 or ny == 0 else max(-1.0, min(1.0, sum(v*w for v, w in zip(x, y)) / (nx*ny)))
    return {
        'cosine_similarity': cosine,
        'euclidean_distance': sqrt(sum(v*v for v in diff)),
        'manhattan_distance': sum(abs(v) for v in diff),
        'scaled_euclidean_distance': sqrt(sum((v / scale)**2 for v, scale in zip(diff, s))),
    }

def nearest_reference(query, references, *, scales=None):
    """Nearest reference by scaled Euclidean distance; deterministic ties."""
    refs = list(references)
    if not refs:
        raise ValueError('no references')
    scores = [compare(query, ref, scales=scales)['scaled_euclidean_distance'] for ref in refs]
    i = min(range(len(scores)), key=lambda j: (scores[j], j))
    return {'index': i, 'distance': scores[i], 'distances': scores}
