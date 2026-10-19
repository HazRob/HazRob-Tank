"""Inspect original UCI270 bundle without altering or re-publishing raw samples."""
import zipfile,sys,hashlib,collections,re
src=sys.argv[1]
h=hashlib.sha256(open(src,"rb").read()).hexdigest()
with zipfile.ZipFile(src) as z:
 names=[i for i in z.infolist() if not i.is_dir()]
 print("SOURCE_SHA256:",h)
 print("FILES:",len(names),"uncompressed_bytes:",sum(i.file_size for i in names))
 for i in names:
  print("FILE",i.filename,i.file_size)
  if re.search(r"batch\d+\.dat$",i.filename,re.I):
   lines=z.read(i).decode("utf-8-sig",errors="replace").splitlines()
   good=[line for line in lines if line.strip()]
   print("ROWS",len(good))
   print("FIRST",repr(good[0][:650]) if good else "empty")
   print("LAST",repr(good[-1][:180]) if good else "empty")
