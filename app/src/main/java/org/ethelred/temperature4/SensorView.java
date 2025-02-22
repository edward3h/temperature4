// (C) Edward Harman 2024
package org.ethelred.temperature4;

import java.time.OffsetDateTime;
import java.time.temporal.Temporal;

public interface SensorView {
    String channel();

    String name();

    Temperature temperature();

    boolean batteryOk();

    String age(Temporal now);

    OffsetDateTime time();
}
