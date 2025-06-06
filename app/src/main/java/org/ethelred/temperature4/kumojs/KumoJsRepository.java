// (C) Edward Harman 2025
package org.ethelred.temperature4.kumojs;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.avaje.config.Configuration;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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
    private final Semaphore help;

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
        this.help = new Semaphore(1);
    }

    private <I, O> Function<I, O> serialize(Function<I, O> inner) {
        return input -> {
            try {
                help.acquire();
                return inner.apply(input);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                help.release();
            }
        };
    }

    private void serialize(Runnable runnable) {
        try {
            help.acquire();
            runnable.run();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            help.release();
        }
    }

    public List<NamedResult<RoomView>> getRoomStatuses() {
        LOGGER.debug("getRoomStatuses");
        return namedRoomStatuses(getRoomList());
    }

    public List<String> getRoomList() {
        LOGGER.debug("getRoomList");
        return Objects.requireNonNull(roomListCache.get(ROOM_LIST_KEY, serialize(x -> client.getRoomList())));
    }

    public RoomStatus getRoomStatus(String name) {
        LOGGER.debug("getRoomStatus {}", name);
        return Objects.requireNonNull(roomStatusCache.get(name, serialize(client::getRoomStatus)));
    }

    public void setMode(String name, String mode) {
        serialize(() -> client.setMode(name, mode));
        roomStatusCache.invalidate(name);
    }

    public void setTemperature(String name, String mode, int newTemp) {
        serialize(() -> client.setTemperature(name, mode, newTemp));
        roomStatusCache.invalidate(name);
    }

    record RoomTask(String room, StructuredTaskScope.Subtask<NamedResult<RoomView>> task) {
        private NamedResult<RoomView> result() {
            return switch (task.state()) {
                case SUCCESS -> task.get();
                case FAILED -> {
                    LOGGER.error("While getting room {}", room, task.exception());
                    yield new ErrorNamedResult<>(room, task.exception());
                }
                case UNAVAILABLE -> new ErrorNamedResult<>(room, "Result unavailable");
            };
        }
    }

    private List<NamedResult<RoomView>> namedRoomStatuses(List<String> rooms) {
        try (var scope = new StructuredTaskScope<NamedResult<RoomView>>()) {
            var tasks = rooms.stream()
                    .map(r -> new RoomTask(r, scope.fork(() -> namedRoomStatus(r))))
                    .toList();
            scope.join();
            return tasks.stream().map(RoomTask::result).toList();
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
}
