// (C) Edward Harman 2024
package org.ethelred.temperature4.kumojs;

import org.ethelred.temperature4.RoomView;

public record NamedRoomStatus(String name, RoomStatus roomStatus) implements RoomView {
    public String mode() {
        return roomStatus.mode();
    }

    public String roomTemp() {
        return roomStatus.roomTemp() == null ? "-" : roomStatus.roomTemp().display();
    }

    public String displaySetting() {
        return roomStatus.setting();
    }
}
