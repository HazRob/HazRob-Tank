#!/usr/bin/env python3
"""Acquire public MQ research file bytes with explicit provenance and failure records.
One independent source ID per original study. No food source is imported into gas.
This is a source ACQUISITION utility, never calibration/ppm validation.
"""
from __future__ import annotations
import csv, hashlib, io, json, os, pathlib, re, time, urllib.request, urllib.error
from datetime import datetime, timezone

ROOT=pathlib.Path("mq_acquisition")
RAW=ROOT/"raw"
RAW.mkdir(parents=True,exist_ok=True)
UA={"User-Agent":"HazRob ChemCore academic reproducibility research/1.0 (source provenance)","Accept":"application/json, text/csv, application/octet-stream, */*"}
MAX_BYTES=22*1024*1024
manifest={"schema":"hazrob-mq-real-source-acquisition-2026-10-30/1",
 "created_utc":datetime.now(timezone.utc).isoformat(),"gas_food_separation":True,
 "tank_g3":False,"source_items":[],"methodological_limits":[
 "A source sensor result is not device-specific R0 or proof of gas ppm.",
 "Consecutive rows are not independent exposures; group by date/device/session where provided.",
 "Unknown ppm inferred by producer is NOT reference-certified ppm.",
 "Food datasets intentionally excluded from gas source payload."]}
def get(url,limit=MAX_BYTES):
    req=urllib.request.Request(url,headers=UA)
    with urllib.request.urlopen(req,timeout=35) as f:
        typ=f.headers.get("content-type","")
        data=f.read(limit+1)
        if len(data)>limit:raise ValueError("asset exceeds configured source archive cap")
        return data,typ,f.geturl()
def text_json(url):
    data,content_type,resolved=get(url,2*1024*1024)
    return json.loads(data),resolved
def store(source_id,file_label,url,lower=60,require_csv=False):
    entry={"source_id":source_id,"label":file_label,"requested_url":url}
    try:
        data,typ,final=get(url)
        if len(data)<lower:raise ValueError("retrieved file smaller than expected")
        if data[:100].lstrip().lower().startswith((b"<!doctype",b"<html")):
            raise ValueError("HTML login/error content refused")
        if require_csv:
            sample=data[:3500].decode("utf-8-sig",errors="replace")
            if "," not in sample.splitlines()[0]:
                raise ValueError("not a valid CSV header")
            entry["first_columns"]=next(csv.reader(io.StringIO(sample)))
            entry["line_count"]=data.count(b"\n")
            if entry["line_count"]<5:raise ValueError("insufficient CSV measurements")
        suffix=".csv" if require_csv else pathlib.Path(final.split("?")[0]).suffix.lower()
        if suffix not in (".csv",".zip",".xlsx",".xls",".json",".txt",".tsv"):
            suffix=".bin"
        name=f"{source_id}_{file_label}{suffix}"
        path=RAW/name
        path.write_bytes(data)
        entry.update(status="RAW_DOWNLOADED_SHA256_VERIFIED",
          sha256=hashlib.sha256(data).hexdigest(),bytes=len(data),
          content_type=typ,resolved_url=final,file=str(path),
          evidence_scope="external source only; no Tank G3")
        print("DOWNLOADED",source_id,file_label,len(data),entry["sha256"])
    except Exception as e:
        entry.update(status="SOURCE_ONLY_NOT_DOWNLOADED",error=type(e).__name__+": "+str(e)[:280])
        print("NOT_DOWNLOADED",source_id,file_label,entry["error"])
    manifest["source_items"].append(entry)
    return entry

# A. Direct new public CSV: independent from previously duplicated MQ6400.
store("MQ-ACQ-2026-01","mobile_aq_mq3_mq4_mq8_voltage",
 "https://raw.githubusercontent.com/NohPei/mobile-AQ-documentation/main/sensor_log.csv",
 100,True)
# B. Public Thingspeak archive: ppm are device-reported / calculated, not reference gas.
store("MQ-ACQ-2026-02","thingspeak_mq2_mq9_mq135",
 "https://api.thingspeak.com/channels/1435139/feeds.csv?results=8000",
 80,True)

