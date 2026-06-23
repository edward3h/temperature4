// (C) Edward Harman 2026
package org.ethelred.kumoconfig;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class KumoCloudConfigParserTest {

    private final KumoCloudConfigParser parser = new KumoCloudConfigParser();

    @Test
    void parse_extractsUsernameAndNestedDevices() {
        // Index 0 carries the username; index 1 is unused; index 2 carries the
        // children tree. One device lives directly under index 2's children,
        // a second is nested one level deeper under that child's own "children".
        var response = """
                [
                  {"username":"user@example.com"},
                  {},
                  {"children":{
                    "site1":{
                      "zoneTable":{
                        "zone1":{"serial":"SERIAL1","label":"Room A","cryptoSerial":"abc123",
                                 "cryptoKeySet":"F","password":"pw1","address":"10.0.0.1"}
                      },
                      "children":{
                        "site2":{
                          "zoneTable":{
                            "zone2":{"serial":"SERIAL2","label":"Room B","cryptoSerial":"def456",
                                     "cryptoKeySet":"F","password":"pw2","address":"10.0.0.2"}
                          }
                        }
                      }
                    }
                  }}
                ]
                """;

        var result = parser.parse(response);

        assertThat(result.accountName()).isEqualTo("user@example.com");
        assertThat(result.devices()).hasSize(2);
        assertThat(result.devices().stream().map(KumoCloudDevice::label).toList())
                .containsExactly("Room A", "Room B");
        var roomA = result.devices().get(0);
        assertThat(roomA.serial()).isEqualTo("SERIAL1");
        assertThat(roomA.cryptoSerial()).isEqualTo("abc123");
        assertThat(roomA.cryptoKeySet()).isEqualTo("F");
        assertThat(roomA.password()).isEqualTo("pw1");
        assertThat(roomA.address()).isEqualTo("10.0.0.1");
    }

    @Test
    void parse_childWithNoChildrenField_doesNotRecurse() {
        var response = """
                [
                  {"username":"user@example.com"},
                  {},
                  {"children":{
                    "site1":{
                      "zoneTable":{
                        "zone1":{"serial":"SERIAL1","label":"Room A","cryptoSerial":"abc123",
                                 "cryptoKeySet":"F","password":"pw1","address":"10.0.0.1"}
                      }
                    }
                  }}
                ]
                """;

        var result = parser.parse(response);

        assertThat(result.devices()).hasSize(1);
    }

    @Test
    void parse_notAnArray_throwsKumoCloudException() {
        var exception = assertThrows(KumoCloudException.class, () -> parser.parse("{}"));

        assertThat(exception).hasMessageThat().contains("Unexpected response from Kumo Cloud");
    }

    @Test
    void parse_index2MissingChildren_throwsKumoCloudException() {
        var response = """
                [{"username":"user@example.com"},{},{}]
                """;

        var exception = assertThrows(KumoCloudException.class, () -> parser.parse(response));

        assertThat(exception).hasMessageThat().contains("Unexpected response from Kumo Cloud");
    }
}
