// (C) Edward Harman 2025
package org.ethelred.temperature4.sensors;

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.CustomAdapter;
import io.avaje.jsonb.Jsonb;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@CustomAdapter
public class ParseDateTimeSerializer implements JsonAdapter<OffsetDateTime> {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("u-M-d H:m:sZ");

    public ParseDateTimeSerializer(Jsonb ignore) {}

    @Override
    public void toJson(JsonWriter writer, OffsetDateTime value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OffsetDateTime fromJson(JsonReader reader) {
        return OffsetDateTime.parse(reader.readString(), formatter);
    }
}
