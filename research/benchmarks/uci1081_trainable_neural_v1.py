"""Genuine trainable 1D-CNN, causal TCN, GRU and sensor GNN on real UCI1081.

Research ONLY. The 90 exposures use EXTERNAL gas sensors (not Tank MQ8).
Training, scaling, graph edges use training folds ONLY. Real dataset source
is validated against the canonical UCI1081 source hash in the shared loader.
No model is calibrated for gas alarms, Rs/R0 or ppm.
"""
import argparse
import json
import statistics
from pathlib import Path

import numpy as np
import torch
from torch import nn
from torch.nn import functional as F
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score,f1_score,confusion_matrix
from sklearn.preprocessing import StandardScaler

from uci1081_actual_modules_v1 import parse_archive,splits,GASES

KINDS=("cnn1d","tcn_causal","gru","gnn_train_correlations","gnn_identity")
EPOCHS=25
SEED=19

def measured_trace_features(trace):
    """900x10 sensor-major authentic values → 10 channels x 30 measured time bins.
    Window: injection seconds 300..899; 20s non-overlapping means.
    Each channel subtracts its own pre-exposure baseline t180..299.
    """
    if len(trace)!=10 or any(len(ch)!=900 for ch in trace):
        raise ValueError("expected exactly 10 channel-major 900-sample traces")
    x=[]
    for ch in trace:
        baseline=statistics.median(ch[180:300])
        response=np.asarray(ch[300:900],dtype=np.float64)-baseline
        if not np.isfinite(response).all():raise ValueError("invalid response")
        x.append(response.reshape(30,20).mean(axis=1))
    a=np.asarray(x,dtype=np.float32)
    if a.shape!=(10,30):raise AssertionError("feature shape mismatch")
    return a

def fit_time_scaling(xtrain):
    a=np.asarray(xtrain,dtype=np.float32)
    if a.ndim!=3 or a.shape[1]!=10 or a.shape[2]!=30:
        raise ValueError("10 source channels x 30 bins required")
    mean=a.mean(axis=(0,2),keepdims=True)
    std=a.std(axis=(0,2),keepdims=True)
    std=np.where(std>1e-8,std,1.0)
    return mean,std

def apply_time_scaling(x,params):
    mean,std=params
    a=np.asarray(x,dtype=np.float32)
    if a.ndim!=3 or a.shape[1:]!=(10,30):raise ValueError("feature schema mismatch")
    return (a-mean)/std

def train_only_adjacency(scaled_train,*,neighbors=2,identity=False):
    """Graph edges are *correlations in training* not physical sensor topology."""
    if identity:
        return np.eye(10,dtype=np.float32)
    x=np.asarray(scaled_train,dtype=np.float32)
    if x.ndim!=3 or x.shape[1:]!=(10,30) or len(x)<3:
        raise ValueError("insufficient aligned training arrays")
    summary=x.mean(axis=2)
    if not 1<=neighbors<10:raise ValueError("invalid neighbor count")
    rho=np.corrcoef(summary,rowvar=False)
    rho=np.nan_to_num(rho,nan=0.,posinf=0.,neginf=0.)
    abs_rho=np.abs(rho)
    np.fill_diagonal(abs_rho,0)
    adj=np.eye(10,dtype=np.float64)
    for sensor in range(10):
        selected=sorted((k for k in range(10) if k!=sensor),
                        key=lambda k:(-abs_rho[sensor,k],k))[:neighbors]
        for k in selected:
            adj[sensor,k]=adj[k,sensor]=1.
    return (adj/adj.sum(axis=1,keepdims=True)).astype(np.float32)

class CNN1D(nn.Module):
    def __init__(self,n_classes):
        super().__init__()
        self.layers=nn.Sequential(
            nn.Conv1d(10,16,5,padding=2),nn.ReLU(),
            nn.Conv1d(16,16,3,padding=1),nn.ReLU(),
            nn.AdaptiveAvgPool1d(1))
        self.head=nn.Linear(16,n_classes)
    def forward(self,x):
        return self.head(self.layers(x).squeeze(-1))

