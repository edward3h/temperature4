// (C) Edward Harman 2026
package org.ethelred.temperature4;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

class TemperatureTest {

    @Test
    void freezingPoint() {
        var t = new Temperature(0.0);
        assertThat(t.fahrenheit()).isWithin(0.001).of(32.0);
    }

    @Test
    void boilingPoint() {
        var t = new Temperature(100.0);
        assertThat(t.fahrenheit()).isWithin(0.001).of(212.0);
    }

    @Test
    void fromFahrenheit_roundTrip() {
        var t = Temperature.fromFahrenheit(72.0);
        assertThat(t.fahrenheit()).isWithin(0.001).of(72.0);
    }

    @Test
    void fromScaledInt_celsius() {
        var t = Temperature.fromScaledInt(2150, 2, Temperature.Unit.CELSIUS);
        assertThat(t.celsius()).isWithin(0.001).of(21.5);
    }

    @Test
    void display_roundsToNearestFahrenheit() {
        // 21.3°C = 70.34°F → rounds to 70
        var t = new Temperature(21.3);
        assertThat(t.display()).isEqualTo("70");
    }

    @Test
    void display_unit_fahrenheit() {
        // 21.3°C = 70.34°F → rounds to 70
        var t = new Temperature(21.3);
        assertThat(t.display(Temperature.Unit.FAHRENHEIT)).isEqualTo("70");
    }

    @Test
    void display_unit_celsius() {
        // 21.3°C rounds to 21
        var t = new Temperature(21.3);
        assertThat(t.display(Temperature.Unit.CELSIUS)).isEqualTo("21");
    }
}
