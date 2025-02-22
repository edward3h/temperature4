// (C) Edward Harman 2024
package org.ethelred.temperature4.kumojs;

import io.soabase.recordbuilder.core.RecordBuilderFull;
import org.ethelred.temperature4.Temperature;
import org.jspecify.annotations.Nullable;

// @Json
@RecordBuilderFull
public record RoomStatus(
        Temperature roomTemp, String mode, @Nullable Temperature spCool, @Nullable Temperature spHeat) {
    public String setting() {
        if ("heat".equalsIgnoreCase(mode()) && spHeat != null) {
            return spHeat.display();
        }
        if ("cool".equalsIgnoreCase(mode()) && spCool != null) {
            return spCool.display();
        }
        return "--";
    }

    public int sp() {
        if ("heat".equalsIgnoreCase(mode()) && spHeat != null) {
            return toInt(spHeat);
        }
        if ("cool".equalsIgnoreCase(mode()) && spCool != null) {
            return toInt(spCool);
        }
        return toInt(roomTemp);
    }

    private int toInt(Temperature temperature) {
        return (int) Math.round(temperature.fahrenheit());
    }
}
