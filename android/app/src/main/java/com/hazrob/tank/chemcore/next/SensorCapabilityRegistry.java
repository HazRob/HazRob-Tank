package com.hazrob.tank.chemcore.next;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Device-defined sensor capability registry. Never assume exactly 8 channels. */
public final class SensorCapabilityRegistry {
    private final Map<String, CoreContracts.SensorInstance> sensors;
    public SensorCapabilityRegistry(List<CoreContracts.SensorInstance> instances) {
        if (instances == null) throw new IllegalArgumentException("sensor inventory required");
        Map<String, CoreContracts.SensorInstance> out = new LinkedHashMap<>();
        for (CoreContracts.SensorInstance sensor : instances) {
            if (sensor == null || out.putIfAbsent(sensor.uid, sensor) != null)
                throw new IllegalArgumentException("duplicate/null physical sensor UID");
        }
        sensors = Collections.unmodifiableMap(out);
    }
    public CoreContracts.SensorInstance byUid(String uid) { return sensors.get(uid); }
    public int installedCount() { return sensors.size(); }
    public List<CoreContracts.SensorInstance> installed() {
        return Collections.unmodifiableList(new ArrayList<>(sensors.values()));
    }
}
