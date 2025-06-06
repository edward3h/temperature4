// (C) Edward Harman 2025
package org.ethelred.temperature4;

import io.avaje.http.api.Controller;
import io.avaje.http.api.Get;
import io.avaje.http.api.Post;
import java.util.List;

@Controller("/api/settings")
public class SettingController {
    private final SettingRepository settingRepository;

    public SettingController(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Get
    public List<Setting> getAll() {
        return settingRepository.findAll();
    }

    @Post
    public void update(Setting setting) {
        settingRepository.update(setting);
    }
}
