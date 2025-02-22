// (C) Edward Harman 2024
package org.ethelred.temperature4;

import io.avaje.jsonb.Json;

@Json
public record Humidity(double percentage) {}
