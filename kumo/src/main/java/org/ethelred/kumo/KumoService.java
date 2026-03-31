// (C) Edward Harman 2026
package org.ethelred.kumo;

import java.util.List;

public interface KumoService {
    List<String> getRoomList();

    KumoRoomStatus getRoomStatus(String roomLabel);

    void setMode(String roomLabel, String mode);

    void setFanSpeed(String roomLabel, String speed);

    void setVentDirection(String roomLabel, String direction);

    void setCoolTemperature(String roomLabel, int tempFahrenheit);

    void setHeatTemperature(String roomLabel, int tempFahrenheit);
}
