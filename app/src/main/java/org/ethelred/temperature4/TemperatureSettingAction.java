// (C) Edward Harman 2025
package org.ethelred.temperature4;

import java.util.function.IntUnaryOperator;

public enum TemperatureSettingAction {
    NONE(i -> i),
    INCREMENT(i -> i + 2),
    DECREMENT(i -> i - 2);

    private final IntUnaryOperator op;

    TemperatureSettingAction(IntUnaryOperator op) {
        this.op = op;
    }

    int apply(int currentTemp) {
        return op.applyAsInt(currentTemp);
    }
}
