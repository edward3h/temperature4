// (C) Edward Harman 2024
package org.ethelred.temperature4.openweather;

import io.avaje.jsonb.Json;
import java.time.LocalDateTime;
import java.util.List;

@Json
public record Daily(LocalDateTime dt, DailyTemp temp, List<Weather> weather) {}
