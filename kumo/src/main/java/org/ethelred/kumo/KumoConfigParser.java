// (C) Edward Harman 2026
package org.ethelred.kumo;

import io.avaje.json.JsonReader;
import io.avaje.jsonb.Jsonb;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class KumoConfigParser {
    private final String configFilePath;

    public KumoConfigParser(String configFilePath) {
        this.configFilePath = configFilePath;
    }

    public List<KumoDeviceConfig> parse() throws IOException {
        var content = Files.readString(Path.of(configFilePath));
        var json = content.strip();
        if (json.startsWith("module.exports = ")) {
            json = json.substring("module.exports = ".length()).strip();
        }
        if (json.endsWith(";")) {
            json = json.substring(0, json.length() - 1).strip();
        }
        return parseJson(json);
    }

    private List<KumoDeviceConfig> parseJson(String json) {
        var jsonb = Jsonb.builder().build();
        var result = new ArrayList<KumoDeviceConfig>();
        try (var reader = jsonb.reader(json)) {
            reader.beginObject();
            while (reader.hasNextField()) {
                if ("account".equals(reader.nextField())) {
                    reader.beginObject();
                    while (reader.hasNextField()) {
                        if ("hash".equals(reader.nextField())) {
                            reader.beginObject();
                            while (reader.hasNextField()) {
                                var label = reader.nextField();
                                result.add(parseDevice(label, reader));
                            }
                            reader.endObject();
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        }
        return result;
    }

    private KumoDeviceConfig parseDevice(String label, JsonReader reader) {
        reader.beginObject();
        String address = null;
        byte[] password = null;
        byte[] cryptoSerial = null;
        int s = 0;
        byte[] w = null;
        while (reader.hasNextField()) {
            switch (reader.nextField()) {
                case "address" -> address = reader.readString();
                case "password" -> password = readByteArray(reader);
                case "cryptoSerial" -> cryptoSerial = readByteArray(reader);
                case "s" -> s = reader.readInt();
                case "w" -> w = readByteArray(reader);
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return new KumoDeviceConfig(label, address, password, cryptoSerial, s, w);
    }

    private byte[] readByteArray(JsonReader reader) {
        var list = new ArrayList<Integer>();
        reader.beginArray();
        while (reader.hasNextElement()) {
            list.add(reader.readInt());
        }
        reader.endArray();
        var bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            bytes[i] = (byte) (int) list.get(i);
        }
        return bytes;
    }
}
