// (C) Edward Harman 2024
package org.ethelred.temperature4;

import java.util.Optional;

public interface RoomService {

    RoomsAndSensors getRoomsAndSensors();

    Optional<RoomView> getRoom(String name);

    void updateRoom(String name, Mode mode, TemperatureSettingAction action);
}
