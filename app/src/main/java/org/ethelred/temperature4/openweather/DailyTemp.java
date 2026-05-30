// (C) Edward Harman 2024
package org.ethelred.temperature4.openweather;

import io.avaje.jsonb.Json;
import org.ethelred.temperature4.Temperature;

@Json
public record DailyTemp(Temperature min, Temperature max) {
    public String display() {
        return """
    <span class="low">%s</span>/<span class="high">%s</span>
    """.formatted(min.display(), max.display());
    }

    public String display(Temperature.Unit unit) {
        return """
    <span class="low">%s</span>/<span class="high">%s</span>
    """.formatted(min.display(unit), max.display(unit));
    }
}
