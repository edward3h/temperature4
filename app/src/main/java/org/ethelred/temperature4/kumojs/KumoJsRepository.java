// (C) Edward Harman 2025
package org.ethelred.temperature4.kumojs;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.avaje.config.Configuration;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.ethelred.temperature4.ErrorNamedResult;
import org.ethelred.temperature4.NamedResult;
import org.ethelred.temperature4.RoomView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class KumoJsRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(KumoJsRepository.class);
    private final Object ROOM_LIST_KEY = new Object();
    private final Cache<Object, List<String>> roomListCache;
    private final Cache<String, RoomStatus> roomStatusCache;
    private final KumoJsClient client;

    public KumoJsRepository(Configuration configuration, KumoJsClient client) {
        this.client = client;
        this.roomListCache = Caffeine.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .expireAfterWrite(configuration.getLong("cache.roomlist.minutes", 15L), TimeUnit.MINUTES)
                .build();
        this.roomStatusCache = Caffeine.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .expireAfterWrite(configuration.getLong("cache.roomstatus.minutes", 4L), TimeUnit.MINUTES)
                .build();
    }

    public List<NamedResult<RoomView>> getRoomStatuses() {
        LOGGER.debug("getRoomStatuses");
        return namedRoomStatuses(getRoomList());
    }

    public List<String> getRoomList() {
        LOGGER.debug("getRoomList");
        return Objects.requireNonNull(roomListCache.get(ROOM_LIST_KEY, x -> client.getRoomList()));
    }

    public RoomStatus getRoomStatus(String name) {
        LOGGER.debug("getRoomStatus {}", name);
        return Objects.requireNonNull(roomStatusCache.get(name, client::getRoomStatus));
    }

    public void setMode(String name, String mode) {
        client.setMode(name, mode);
        roomStatusCache.invalidate(name);
    }

    public void setTemperature(String name, String mode, int newTemp) {
        client.setTemperature(name, mode, newTemp);
        roomStatusCache.invalidate(name);
    }

    private List<NamedResult<RoomView>> namedRoomStatuses(List<String> rooms) {
        return rooms.stream().map(this::namedRoomStatus).toList();
    }

    private NamedResult<RoomView> namedRoomStatus(String room) {
        try {
            return new NamedRoomStatus(room, getRoomStatus(room));
        } catch (Exception e) {
            LOGGER.error("While getting room {}", room, e);
            return new ErrorNamedResult<>(room, e.getMessage());
        }
    }
}
