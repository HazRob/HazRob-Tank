# 03.7 Concentration confounding — HazRob Tank research prototype

Reports **within-gas** Pearson correlation between each numeric sensor feature and log10 of **known, positive, experimentally controlled concentration in ppm**. Groups are analyzed separately; groups with fewer than three observations or fewer than two distinct concentrations are flagged INSUFFICIENT. Constant features have undefined correlation (null). These correlations describe associations; they **do not** prove concentration causation or chemical selectivity and do not establish a predictive concentration model.

Critical: input concentrations must be independently known from controlled reference conditions, not inferred from MQ response. Match units, measurement times, heater phase, baseline, sensor order, humidity and temperature. Repeated time points in one exposure are not independent samples. A valid next step is grouped cross-session concentration-stratified validation and comparison of within-gas versus between-gas distances. Zero/below-LOD concentrations need a separate explicit handling protocol; this prototype rejects them instead of silently applying log10.

16 synthetic unit tests authored; clean-checkout execution and real Tank validation pending. Run `cd research/techniques/03.7 && python -m unittest -v`. No production changes. Status PROTOTYPE.
