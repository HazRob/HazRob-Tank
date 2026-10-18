# 02.6 3D/4D response trajectories — HazRob Tank research prototype

**Definition:** 3D coordinate = (elapsed seconds, sensor A baseline-normalized log-resistance response, sensor B response); optional 4D coordinate adds one measured environmental covariate (e.g. temperature or humidity). These are trajectories in feature space, **not** molecular structures, ppm concentrations or a complete MQDataScience 3D/4D method.

The path-length feature requires user-supplied positive scales for each coordinate, since seconds, log ratios and environmental units must not be added naively. Scales should be fitted only on training sessions (02.2), then fixed. No gas identity inference.

Prerequisites: timestamps aligned across sensors, matched MQ7/MQ9 heater phases, BME280 calibration and time synchronization, valid exposure/baseline windows, documented sensor order. Multi-sensor feature combinations and selection require held-out cross-session evaluation; no proof of discrimination is claimed.

16 unit tests authored, clean checkout execution pending. Run `cd research/techniques/02.6 && python -m unittest -v`. No production changes. Status PROTOTYPE.
