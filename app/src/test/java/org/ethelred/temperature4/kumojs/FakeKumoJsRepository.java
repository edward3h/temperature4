// (C) Edward Harman 2026
package org.ethelred.temperature4.kumojs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.ethelred.temperature4.NamedResult;
import org.ethelred.temperature4.RoomView;
import org.ethelred.temperature4.Temperature;

public class FakeKumoJsRepository implements KumoJsRepository {
    private final Map<String, RoomStatus> statuses = new LinkedHashMap<>();
    public final List<String> modeCallArgs = new ArrayList<>();
    public final List<String> tempCallArgs = new ArrayList<>();

    public void addRoom(String name, RoomStatus status) {
        statuses.put(name, status);
    }

    @Override
    public List<String> getRoomList() {
        return List.copyOf(statuses.keySet());
    }

    @Override
    public RoomStatus getRoomStatus(String name) {
        return statuses.get(name);
    }

    @Override
    public List<NamedResult<RoomView>> getRoomStatuses() {
        return statuses.entrySet().stream()
                .map(e -> (NamedResult<RoomView>) new NamedRoomStatus(e.getKey(), e.getValue()))
                .toList();
    }

    @Override
    public void setMode(String name, String mode) {
        modeCallArgs.add(name + ":" + mode);
    }

    @Override
    public void setTemperature(String name, String mode, int t) {
        tempCallArgs.add(name + ":" + mode + ":" + t);
        var old = statuses.get(name);
        if (old != null) {
            var newTemp = Temperature.fromFahrenheit(t);
            if ("cool".equalsIgnoreCase(mode)) {
                statuses.put(name, new RoomStatus(old.roomTemp(), old.mode(), newTemp, old.spHeat()));
            } else {
                statuses.put(name, new RoomStatus(old.roomTemp(), old.mode(), old.spCool(), newTemp));
            }
        }
    }
}
