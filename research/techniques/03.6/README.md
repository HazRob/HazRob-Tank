# 03.6 Sensor subsets — HazRob Tank research prototype

Enumerates subsets of aligned sensor features and compares **descriptive leave-one-out nearest-neighbor ambiguity** (squared Euclidean) on the supplied dataset. Sorts by lower ambiguity, fewer sensors and larger average squared-distance margin. Can project later data onto a chosen frozen subset.

For eight channels, there are 255 nonempty subsets. Exhaustive enumeration becomes expensive for more channels or large datasets. **Important:** choosing a subset by minimizing ambiguity on the same data is selection-biased and cannot be presented as independent predictive performance. Proper evaluation requires nested grouped cross-validation: choose subset using training groups only, then assess untouched sessions/devices, with preprocessing fit on training folds. Raw MQ feature scales and concentration confounding can dominate distances. Sensor removal also affects chemical coverage, power, safety and redundancy; do not recommend hardware removal from this prototype.

16 synthetic tests authored; clean-checkout and real Tank validation pending. Run `cd research/techniques/03.6 && python -m unittest -v`. No production changes. Status PROTOTYPE.
