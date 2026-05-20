// (C) Edward Harman 2026
package org.ethelred.temperature4.sensors;

import java.util.List;

public interface SensorsRepository {
    List<SensorResult> getSensorResults();
}
