// (C) Edward Harman 2024
package org.ethelred.temperature4;

import io.avaje.config.Configuration;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.ethelred.temperature4.sensors.SensorResult;
import org.jspecify.annotations.Nullable;

@Singleton
public class DefaultSensorMapping implements SensorMapping {
    private static final Comparator<SensorResult> LATEST_BY_CHANNEL = Comparator.<SensorResult, Integer>comparing(
                    r -> Integer.parseInt(r.channel()))
            .thenComparing(Comparator.comparing(SensorResult::time).reversed());
    private final List<String> sensorNames;

    public DefaultSensorMapping(Configuration configuration) {
        this.sensorNames = configuration.list().of("sensors.channels").stream()
                .map(String::strip)
                .toList();
    }

    private String channelToRoom(String channel) {
        try {
            int index = Integer.parseInt(channel) - 1;
            if (index < sensorNames.size()) {
                return sensorNames.get(index);
            }
        } catch (NumberFormatException e) {
            // ignore - use default below
        }
        return channel;
    }

    @Override
    public @Nullable SensorView channelForRoom(String room, Collection<SensorResult> sensorResults) {
        return sensorResults.stream()
                .filter(Predicate.not(tooOld()))
                .filter(r -> channelToRoom(r.channel()).equals(room))
                .map(r -> new MappedSensorView(room, r))
                .findFirst()
                .orElse(null);
    }

    private Predicate<SensorResult> tooOld() {
        var now = OffsetDateTime.now();
        return sensorResult -> {
            var ago = Duration.between(sensorResult.time(), now).abs();
            return ago.toHours() > 6;
        };
    }

    @Override
    public List<SensorView> allChannels(Collection<SensorResult> sensorResults) {
        var distinct = sensorResults.stream()
                .filter(Predicate.not(tooOld()))
                .collect(Collectors.groupingBy(SensorResult::channel, Collectors.maxBy(LATEST_BY_CHANNEL)))
                .values();
        return distinct.stream()
                .flatMap(Optional::stream)
                .sorted(LATEST_BY_CHANNEL)
                .map(this::channelToRoom)
                .toList();
    }

    @Override
    public boolean hasSensor(String room) {
        return sensorNames.contains(room);
    }

    @Override
    public SensorView channelToRoom(SensorResult sensorResult) {
        return new MappedSensorView(channelToRoom(sensorResult.channel()), sensorResult);
    }

    record MappedSensorView(String name, SensorResult result) implements SensorView {
        @Override
        public String channel() {
            return result.channel();
        }

        @Override
        public Temperature temperature() {
            return result.temperature();
        }

        @Override
        public boolean batteryOk() {
            return result.batteryOk();
        }

        @Override
        public String age(Temporal now) {
            return result.age(now);
        }

        @Override
        public OffsetDateTime time() {
            return result.time();
        }
    }
}
