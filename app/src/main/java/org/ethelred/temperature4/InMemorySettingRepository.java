// (C) Edward Harman 2025
package org.ethelred.temperature4;

import io.avaje.inject.Secondary;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

@Singleton
@Secondary
public class InMemorySettingRepository implements SettingRepository {
    private final Map<String, Setting> roomNameToSetting = new ConcurrentSkipListMap<>();

    @Override
    public List<Setting> findAll() {
        return List.copyOf(roomNameToSetting.values());
    }

    @Override
    public Setting findByRoom(String room) {
        return roomNameToSetting.get(room);
    }

    @Override
    public void update(Setting setting) {
        roomNameToSetting.put(setting.room(), setting);
    }
}
