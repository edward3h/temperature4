// (C) Edward Harman 2026
package org.ethelred.temperature4.kumojs;

import java.util.List;
import org.ethelred.temperature4.NamedResult;
import org.ethelred.temperature4.RoomView;

public interface KumoJsRepository {
    List<NamedResult<RoomView>> getRoomStatuses();
    List<String> getRoomList();
    RoomStatus getRoomStatus(String name);
    void setMode(String name, String mode);
    void setTemperature(String name, String mode, int newTemp);
}
