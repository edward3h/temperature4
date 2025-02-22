@import java.util.List
@import org.ethelred.temperature4.RoomView

@param List<String> modeOptions
@param RoomView room

<div id="current">$unsafe{room.roomTemp()}</div>
<form method="post" data-hx-trigger="change from:.moderadio" data-hx-post="#" data-hx-target="#content">
    @for(var mode: modeOptions)
        <input class="moderadio" type="radio" id="${mode}" name="mode" value="${mode}"
                    checked="${mode.equalsIgnoreCase(room.mode())}"
        />
        <label for="${mode}">${mode}</label>
    @endfor
    <div>
        <button name="setting" value="minus">-</button>
        <span>$unsafe{room.displaySetting()}</span>
        <button name="setting" value="plus">+</button>
    </div>
</form>
<div><a href="/">Back</a></div>

