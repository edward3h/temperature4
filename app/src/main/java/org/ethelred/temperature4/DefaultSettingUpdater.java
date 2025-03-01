// (C) Edward Harman 2025
package org.ethelred.temperature4;

import io.avaje.inject.RequiresProperty;
import jakarta.inject.Singleton;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import org.ethelred.temperature4.kumojs.KumoJsClient;
import org.ethelred.temperature4.sensors.SensorsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@RequiresProperty(value = "server.enableUpdate", equalTo = "true")
public class DefaultSettingUpdater implements SettingUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSettingUpdater.class);
    private static final int MAX_TEMP_SETTING = 85;
    private static final int MIN_TEMP_SETTING = 62;
    private final ScheduledExecutorService executorService;
    private final SettingRepository settingRepository;
    private final KumoJsClient kumoJsClient;
    private final SensorsClient sensorsClient;
    private final SensorMapping sensorMapping;

    public DefaultSettingUpdater(
            SettingRepository settingRepository,
            KumoJsClient kumoJsClient,
            SensorsClient sensorsClient,
            SensorMapping sensorMapping) {
        this.settingRepository = settingRepository;
        this.kumoJsClient = kumoJsClient;
        this.sensorsClient = sensorsClient;
        this.sensorMapping = sensorMapping;
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void start() {
        executorService.scheduleWithFixedDelay(this::checkForUpdates, 0, 5, TimeUnit.MINUTES);
    }

    @Override
    public void checkForUpdates() {
        var settings = settingRepository.findAll();
        if (settings.isEmpty()) {
            return;
        }
        var sensorResults = sensorsClient.getSensorResults();
        try (var scope = new StructuredTaskScope<>()) {
            for (var setting : settings) {
                scope.fork(() -> {
                    var sensorResult = sensorMapping.channelForRoom(setting.room(), sensorResults);
                    if (sensorResult != null) {
                        checkForUpdate(setting, sensorResult);
                    }
                    return null;
                });
            }
            scope.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkForUpdate(Setting setting, SensorView sensorResult) {
        try {
            var roomStatus = kumoJsClient.getRoomStatus(setting.room());
            var kumoMode = Mode.valueOf(roomStatus.mode());
            if (setting.mode() != kumoMode) {
                kumoJsClient.setMode(setting.room(), setting.mode().toString());
                LOGGER.info("Set mode {} for {}", setting.mode(), setting.room());
            }
            var kumoTemp = roomStatus.sp();
            var settingTemp = setting.settingFahrenheit();
            var sensorTemp = sensorResult.temperature().fahrenheit();
            if (sensorTemp < settingTemp && kumoTemp < MAX_TEMP_SETTING) {
                setTemperature(setting, sensorTemp, kumoTemp + 1);
            } else if (sensorTemp > settingTemp && kumoTemp > MIN_TEMP_SETTING) {
                setTemperature(setting, sensorTemp, kumoTemp - 1);
            }
        } catch (Exception e) {
            LOGGER.error("Unhandled exception", e);
        }
    }

    private void setTemperature(Setting setting, double sensorTemp, int newKumoTemp) {

        kumoJsClient.setTemperature(setting.room(), setting.mode().toString(), newKumoTemp);
        LOGGER.info(
                "Update temp for room {}. Setting {}, sensor {}, kumo {}",
                setting.room(),
                setting.settingFahrenheit(),
                sensorTemp,
                newKumoTemp);
    }
}
