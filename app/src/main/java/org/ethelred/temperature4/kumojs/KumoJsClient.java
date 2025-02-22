// (C) Edward Harman 2024
package org.ethelred.temperature4.kumojs;

import io.avaje.http.api.Client;
import io.avaje.http.api.Get;
import io.avaje.http.api.Put;
import java.util.List;
import java.util.concurrent.StructuredTaskScope;
import org.ethelred.temperature4.ErrorNamedResult;
import org.ethelred.temperature4.NamedResult;
import org.ethelred.temperature4.RoomView;
import org.ethelred.temperature4.cache.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// @Retryable
@Client
public interface KumoJsClient {
    Logger LOGGER = LoggerFactory.getLogger(KumoJsClient.class);

    List<String> MODES = List.of("off", "heat", "cool");

    @Cacheable("roomlist")
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

    default List<NamedResult<RoomView>> getRoomStatuses() {
        return namedRoomStatuses(getRoomList());
    }

    private List<NamedResult<RoomView>> namedRoomStatuses(List<String> rooms) {
        try (var scope = new StructuredTaskScope<NamedResult<RoomView>>()) {
            var tasks = rooms.stream()
                    .map(r -> scope.fork(() -> namedRoomStatus(r)))
                    .toList();
            scope.join();
            return tasks.stream().map(StructuredTaskScope.Subtask::get).toList();
        } catch (InterruptedException e) {
            return List.of();
        }
    }

    private NamedResult<RoomView> namedRoomStatus(String room) {
        try {
            return new NamedRoomStatus(room, getRoomStatus(room));
        } catch (Exception e) {
            LOGGER.error("While getting room {}", room, e);
            return new ErrorNamedResult<>(room, e.getMessage());
        }
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
