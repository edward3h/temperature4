// (C) Edward Harman 2024
package org.ethelred.temperature4.openweather;

import io.avaje.jsonb.Json;
import java.time.LocalDateTime;
import java.util.List;
import org.ethelred.temperature4.Temperature;

@Json
public record Current(LocalDateTime dt, Temperature temp, /*Humidity humidity,*/ List<Weather> weather) {}
