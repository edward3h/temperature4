package org.ethelred.temperature4;

import io.avaje.inject.Secondary;
import jakarta.inject.Singleton;

@Singleton
@Secondary
public class NoOpSettingUpdater implements SettingUpdater {
    @Override
    public void start() {

    }

    @Override
    public void checkForUpdates() {

    }
}
