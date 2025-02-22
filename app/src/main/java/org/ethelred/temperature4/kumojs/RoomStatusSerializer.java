// (C) Edward Harman 2024
package org.ethelred.temperature4.kumojs;

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.CustomAdapter;
import io.avaje.jsonb.Jsonb;
import org.ethelred.temperature4.Temperature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CustomAdapter
public class RoomStatusSerializer implements JsonAdapter<RoomStatus> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomStatusSerializer.class);
    /*
    {
      "r": {
        "indoorUnit": {
          "status": {
            "roomTemp": 19,
            "mode": "heat",
            "spCool": 21.5,
            "spHeat": 20,
            "vaneDir": "auto",
            "fanSpeed": "auto",
            "tempSource": "unset",
            "activeThermistor": "unset",
            "filterDirty": false,
            "hotAdjust": false,
            "defrost": false,
            "standby": false,
            "runTest": 0,
            "humidTest": 0
          }
        }
      }
    }
    */

    public RoomStatusSerializer(Jsonb ignore) {}

    @Override
    public void toJson(JsonWriter writer, RoomStatus value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RoomStatus fromJson(JsonReader r) {
        var builder = RoomStatusBuilder.builder();
        r.beginObject();
        descend(r, "r", () -> {
            descend(r, "indoorUnit", () -> {
                descend(r, "status", () -> {
                    while (r.hasNextField()) {
                        var field = r.nextField();
                        switch (field) {
                            case "roomTemp" -> builder.roomTemp(new Temperature(r.readDouble()));
                            case "mode" -> builder.mode(r.readString());
                            case "spCool" -> {
                                if (!r.isNullValue()) {
                                    builder.spCool(new Temperature(r.readDouble()));
                                }
                            }
                            case "spHeat" -> {
                                if (!r.isNullValue()) {
                                    builder.spHeat(new Temperature(r.readDouble()));
                                }
                            }
                            default -> r.skipValue();
                        }
                    }
                });
            });
        });
        r.endObject();
        return builder.build();
    }

    private void descend(JsonReader reader, String field, Runnable runnable) {
        if (reader.hasNextField() && field.equals(reader.nextField())) {
            reader.beginObject();
            runnable.run();
            reader.endObject();
        }
    }
}
