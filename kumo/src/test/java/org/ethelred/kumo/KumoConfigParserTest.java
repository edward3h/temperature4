// (C) Edward Harman 2026
package org.ethelred.kumo;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KumoConfigParserTest {

    // base64([1,2,3,4]) = "AQIDBA=="
    // hex([0,1,2,3,4,5,6,7,8]) = "000102030405060708"
    // hex(32 zero bytes) = 64 zeros

    @Test
    void parse_plainJson(@TempDir Path tempDir) throws Exception {
        var configFile = tempDir.resolve("kumo.cfg");
        Files.writeString(
                configFile,
                "{\"user@example.com\":{\"SERIAL002\":{"
                        + "\"label\":\"Bedroom\","
                        + "\"address\":\"192.168.1.101\","
                        + "\"password\":\"BQY=\","
                        + "\"cryptoSerial\":\"000102030405060708\","
                        + "\"S\":0,"
                        + "\"W\":\"0000000000000000000000000000000000000000000000000000000000000000\""
                        + "}}}");
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
                "{\"user@example.com\":{"
                        + "\"SERIAL_A\":{\"label\":\"Room A\",\"address\":\"10.0.0.1\",\"password\":\"AQ==\",\"cryptoSerial\":\"000102030405060708\",\"S\":0,\"W\":\"0000000000000000000000000000000000000000000000000000000000000000\"},"
                        + "\"SERIAL_B\":{\"label\":\"Room B\",\"address\":\"10.0.0.2\",\"password\":\"Ag==\",\"cryptoSerial\":\"000102030405060708\",\"S\":0,\"W\":\"0000000000000000000000000000000000000000000000000000000000000000\"}"
                        + "}}");
        var parser = new KumoConfigParser(configFile.toString());
        var devices = parser.parse();
        assertThat(devices).hasSize(2);
        assertThat(devices.stream().map(KumoDeviceConfig::label).toList()).containsExactly("Room A", "Room B");
    }
}
