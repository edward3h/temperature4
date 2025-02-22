@import java.time.OffsetDateTime
@import java.util.List
@import org.ethelred.temperature4.ErrorNamedResult
@import org.ethelred.temperature4.NamedResult
@import org.ethelred.temperature4.RoomView
@import org.ethelred.temperature4.SensorView
@import org.ethelred.temperature4.openweather.OpenWeatherResult

@param List<NamedResult<RoomView>> rooms
@param OpenWeatherResult weather
@param List<SensorView> sensors

!{var now = OffsetDateTime.now();}
<table>
    <tr><th>Room</th><th>Current</th><th>Mode</th><th>Setting</th></tr>
    @for(var result: rooms)
        @if(result.success() && result instanceof RoomView room)
            <tr><td><a href="/room/${room.name()}">${room.name()}</a></td><td>$unsafe{room.roomTemp()}</td><td>${room.mode()}</td><td>$unsafe{room.displaySetting()}</td></tr>
        @elseif(result instanceof ErrorNamedResult<RoomView> error)
            <tr><td><a href="/room/${result.name()}">${result.name()}</a></td><td colspan="3">${error.message()}</td></tr>
        @endif
    @endfor
</table>
<h2>Weather</h2>
<table class="weather"><tr>
    <td>${weather.current().temp().display()}</td>
    <td>$unsafe{weather.minMax()}</td>
    <td>${weather.summary()}</td>
    </tr>
</table>
<h2>Sensors</h2>
<table>
    <tr><th>#</th><th>Name</th><th>Current</th><th class="ago">Battery</th><th class="ago">Last Reading</th></tr>
    @for(var sensor: sensors)
        <tr><td>${sensor.channel()}</td><td>${sensor.name()}</td><td>${sensor.temperature().display()}</td><td class="ago">$unsafe{sensor.batteryOk() ? "&#x1FAAB;" : "&#x1F50B;"}</td><td class="ago">${sensor.age(now)}</td></tr>
    @endfor
</table>