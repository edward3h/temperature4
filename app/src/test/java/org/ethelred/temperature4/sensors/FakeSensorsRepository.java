// (C) Edward Harman 2026
package org.ethelred.temperature4.sensors;

import java.util.List;

public class FakeSensorsRepository implements SensorsRepository {
    private List<SensorResult> results = List.of();

    public void setResults(List<SensorResult> results) {
        this.results = results;
    }

    @Override
    public List<SensorResult> getSensorResults() {
        return results;
    }
}
