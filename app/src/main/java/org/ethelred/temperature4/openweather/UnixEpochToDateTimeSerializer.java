// (C) Edward Harman 2024
package org.ethelred.temperature4.openweather;

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.CustomAdapter;
import io.avaje.jsonb.Jsonb;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@CustomAdapter
public class UnixEpochToDateTimeSerializer implements JsonAdapter<LocalDateTime> {
    public UnixEpochToDateTimeSerializer(Jsonb ignore) {}

    @Override
    public void toJson(JsonWriter writer, LocalDateTime value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocalDateTime fromJson(JsonReader reader) {
        var value = reader.readLong();
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(value), ZoneId.systemDefault());
    }
}
