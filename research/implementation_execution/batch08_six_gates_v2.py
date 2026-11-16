"""Batch08 six research-only gates; no ppm, motor commands, or physical qualification."""
from math import isfinite,hypot
def num(x):return type(x) in (int,float) and isfinite(x)
def interference_gate(target_before,target_after,blank_before,blank_after,minimum_retention=.8):
 v=(target_before,target_after,blank_before,blank_after,minimum_retention)
 if not all(map(num,v)) or target_before<=0 or min(target_after,blank_before,blank_after)<0 or not 0<minimum_retention<=1 or blank_before==0:raise ValueError('invalid paired response')
 retention=target_after/target_before
 return dict(retention=retention,interference_suppression=1-blank_after/blank_before,status='REJECT_TARGET_LOSS' if retention<minimum_retention else 'RESEARCH_ONLY',ppm_approved=False)
def complex_product_gate(samples,required_groups=3):
 if type(required_groups) is not int or required_groups<2 or not isinstance(samples,(list,tuple)) or not samples:raise ValueError('invalid dataset')
 groups=set()
 for r in samples:
  if not isinstance(r,dict) or not isinstance(r.get('group'),str) or not r['group'] or not isinstance(r.get('product'),str) or not r['product'] or not num(r.get('response')):raise ValueError('invalid row')
  groups.add(r['group'])
 return dict(status='GROUP_HOLDOUT_POSSIBLE' if len(groups)>=required_groups else 'NEEDS_INDEPENDENT_GROUPS',groups=len(groups),pure_gas_identity=None,ppm_approved=False)
def temporal_plume_events(rows,threshold,minimum_gap=0):
 if not num(threshold) or not num(minimum_gap) or minimum_gap<0:raise ValueError('invalid threshold')
 events=[];previous=None;active=False
 for r in rows:
  if not isinstance(r,dict) or not num(r.get('time')) or not num(r.get('response')):raise ValueError('invalid sample')
  t=r['time']
  if previous is not None and t<=previous:raise ValueError('nonmonotonic time')
  if r['response']>=threshold and not active and (not events or t-events[-1]>=minimum_gap):events.append(t)
  active=r['response']>=threshold;previous=t
 return dict(crossings=events,source_location=None,research_only=True)
def airflow_validity(rows,min_speed,max_speed):
 if not all(map(num,(min_speed,max_speed))) or not 0<=min_speed<max_speed:raise ValueError('invalid bounds')
 result=[]
 for r in rows:
  if not isinstance(r,dict) or not num(r.get('speed')) or not num(r.get('response')) or r['speed']<0:raise ValueError('invalid sample')
  result.append(dict(valid=min_speed<=r['speed']<=max_speed,response=r['response']))
 return dict(rows=result,flow_calibrated=False)
def pose_response_association(rows,max_pose_age):
 if not num(max_pose_age) or max_pose_age<0:raise ValueError('invalid age')
 out=[];last=None
 for r in rows:
  if not isinstance(r,dict) or any(not num(r.get(k)) for k in ('time','pose_time','x','y','response')):raise ValueError('invalid row')
  if last is not None and r['time']<=last:raise ValueError('nonmonotonic time')
  if not 0<=r['time']-r['pose_time']<=max_pose_age:raise ValueError('invalid pose age')
  out.append(dict(x=r['x'],y=r['y'],response=r['response'],source_location_claim=False));last=r['time']
 return out
def hotspot_hypotheses(rows,min_separation):
 if not num(min_separation) or min_separation<=0 or not rows:raise ValueError('invalid separation')
 for r in rows:
  if not isinstance(r,dict) or any(not num(r.get(k)) for k in ('x','y','response')):raise ValueError('invalid point')
 candidates=[]
 for r in sorted(rows,key=lambda p:-p['response']):
  if all(hypot(r['x']-q['x'],r['y']-q['y'])>=min_separation for q in candidates):candidates.append(dict(r))
 return dict(candidates=candidates,source_confirmed=False,motor_command=None)
