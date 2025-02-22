// (C) Edward Harman 2024
package org.ethelred.temperature4.sensors;

import io.avaje.jsonb.Json;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import org.ethelred.temperature4.Temperature;

@Json
public record SensorResult(
        OffsetDateTime time,
        String channel,
        @Json.Serializer(ScaledFahrenheitSerializer.class) Temperature temperature,
        boolean batteryOk) {
    public String age(Temporal now) {
        var duration = Duration.between(time, now).abs();
        long n = duration.toDays();
        if (n > 0) {
            return plural(n, "day");
        }
        n = duration.toHours();
        if (n > 0) {
            return plural(n, "hour");
        }
        n = duration.toMinutes();
        if (n > 0) {
            return plural(n, "minute");
        }
        return plural(n, "second");
    }

    private String plural(long n, String singular) {
        if (n > 1) {
            return n + " " + singular + "s";
        }
        return n + " " + singular;
    }
}
