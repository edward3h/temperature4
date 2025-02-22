// (C) Edward Harman 2024
package org.ethelred.temperature4.openweather;

import io.avaje.jsonb.Json;

@Json
public record Weather(int id, String main, String icon) {}
