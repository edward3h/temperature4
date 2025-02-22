// (C) Edward Harman 2024
package org.ethelred.temperature4;

import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.StructuredTaskScope;
import org.ethelred.temperature4.kumojs.KumoJsClient;
import org.ethelred.temperature4.sensors.SensorsClient;

@Singleton
public class DefaultRoomService implements RoomService {
    private final KumoJsClient kumoJsClient;
    private final SensorsClient sensorsClient;
    private final SettingRepository settingRepository;
    private final SensorMapping sensorMapping;
    private final RoomCombiner roomCombiner;
    private final SettingUpdater settingUpdater;

    public DefaultRoomService(
            KumoJsClient kumoJsClient,
            SensorsClient sensorsClient,
            SettingRepository settingRepository,
            SensorMapping sensorMapping,
            RoomCombiner roomCombiner,
            SettingUpdater settingUpdater) {
        this.kumoJsClient = kumoJsClient;
        this.sensorsClient = sensorsClient;
        this.settingRepository = settingRepository;
        this.sensorMapping = sensorMapping;
        this.roomCombiner = roomCombiner;
        this.settingUpdater = settingUpdater;
    }

    @Override
    public RoomsAndSensors getRoomsAndSensors() {
        try (var scope = new StructuredTaskScope<>()) {
            var roomStatuses = scope.fork(kumoJsClient::getRoomStatuses);
            var sensorResults = scope.fork(() -> sensorMapping.allChannels(sensorsClient.getSensorResults()));
            var settings = scope.fork(settingRepository::findAll);
            scope.join();
            return roomCombiner.combine(new RoomsAndSensors(roomStatuses.get(), sensorResults.get()), settings.get());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<RoomView> getRoom(String name) {
        if (!kumoJsClient.getRoomList().contains(name)) {
            return Optional.empty();
        }
        var roomStatus = kumoJsClient.getRoomStatus(name);
        var sensorResult = sensorMapping.channelForRoom(name, sensorsClient.getSensorResults());
        var setting = settingRepository.findByRoom(name);
        return Optional.of(roomCombiner.combine(name, roomStatus, sensorResult, setting));
    }

    @Override
    public void updateRoom(String name, Mode mode, TemperatureSettingAction action) {
        var roomStatus = kumoJsClient.getRoomStatus(name);
        var setting = settingRepository.findByRoom(name);
        var currentMode = setting == null ? Mode.valueOf(roomStatus.mode()) : setting.mode();
        var currentTemp = setting == null ? roomStatus.sp() : setting.settingFahrenheit();
        if (mode != currentMode || action != TemperatureSettingAction.NONE) {
            var newTemp = action.apply(currentTemp);
            if (sensorMapping.hasSensor(name)) {
                settingRepository.update(new Setting(name, newTemp, mode));
                settingUpdater.checkForUpdates();
            } else {
                kumoJsClient.setMode(name, mode.toString());
                kumoJsClient.setTemperature(name, mode.toString(), newTemp);
            }
        }
    }
}
