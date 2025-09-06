// (C) Edward Harman 2025
package org.ethelred.temperature4.kumojs;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.avaje.config.Configuration;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.ethelred.temperature4.ErrorNamedResult;
import org.ethelred.temperature4.NamedResult;
import org.ethelred.temperature4.NoOpCache;
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
    private final Set<String> excludeRooms;

    public KumoJsRepository(Configuration configuration, KumoJsClient client) {
        this.client = client;
        this.excludeRooms = configuration.set().of("rooms.exclude").stream()
                .map(String::strip)
                .collect(Collectors.toSet());
        this.roomListCache = Caffeine.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .expireAfterWrite(configuration.getLong("cache.roomlist.minutes", 15L), TimeUnit.MINUTES)
                .build();
        long roomStatusMinutes = configuration.getLong("cache.roomstatus.minutes", 4L);
        this.roomStatusCache = roomStatusMinutes > 0L
                ? Caffeine.newBuilder()
                        .executor(Executors.newVirtualThreadPerTaskExecutor())
                        .expireAfterWrite(roomStatusMinutes, TimeUnit.MINUTES)
                        .build()
                : new NoOpCache<>();
    }

    public List<NamedResult<RoomView>> getRoomStatuses() {
        LOGGER.debug("getRoomStatuses");
        return namedRoomStatuses(getRoomList());
    }

    public List<String> getRoomList() {
        LOGGER.debug("getRoomList");
        return Objects.requireNonNull(roomListCache.get(ROOM_LIST_KEY, _ignore -> _filteredRoomList()));
    }

    private List<String> _filteredRoomList() {
        return client.getRoomList().stream()
                .filter(Predicate.not(excludeRooms::contains))
                .toList();
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
