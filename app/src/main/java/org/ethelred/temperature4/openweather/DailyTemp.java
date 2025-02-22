// (C) Edward Harman 2024
package org.ethelred.temperature4.openweather;

import io.avaje.jsonb.Json;
import org.ethelred.temperature4.Temperature;

@Json
public record DailyTemp(Temperature min, Temperature max) {
    public String display() {
        return """
    <span class="low">%s</span>/<span class="high">%s</span>
    """
                .formatted(min.display(), max.display());
    }
}
