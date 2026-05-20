// (C) Edward Harman 2026
package org.ethelred.temperature4;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class FakeRoomService implements RoomService {
    private RoomsAndSensors roomsAndSensors = new RoomsAndSensors(List.of(), List.of());
    private final Map<String, RoomView> rooms = new LinkedHashMap<>();
    final List<String> updateRoomCalls = new ArrayList<>();

    void addRoom(RoomView view) {
        rooms.put(view.name(), view);
        List<NamedResult<RoomView>> namedRooms =
                rooms.values().stream().map(r -> (NamedResult<RoomView>) r).toList();
        roomsAndSensors = new RoomsAndSensors(namedRooms, List.of());
    }

    @Override
    public RoomsAndSensors getRoomsAndSensors() {
        return roomsAndSensors;
    }

    @Override
    public Optional<RoomView> getRoom(String name) {
        return Optional.ofNullable(rooms.get(name));
    }

    @Override
    public void updateRoom(String name, Mode mode, TemperatureSettingAction action) {
        updateRoomCalls.add(name + ":" + mode + ":" + action);
    }
}
