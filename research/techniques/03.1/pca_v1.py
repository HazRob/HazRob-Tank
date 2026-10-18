"""Research PCA via pure Python Jacobi eigensolver. Fit training split only."""
from math import sqrt,isfinite
def _matrix(rows,dim=None):
    a=[list(row) for row in rows]
    if len(a)<2 or not a[0] or (dim is not None and len(a[0])!=dim):
        raise ValueError("insufficient rows or feature mismatch")
    if any(len(row)!=len(a[0]) or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not isfinite(v) for v in row) for row in a):
        raise ValueError("invalid matrix")
    return a

def fit_pca(train_rows,n_components=2,*,max_iterations=10000,tolerance=1e-12):
    x=_matrix(train_rows);n=len(x);d=len(x[0])
    if not isinstance(n_components,int) or isinstance(n_components,bool) or not 1<=n_components<=min(n-1,d):
        raise ValueError("invalid component count")
    mu=[sum(row[j] for row in x)/n for j in range(d)]
    z=[[row[j]-mu[j] for j in range(d)] for row in x]
    cov=[[sum(row[i]*row[j] for row in z)/(n-1) for j in range(d)] for i in range(d)]
    a=[row[:] for row in cov];v=[[float(i==j) for j in range(d)] for i in range(d)]
    for _ in range(max_iterations):
        p,q=max(((i,j) for i in range(d) for j in range(i+1,d)),key=lambda ij:abs(a[ij[0]][ij[1]]),default=(0,0))
        if p==q or abs(a[p][q])<tolerance:break
        from math import atan2,cos,sin
        theta=.5*atan2(2*a[p][q],a[q][q]-a[p][p])
        c=cos(theta);s=sin(theta)
        app=a[p][p];aqq=a[q][q];apq=a[p][q]
        a[p][p]=c*c*app-2*s*c*apq+s*s*aqq
        a[q][q]=s*s*app+2*s*c*apq+c*c*aqq
        a[p][q]=a[q][p]=0.
        for k in range(d):
            if k!=p and k!=q:
                akp=a[k][p];akq=a[k][q]
                a[k][p]=a[p][k]=c*akp-s*akq
                a[k][q]=a[q][k]=s*akp+c*akq
            vkp=v[k][p];vkq=v[k][q]
            v[k][p]=c*vkp-s*vkq;v[k][q]=s*vkp+c*vkq
    else:raise ValueError("eigensolver did not converge")
    eig=sorted(((max(0.,a[j][j]),[v[i][j] for i in range(d)]) for j in range(d)),key=lambda t:-t[0])
    total=sum(item[0] for item in eig)
    components=[]
    for val,vec in eig[:n_components]:
        idx=max(range(d),key=lambda j:abs(vec[j]))
        if vec[idx]<0:vec=[-w for w in vec]
        components.append(vec)
    return {"mean":mu,"components":components,"eigenvalues":[item[0] for item in eig[:n_components]],
            "explained_variance_ratio":[item[0]/total if total>0 else 0. for item in eig[:n_components]],
            "features":d,"n_train":n}

def transform_pca(rows,fitted):
    x=_matrix(rows,fitted["features"])
    return [[sum((row[j]-fitted["mean"][j])*vec[j] for j in range(fitted["features"])) for vec in fitted["components"]] for row in x]
