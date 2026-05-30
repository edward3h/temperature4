@import java.util.List
@import org.ethelred.temperature4.RoomView
@import org.ethelred.temperature4.UIRequestContext

@param UIRequestContext req
@param List<String> modeOptions
@param RoomView room

<div id="current">$unsafe{room.roomTemp(req.temperatureUnit())}</div>
<form method="post" data-hx-trigger="change from:.moderadio" data-hx-post="#" data-hx-target="#content">
    @for(var mode: modeOptions)
        <input class="moderadio" type="radio" id="${mode}" name="mode" value="${mode}"
                    checked="${mode.equalsIgnoreCase(room.mode())}"
        />
        <label for="${mode}">${mode}</label>
    @endfor
    <div>
        <button type="button" name="setting" value="minus"
                data-hx-post="#" data-hx-target="#content" data-hx-include="closest form">-</button>
        <span>$unsafe{room.displaySetting(req.temperatureUnit())}</span>
        <button type="button" name="setting" value="plus"
                data-hx-post="#" data-hx-target="#content" data-hx-include="closest form">+</button>
    </div>
</form>
<div><a href="${req.contextPath()}">Back</a></div>

