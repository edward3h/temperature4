// (C) Edward Harman 2025
package org.ethelred.temperature4.kumojs;

import static com.google.common.truth.Truth.assertThat;

import io.avaje.jsonb.Jsonb;
import org.junit.jupiter.api.Test;

public class RoomStatusSerializerTest {
    String sample =
            """
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
    public void readSampleJson() {
        var json = Jsonb.builder().build();
        var type = json.type(RoomStatus.class);
        var status = type.fromJson(sample);
        assertThat(status).isNotNull();
        assertThat(status.mode()).isEqualTo("heat");
        assertThat(status.setting()).isEqualTo("68");
    }
}
