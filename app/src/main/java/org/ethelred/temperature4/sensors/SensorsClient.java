// (C) Edward Harman 2024
package org.ethelred.temperature4.sensors;

import io.avaje.http.api.Client;
import io.avaje.http.api.Get;
import java.util.List;

@Client
public interface SensorsClient {
    //    @Cacheable("sensors")
    @Get
    List<SensorResult> getSensorResults();
}
