// (C) Edward Harman 2024
package org.ethelred.temperature4.openweather;

import io.avaje.jsonb.Json;
import java.util.List;
import org.ethelred.temperature4.Temperature;

@Json
public record OpenWeatherResult(Current current, List<Daily> daily) {
    public String minMax() {
        if (daily.isEmpty()) {
            return "";
        }
        var first = daily.getFirst();
        return first.temp().display();
    }

    public String minMax(Temperature.Unit unit) {
        if (daily.isEmpty()) {
            return "";
        }
        return daily.getFirst().temp().display(unit);
    }

    public String summary() {
        if (current.weather().isEmpty()) {
            return "";
        }
        var first = current.weather().getFirst();
        return first.main();
    }
}