# C. API download of open Mendeley dataset files, recording each original ID.
MENDELEY=[
 ("MQ-ACQ-2026-03","kvcfp5y95z",1,"MQ3_MQ135_alcohol_presence"),
 ("MQ-ACQ-2026-04","kn3x9rz3kd",1,"MQ_ADL_ambient_air_four_labels"),
 ("MQ-ACQ-2026-05","5c2fwppsvs",1,"MQ135_indoor_air"),
]
for source_id,short,version,label in MENDELEY:
    base="https://api.data.mendeley.com"
    try:
        j,orig=text_json(f"{base}/datasets/{short}/files?version={version}")
        files=j if isinstance(j,list) else j.get("results",j.get("files",[]))
        if not files:raise ValueError("No public downloadable files in Mendeley metadata")
        print("MENDELEY_LISTED",source_id,len(files))
        for pos,f in enumerate(files[:8]):
            file_id=f.get("id") or f.get("file_id")
            name=f.get("filename") or f.get("name") or str(pos)
            if not file_id:continue
            direct=((f.get("content_details") or {}).get("download_url") or
                    f"{base}/datasets/{short}/files/{file_id}/file_downloaded?version={version}")
            # Only archive tabular and compact raw datasets, do not download PDFs.
            extension=pathlib.Path(name).suffix.lower()
            if extension not in (".csv",".zip",".xlsx",".xls",".tsv",".txt"):
                manifest["source_items"].append({"source_id":source_id,"filename":name,
                    "status":"METADATA_ONLY_FILE_TYPE_EXCLUDED","file_id":file_id});continue
            obj=store(source_id,f"file{pos}_{re.sub('[^A-Za-z0-9_-]','_',name)[:55]}",direct,
                      25,extension in (".csv",".tsv"))
            obj["original_filename"]=name;obj["file_uuid"]=file_id
            obj["mendeley_dataset"]=short;obj["mendeley_version"]=version
    except Exception as e:
        manifest["source_items"].append({"source_id":source_id,
          "original_url":f"https://data.mendeley.com/datasets/{short}/{version}",
          "status":"METADATA_UNAVAILABLE_DOWNLOAD_NOT_VERIFIED",
          "error":type(e).__name__+": "+str(e)[:280]})
        print("MENDELEY_UNAVAILABLE",source_id,type(e).__name__,str(e)[:100])

# D. Official University of Minnesota record: attempt DSpace REST mapping.
# Do not guess direct bitstream URL, never scrape arbitrary documents.
source_id="MQ-ACQ-2026-06"
handle="11299/226714"
endpoints=[
 f"https://conservancy.umn.edu/server/api/core/handles/{handle}",
 f"https://conservancy.umn.edu/server/api/core/handles/{handle.replace('/','%2F')}",
 f"https://conservancy.umn.edu/server/api/discover/search/objects?query=Furuta%20methane&size=10"
]
found=False
for endpoint in endpoints:
    try:
        j,resolved=text_json(endpoint)
        filename=f"{source_id}_umn_handle_{len(manifest['source_items'])}.json"
        blob=json.dumps(j,indent=2).encode("utf8")
        (RAW/filename).write_bytes(blob)
        manifest["source_items"].append({"source_id":source_id,
           "status":"UMN_API_METADATA_RETRIEVED","requested_url":endpoint,
           "sha256":hashlib.sha256(blob).hexdigest(),"file":str(RAW/filename),
           "content_type":"application/json"})
        print("MINNESOTA_METADATA",endpoint)
        found=True
        # DSpace may give item UUID via _embedded or direct object
        item=(j.get("_embedded") or {}).get("item") or j.get("item") or {}
        uid=item.get("uuid") or j.get("uuid")
        if uid:
            try:
                bundles,_=text_json(f"https://conservancy.umn.edu/server/api/core/items/{uid}/bundles?size=100")
                for b in (bundles.get("_embedded") or {}).get("bundles",[]):
                    if b.get("name")!="ORIGINAL":continue
                    bid=b.get("uuid")
                    filesj,_=text_json(f"https://conservancy.umn.edu/server/api/core/bundles/{bid}/bitstreams?size=100")
                    for f in (filesj.get("_embedded") or {}).get("bitstreams",[]):
                        name=f.get("name","")
                        if name.lower().endswith(".csv"):
                            store(source_id,"umn_"+re.sub('[^A-Za-z0-9_-]','_',name),
                                  f"https://conservancy.umn.edu/server/api/core/bitstreams/{f['uuid']}/content",100,True)
            except Exception as e:
                manifest["source_items"].append({"source_id":source_id,
                    "status":"UMN_FILE_LIST_UNAVAILABLE","error":str(e)[:250]})
        break
    except Exception as e:
        print("MINNESOTA_API_TRY",type(e).__name__,str(e)[:110])
