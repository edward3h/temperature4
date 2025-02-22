// (C) Edward Harman 2025
package org.ethelred.temperature4.storage;

import java.util.Map;
import java.util.TreeMap;
import org.ethelred.temperature4.Setting;

public class StorageRoot {
    private Map<String, Setting> settings = new TreeMap<>();

    public Map<String, Setting> getSettings() {
        return settings;
    }

    @Override
    public String toString() {
        return "StorageRoot{" + "settings=" + settings + '}';
    }
}
