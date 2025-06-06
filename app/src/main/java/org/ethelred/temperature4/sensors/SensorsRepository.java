// (C) Edward Harman 2025
package org.ethelred.temperature4.sensors;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.avaje.config.Configuration;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Singleton
public class SensorsRepository {
    private final SensorsClient client;
    private final Object RESULT_KEY = new Object();
    private final Cache<Object, List<SensorResult>> cache;

    public SensorsRepository(Configuration configuration, SensorsClient client) {
        this.client = client;
        this.cache = Caffeine.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .expireAfterWrite(configuration.getLong("cache.sensors.minutes", 5L), TimeUnit.MINUTES)
                .build();
    }

    public List<SensorResult> getSensorResults() {
        return Objects.requireNonNull(cache.get(RESULT_KEY, x -> client.getSensorResults()));
    }
}