class CausalTCN(nn.Module):
    def __init__(self,n_classes):
        super().__init__()
        self.first=nn.Conv1d(10,16,3,dilation=1)
        self.second=nn.Conv1d(16,16,3,dilation=2)
        self.head=nn.Linear(16,n_classes)
    def temporal_hidden(self,x):
        # End-exclusive zero left-pad: output at t cannot depend on any x[s>t].
        h=F.relu(self.first(F.pad(x,(2,0))))
        h=F.relu(self.second(F.pad(h,(4,0))))
        return h
    def forward(self,x):
        return self.head(self.temporal_hidden(x)[:,:,-1])

class GRUSensor(nn.Module):
    def __init__(self,n_classes):
        super().__init__()
        self.rnn=nn.GRU(input_size=10,hidden_size=16,batch_first=True)
        self.head=nn.Linear(16,n_classes)
    def forward(self,x):
        seq=x.transpose(1,2).contiguous()
        _,hidden=self.rnn(seq)
        return self.head(hidden[-1])

class SensorGNN(nn.Module):
    def __init__(self,n_classes,adjacency):
        super().__init__()
        graph=torch.as_tensor(adjacency,dtype=torch.float32)
        if tuple(graph.shape)!=(10,10):raise ValueError("graph must have 10 nodes")
        self.register_buffer("adjacency",graph)
        self.embed=nn.Linear(2,16)
        self.message=nn.Linear(16,16)
        self.head=nn.Linear(16,n_classes)
    def forward(self,x):
        # Node features: early and late mean baseline-relative responses.
        nodes=torch.stack([x[:,:,:6].mean(dim=2),x[:,:,-6:].mean(dim=2)],dim=-1)
        h=F.relu(self.embed(nodes))
        aggregate=torch.einsum("ij,bjh->bih",self.adjacency,h)
        h=F.relu(self.message(aggregate+h))
        return self.head(h.mean(dim=1))

def architecture(kind,classes,adj=None):
    if kind=="cnn1d":return CNN1D(classes)
    if kind=="tcn_causal":return CausalTCN(classes)
    if kind=="gru":return GRUSensor(classes)
    if kind.startswith("gnn_"):return SensorGNN(classes,adj)
    raise ValueError("unsupported architecture")

def fit_predict(kind,xtrain,ytrain,xtest,*,epochs=EPOCHS):
    """Train full-batch cross-entropy on external-source training rows only."""
    torch.set_num_threads(1)
    torch.manual_seed(SEED)
    np.random.seed(SEED)
    classes=tuple(sorted(set(ytrain)))
    if len(classes)<2 or len(xtrain)<len(classes)*2 or not 1<=epochs<=200:
        raise ValueError("insufficient supervised train exposures")
    labels=[classes.index(y) for y in ytrain]
    mean_std=fit_time_scaling(xtrain)
    train=apply_time_scaling(xtrain,mean_std)
    test=apply_time_scaling(xtest,mean_std)
    graph=None
    if kind.startswith("gnn_"):
        graph=train_only_adjacency(train,identity=kind=="gnn_identity")
    model=architecture(kind,len(classes),graph)
    optimizer=torch.optim.Adam(model.parameters(),lr=.012,weight_decay=1e-4)
    xx=torch.as_tensor(train,dtype=torch.float32)
    yy=torch.as_tensor(labels,dtype=torch.long)
    history=[]
    for _ in range(epochs):
        model.train()
        optimizer.zero_grad()
        logits=model(xx)
        loss=F.cross_entropy(logits,yy)
        if not torch.isfinite(loss):raise ValueError("nonfinite training loss")
        loss.backward()
        nn.utils.clip_grad_norm_(model.parameters(),5.0)
        optimizer.step()
        history.append(float(loss.item()))
    model.eval()
    with torch.no_grad():
        pred=model(torch.as_tensor(test,dtype=torch.float32)).argmax(dim=1).tolist()
    return [classes[k] for k in pred],{
        "n_epochs":epochs,"train_loss_first":history[0],"train_loss_last":history[-1],
        "train_loss_improved":history[-1]<history[0],
        "parameter_count":sum(p.numel() for p in model.parameters()),
        "graph_edges":"identity" if kind=="gnn_identity" else ("training_only_sensor_correlations_top2" if graph is not None else None),
    }

