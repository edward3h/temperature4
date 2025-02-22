// (C) Edward Harman 2025
package org.ethelred.temperature4.sensors;

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.CustomAdapter;
import io.avaje.jsonb.Jsonb;
import org.ethelred.temperature4.Temperature;

@CustomAdapter(global = false)
public class ScaledFahrenheitSerializer implements JsonAdapter<Temperature> {
    public ScaledFahrenheitSerializer(Jsonb ignore) {}

    @Override
    public void toJson(JsonWriter jsonWriter, Temperature temperature) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Temperature fromJson(JsonReader jsonReader) {
        return Temperature.fromScaledInt(jsonReader.readInt(), 1, Temperature.Unit.FAHRENHEIT);
    }
}
