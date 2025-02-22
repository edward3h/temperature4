// (C) Edward Harman 2024
package org.ethelred.temperature4.openweather;

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.CustomAdapter;
import io.avaje.jsonb.Jsonb;
import org.ethelred.temperature4.Temperature;

@CustomAdapter
public class DoubleFahrenheitSerializer implements JsonAdapter<Temperature> {
    public DoubleFahrenheitSerializer(Jsonb ignore) {}

    @Override
    public void toJson(JsonWriter jsonWriter, Temperature temperature) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Temperature fromJson(JsonReader jsonReader) {
        return Temperature.fromFahrenheit(jsonReader.readDouble());
    }
}
