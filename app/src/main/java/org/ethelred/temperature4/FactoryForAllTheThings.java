// (C) Edward Harman 2025
package org.ethelred.temperature4;

import io.avaje.config.Config;
import io.avaje.config.Configuration;
import io.avaje.http.client.HttpClient;
import io.avaje.http.client.RequestListener;
import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import java.util.concurrent.Executors;
import org.ethelred.temperature4.kumojs.KumoJsClient;
import org.ethelred.temperature4.openweather.OpenWeatherClient;
import org.ethelred.temperature4.sensors.SensorsClient;
import org.ethelred.temperature4.template.StaticTemplates;
import org.ethelred.temperature4.template.Templates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Factory
public class FactoryForAllTheThings {
    private final Logger requestTimingLogger = LoggerFactory.getLogger("REQUEST_TIMING");

    @Bean
    public Configuration getConfiguration() {
        return Config.asConfiguration();
    }

    @Bean
    public Templates staticTemplates() {
        return new StaticTemplates();
    }

    private <T> T client(Class<T> type, Configuration configuration, String name) {
        return HttpClient.builder()
                .baseUrl(configuration.get(name + ".url"))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .requestListener(this::requestTiming)
                .build()
                .create(type);
    }

    private void requestTiming(RequestListener.Event event) {
        requestTimingLogger.info("{}: {}ms", event.uri().getPath(), event.responseTimeMicros() / 1000);
    }

    @Bean
    public KumoJsClient kumoJsClient(Configuration configuration) {
        return client(KumoJsClient.class, configuration, "kumojs");
    }

    @Bean
    public OpenWeatherClient openWeatherClient(Configuration configuration) {
        return client(OpenWeatherClient.class, configuration, "openweather");
    }

    @Bean
    public SensorsClient sensorsClient(Configuration configuration) {
        return client(SensorsClient.class, configuration, "sensors");
    }
}
