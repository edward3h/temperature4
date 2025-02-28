// (C) Edward Harman 2024
package org.ethelred.temperature4.kumojs;

import io.avaje.http.api.Client;
import io.avaje.http.api.Get;
import io.avaje.http.api.Put;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// @Retryable
@Client
public interface KumoJsClient {
    Logger LOGGER = LoggerFactory.getLogger(KumoJsClient.class);

    List<String> MODES = List.of("off", "heat", "cool");

    @Get("/rooms")
    List<String> getRoomList();

    @Get("/room/{room}/status")
    RoomStatus getRoomStatusEnc(String room);

    default RoomStatus getRoomStatus(String room) {
        return getRoomStatusEnc(encodeRoom(room));
    }

    default String encodeRoom(String room) {
        return room.replace(" ", "%20");
    }

    default void setMode(String room, String mode) {
        setModeEnc(encodeRoom(room), mode);
    }

    default void setTemperature(String room, String mode, int temperature) {
        setTemperatureEnc(encodeRoom(room), mode, temperature);
    }

    @Put("/room/{room}/mode/{mode}")
    void setModeEnc(String room, String mode);

    @Put("/room/{room}/{mode}/temp/{temperature}")
    void setTemperatureEnc(String room, String mode, int temperature);
}
