// (C) Edward Harman 2024
package org.ethelred.temperature4.openweather;

import io.avaje.http.api.Client;
import io.avaje.http.api.Get;
import org.ethelred.temperature4.cache.Cacheable;

@Client
public interface OpenWeatherClient {

    @Cacheable("openweather")
    @Get
    OpenWeatherResult getWeather();
}
