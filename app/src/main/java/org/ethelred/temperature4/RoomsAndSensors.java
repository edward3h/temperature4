// (C) Edward Harman 2025
package org.ethelred.temperature4;

import java.util.List;

public record RoomsAndSensors(List<NamedResult<RoomView>> rooms, List<SensorView> sensors) {}
