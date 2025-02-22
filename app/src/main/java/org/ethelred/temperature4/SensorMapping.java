// (C) Edward Harman 2024
package org.ethelred.temperature4;

import java.util.Collection;
import java.util.List;
import org.ethelred.temperature4.sensors.SensorResult;
import org.jspecify.annotations.Nullable;

public interface SensorMapping {
    SensorView channelToRoom(SensorResult sensorResult);

    @Nullable SensorView channelForRoom(String room, Collection<SensorResult> sensorResults);

    List<SensorView> allChannels(Collection<SensorResult> sensorResults);

    boolean hasSensor(String room);
}
