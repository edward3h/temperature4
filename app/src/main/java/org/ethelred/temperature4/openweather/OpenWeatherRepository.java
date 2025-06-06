// (C) Edward Harman 2025
package org.ethelred.temperature4.openweather;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import jakarta.inject.Singleton;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OpenWeatherRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenWeatherRepository.class);

    private final Object WEATHER_KEY = new Object();
    private final LoadingCache<Object, OpenWeatherResult> weatherResultCache;

    public OpenWeatherRepository(OpenWeatherClient client) {
        this.weatherResultCache = Caffeine.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .build(x -> client.getWeather());
    }

    public OpenWeatherResult getWeather() {
        LOGGER.debug("getWeather");
        return Objects.requireNonNull(weatherResultCache.get(WEATHER_KEY));
    }
}