def run(source):
    records=parse_archive(source)
    x=np.stack([measured_trace_features(row["trace"]) for row in records],axis=0)
    if x.shape!=(90,10,30):raise AssertionError("unexpected reference dataset")
    y=[row["gas"] for row in records]
    folds=splits(records,"leave_one_concentration_ppb")
    results={}
    for kind in ("majority","logreg_flat")+KINDS:
        true=[];pred=[];fold_results=[]
        for f in folds:
            it,iv=f["train"],f["test"]
            ys=[y[i] for i in it];truth=[y[i] for i in iv]
            train=x[it];test=x[iv]
            meta={}
            if kind=="majority":
                pred_fold=["acetone"]*len(iv)
            elif kind=="logreg_flat":
                scaler=StandardScaler().fit(train.reshape(len(it),-1))
                clf=LogisticRegression(max_iter=1000,C=1.,random_state=SEED)
                clf.fit(scaler.transform(train.reshape(len(it),-1)),ys)
                pred_fold=clf.predict(scaler.transform(test.reshape(len(iv),-1))).tolist()
            else:
                pred_fold,meta=fit_predict(kind,train,ys,test)
            true.extend(truth);pred.extend(pred_fold)
            fold_results.append({"heldout_ppb":f["heldout"],"train":len(it),"test":len(iv),
                                 "correct":sum(a==b for a,b in zip(truth,pred_fold)),
                                 **meta})
        results[kind]={
            "correct":sum(a==b for a,b in zip(true,pred)),
            "total":len(true),"accuracy":float(accuracy_score(true,pred)),
            "macro_f1":float(f1_score(true,pred,labels=GASES,average="macro",zero_division=0)),
            "confusion_labels":list(GASES),
            "confusion":confusion_matrix(true,pred,labels=GASES).tolist(),
            "folds":fold_results
        }
    return {
        "dataset":"UCI1081 DOI 10.24432/C5CK6F; CC BY 4.0",
        "n_unique_external_exposures":90,
        "source_not_tank_mq8":True,
        "channels":10,"timebins":30,"seconds_per_bin":20,"exposure_seconds":600,
        "fixed_model_training_epochs":EPOCHS,"seed":SEED,
        "train_only_scaling_and_gnn_edges":True,
        "grouping":"held-out entire concentration levels 50/100/200 ppb; source session/device identifiers unavailable",
        "torch_version":torch.__version__,
        "g3_tank_validated":False,
        "architectural_truth":{
           "cnn1d":"genuine trainable Conv1d 2 layer network",
           "tcn_causal":"genuine trainable causal dilated convolutions",
           "gru":"genuine torch.nn.GRU trained with cross entropy",
           "gnn_train_correlations":"genuine differentiable graph message passing with fixed train-only correlation edges",
           "gnn_identity":"graph ablation control without inter-sensor edges"
        },
        "warning":"Research-only small external dataset, no independent devices, no Tank MQ8 calibration, no gas safety or ppm qualification.",
        "results":results
    }

def main():
    p=argparse.ArgumentParser()
    p.add_argument("source_zip")
    p.add_argument("--output",required=True)
    a=p.parse_args()
    result=run(a.source_zip)
    Path(a.output).write_text(json.dumps(result,ensure_ascii=False,indent=2,allow_nan=False)+"\n",encoding="utf-8")
    for kind,item in result["results"].items():
        print(kind,item["correct"],"/90",f"macro_F1={item['macro_f1']:.4f}",
              "per_fold",[f["correct"] for f in item["folds"]])
if __name__=="__main__":main()
