// (C) Edward Harman 2025
package org.ethelred.temperature4.kumojs;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.avaje.config.Configuration;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.StructuredTaskScope;
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
    private final KumoJsClient client;

    public KumoJsRepository(Configuration configuration, KumoJsClient client) {
        this.client = client;
        this.roomListCache = Caffeine.newBuilder()
                .expireAfterWrite(configuration.getLong("cache.roomlist.minutes", 15L), TimeUnit.MINUTES)
                .build();
    }

    public List<NamedResult<RoomView>> getRoomStatuses() {
        return namedRoomStatuses(getRoomList());
    }

    private List<String> getRoomList() {
        return Objects.requireNonNull(roomListCache.get(ROOM_LIST_KEY, x -> client.getRoomList()));
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
            return new NamedRoomStatus(room, client.getRoomStatus(room));
        } catch (Exception e) {
            LOGGER.error("While getting room {}", room, e);
            return new ErrorNamedResult<>(room, e.getMessage());
        }
    }
}
