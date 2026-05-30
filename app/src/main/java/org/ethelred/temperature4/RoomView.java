// (C) Edward Harman 2024
package org.ethelred.temperature4;

public interface RoomView extends NamedResult<RoomView> {

    String roomTemp();

    default String roomTemp(Temperature.Unit unit) {
        return roomTemp();
    }

    String mode();

    String displaySetting();

    default String displaySetting(Temperature.Unit unit) {
        return displaySetting();
    }

    @Override
    default RoomView get() {
        return this;
    }

    @Override
    default boolean success() {
        return true;
    }
}
