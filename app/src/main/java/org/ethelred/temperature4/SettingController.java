// (C) Edward Harman 2025
package org.ethelred.temperature4;

import io.avaje.http.api.Controller;
import io.avaje.http.api.Get;
import io.avaje.http.api.Post;
import java.util.List;

@Controller("/api/settings")
public class SettingController {
    private final SettingRepository settingRepository;
    private final SettingUpdater settingUpdater;

    public SettingController(SettingRepository settingRepository, SettingUpdater settingUpdater) {
        this.settingRepository = settingRepository;
        this.settingUpdater = settingUpdater;
    }

    @Get
    public List<Setting> getAll() {
        return settingRepository.findAll();
    }

    @Post
    public void update(Setting setting) {
        settingRepository.update(setting);
    }

    @Post("/checkForUpdates")
    public void check() {
        settingUpdater.checkForUpdates(false);
    }
}
