@import java.util.List
@import org.ethelred.temperature4.RoomView
@import org.ethelred.temperature4.UIRequestContext

@param UIRequestContext req
@param List<String> modeOptions
@param RoomView room

<div class="room">
    <div id="current" class="current-temp">$unsafe{room.roomTemp(req.temperatureUnit())}</div>
    <form method="post" data-hx-trigger="change from:.moderadio" data-hx-post="#" data-hx-target="#content">
        <div class="modes">
            @for(var mode: modeOptions)
                !{var selected = mode.equalsIgnoreCase(room.mode());}
                <input class="moderadio" type="radio" id="${mode}" name="mode" value="${mode}"
                            checked="${selected}"
                />
                <label for="${mode}" class="mode-option${selected ? " selected" : ""}">${mode}</label>
            @endfor
        </div>
        <div class="setting">
            <button type="button" class="button minus" name="setting" value="minus"
                    data-hx-post="#" data-hx-target="#content" data-hx-include="closest form">-</button>
            <span class="setting-value">$unsafe{room.displaySetting(req.temperatureUnit())}</span>
            <button type="button" class="button plus" name="setting" value="plus"
                    data-hx-post="#" data-hx-target="#content" data-hx-include="closest form">+</button>
        </div>
    </form>
    <div class="back"><a href="${req.contextPath()}">Back</a></div>
</div>

