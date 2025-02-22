// (C) Edward Harman 2024
package org.ethelred.temperature4;

public interface RoomView extends NamedResult<RoomView> {

    String roomTemp();

    String mode();

    String displaySetting();

    @Override
    default RoomView get() {
        return this;
    }

    @Override
    default boolean success() {
        return true;
    }
}
