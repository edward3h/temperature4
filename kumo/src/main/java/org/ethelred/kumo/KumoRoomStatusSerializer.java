// (C) Edward Harman 2026
package org.ethelred.kumo;

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.CustomAdapter;
import io.avaje.jsonb.Jsonb;

@CustomAdapter
public class KumoRoomStatusSerializer implements JsonAdapter<KumoRoomStatus> {

    public KumoRoomStatusSerializer(Jsonb ignore) {}

    @Override
    public void toJson(JsonWriter writer, KumoRoomStatus value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public KumoRoomStatus fromJson(JsonReader r) {
        var holder = new Object() {
            double roomTempCelsius = 0;
            String mode = "off";
            Double spCoolCelsius = null;
            Double spHeatCelsius = null;
            String fanSpeed = "auto";
            String vaneDir = "auto";
        };
        r.beginObject();
        descend(r, "r", () -> {
            descend(r, "indoorUnit", () -> {
                descend(r, "status", () -> {
                    while (r.hasNextField()) {
                        var field = r.nextField();
                        switch (field) {
                            case "roomTemp" -> holder.roomTempCelsius = r.readDouble();
                            case "mode" -> holder.mode = r.readString();
                            case "spCool" -> {
                                if (!r.isNullValue()) {
                                    holder.spCoolCelsius = r.readDouble();
                                }
                            }
                            case "spHeat" -> {
                                if (!r.isNullValue()) {
                                    holder.spHeatCelsius = r.readDouble();
                                }
                            }
                            case "fanSpeed" -> holder.fanSpeed = r.readString();
                            case "vaneDir" -> holder.vaneDir = r.readString();
                            default -> r.skipValue();
                        }
                    }
                });
            });
        });
        r.endObject();
        return new KumoRoomStatus(
                holder.roomTempCelsius,
                holder.mode,
                holder.spCoolCelsius,
                holder.spHeatCelsius,
                holder.fanSpeed,
                holder.vaneDir);
    }

    private void descend(JsonReader reader, String field, Runnable runnable) {
        if (reader.hasNextField() && field.equals(reader.nextField())) {
            reader.beginObject();
            runnable.run();
            reader.endObject();
        }
    }
}
