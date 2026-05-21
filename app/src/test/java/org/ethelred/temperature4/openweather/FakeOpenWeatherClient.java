// (C) Edward Harman 2026
package org.ethelred.temperature4.openweather;

import java.time.LocalDateTime;
import java.util.List;
import org.ethelred.temperature4.Temperature;

public class FakeOpenWeatherClient implements OpenWeatherClient {
    private final OpenWeatherResult result = new OpenWeatherResult(
            new Current(LocalDateTime.now(), Temperature.fromFahrenheit(0.0), List.of()), List.of());

    @Override
    public OpenWeatherResult getWeather() {
        return result;
    }
}
