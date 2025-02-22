// (C) Edward Harman 2024
package org.ethelred.temperature4;

public record Temperature(double celsius) {
    public enum Unit {
        CELSIUS("C"),
        FAHRENHEIT("F") {
            @Override
            double toCelsius(double v) {
                return (v - 32.0) * 5.0 / 9.0;
            }
        };

        private final String symbol;

        Unit(String symbol) {

            this.symbol = symbol;
        }

        double toCelsius(double v) {
            return v;
        }

        public String symbol() {
            return symbol;
        }
    }

    public Temperature(double value, Unit unit) {
        this(unit.toCelsius(value));
    }

    public static Temperature fromScaledInt(int value, int scale, Unit unit) {
        return new Temperature(unit.toCelsius(((double) value) / Math.pow(10, scale)));
    }

    public static Temperature fromFahrenheit(double value) {
        return new Temperature(value, Unit.FAHRENHEIT);
    }

    public double fahrenheit() {
        return celsius * 9.0 / 5.0 + 32.0;
    }

    public double temperature(Unit unit) {
        return switch (unit) {
            case FAHRENHEIT -> fahrenheit();
            case CELSIUS -> celsius();
        };
    }

    public String display() {
        // hard coded to F for now.
        return String.valueOf(Math.round(fahrenheit()));
    }
}
