// (C) Edward Harman 2024
package org.ethelred.temperature4;

import org.ethelred.temperature4.kumojs.RoomStatus;
import org.jspecify.annotations.Nullable;

public interface RoomCombiner {
    RoomsAndSensors combine(RoomsAndSensors roomsAndSensors, Iterable<Setting> settings);

    RoomView combine(String name, RoomStatus room, @Nullable SensorView sensorView, @Nullable Setting setting);
}
