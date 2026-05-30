// (C) Edward Harman 2024
package org.ethelred.temperature4.kumojs;

import org.ethelred.temperature4.RoomView;
import org.ethelred.temperature4.Temperature;

public record NamedRoomStatus(String name, RoomStatus roomStatus) implements RoomView {
    public String mode() {
        return roomStatus.mode();
    }

    public String roomTemp() {
        return roomStatus.roomTemp() == null ? "-" : roomStatus.roomTemp().display();
    }

    @Override
    public String roomTemp(Temperature.Unit unit) {
        return roomStatus.roomTemp() == null ? "-" : roomStatus.roomTemp().display(unit);
    }

    public String displaySetting() {
        return roomStatus.setting();
    }

    @Override
    public String displaySetting(Temperature.Unit unit) {
        return roomStatus.setting(unit);
    }
}
