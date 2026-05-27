// (C) Edward Harman 2026
package org.ethelred.kumo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Integration test against real Kumo devices. Run with:
 *
 * <pre>./gradlew :kumo:test -Pintegration</pre>
 *
 * <p>Requires kumo.cfg at ../app/src/main/resources/kumo.cfg (relative to the kumo module), or set
 * -Dkumo.config=/path/to/kumo.cfg
 */
@Tag("integration")
class KumoIntegrationTest {

    private static final Path CONFIG_FILE =
            Path.of(System.getProperty("kumo.config", "../app/src/main/resources/kumo.cfg"));

    @BeforeAll
    static void requireConfig() {
        Assumptions.assumeTrue(
                Files.exists(CONFIG_FILE),
                "Integration test skipped: kumo.cfg not found at " + CONFIG_FILE.toAbsolutePath());
    }

    @Test
    void parseConfig() throws Exception {
        var parser = new KumoConfigParser(CONFIG_FILE.toString());
        var devices = parser.parse();
        System.out.println("Parsed " + devices.size() + " devices:");
        devices.forEach(d -> System.out.printf("  %-14s  %s%n", d.label(), d.address()));
        if (devices.isEmpty()) {
            throw new AssertionError("No devices found in kumo.cfg");
        }
    }

    @Test
    void deviceRawRequest() throws Exception {
        var parser = new KumoConfigParser(CONFIG_FILE.toString());
        var devices = parser.parse();
        Assumptions.assumeFalse(devices.isEmpty(), "No devices parsed — run parseConfig first");

        var crypto = new KumoCrypto();
        var client = HttpClient.newBuilder().build();
        var payload = "{\"c\":{\"indoorUnit\":{\"status\":{}}}}";

        System.out.println("Raw device responses:");
        for (var device : devices) {
            var hash = crypto.computeHash(device, payload);
            var uri = URI.create("http://" + device.address() + "/api?m=" + hash);
            System.out.printf("%n  %-14s  %s%n", device.label(), device.address());
            try {
                var request = HttpRequest.newBuilder(uri)
                        .PUT(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                        .header("Content-Type", "application/json")
                        .build();
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.printf("  HTTP %d  %s%n", response.statusCode(), response.body());
            } catch (Exception e) {
                System.out.printf("  ERROR   %s%n", e.getMessage());
            }
        }
        // diagnostic only — always passes so all devices are reported
    }

    @Test
    void cryptoDiagnostic() throws Exception {
        var parser = new KumoConfigParser(CONFIG_FILE.toString());
        var devices = parser.parse();
        Assumptions.assumeFalse(devices.isEmpty(), "No devices parsed");

        var payload = "{\"c\":{\"indoorUnit\":{\"status\":{}}}}";
        var crypto = new KumoCrypto();

        System.out.println("Crypto diagnostic (payload: " + payload + "):");
        for (var device : devices) {
            System.out.printf("%n  %-14s  %s%n", device.label(), device.address());
            System.out.printf("  password bytes (%d): %s%n", device.password().length, hex(device.password()));
            System.out.printf("  cryptoSerial  (%d): %s%n", device.cryptoSerial().length, hex(device.cryptoSerial()));
            System.out.printf("  W             (%d): %s%n", device.w().length, hex(device.w()));
            System.out.printf("  S: %d%n", device.s());

            // Current approach: password is base64-decoded bytes
            var hashB64 = crypto.computeHash(device, payload);
            System.out.printf("  hash (base64 password): %s%n", hashB64);

            // Alternative: password as UTF-8 bytes of the base64 string
            var pwdUtf8 = readPasswordAsUtf8(CONFIG_FILE, device.label());
            if (pwdUtf8 != null) {
                var altDevice = new KumoDeviceConfig(
                        device.label(), device.address(), pwdUtf8, device.cryptoSerial(), device.s(), device.w());
                var hashUtf8 = crypto.computeHash(altDevice, payload);
                System.out.printf("  hash (utf-8  password): %s%n", hashUtf8);
            }
        }
    }

    private static byte[] readPasswordAsUtf8(Path configFile, String label) {
        try {
            var content = Files.readString(configFile);
            // find "label":"<label>","... or similar then look for "password":"<value>"
            // crude extraction: find password value as raw string bytes
            // This is only for diagnostic comparison
            var idx = content.indexOf("\"password\":\"");
            while (idx >= 0) {
                var start = idx + 12;
                var end = content.indexOf('"', start);
                var pw = content.substring(start, end);
                // check if this password is near this label
                var labelIdx = content.lastIndexOf("\"label\":\"" + label + "\"", idx);
                var nextLabelIdx = content.indexOf("\"label\":", idx);
                if (labelIdx >= 0 && (nextLabelIdx < 0 || labelIdx > nextLabelIdx - 200)) {
                    return pw.getBytes(StandardCharsets.UTF_8);
                }
                idx = content.indexOf("\"password\":\"", end);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    @Test
    @Timeout(30)
    void setModeTiming() throws Exception {
        var service = new KumoServiceImpl(CONFIG_FILE.toString());
        Assumptions.assumeTrue(
                service.getRoomList().contains("Basement"),
                "Integration test skipped: Basement not found in kumo.cfg");

        var initial = service.getRoomStatus("Basement");
        var initialMode = initial.mode();
        var targetMode = "off".equals(initialMode) ? "cool" : "off";

        System.out.printf("Basement initial mode: %s → setting to: %s%n", initialMode, targetMode);

        try {
            service.setMode("Basement", targetMode);
            var startNs = System.nanoTime();
            System.out.printf("setMode sent at t=0%n");

            var changed = false;
            for (int i = 0; i < 20; i++) {
                Thread.sleep(500);
                var elapsed = (System.nanoTime() - startNs) / 1_000_000;
                var status = service.getRoomStatus("Basement");
                System.out.printf("  t=%4dms  mode=%s%n", elapsed, status.mode());
                if (targetMode.equals(status.mode())) {
                    System.out.printf("  => mode reflected in status after %dms%n", elapsed);
                    changed = true;
                    break;
                }
            }
            if (!changed) {
                System.out.println("  => mode not reflected in status within 10s");
            }
        } finally {
            System.out.printf("Restoring mode to: %s%n", initialMode);
            service.setMode("Basement", initialMode);
        }
    }

    private static String hex(byte[] bytes) {
        var sb = new StringBuilder(bytes.length * 2);
        for (var b : bytes) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}
