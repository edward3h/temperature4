// (C) Edward Harman 2026
package org.ethelred.temperature4;

import static com.google.common.truth.Truth.assertThat;

import io.avaje.jsonb.Jsonb;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SettingControllerTest {

    Javalin app;
    InMemorySettingRepository settingRepo;
    HttpClient http;
    int port;

    @BeforeEach
    void setUp() {
        settingRepo = new InMemorySettingRepository();
        var controller = new SettingController(settingRepo, new NoOpSettingUpdater());
        var jsonb = Jsonb.builder().build();
        app = Javalin.create(cfg -> cfg.registerPlugin(new SettingController$Route(controller, jsonb)));
        app.start(0);
        port = app.port();
        http = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        app.stop();
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void getAll_returnsEmptyList_initially() throws Exception {
        var response = http.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + "/api/settings"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("[]");
    }

    @Test
    void update_thenGetAll_returnsSetting() throws Exception {
        var postResponse = http.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + "/api/settings"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"room\":\"TestRoom\",\"settingFahrenheit\":70,\"mode\":\"heat\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(postResponse.statusCode()).isEqualTo(201);

        var response = http.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + "/api/settings"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.body()).contains("TestRoom");
        assertThat(response.body()).contains("heat");
    }

    @Test
    void checkForUpdates_returns201() throws Exception {
        var response = http.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + "/api/settings/checkForUpdates"))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);
    }
}
