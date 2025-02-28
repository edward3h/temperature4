// (C) Edward Harman 2025
package org.ethelred.temperature4.openweather;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import jakarta.inject.Singleton;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Singleton
public class OpenWeatherRepository {
    private final Object WEATHER_KEY = new Object();
    private final LoadingCache<Object, OpenWeatherResult> weatherResultCache;

    public OpenWeatherRepository(OpenWeatherClient client) {
        this.weatherResultCache =
                Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build(x -> client.getWeather());
    }

    public OpenWeatherResult getWeather() {
        return Objects.requireNonNull(weatherResultCache.get(WEATHER_KEY));
    }
}
