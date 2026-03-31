// (C) Edward Harman 2026
package org.ethelred.kumo;

import static com.google.common.truth.Truth.assertThat;

import io.avaje.jsonb.Jsonb;
import org.junit.jupiter.api.Test;

class KumoRoomStatusSerializerTest {
    String sample = """
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
            """;

    @Test
    void readSampleJson() {
        var jsonb = Jsonb.builder().build();
        var type = jsonb.type(KumoRoomStatus.class);
        var status = type.fromJson(sample);
        assertThat(status).isNotNull();
        assertThat(status.mode()).isEqualTo("heat");
        assertThat(status.roomTempCelsius()).isEqualTo(19.0);
        assertThat(status.spHeatCelsius()).isEqualTo(20.0);
        assertThat(status.spCoolCelsius()).isEqualTo(21.5);
        assertThat(status.fanSpeed()).isEqualTo("auto");
        assertThat(status.vaneDir()).isEqualTo("auto");
    }
}
