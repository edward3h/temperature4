// (C) Edward Harman 2025
package org.ethelred.temperature4;

import java.util.List;
import org.jspecify.annotations.Nullable;

public interface SettingRepository {
    List<Setting> findAll();

    @Nullable Setting findByRoom(String room);

    void update(Setting setting);
}
