// (C) Edward Harman 2026
package org.ethelred.kumoconfig;

import io.avaje.json.JsonReader;
import io.avaje.jsonb.Jsonb;
import java.util.ArrayList;
import java.util.List;

public class KumoCloudConfigParser {

    public record Result(String accountName, List<KumoCloudDevice> devices) {}

    public Result parse(String responseBody) {
        var jsonb = Jsonb.builder().build();
        try (var reader = jsonb.reader(responseBody)) {
            return parseTopLevel(reader);
        } catch (RuntimeException e) {
            throw new KumoCloudException("Unexpected response from Kumo Cloud", e);
        }
    }

    private Result parseTopLevel(JsonReader reader) {
        reader.beginArray();
        String username = null;
        List<KumoCloudDevice> devices = null;
        int index = 0;
        while (reader.hasNextElement()) {
            switch (index) {
                case 0 -> username = readUsername(reader);
                case 2 -> devices = readIndexTwo(reader);
                default -> reader.skipValue();
            }
            index++;
        }
        reader.endArray();
        if (username == null || devices == null) {
            throw new KumoCloudException("Unexpected response from Kumo Cloud: missing username or children");
        }
        return new Result(username, devices);
    }

    private String readUsername(JsonReader reader) {
        reader.beginObject();
        String username = null;
        while (reader.hasNextField()) {
            if (reader.nextField().equals("username")) {
                username = reader.readString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return username;
    }

    private List<KumoCloudDevice> readIndexTwo(JsonReader reader) {
        reader.beginObject();
        List<KumoCloudDevice> devices = null;
        while (reader.hasNextField()) {
            if (reader.nextField().equals("children")) {
                devices = readChildren(reader);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return devices;
    }

    private List<KumoCloudDevice> readChildren(JsonReader reader) {
        var devices = new ArrayList<KumoCloudDevice>();
        reader.beginObject();
        while (reader.hasNextField()) {
            reader.nextField(); // opaque child id, not needed
            devices.addAll(readChild(reader));
        }
        reader.endObject();
        return devices;
    }

    private List<KumoCloudDevice> readChild(JsonReader reader) {
        var devices = new ArrayList<KumoCloudDevice>();
        reader.beginObject();
        while (reader.hasNextField()) {
            switch (reader.nextField()) {
                case "zoneTable" -> devices.addAll(readZoneTable(reader));
                case "children" -> devices.addAll(readChildren(reader));
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return devices;
    }

    private List<KumoCloudDevice> readZoneTable(JsonReader reader) {
        var devices = new ArrayList<KumoCloudDevice>();
        reader.beginObject();
        while (reader.hasNextField()) {
            reader.nextField(); // zone id, not needed
            devices.add(readDevice(reader));
        }
        reader.endObject();
        return devices;
    }

    private KumoCloudDevice readDevice(JsonReader reader) {
        reader.beginObject();
        String serial = null;
        String label = null;
        String cryptoSerial = null;
        String cryptoKeySet = null;
        String password = null;
        String address = null;
        while (reader.hasNextField()) {
            switch (reader.nextField()) {
                case "serial" -> serial = reader.readString();
                case "label" -> label = reader.readString();
                case "cryptoSerial" -> cryptoSerial = reader.readString();
                case "cryptoKeySet" -> cryptoKeySet = reader.readString();
                case "password" -> password = reader.readString();
                case "address" -> address = reader.readString();
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        if (serial == null
                || label == null
                || cryptoSerial == null
                || cryptoKeySet == null
                || password == null
                || address == null) {
            throw new KumoCloudException("Unexpected response from Kumo Cloud: device missing required field");
        }
        return new KumoCloudDevice(serial, label, cryptoSerial, cryptoKeySet, password, address);
    }
}
