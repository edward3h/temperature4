// (C) Edward Harman 2026
package org.ethelred.kumo;

import io.avaje.jsonb.Jsonb;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KumoServiceImpl implements KumoService {
    private final Map<String, KumoDeviceConfig> devices;
    private final HttpClient httpClient;
    private final KumoCrypto crypto;
    private final Jsonb jsonb;

    public KumoServiceImpl(String configFilePath) {
        try {
            var parser = new KumoConfigParser(configFilePath);
            var deviceList = parser.parse();
            this.devices = deviceList.stream().collect(Collectors.toMap(KumoDeviceConfig::label, d -> d));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load kumo config from " + configFilePath, e);
        }
        this.httpClient = HttpClient.newBuilder().build();
        this.crypto = new KumoCrypto();
        this.jsonb = Jsonb.builder().build();
    }

    @Override
    public List<String> getRoomList() {
        return List.copyOf(devices.keySet());
    }

    @Override
    public KumoRoomStatus getRoomStatus(String roomLabel) {
        var body = "{\"c\":{\"indoorUnit\":{\"status\":{}}}}";
        var response = sendRequest(roomLabel, body);
        return jsonb.type(KumoRoomStatus.class).fromJson(response);
    }

    @Override
    public void setMode(String roomLabel, String mode) {
        var body = String.format("{\"c\":{\"indoorUnit\":{\"status\":{\"mode\":\"%s\"}}}}", mode);
        sendRequest(roomLabel, body);
    }

    @Override
    public void setFanSpeed(String roomLabel, String speed) {
        var body = String.format("{\"c\":{\"indoorUnit\":{\"status\":{\"fanSpeed\":\"%s\"}}}}", speed);
        sendRequest(roomLabel, body);
    }

    @Override
    public void setVentDirection(String roomLabel, String direction) {
        var body = String.format("{\"c\":{\"indoorUnit\":{\"status\":{\"vaneDir\":\"%s\"}}}}", direction);
        sendRequest(roomLabel, body);
    }

    @Override
    public void setCoolTemperature(String roomLabel, int tempFahrenheit) {
        double celsius = fahrenheitToCelsius(tempFahrenheit);
        var body = String.format("{\"c\":{\"indoorUnit\":{\"status\":{\"spCool\":%.1f}}}}", celsius);
        sendRequest(roomLabel, body);
    }

    @Override
    public void setHeatTemperature(String roomLabel, int tempFahrenheit) {
        double celsius = fahrenheitToCelsius(tempFahrenheit);
        var body = String.format("{\"c\":{\"indoorUnit\":{\"status\":{\"spHeat\":%.1f}}}}", celsius);
        sendRequest(roomLabel, body);
    }

    private double fahrenheitToCelsius(int fahrenheit) {
        return Math.round((double) (fahrenheit - 32) * 10 / 9) / 2.0;
    }

    private String sendRequest(String roomLabel, String body) {
        var device = devices.get(roomLabel);
        if (device == null) {
            throw new IllegalArgumentException("Unknown room: " + roomLabel);
        }
        var hash = crypto.computeHash(device, body);
        var uri = URI.create(String.format("http://%s/api?m=%s", device.address(), hash));
        var request = HttpRequest.newBuilder(uri)
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Request failed for room " + roomLabel, e);
        }
    }
}
