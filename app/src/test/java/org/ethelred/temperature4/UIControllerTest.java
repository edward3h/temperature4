// (C) Edward Harman 2026
package org.ethelred.temperature4;

import static com.google.common.truth.Truth.assertThat;

import io.avaje.config.Config;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.ethelred.temperature4.openweather.FakeOpenWeatherClient;
import org.ethelred.temperature4.openweather.OpenWeatherRepository;
import org.ethelred.temperature4.template.StaticTemplates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UIControllerTest {

    Javalin app;
    FakeRoomService fakeRoomService;
    HttpClient http;
    int port;

    @BeforeEach
    void setUp() {
        fakeRoomService = new FakeRoomService();
        var weatherRepo = new OpenWeatherRepository(new FakeOpenWeatherClient());
        var controller =
                new UIController(Config.asConfiguration(), new StaticTemplates(), weatherRepo, fakeRoomService);
        app = Javalin.create(cfg -> cfg.registerPlugin(new UIController$Route(controller)));
        app.start(0);
        port = app.port();
        http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @AfterEach
    void tearDown() {
        app.stop();
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void getIndex_returns200() throws Exception {
        var response = http.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + "/")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type").orElse("")).contains("text/html");
    }

    @Test
    void getRoom_returns200_forKnownRoom() throws Exception {
        fakeRoomService.addRoom(new SimpleRoomView("TestRoom"));
        var response = http.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + "/room/TestRoom"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void getRoom_returns404_forUnknownRoom() throws Exception {
        var response = http.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + "/room/NoSuchRoom"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void postRoom_redirects_afterUpdate() throws Exception {
        fakeRoomService.addRoom(new SimpleRoomView("TestRoom"));
        var response = http.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + "/room/TestRoom"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString("mode=heat"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(303);
        assertThat(response.headers().firstValue("location").orElse("")).contains("/room/TestRoom");
    }

    private record SimpleRoomView(String name) implements RoomView {
        @Override
        public String roomTemp() {
            return "70";
        }

        @Override
        public String mode() {
            return "heat";
        }

        @Override
        public String displaySetting() {
            return "70";
        }
    }
}
