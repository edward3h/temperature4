// (C) Edward Harman 2026
package org.ethelred.kumoconfig;

import io.avaje.jsonb.Jsonb;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class KumoCloudClient {
    private static final URI LOGIN_URI = URI.create("https://geo-c.kumocloud.com/login");
    private static final String APP_VERSION = "2.2.0";

    private final URI loginUri;
    private final HttpClient httpClient;

    public KumoCloudClient() {
        this(LOGIN_URI);
    }

    KumoCloudClient(URI loginUri) {
        this.loginUri = loginUri;
        this.httpClient =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String login(String username, String password) {
        var body = buildLoginBody(username, password);
        var request = HttpRequest.newBuilder(loginUri)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "en-US,en")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new KumoCloudException("Unable to reach Kumo Cloud", e);
        }
        if (response.statusCode() / 100 != 2) {
            throw new KumoCloudException("Kumo Cloud login rejected (HTTP " + response.statusCode() + ")");
        }
        return response.body();
    }

    private String buildLoginBody(String username, String password) {
        var jsonb = Jsonb.builder().build();
        var out = new StringWriter();
        try (var writer = jsonb.writer(out)) {
            writer.beginObject();
            writer.name("username");
            writer.value(username);
            writer.name("password");
            writer.value(password);
            writer.name("appVersion");
            writer.value(APP_VERSION);
            writer.endObject();
        }
        return out.toString();
    }
}
