// (C) Edward Harman 2024
package org.ethelred.temperature4;

import jakarta.inject.Singleton;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.ethelred.temperature4.kumojs.NamedRoomStatus;
import org.ethelred.temperature4.kumojs.RoomStatus;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultRoomCombiner implements RoomCombiner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRoomCombiner.class);

    @Override
    public RoomsAndSensors combine(RoomsAndSensors roomsAndSensors, Iterable<Setting> settings) {
        var sensorsByName =
                roomsAndSensors.sensors().stream().collect(Collectors.toMap(SensorView::name, Function.identity()));
        var settingByName = Util.mapBy(settings, Setting::room);
        var rooms = roomsAndSensors.rooms().stream()
                .map(room -> combineRoom(room, sensorsByName, settingByName.get(room.name())))
                .toList();
        return new RoomsAndSensors(rooms, roomsAndSensors.sensors());
    }

    @Override
    public RoomView combine(String name, RoomStatus room, SensorView sensorView, Setting setting) {
        return combineRoom(new NamedRoomStatus(name, room), Map.of(name, sensorView), setting)
                .get();
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
