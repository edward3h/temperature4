// (C) Edward Harman 2024
package org.ethelred.temperature4;

import io.avaje.jsonb.Json;

@Json
public record Setting(String room, int settingFahrenheit, Mode mode) {}
