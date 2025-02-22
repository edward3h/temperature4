// (C) Edward Harman 2025
package org.ethelred.temperature4.storage;

import io.avaje.inject.Primary;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import org.eclipse.serializer.concurrency.LockScope;
import org.ethelred.temperature4.Setting;
import org.ethelred.temperature4.SettingRepository;
import org.jspecify.annotations.Nullable;

@Singleton
@Primary
public class StorageSettingRepository extends LockScope implements SettingRepository {
    private final Storage storage;
    private Map<String, Setting> settings;

    public StorageSettingRepository(Storage storage) {
        this.storage = storage;
        this.settings = storage.getRoot().getSettings();
    }

    @Override
    public List<Setting> findAll() {
        return read(() -> List.copyOf(settings.values()));
    }

    @Override
    public @Nullable Setting findByRoom(String room) {
        return read(() -> settings.get(room));
    }

    @Override
    public void update(Setting setting) {
        write(() -> {
            settings.put(setting.room(), setting);
            storage.store(settings);
        });
    }
}
