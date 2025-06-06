// (C) Edward Harman 2024
package org.ethelred.temperature4;

import jakarta.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.ethelred.temperature4.kumojs.KumoJsRepository;
import org.ethelred.temperature4.kumojs.NamedRoomStatus;
import org.ethelred.temperature4.kumojs.RoomStatus;
import org.ethelred.temperature4.sensors.SensorsRepository;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultRoomService implements RoomService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRoomService.class);
    private final SensorsRepository sensorsClient;
    private final SettingRepository settingRepository;
    private final SensorMapping sensorMapping;
    private final SettingUpdater settingUpdater;
    private final KumoJsRepository kumoJsRepository;

    public DefaultRoomService(
            SensorsRepository sensorsClient,
            SettingRepository settingRepository,
            SensorMapping sensorMapping,
            SettingUpdater settingUpdater,
            KumoJsRepository kumoJsRepository) {
        this.sensorsClient = sensorsClient;
        this.settingRepository = settingRepository;
        this.sensorMapping = sensorMapping;
        this.settingUpdater = settingUpdater;
        this.kumoJsRepository = kumoJsRepository;
    }

    @Override
    public RoomsAndSensors getRoomsAndSensors() {
        try (var scope = new StructuredTaskScope<>()) {
            var roomStatuses = scope.fork(kumoJsRepository::getRoomStatuses);
            var sensorResults = scope.fork(() -> sensorMapping.allChannels(sensorsClient.getSensorResults()));
            var settings = scope.fork(settingRepository::findAll);
            scope.join();
            return combine(new RoomsAndSensors(roomStatuses.get(), sensorResults.get()), settings.get());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<RoomView> getRoom(String name) {
        if (!kumoJsRepository.getRoomList().contains(name)) {
            return Optional.empty();
        }
        var roomStatus = kumoJsRepository.getRoomStatus(name);
        var sensorResult = sensorMapping.channelForRoom(name, sensorsClient.getSensorResults());
        var setting = settingRepository.findByRoom(name);
        return Optional.of(combine(name, roomStatus, sensorResult, setting));
    }

    @Override
    public void updateRoom(String name, Mode mode, TemperatureSettingAction action) {
        var roomStatus = kumoJsRepository.getRoomStatus(name);
        var setting = settingRepository.findByRoom(name);
        var currentMode = setting == null ? Mode.valueOf(roomStatus.mode()) : setting.mode();
        var currentTemp = setting == null ? roomStatus.sp() : setting.settingFahrenheit();
        if (mode != currentMode || action != TemperatureSettingAction.NONE) {
            var newTemp = action.apply(currentTemp);
            if (sensorMapping.hasSensor(name)) {
                settingRepository.update(new Setting(name, newTemp, mode));
                Thread.ofVirtual().start(settingUpdater::checkForUpdates);
            } else {
                kumoJsRepository.setMode(name, mode.toString());
                kumoJsRepository.setTemperature(name, mode.toString(), newTemp);
            }
        }
    }

    public RoomsAndSensors combine(RoomsAndSensors roomsAndSensors, Iterable<Setting> settings) {
        var sensorsByName =
                roomsAndSensors.sensors().stream().collect(Collectors.toMap(SensorView::name, Function.identity()));
        var settingByName = Util.mapBy(settings, Setting::room);
        var rooms = roomsAndSensors.rooms().stream()
                .map(room -> combineRoom(room, sensorsByName, settingByName.get(room.name())))
                .toList();
        return new RoomsAndSensors(rooms, roomsAndSensors.sensors());
    }

    public RoomView combine(String name, RoomStatus room, @Nullable SensorView sensorView, @Nullable Setting setting) {
        NamedRoomStatus roomStatus = new NamedRoomStatus(name, room);
        if (sensorView == null || setting == null) {
            return roomStatus;
        }
        return combineRoom(roomStatus, Map.of(name, sensorView), setting).get();
    }

    private NamedResult<RoomView> combineRoom(
            NamedResult<RoomView> room, Map<String, SensorView> sensorView, Setting setting) {
        LOGGER.debug("combineRoom {} {}", room, sensorView);
        if (room.success()) {
            return new CombinedRoom(room.get(), sensorView.get(room.name()), setting);
        }
        return room;
    }

    private record CombinedRoom(RoomView room, @Nullable SensorView sensorView, @Nullable Setting setting)
            implements RoomView {
        @Override
        public String name() {
            return room.name();
        }

        @Override
        public String roomTemp() {
            if (sensorView == null) {
                return room.roomTemp();
            }
            return """
                    %s <span class="ago">(%s)</span>
                    """
                    .formatted(sensorView.temperature().display(), room.roomTemp());
        }

        @Override
        public String mode() {
            return room.mode();
        }

        @Override
        public String displaySetting() {
            if (setting == null) {
                return room.displaySetting();
            }
            return """
                    %s <span class="ago">(%s)</span>
                    """
                    .formatted(setting.settingFahrenheit(), room.displaySetting());
        }
    }
}
