// (C) Edward Harman 2024
package org.ethelred.temperature4;

import gg.jte.models.runtime.*;
import io.avaje.config.Configuration;
import io.avaje.http.api.Consumes;
import io.avaje.http.api.Controller;
import io.avaje.http.api.Form;
import io.avaje.http.api.Get;
import io.avaje.http.api.Header;
import io.avaje.http.api.MediaType;
import io.avaje.http.api.Post;
import io.avaje.http.api.Produces;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.concurrent.StructuredTaskScope;
import org.ethelred.temperature4.kumojs.KumoJsClient;
import org.ethelred.temperature4.openweather.OpenWeatherClient;
import org.ethelred.temperature4.sensors.SensorsClient;
import org.ethelred.temperature4.template.Templates;
import org.jspecify.annotations.Nullable;

@Controller
public class UIController {
    private final String contextPath;
    private final Templates templates;
    private final KumoJsClient kumoJsClient;
    private final OpenWeatherClient openWeatherClient;
    private final SettingRepository settingRepository;
    private final RoomService roomService;
    private final SensorMapping sensorMapping;

    public UIController(
            Configuration configuration,
            Templates templates,
            KumoJsClient kumoJsClient,
            OpenWeatherClient openWeatherClient,
            SensorsClient sensorsClient,
            SettingRepository settingRepository,
            RoomService roomService,
            SensorMapping sensorMapping) {
        this.templates = templates;
        this.kumoJsClient = kumoJsClient;
        this.openWeatherClient = openWeatherClient;
        this.roomService = roomService;
        this.sensorMapping = sensorMapping;
        this.settingRepository = settingRepository;
        this.contextPath = configuration.get("server.contextPath", "/");
    }

    @Get("/")
    @Produces(MediaType.TEXT_HTML)
    public String index(@Header("hx-request") Boolean htmx, Context javalinContext) {
        try (var scope = new StructuredTaskScope<>()) {
            var weather = scope.fork(openWeatherClient::getWeather);
            var roomsAndSensors = scope.fork(roomService::getRoomsAndSensors);
            scope.join();
            var sensors = roomsAndSensors.get().sensors();
            var roomViews = roomsAndSensors.get().rooms();
            var req = new UIRequestContext("", "index", contextPath);
            return withLayout(htmx, req, templates.index(req, roomViews, weather.get(), sensors), javalinContext);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Get("/room/{room}")
    @Produces(MediaType.TEXT_HTML)
    public String room(String room, @Header("hx-request") Boolean htmx, Context javalinContext) {

        var status = roomService.getRoom(room);
        if (status.isPresent()) {
            var req = new UIRequestContext(room, "room", contextPath);
            return withLayout(htmx, req, templates.room(req, KumoJsClient.MODES, status.get()), javalinContext);
        }
        javalinContext.status(HttpStatus.NOT_FOUND);
        return "";
    }

    @Post("/room/{room}")
    @Form
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void updateRoom(String room, String mode, @Nullable String setting, Context javalinContext) {

        var modeE = Mode.valueOf(mode);
        var action = TemperatureSettingAction.NONE;
        if ("plus".equalsIgnoreCase(setting)) {
            action = TemperatureSettingAction.INCREMENT;
        }
        if ("minus".equalsIgnoreCase(setting)) {
            action = TemperatureSettingAction.DECREMENT;
        }
        roomService.updateRoom(room, modeE, action);
        javalinContext.redirect("/room/" + room, HttpStatus.SEE_OTHER);
    }

    String withLayout(@Nullable Boolean htmx, UIRequestContext req, JteModel content, Context javalinContext) {
        javalinContext.contentType(ContentType.TEXT_HTML);
        if (htmx != null && htmx) {
            return writable(content);
        } else {
            return writable(templates.layout(req, content));
        }
    }

    static String writable(JteModel model) {
        return model.render();
    }
}
