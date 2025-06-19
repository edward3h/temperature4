// (C) Edward Harman 2025
package org.ethelred.temperature4;

public interface SettingUpdater {
    void start();

    void checkForUpdates(boolean immediately);
}
