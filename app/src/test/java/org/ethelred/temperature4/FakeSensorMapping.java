// (C) Edward Harman 2026
package org.ethelred.temperature4;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ethelred.temperature4.sensors.SensorResult;
import org.jspecify.annotations.Nullable;

class FakeSensorMapping implements SensorMapping {
    private final Set<String> rooms = new LinkedHashSet<>();
    private final Map<String, SensorView> channelMap = new HashMap<>();

    /** Register room with a sensor reading at the given Fahrenheit temperature. */
    void addSensorRoom(String name, double fahrenheit) {
        rooms.add(name);
        var result = new SensorResult(OffsetDateTime.now(), "1", Temperature.fromFahrenheit(fahrenheit), true);
        channelMap.put(name, new DefaultSensorMapping.MappedSensorView(name, result));
    }

    /** Register room as sensor-equipped but with no reading (hasSensor=true, channelForRoom=null). */
    void addSensorRoom(String name) {
        rooms.add(name);
    }

    @Override
    public SensorView channelToRoom(SensorResult sensorResult) {
        throw new UnsupportedOperationException("not used in tests");
    }

    @Override
    public @Nullable SensorView channelForRoom(String room, Collection<SensorResult> sensorResults) {
        return channelMap.get(room);
    }

    @Override
    public List<SensorView> allChannels(Collection<SensorResult> sensorResults) {
        return List.copyOf(channelMap.values());
    }

    @Override
    public boolean hasSensor(String room) {
        return rooms.contains(room);
    }
}
