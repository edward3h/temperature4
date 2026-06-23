// (C) Edward Harman 2026
package org.ethelred.kumoconfig;

import io.avaje.json.JsonWriter;
import io.avaje.jsonb.Jsonb;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class KumoConfigWriter {
    private static final int S = 0;
    private static final String W = "44c73283b498d432ff25f5c8e06a016aef931e68f0a00ea710e36e6338fb22db";

    public void write(Path path, String accountName, List<KumoCloudDevice> devices) throws IOException {
        var jsonb = Jsonb.builder().build();
        try (OutputStream out = Files.newOutputStream(path);
                var writer = jsonb.writer(out)) {
            writer.beginObject();
            writer.name(accountName);
            writer.beginObject();
            for (var device : devices) {
                writer.name(device.serial());
                writeDevice(writer, device);
            }
            writer.endObject();
            writer.endObject();
        }
    }

    private void writeDevice(JsonWriter writer, KumoCloudDevice device) {
        writer.beginObject();
        writer.name("serial");
        writer.value(device.serial());
        writer.name("label");
        writer.value(device.label());
        writer.name("cryptoSerial");
        writer.value(device.cryptoSerial());
        writer.name("cryptoKeySet");
        writer.value(device.cryptoKeySet());
        writer.name("password");
        writer.value(device.password());
        writer.name("address");
        writer.value(device.address());
        writer.name("S");
        writer.value(S);
        writer.name("W");
        writer.value(W);
        writer.endObject();
    }
}
