// (C) Edward Harman 2026
package org.ethelred.kumo;

import org.jspecify.annotations.Nullable;

public record KumoRoomStatus(
        double roomTempCelsius,
        String mode,
        @Nullable Double spCoolCelsius,
        @Nullable Double spHeatCelsius,
        String fanSpeed,
        String vaneDir) {}
