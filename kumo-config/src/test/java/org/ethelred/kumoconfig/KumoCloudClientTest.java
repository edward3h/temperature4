// (C) Edward Harman 2026
package org.ethelred.kumoconfig;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class KumoCloudClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void login_onSuccess_returnsResponseBody() throws IOException {
        var capturedBody = new StringBuilder();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/login", exchange -> {
            capturedBody.append(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            var response = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        var client = new KumoCloudClient(
                URI.create("http://localhost:" + server.getAddress().getPort() + "/login"));

        var body = client.login("user@example.com", "secret");

        assertThat(body).isEqualTo("{\"ok\":true}");
        assertThat(capturedBody.toString()).contains("\"username\":\"user@example.com\"");
        assertThat(capturedBody.toString()).contains("\"password\":\"secret\"");
        assertThat(capturedBody.toString()).contains("\"appVersion\":\"2.2.0\"");
    }

    @Test
    void login_onRejection_throwsKumoCloudExceptionWithStatus() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/login", exchange -> {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        });
        server.start();
        var client = new KumoCloudClient(
                URI.create("http://localhost:" + server.getAddress().getPort() + "/login"));

        var exception = assertThrows(KumoCloudException.class, () -> client.login("user@example.com", "wrong"));

        assertThat(exception).hasMessageThat().contains("401");
    }

    @Test
    void login_whenServerUnreachable_throwsKumoCloudException() throws IOException {
        // Bind then immediately release a port so nothing is listening on it.
        int unusedPort;
        try (var socket = new ServerSocket(0)) {
            unusedPort = socket.getLocalPort();
        }
        var client = new KumoCloudClient(URI.create("http://localhost:" + unusedPort + "/login"));

        var exception = assertThrows(KumoCloudException.class, () -> client.login("user@example.com", "secret"));

        assertThat(exception).hasMessageThat().contains("Unable to reach Kumo Cloud");
    }
}
