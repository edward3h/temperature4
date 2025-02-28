// (C) Edward Harman 2024
package org.ethelred.temperature4.openweather;

import io.avaje.http.api.Client;
import io.avaje.http.api.Get;

@Client
public interface OpenWeatherClient {

    @Get
    OpenWeatherResult getWeather();
}
