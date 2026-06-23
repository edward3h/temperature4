// (C) Edward Harman 2026
package org.ethelred.kumoconfig;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import org.ethelred.kumo.KumoConfigParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KumoConfigWriterTest {

    @Test
    void write_roundTripsThroughExistingKumoConfigParser(@TempDir Path tempDir) throws Exception {
        var device = new KumoCloudDevice(
                "SERIAL001",
                "Living Room",
                "000102030405060708", // hex
                "F",
                Base64.getEncoder().encodeToString(new byte[] {1, 2, 3, 4}),
                "192.168.1.100");

        var configFile = tempDir.resolve("kumo.cfg");
        new KumoConfigWriter().write(configFile, "user@example.com", List.of(device));

        var devices = new KumoConfigParser(configFile.toString()).parse();

        assertThat(devices).hasSize(1);
        var parsed = devices.get(0);
        assertThat(parsed.label()).isEqualTo("Living Room");
        assertThat(parsed.address()).isEqualTo("192.168.1.100");
        assertThat(parsed.s()).isEqualTo(0);
        assertThat(parsed.password()).isEqualTo(new byte[] {1, 2, 3, 4});
        assertThat(parsed.cryptoSerial()).isEqualTo(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8});
        assertThat(parsed.w()).hasLength(32); // the fixed W constant decodes to 32 bytes
    }
}
