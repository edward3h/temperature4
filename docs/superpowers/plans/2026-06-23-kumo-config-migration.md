# Migrate kumojs config tool to Java Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the last remaining `kumojs` function — the interactive Kumo Cloud login + config-download tool — into a new `kumo-config` Java Gradle module, and switch `kumo.cfg` from a JS-wrapped file to pure JSON.

**Architecture:** A new standalone `kumo-config` module (the `application` plugin, its own `main()`), with three small collaborators (`KumoCloudClient`, `KumoCloudConfigParser`, `KumoConfigWriter`) wired together by `KumoConfigMain`. It shares no production code with the existing `kumo` module — only the JSON file format, verified by a test-only dependency. The existing `KumoConfigParser` in `kumo` is simplified to read pure JSON only.

**Tech Stack:** Java 25, Gradle Kotlin DSL, avaje-jsonb (streaming `JsonReader`/`JsonWriter`), `java.net.http.HttpClient`, JUnit 5, Truth assertions, Spotless.

**Spec:** `docs/superpowers/specs/2026-06-23-kumo-config-migration-design.md`

---

## Deviations from the spec (read before implementing)

Two judgment calls were made while turning the spec into concrete code. Both are called out here instead of being silently buried in a task, per the spec's own intent of leaving nothing for the implementer to guess:

