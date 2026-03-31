// (C) Edward Harman 2026
package org.ethelred.kumo;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KumoConfigParserTest {

    @Test
    void parse_validConfigWithModuleExports(@TempDir Path tempDir) throws Exception {
        var configFile = tempDir.resolve("kumo.cfg");
        Files.writeString(
                configFile,
                "module.exports = {\"account\":{\"hash\":{\"Living Room\":"
                        + "{\"address\":\"192.168.1.100\","
                        + "\"password\":[1,2,3,4],"
                        + "\"cryptoSerial\":[0,1,2,3,4,5,6,7,8],"
                        + "\"s\":2,"
                        + "\"w\":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]"
                        + "}}}};");
        var parser = new KumoConfigParser(configFile.toString());
        var devices = parser.parse();
        assertThat(devices).hasSize(1);
        var device = devices.get(0);
        assertThat(device.label()).isEqualTo("Living Room");
        assertThat(device.address()).isEqualTo("192.168.1.100");
        assertThat(device.s()).isEqualTo(2);
        assertThat(device.password()).isEqualTo(new byte[] {1, 2, 3, 4});
        assertThat(device.cryptoSerial()).isEqualTo(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8});
    }

    @Test
    void parse_plainJson(@TempDir Path tempDir) throws Exception {
        var configFile = tempDir.resolve("kumo.cfg");
        Files.writeString(
                configFile,
                "{\"account\":{\"hash\":{\"Bedroom\":"
                        + "{\"address\":\"192.168.1.101\","
                        + "\"password\":[5,6],"
                        + "\"cryptoSerial\":[0,1,2,3,4,5,6,7,8],"
                        + "\"s\":0,"
                        + "\"w\":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]"
                        + "}}}}");
        var parser = new KumoConfigParser(configFile.toString());
        var devices = parser.parse();
        assertThat(devices).hasSize(1);
        assertThat(devices.get(0).label()).isEqualTo("Bedroom");
        assertThat(devices.get(0).address()).isEqualTo("192.168.1.101");
    }

    @Test
    void parse_multipleRooms(@TempDir Path tempDir) throws Exception {
        var configFile = tempDir.resolve("kumo.cfg");
        Files.writeString(
                configFile,
                "{\"account\":{\"hash\":{"
                        + "\"Room A\":{\"address\":\"10.0.0.1\",\"password\":[1],\"cryptoSerial\":[0,1,2,3,4,5,6,7,8],\"s\":0,\"w\":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]},"
                        + "\"Room B\":{\"address\":\"10.0.0.2\",\"password\":[2],\"cryptoSerial\":[0,1,2,3,4,5,6,7,8],\"s\":0,\"w\":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]}"
                        + "}}}");
        var parser = new KumoConfigParser(configFile.toString());
        var devices = parser.parse();
        assertThat(devices).hasSize(2);
        assertThat(devices.stream().map(KumoDeviceConfig::label).toList()).containsExactly("Room A", "Room B");
    }
}
