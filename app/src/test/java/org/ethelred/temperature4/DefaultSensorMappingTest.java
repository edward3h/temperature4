// (C) Edward Harman 2026
package org.ethelred.temperature4;

import static com.google.common.truth.Truth.assertThat;

import io.avaje.config.Config;
import java.time.OffsetDateTime;
import java.util.List;
import org.ethelred.temperature4.sensors.SensorResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultSensorMappingTest {

    DefaultSensorMapping mapping;

    @BeforeEach
    void setUp() {
        mapping = new DefaultSensorMapping(Config.asConfiguration());
    }

    private SensorResult freshResult(String channel) {
        return new SensorResult(OffsetDateTime.now().minusMinutes(5), channel, Temperature.fromFahrenheit(68.0), true);
    }

    private SensorResult staleResult(String channel) {
        return new SensorResult(OffsetDateTime.now().minusHours(7), channel, Temperature.fromFahrenheit(65.0), true);
    }

    @Test
    void channelForRoom_mapsChannelOneToFirstName() {
        var result = mapping.channelForRoom("Living Room", List.of(freshResult("1")));
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Living Room");
    }

    @Test
    void channelForRoom_mapsChannelTwoToSecondName() {
        var result = mapping.channelForRoom("Bedroom", List.of(freshResult("2")));
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Bedroom");
    }

    @Test
    void channelForRoom_filtersStaleReadings() {
        var result = mapping.channelForRoom("Living Room", List.of(staleResult("1")));
        assertThat(result).isNull();
    }

    @Test
    void channelForRoom_acceptsFreshReadings() {
        var result = mapping.channelForRoom("Living Room", List.of(freshResult("1")));
        assertThat(result).isNotNull();
    }

    @Test
    void allChannels_deduplicatesByChannel_returnsNewest() {
        var older = new SensorResult(OffsetDateTime.now().minusHours(2), "1", Temperature.fromFahrenheit(65.0), true);
        var newer = new SensorResult(OffsetDateTime.now().minusMinutes(5), "1", Temperature.fromFahrenheit(70.0), true);
        var views = mapping.allChannels(List.of(older, newer));
        assertThat(views).hasSize(1);
        assertThat(views.get(0).temperature().fahrenheit()).isWithin(0.1).of(70.0);
    }

    @Test
    void hasSensor_trueForConfiguredRoom() {
        assertThat(mapping.hasSensor("Living Room")).isTrue();
    }

    @Test
    void hasSensor_falseForUnknownRoom() {
        assertThat(mapping.hasSensor("Garage")).isFalse();
    }
}
