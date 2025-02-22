// (C) Edward Harman 2024
package org.ethelred.temperature4.openweather;

import io.avaje.jsonb.Json;
import java.util.List;

@Json
public record OpenWeatherResult(Current current, List<Daily> daily) {
    public String minMax() {
        if (daily.isEmpty()) {
            return "";
        }
        var first = daily.getFirst();
        return first.temp().display();
    }

    public String summary() {
        if (current.weather().isEmpty()) {
            return "";
        }
        var first = current.weather().getFirst();
        return first.main();
    }
}