1. **`KumoCloudClient` does not send `Accept-Encoding: gzip, deflate, br`**, even though the spec said to mirror the legacy tool's headers verbatim. `java.net.http.HttpClient` does not auto-decompress response bodies. If the server honoured that header and returned a compressed body, `KumoCloudConfigParser` would fail trying to parse compressed bytes as JSON — and there's no live-cloud test to catch that regression (by design, per the spec's Non-goals). Omitting the header asks the server for an uncompressed response, which is what we can actually parse. `Accept` and `Accept-Language` are kept since they don't carry this risk.
2. **`KumoConfigMain` should not be run via `./gradlew :kumo-config:run`.** The spec's Verification section says setting `standardInput = System.in` on the `run` task is enough to make the password prompt work — but masking the password requires `System.console()`, and `System.console()` returns `null` for a forked Gradle worker JVM regardless of stdin forwarding (console-attachment is a separate OS-level check from stdin redirection). So `gradle run` would always hit the "No console available" path. The supported invocation is `./gradlew :kumo-config:installDist` followed by running the generated script directly from a real terminal — that's a real attached console, and it's the same way the legacy `npm run config` worked. Task 7 and the Verification section reflect this; no `run`-task stdin configuration is added.

---

## Chunk 1: New `kumo-config` module

### Task 1: Module scaffolding

**Files:**
- Modify: `settings.gradle.kts`
- Create: `kumo-config/build.gradle.kts`

- [x] **Step 1: Add the module to settings**

Edit `settings.gradle.kts`:

```kotlin
rootProject.name = "temperature4"
include("app")
include("kumo")
include("kumo-config")
```

- [x] **Step 2: Create the module's build file**

Create `kumo-config/build.gradle.kts`:

```kotlin
plugins {
    application
    id("com.diffplug.spotless") version "8.7.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.avaje:avaje-jsonb:3.14")
    testImplementation(project(":kumo"))
    testImplementation("com.google.truth:truth:1.4.5")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.11.1")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "org.ethelred.kumoconfig.KumoConfigMain"
}

spotless {
    java {
        target("src/**/*.java")
        importOrder()
        removeUnusedImports()
        palantirJavaFormat().formatJavadoc(true)
        formatAnnotations()
        licenseHeader("// (C) Edward Harman \$YEAR")
    }
}
```

`testImplementation(project(":kumo"))` is intentional and test-only: production code in `kumo-config` depends on nothing from `kumo` (per the spec's Architecture section), but `KumoConfigWriterTest` (Task 5) needs the real `KumoConfigParser` to verify round-trip compatibility with the file format.

- [x] **Step 3: Verify the module is recognised**

Run: `./gradlew :kumo-config:tasks`
Expected: task list prints without error (no source files yet, so nothing to compile).

- [x] **Step 4: Commit**

```bash
git add settings.gradle.kts kumo-config/build.gradle.kts
git commit -m "build: scaffold kumo-config module"
```

---

### Task 2: `KumoCloudException`

**Files:**
- Create: `kumo-config/src/main/java/org/ethelred/kumoconfig/KumoCloudException.java`

No dedicated test for this step — it's a two-constructor exception type with no logic of its own. It's exercised indirectly by every test in Tasks 4 and 6 that asserts a `KumoCloudException` is thrown.

- [x] **Step 1: Create the exception**

```java
// (C) Edward Harman 2026
package org.ethelred.kumoconfig;

public class KumoCloudException extends RuntimeException {
    public KumoCloudException(String message) {
        super(message);
    }

    public KumoCloudException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [x] **Step 2: Commit**

```bash
git add kumo-config/src/main/java/org/ethelred/kumoconfig/KumoCloudException.java
git commit -m "feat: add KumoCloudException"
```

---

### Task 3: `KumoCloudDevice` record

**Files:**
- Create: `kumo-config/src/main/java/org/ethelred/kumoconfig/KumoCloudDevice.java`

No dedicated test — a plain data record, exercised by Tasks 4 and 5's tests.

- [x] **Step 1: Create the record**

```java
// (C) Edward Harman 2026
package org.ethelred.kumoconfig;

public record KumoCloudDevice(
        String serial, String label, String cryptoSerial, String cryptoKeySet, String password, String address) {}
```

All six fields are passed through verbatim from the cloud response — `password` is already base64-encoded and `cryptoSerial` is already hex-encoded by Kumo Cloud, matching exactly what the existing `KumoConfigParser.parseDevice` expects to decode (`Base64.getDecoder().decode(...)` / hex-to-bytes). Nothing in this module re-encodes them.

- [x] **Step 2: Commit**

```bash
git add kumo-config/src/main/java/org/ethelred/kumoconfig/KumoCloudDevice.java
git commit -m "feat: add KumoCloudDevice record"
```

---

### Task 4: `KumoCloudConfigParser`

**Files:**
- Create: `kumo-config/src/main/java/org/ethelred/kumoconfig/KumoCloudConfigParser.java`
- Test: `kumo-config/src/test/java/org/ethelred/kumoconfig/KumoCloudConfigParserTest.java`

This mirrors the legacy `kumoConfig.ts`'s `processcfg`/`parsechildren`/`parsezone` recursion: `username` comes from response index 0, the children map comes from response index 2's `children` field (index 1 is ignored), and each child in that map may have its own `zoneTable` (a map of device entries) and its own nested `children` (recursed). A child with no `children` field simply isn't recursed into — that's the normal terminating case, not an error.

- [x] **Step 1: Write the failing tests**

Create `kumo-config/src/test/java/org/ethelred/kumoconfig/KumoCloudConfigParserTest.java`:

```java
// (C) Edward Harman 2026
package org.ethelred.kumoconfig;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class KumoCloudConfigParserTest {

    private final KumoCloudConfigParser parser = new KumoCloudConfigParser();

    @Test
    void parse_extractsUsernameAndNestedDevices() {
        // Index 0 carries the username; index 1 is unused; index 2 carries the
        // children tree. One device lives directly under index 2's children,
        // a second is nested one level deeper under that child's own "children".
        var response =
                """
                [
                  {"username":"user@example.com"},
                  {},
                  {"children":{
                    "site1":{
                      "zoneTable":{
                        "zone1":{"serial":"SERIAL1","label":"Room A","cryptoSerial":"abc123",
                                 "cryptoKeySet":"F","password":"pw1","address":"10.0.0.1"}
                      },
                      "children":{
                        "site2":{
                          "zoneTable":{
                            "zone2":{"serial":"SERIAL2","label":"Room B","cryptoSerial":"def456",
                                     "cryptoKeySet":"F","password":"pw2","address":"10.0.0.2"}
                          }
                        }
                      }
                    }
                  }}
                ]
                """;

        var result = parser.parse(response);

        assertThat(result.accountName()).isEqualTo("user@example.com");
        assertThat(result.devices()).hasSize(2);
        assertThat(result.devices().stream().map(KumoCloudDevice::label).toList())
                .containsExactly("Room A", "Room B");
        var roomA = result.devices().get(0);
        assertThat(roomA.serial()).isEqualTo("SERIAL1");
        assertThat(roomA.cryptoSerial()).isEqualTo("abc123");
        assertThat(roomA.cryptoKeySet()).isEqualTo("F");
        assertThat(roomA.password()).isEqualTo("pw1");
        assertThat(roomA.address()).isEqualTo("10.0.0.1");
    }

    @Test
    void parse_childWithNoChildrenField_doesNotRecurse() {
        var response =
                """
                [
                  {"username":"user@example.com"},
                  {},
                  {"children":{
                    "site1":{
                      "zoneTable":{
                        "zone1":{"serial":"SERIAL1","label":"Room A","cryptoSerial":"abc123",
                                 "cryptoKeySet":"F","password":"pw1","address":"10.0.0.1"}
                      }
                    }
                  }}
                ]
                """;

        var result = parser.parse(response);

        assertThat(result.devices()).hasSize(1);
    }

    @Test
    void parse_notAnArray_throwsKumoCloudException() {
        var exception = assertThrows(KumoCloudException.class, () -> parser.parse("{}"));

        assertThat(exception).hasMessageThat().contains("Unexpected response from Kumo Cloud");
    }

    @Test
    void parse_index2MissingChildren_throwsKumoCloudException() {
        var response = """
                [{"username":"user@example.com"},{},{}]
                """;

        var exception = assertThrows(KumoCloudException.class, () -> parser.parse(response));

        assertThat(exception).hasMessageThat().contains("Unexpected response from Kumo Cloud");
    }
}
```

- [x] **Step 2: Run tests to verify they fail**

Run: `./gradlew :kumo-config:test`
Expected: FAIL — `KumoCloudConfigParser` does not exist yet.

- [x] **Step 3: Implement `KumoCloudConfigParser`**

Create `kumo-config/src/main/java/org/ethelred/kumoconfig/KumoCloudConfigParser.java`:

```java
// (C) Edward Harman 2026
package org.ethelred.kumoconfig;

import io.avaje.json.JsonReader;
import io.avaje.jsonb.Jsonb;
import java.util.ArrayList;
import java.util.List;

public class KumoCloudConfigParser {

    public record Result(String accountName, List<KumoCloudDevice> devices) {}

    public Result parse(String responseBody) {
        var jsonb = Jsonb.builder().build();
        try (var reader = jsonb.reader(responseBody)) {
            return parseTopLevel(reader);
        } catch (RuntimeException e) {
            throw new KumoCloudException("Unexpected response from Kumo Cloud", e);
        }
    }

    private Result parseTopLevel(JsonReader reader) {
        reader.beginArray();
        String username = null;
        List<KumoCloudDevice> devices = null;
        int index = 0;
        while (reader.hasNextElement()) {
            switch (index) {
                case 0 -> username = readUsername(reader);
                case 2 -> devices = readIndexTwo(reader);
                default -> reader.skipValue();
            }
            index++;
        }
        reader.endArray();
        if (username == null || devices == null) {
            throw new KumoCloudException("Unexpected response from Kumo Cloud: missing username or children");
        }
        return new Result(username, devices);
    }

    private String readUsername(JsonReader reader) {
        reader.beginObject();
        String username = null;
        while (reader.hasNextField()) {
            if (reader.nextField().equals("username")) {
                username = reader.readString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return username;
    }

    private List<KumoCloudDevice> readIndexTwo(JsonReader reader) {
        reader.beginObject();
        List<KumoCloudDevice> devices = null;
        while (reader.hasNextField()) {
            if (reader.nextField().equals("children")) {
                devices = readChildren(reader);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return devices;
    }

    private List<KumoCloudDevice> readChildren(JsonReader reader) {
        var devices = new ArrayList<KumoCloudDevice>();
        reader.beginObject();
        while (reader.hasNextField()) {
            reader.nextField(); // opaque child id, not needed
            devices.addAll(readChild(reader));
        }
        reader.endObject();
        return devices;
    }

    private List<KumoCloudDevice> readChild(JsonReader reader) {
        var devices = new ArrayList<KumoCloudDevice>();
        reader.beginObject();
        while (reader.hasNextField()) {
            switch (reader.nextField()) {
                case "zoneTable" -> devices.addAll(readZoneTable(reader));
                case "children" -> devices.addAll(readChildren(reader));
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return devices;
    }

    private List<KumoCloudDevice> readZoneTable(JsonReader reader) {
        var devices = new ArrayList<KumoCloudDevice>();
        reader.beginObject();
        while (reader.hasNextField()) {
            reader.nextField(); // zone id, not needed
            devices.add(readDevice(reader));
        }
        reader.endObject();
        return devices;
    }

    private KumoCloudDevice readDevice(JsonReader reader) {
        reader.beginObject();
        String serial = null;
        String label = null;
        String cryptoSerial = null;
        String cryptoKeySet = null;
        String password = null;
        String address = null;
        while (reader.hasNextField()) {
            switch (reader.nextField()) {
                case "serial" -> serial = reader.readString();
                case "label" -> label = reader.readString();
                case "cryptoSerial" -> cryptoSerial = reader.readString();
                case "cryptoKeySet" -> cryptoKeySet = reader.readString();
                case "password" -> password = reader.readString();
                case "address" -> address = reader.readString();
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return new KumoCloudDevice(serial, label, cryptoSerial, cryptoKeySet, password, address);
    }
}
```

- [x] **Step 4: Run tests to verify they pass**

Run: `./gradlew :kumo-config:test`
Expected: PASS (all four tests).

- [x] **Step 5: Commit**

```bash
git add kumo-config/src/main/java/org/ethelred/kumoconfig/KumoCloudConfigParser.java \
        kumo-config/src/test/java/org/ethelred/kumoconfig/KumoCloudConfigParserTest.java
git commit -m "feat: add KumoCloudConfigParser"
```

---

### Task 5: `KumoConfigWriter`

**Files:**
- Create: `kumo-config/src/main/java/org/ethelred/kumoconfig/KumoConfigWriter.java`
- Test: `kumo-config/src/test/java/org/ethelred/kumoconfig/KumoConfigWriterTest.java`

- [x] **Step 1: Write the failing test**

Create `kumo-config/src/test/java/org/ethelred/kumoconfig/KumoConfigWriterTest.java`:

```java
// (C) Edward Harman 2026
package org.ethelred.kumoconfig;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import org.ethelred.kumo.KumoConfigParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KumoConfigWriterTest {

    @Test
    void write_roundTripsThroughExistingKumoConfigParser(@TempDir Path tempDir) throws Exception {
        var device = new KumoCloudDevice(
                "SERIAL001",
                "Living Room",
                "000102030405060708", // hex
                "F",
                Base64.getEncoder().encodeToString(new byte[] {1, 2, 3, 4}),
                "192.168.1.100");

        var configFile = tempDir.resolve("kumo.cfg");
        new KumoConfigWriter().write(configFile, "user@example.com", List.of(device));

        var devices = new KumoConfigParser(configFile.toString()).parse();

        assertThat(devices).hasSize(1);
        var parsed = devices.get(0);
        assertThat(parsed.label()).isEqualTo("Living Room");
        assertThat(parsed.address()).isEqualTo("192.168.1.100");
        assertThat(parsed.s()).isEqualTo(0);
        assertThat(parsed.password()).isEqualTo(new byte[] {1, 2, 3, 4});
        assertThat(parsed.cryptoSerial()).isEqualTo(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8});
        assertThat(parsed.w()).hasLength(32); // the fixed W constant decodes to 32 bytes
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `./gradlew :kumo-config:test`
Expected: FAIL — `KumoConfigWriter` does not exist yet.

- [x] **Step 3: Implement `KumoConfigWriter`**

Create `kumo-config/src/main/java/org/ethelred/kumoconfig/KumoConfigWriter.java`:

```java
// (C) Edward Harman 2026
package org.ethelred.kumoconfig;

import io.avaje.json.JsonWriter;
import io.avaje.jsonb.Jsonb;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class KumoConfigWriter {
    private static final int S = 0;
    private static final String W = "44c73283b498d432ff25f5c8e06a016aef931e68f0a00ea710e36e6338fb22db";

    public void write(Path path, String accountName, List<KumoCloudDevice> devices) throws IOException {
        var jsonb = Jsonb.builder().build();
        try (OutputStream out = Files.newOutputStream(path);
                var writer = jsonb.writer(out)) {
            writer.beginObject();
            writer.name(accountName);
            writer.beginObject();
            for (var device : devices) {
                writer.name(device.serial());
                writeDevice(writer, device);
            }
            writer.endObject();
            writer.endObject();
        }
    }

    private void writeDevice(JsonWriter writer, KumoCloudDevice device) {
        writer.beginObject();
        writer.name("serial");
        writer.value(device.serial());
        writer.name("label");
        writer.value(device.label());
        writer.name("cryptoSerial");
        writer.value(device.cryptoSerial());
        writer.name("cryptoKeySet");
        writer.value(device.cryptoKeySet());
        writer.name("password");
        writer.value(device.password());
        writer.name("address");
        writer.value(device.address());
        writer.name("S");
        writer.value(S);
        writer.name("W");
        writer.value(W);
        writer.endObject();
    }
}
```

- [x] **Step 4: Run test to verify it passes**

Run: `./gradlew :kumo-config:test`
Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add kumo-config/src/main/java/org/ethelred/kumoconfig/KumoConfigWriter.java \
        kumo-config/src/test/java/org/ethelred/kumoconfig/KumoConfigWriterTest.java
git commit -m "feat: add KumoConfigWriter"
```

---

### Task 6: `KumoCloudClient`

**Files:**
- Create: `kumo-config/src/main/java/org/ethelred/kumoconfig/KumoCloudClient.java`
- Test: `kumo-config/src/test/java/org/ethelred/kumoconfig/KumoCloudClientTest.java`

Tested against a local `com.sun.net.httpserver.HttpServer` (JDK built-in, no new test dependency needed) rather than the real Kumo Cloud endpoint — see Non-goals in the spec. The login URI is package-private-overridable so the test can point at the local server while production code always uses the real one.

- [x] **Step 1: Write the failing tests**

Create `kumo-config/src/test/java/org/ethelred/kumoconfig/KumoCloudClientTest.java`:

```java
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
        var client = new KumoCloudClient(URI.create("http://localhost:" + server.getAddress().getPort() + "/login"));

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
        var client = new KumoCloudClient(URI.create("http://localhost:" + server.getAddress().getPort() + "/login"));

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
```

- [x] **Step 2: Run tests to verify they fail**

Run: `./gradlew :kumo-config:test`
Expected: FAIL — `KumoCloudClient` does not exist yet.

- [x] **Step 3: Implement `KumoCloudClient`**

Create `kumo-config/src/main/java/org/ethelred/kumoconfig/KumoCloudClient.java`:

```java
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
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
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
```

No `Accept-Encoding` header — see "Deviations from the spec" at the top of this plan.

- [x] **Step 4: Run tests to verify they pass**

Run: `./gradlew :kumo-config:test`
Expected: PASS (all three tests).

- [x] **Step 5: Commit**

```bash
git add kumo-config/src/main/java/org/ethelred/kumoconfig/KumoCloudClient.java \
        kumo-config/src/test/java/org/ethelred/kumoconfig/KumoCloudClientTest.java
git commit -m "feat: add KumoCloudClient"
```

---

### Task 7: `KumoConfigMain`

**Files:**
- Create: `kumo-config/src/main/java/org/ethelred/kumoconfig/KumoConfigMain.java`

No automated test — this is the interactive CLI glue (console I/O), exercised manually in the Verification section.

- [x] **Step 1: Implement `KumoConfigMain`**

```java
// (C) Edward Harman 2026
package org.ethelred.kumoconfig;

import java.io.IOException;
import java.nio.file.Path;

public class KumoConfigMain {
    public static void main(String[] args) {
        var console = System.console();
        if (console == null) {
            System.err.println("No console available - run this from an interactive terminal "
                    + "(see the Verification section in the implementation plan for how to invoke it).");
            System.exit(1);
            return;
        }

        var username = console.readLine("Enter username: ");
        var password = new String(console.readPassword("Enter password: "));

        var outputPath = Path.of("kumo.cfg");
        try {
            var responseBody = new KumoCloudClient().login(username, password);
            var result = new KumoCloudConfigParser().parse(responseBody);
            new KumoConfigWriter().write(outputPath, result.accountName(), result.devices());
            System.out.printf("Downloaded config for %d devices. Written to ./kumo.cfg%n", result.devices().size());
        } catch (KumoCloudException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Failed to write " + outputPath + ": " + e.getMessage());
            System.exit(1);
        }
    }
}
```

- [x] **Step 2: Verify the module builds**

Run: `./gradlew :kumo-config:build`
Expected: BUILD SUCCESSFUL (compiles, runs all `kumo-config` tests, passes Spotless).

- [x] **Step 3: Commit**

```bash
git add kumo-config/src/main/java/org/ethelred/kumoconfig/KumoConfigMain.java
git commit -m "feat: add KumoConfigMain CLI entry point"
```

---

## Chunk 2: Simplify existing code and migrate data

### Task 8: Simplify `KumoConfigParser` to pure JSON only

**Files:**
- Modify: `kumo/src/main/java/org/ethelred/kumo/KumoConfigParser.java:20-30`
- Modify: `kumo/src/test/java/org/ethelred/kumo/KumoConfigParserTest.java:17-39`

- [x] **Step 1: Remove the now-obsolete wrapper test**

In `kumo/src/test/java/org/ethelred/kumo/KumoConfigParserTest.java`, delete the entire `parse_validConfigWithModuleExports` test (lines 17-39) — the `module.exports = ...;` format is no longer supported. Leave `parse_plainJson` and `parse_multipleRooms` as-is; they already cover pure JSON.

- [x] **Step 2: Run tests to confirm the remaining ones still pass**

Run: `./gradlew :kumo:test`
Expected: PASS (2 tests: `parse_plainJson`, `parse_multipleRooms`).

- [x] **Step 3: Simplify the parser**

In `kumo/src/main/java/org/ethelred/kumo/KumoConfigParser.java`, replace the `parse()` method:

```java
    public List<KumoDeviceConfig> parse() throws IOException {
        var content = Files.readString(Path.of(configFilePath));
        var json = content.strip();
        if (json.startsWith("module.exports =")) {
            json = json.substring("module.exports =".length()).strip();
        }
        if (json.endsWith(";")) {
            json = json.substring(0, json.length() - 1).strip();
        }
        return parseJson(json);
    }
```

with:

```java
    public List<KumoDeviceConfig> parse() throws IOException {
        var json = Files.readString(Path.of(configFilePath));
        return parseJson(json);
    }
```

- [x] **Step 4: Run tests to confirm they still pass**

Run: `./gradlew :kumo:test`
Expected: PASS (same 2 tests).

- [x] **Step 5: Commit**

```bash
git add kumo/src/main/java/org/ethelred/kumo/KumoConfigParser.java \
        kumo/src/test/java/org/ethelred/kumo/KumoConfigParserTest.java
git commit -m "refactor: simplify KumoConfigParser to read pure JSON only"
```

---

### Task 9: Convert local `kumo.cfg` to pure JSON

**Files:**
- Modify: `app/src/main/resources/kumo.cfg` (gitignored — no commit for this step)

This file currently starts with `module.exports = \n` followed by the JSON object on its own line, and has no trailing `;`. After Task 8, `KumoConfigParser` no longer strips that prefix, so the app would fail to parse its own config on next start unless this file is converted now.

- [x] **Step 1: Read the current file**

Run: `cat app/src/main/resources/kumo.cfg`
Confirm it still has the two-line shape: `module.exports = ` on line 1, the JSON object on line 2.

- [x] **Step 2: Rewrite as pure JSON**

Edit the file to delete line 1 (`module.exports = `) entirely, leaving only the JSON object as the file's full content.

- [x] **Step 3: Verify it's valid JSON**

Run: `python3 -m json.tool app/src/main/resources/kumo.cfg > /dev/null && echo OK`
Expected: `OK`

- [x] **Step 4: No commit needed**

This file is gitignored (confirmed by its absence from `git status`), so this is a local-environment change only. It does need to be repeated on any other machine/deployment running this app with the old-format file — most notably the production host, which is a manual step for the project owner (see Verification section).

---

### Task 10: Final verification

- [x] **Step 1: Full build across all modules**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL — compiles `app`, `kumo`, `kumo-config`; runs all non-integration tests; passes Spotless on all three modules.

- [x] **Step 2: Build the installable CLI distribution**

Run: `./gradlew :kumo-config:installDist`
Expected: BUILD SUCCESSFUL, producing `kumo-config/build/install/kumo-config/bin/kumo-config`.

- [x] **Step 3: Manual smoke test of the CLI (project owner only — needs real Kumo Cloud credentials)**

From a real terminal (not through Gradle):

```bash
kumo-config/build/install/kumo-config/bin/kumo-config
```

Enter real Kumo Cloud credentials when prompted. Confirm:
- The password isn't echoed to the terminal.
- A `kumo.cfg` is written in the current directory.
- Its account/device/field shape matches what the legacy `kumojs` `npm run config` tool used to produce (same device count, same field names), just without the `module.exports = ` wrapper.

I should not run this step myself — it requires real account credentials that only the project owner should be entering, and matches the "interactive masked prompt" decision made during brainstorming specifically so credentials never need to be typed into an automated context.

- [x] **Step 4: Remind the project owner about the production `eternal.local` config**

The deployed `kumo.cfg` on `eternal.local` is still in the old `module.exports = ...` format. Once this change ships, that file needs the same one-line-deletion conversion done in Task 9 (or a fresh run of the new CLI) before that host's app instance will start successfully. This is a manual deployment step, not something to automate as part of this change.

- [x] **Step 5: Clean up the legacy tool reference (optional, ask before doing)**

`kumojs`'s `config` functionality is now fully superseded. Removing the `kumojs` npm package entirely is out of scope for this plan (it's a separate repository) — flagging it only so the project owner can decide separately whether to retire it.
