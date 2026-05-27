// (C) Edward Harman 2026
package org.ethelred.kumo;

import io.avaje.json.JsonReader;
import io.avaje.jsonb.Jsonb;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class KumoConfigParser {
    private final String configFilePath;

    public KumoConfigParser(String configFilePath) {
        this.configFilePath = configFilePath;
    }

    public List<KumoDeviceConfig> parse() throws IOException {
        var content = Files.readString(Path.of(configFilePath));
        var json = content.strip();
        if (json.startsWith("module.exports =")) {
            json = json.substring("module.exports =".length()).strip();
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
                reader.nextField(); // skip account email key
                reader.beginObject();
                while (reader.hasNextField()) {
                    reader.nextField(); // skip serial number key
                    result.add(parseDevice(reader));
                }
                reader.endObject();
            }
            reader.endObject();
        }
        return result;
    }

    private KumoDeviceConfig parseDevice(JsonReader reader) {
        reader.beginObject();
        String label = null;
        String address = null;
        byte[] password = null;
        byte[] cryptoSerial = null;
        int s = 0;
        byte[] w = null;
        while (reader.hasNextField()) {
            switch (reader.nextField()) {
                case "label" -> label = reader.readString();
                case "address" -> address = reader.readString();
                case "password" -> password = Base64.getDecoder().decode(reader.readString());
                case "cryptoSerial" -> cryptoSerial = hexToBytes(reader.readString());
                case "S" -> s = reader.readInt();
                case "W" -> w = hexToBytes(reader.readString());
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return new KumoDeviceConfig(label, address, password, cryptoSerial, s, w);
    }

    private static byte[] hexToBytes(String hex) {
        var bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex, i * 2, i * 2 + 2, 16);
        }
        return bytes;
    }
}
