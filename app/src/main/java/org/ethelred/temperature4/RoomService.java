// (C) Edward Harman 2024
package org.ethelred.temperature4;

import java.util.Optional;

public interface RoomService {

    RoomsAndSensors getRoomsAndSensors();

    Optional<RoomView> getRoom(String name);

    Optional<RoomView> updateRoom(String name, Mode mode, TemperatureSettingAction action);
}