if not found:manifest["source_items"].append({"source_id":source_id,
  "source_url":"https://hdl.handle.net/11299/226714",
  "status":"LANDING_VERIFIED_RAW_DOWNLOAD_PENDING","reason":"public DSpace API route unavailable"})


# D2. Verified target UUID from Minnesota's actual searchable item metadata
# (handle 11299/226714). Do not confuse it with the same author's thesis.
umn_uuid="863729b3-62bd-4de9-be7a-6d60cc449ce1"
try:
    root="https://conservancy.umn.edu/server/api/core"
    item,actual=text_json(f"{root}/items/{umn_uuid}")
    if item.get("handle")!=handle:
        raise ValueError("Minnesota item UUID/handle mismatch")
    manifest["source_items"].append({"source_id":"MQ-ACQ-2026-06",
      "status":"UMN_EXACT_HANDLE_AND_UUID_VERIFIED","item_uuid":umn_uuid,
      "handle":item.get("handle"),"title":item.get("name")})
    bundles,_=text_json(f"{root}/items/{umn_uuid}/bundles?size=100")
    group=(bundles.get("_embedded") or {}).get("bundles",[])
    print("MINNESOTA_TARGET_BUNDLES",[(x.get("name"),x.get("uuid")) for x in group])
    for b in group:
        if b.get("name") not in ("ORIGINAL","LICENSE"):continue
        b_id=b.get("uuid")
        if not b_id:continue
        bitstreams,_=text_json(f"{root}/bundles/{b_id}/bitstreams?size=100")
        files=(bitstreams.get("_embedded") or {}).get("bitstreams",[])
        for f in files:
            n=f.get("name","");fid=f.get("uuid")
            if not fid:continue
            if n.lower().endswith((".csv",".txt",".tsv",".zip")):
                store("MQ-ACQ-2026-06",
                    "umn_"+re.sub('[^A-Za-z0-9_-]','_',n)[:50],
                    f"{root}/bitstreams/{fid}/content",100,
                    n.lower().endswith((".csv",".tsv")))
            else:
                manifest["source_items"].append({"source_id":"MQ-ACQ-2026-06",
                    "status":"UMN_ORIGINAL_NON_TABULAR_FILE_METADATA",
                    "original_filename":n,"bitstream_uuid":fid})
except Exception as e:
    manifest["source_items"].append({"source_id":"MQ-ACQ-2026-06",
        "status":"UMN_TARGET_BITSTREAM_FETCH_PENDING",
        "item_uuid":umn_uuid,"error":type(e).__name__+": "+str(e)[:280]})
    print("MINNESOTA_TARGET_UNAVAILABLE",type(e).__name__,str(e)[:130])

# E. Existing MQ6400 duplicate from previously benchmarked local source:
manifest["source_items"].append({"source_id":"MQ-EXT-03","status":"PREVIOUSLY_STORED_NOT_DUPLICATED",
 "source_repo":"HazRob/HazRob-Tank",
 "source_path":"research/chemcore_v6/source_snapshots/external/mq_ext_03_multimodal_6400_raw.csv",
 "sha256":"13c2c85cfbb23357a71555b9bc2cbe7dd68e931ffc94df30389c1a4631203745",
 "source_rows":6400,"source_models":["MQ2","MQ3","MQ5","MQ6","MQ7","MQ8","MQ135"],
 "labels":["NoGas","Perfume","Smoke","Mixture"],"independent_reference_ppm":False})

(ROOT/"acquisition_manifest.json").write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+"\n",encoding="utf8")
downloaded=[x for x in manifest["source_items"] if x["status"]=="RAW_DOWNLOADED_SHA256_VERIFIED"]
print("ACQUISITION_SUMMARY downloaded",len(downloaded),
    "bytes",sum(x["bytes"] for x in downloaded),
    "metadata_total",len(manifest["source_items"]))
if not downloaded:
    raise SystemExit("FAIL: no new original physical MQ source could be acquired")
