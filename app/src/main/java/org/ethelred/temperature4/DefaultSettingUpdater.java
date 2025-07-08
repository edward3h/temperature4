// (C) Edward Harman 2025
package org.ethelred.temperature4;

import io.avaje.config.Configuration;
import io.avaje.inject.RequiresProperty;
import jakarta.inject.Singleton;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.ethelred.temperature4.kumojs.KumoJsRepository;
import org.ethelred.temperature4.sensors.SensorsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@RequiresProperty(value = "server.enableUpdate", equalTo = "true")
public class DefaultSettingUpdater implements SettingUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSettingUpdater.class);
    private final int MAX_TEMP_SETTING;
    private final int MIN_TEMP_SETTING;
    private final ScheduledExecutorService executorService;
    private final SettingRepository settingRepository;
    private final KumoJsRepository kumoJsClient;
    private final SensorsRepository sensorsClient;
    private final SensorMapping sensorMapping;

    private long lastUpdateTimeNanos = 0L;
    private final long UPDATE_DELAY = TimeUnit.MINUTES.toNanos(5L);

    public DefaultSettingUpdater(
            Configuration configuration,
            SettingRepository settingRepository,
            KumoJsRepository kumoJsClient,
            SensorsRepository sensorsClient,
            SensorMapping sensorMapping) {
        MAX_TEMP_SETTING = configuration.getInt("updater.maxtemp", 85);
        MIN_TEMP_SETTING = configuration.getInt("updater.mintemp", 62);
        this.settingRepository = settingRepository;
        this.kumoJsClient = kumoJsClient;
        this.sensorsClient = sensorsClient;
        this.sensorMapping = sensorMapping;
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void start() {
        executorService.scheduleWithFixedDelay(() -> checkForUpdates(false), 0, 5, TimeUnit.MINUTES);
    }

    @Override
    public void checkForUpdates(boolean immediately) {
        if (!runNow(immediately)) {
            return;
        }
        try {
            var settings = settingRepository.findAll();
            if (settings.isEmpty()) {
                return;
            }
            var sensorResults = sensorsClient.getSensorResults();
            for (var setting : settings) {
                var sensorResult = sensorMapping.channelForRoom(setting.room(), sensorResults);
                if (sensorResult != null) {
                    checkForUpdate(setting, sensorResult);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unhandled exception", e);
        }
    }

    private synchronized boolean runNow(boolean immediately) {
        var now = System.nanoTime();
        if (immediately || now - lastUpdateTimeNanos > UPDATE_DELAY) {
            lastUpdateTimeNanos = now;
            return true;
        }
        return false;
    }

    private void checkForUpdate(Setting setting, SensorView sensorResult) {
        try {
            var roomStatus = kumoJsClient.getRoomStatus(setting.room());
            var kumoMode = Mode.valueOf(roomStatus.mode());
            if (setting.mode() != kumoMode) {
                kumoJsClient.setMode(setting.room(), setting.mode().toString());
                LOGGER.info("Set mode {} for {}", setting.mode(), setting.room());
            }
            if (setting.mode() == Mode.off) {
                // no temperature in "off" mode!
                return;
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
