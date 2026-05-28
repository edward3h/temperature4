# App Test Coverage Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add meaningful test coverage to the `app` module — unit tests for core logic and HTTP integration tests for all endpoints.

**Architecture:** Extract interfaces from `KumoJsRepository` and `SensorsRepository` to enable hand-written fakes, write unit tests for `Temperature`/`DefaultSensorMapping`/`DefaultSettingUpdater`/`DefaultRoomService`, then wire HTTP integration tests using Javalin's `start(0)` + Java `HttpClient`.

**Tech Stack:** JUnit Jupiter 5.11.1, Google Truth 1.4.5, Javalin 7.1.0, Avaje Jsonb 3.11, JTE `StaticTemplates` (compile-time generated)

---

## File Map

### New production files
- `app/src/main/java/org/ethelred/temperature4/kumojs/KumoJsRepository.java` — new interface (extracted from class)
- `app/src/main/java/org/ethelred/temperature4/kumojs/DefaultKumoJsRepository.java` — renamed class, `implements KumoJsRepository`
- `app/src/main/java/org/ethelred/temperature4/sensors/SensorsRepository.java` — new interface (extracted from class)
- `app/src/main/java/org/ethelred/temperature4/sensors/DefaultSensorsRepository.java` — renamed class, `implements SensorsRepository`

### Modified production files
- `app/src/main/java/org/ethelred/temperature4/DefaultRoomService.java` — update constructor param types to use new interfaces
- `app/src/main/java/org/ethelred/temperature4/DefaultSettingUpdater.java` — same

### New test support files
- `app/src/test/resources/avaje-test.properties` — test configuration (channels, updater bounds, contextPath)
- `app/src/test/java/org/ethelred/temperature4/kumojs/FakeKumoJsRepository.java`
- `app/src/test/java/org/ethelred/temperature4/sensors/FakeSensorsRepository.java`
- `app/src/test/java/org/ethelred/temperature4/FakeSensorMapping.java`
- `app/src/test/java/org/ethelred/temperature4/FakeRoomService.java`
- `app/src/test/java/org/ethelred/temperature4/openweather/FakeOpenWeatherClient.java`

### New test files
- `app/src/test/java/org/ethelred/temperature4/TemperatureTest.java`
- `app/src/test/java/org/ethelred/temperature4/DefaultSensorMappingTest.java`
- `app/src/test/java/org/ethelred/temperature4/DefaultSettingUpdaterTest.java`
- `app/src/test/java/org/ethelred/temperature4/DefaultRoomServiceTest.java`
- `app/src/test/java/org/ethelred/temperature4/UIControllerTest.java`
- `app/src/test/java/org/ethelred/temperature4/SettingControllerTest.java`

---

## Chunk 1: Interface extraction and test properties

- [ ] **Pre-task: Create feature branch**

  ```bash
  git checkout -b add-app-tests
  ```

### Task 1: Extract `KumoJsRepository` interface

**Files:**
- Create: `app/src/main/java/org/ethelred/temperature4/kumojs/KumoJsRepository.java`
- Create: `app/src/main/java/org/ethelred/temperature4/kumojs/DefaultKumoJsRepository.java`
- Delete: original `KumoJsRepository.java` content (replaced by interface + renamed class)
- Modify: `app/src/main/java/org/ethelred/temperature4/DefaultRoomService.java`
- Modify: `app/src/main/java/org/ethelred/temperature4/DefaultSettingUpdater.java`

- [ ] **Step 1: Create the `KumoJsRepository` interface**

  Create `app/src/main/java/org/ethelred/temperature4/kumojs/KumoJsRepository.java`:

  ```java
  // (C) Edward Harman 2026
  package org.ethelred.temperature4.kumojs;

  import java.util.List;
  import org.ethelred.temperature4.NamedResult;
  import org.ethelred.temperature4.RoomView;

  public interface KumoJsRepository {
      List<NamedResult<RoomView>> getRoomStatuses();

      List<String> getRoomList();

      RoomStatus getRoomStatus(String name);

      void setMode(String name, String mode);

      void setTemperature(String name, String mode, int newTemp);
  }
  ```

- [ ] **Step 2: Rename the existing class to `DefaultKumoJsRepository`**

  Create `app/src/main/java/org/ethelred/temperature4/kumojs/DefaultKumoJsRepository.java` by copying the entire existing `KumoJsRepository.java` content and making these changes:
  - Change `public class KumoJsRepository` → `public class DefaultKumoJsRepository implements KumoJsRepository`
  - Keep all imports, `@Singleton` annotation, and all method bodies unchanged

  Then **completely replace** the entire content of `KumoJsRepository.java` with the interface body from Step 1. The file must contain only the interface — not the old class code. Do not append; overwrite the whole file.

- [ ] **Step 3: Verify no source changes needed in `DefaultRoomService` and `DefaultSettingUpdater`**

  The type names `KumoJsRepository` and `SensorsRepository` are unchanged — only the implementing classes were renamed. All existing field declarations and imports in `DefaultRoomService.java` and `DefaultSettingUpdater.java` already use those type names, so they automatically resolve to the new interfaces. Open both files and confirm no edits are required.

- [ ] **Step 4: Build to verify compilation**

  ```bash
  ./gradlew :app:compileJava
  ```
  Expected: BUILD SUCCESSFUL with no errors.

- [ ] **Step 5: Commit**

  ```bash
  git add app/src/main/java/org/ethelred/temperature4/kumojs/
  git commit -m "refactor: extract KumoJsRepository interface for testability"
  ```

---

### Task 2: Extract `SensorsRepository` interface

**Files:**
- Create: `app/src/main/java/org/ethelred/temperature4/sensors/SensorsRepository.java`
- Create: `app/src/main/java/org/ethelred/temperature4/sensors/DefaultSensorsRepository.java`

- [ ] **Step 1: Create the `SensorsRepository` interface**

  Create `app/src/main/java/org/ethelred/temperature4/sensors/SensorsRepository.java`:

  ```java
  // (C) Edward Harman 2026
  package org.ethelred.temperature4.sensors;

  import java.util.List;

  public interface SensorsRepository {
      List<SensorResult> getSensorResults();
  }
  ```

- [ ] **Step 2: Rename the existing class to `DefaultSensorsRepository`**

  Create `app/src/main/java/org/ethelred/temperature4/sensors/DefaultSensorsRepository.java` from the content of the existing `SensorsRepository.java`:
  - Change `public class SensorsRepository` → `public class DefaultSensorsRepository implements SensorsRepository`
  - Keep all fields, constructor, and method bodies unchanged

  Then replace `SensorsRepository.java` with the interface from Step 1.

- [ ] **Step 3: Build to verify**

  ```bash
  ./gradlew :app:compileJava
  ```
  Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run existing kumo tests to check nothing broke**

  ```bash
  ./gradlew :kumo:test
  ```
  Expected: 7 tests pass.

- [ ] **Step 5: Commit**

  ```bash
  git add app/src/main/java/org/ethelred/temperature4/sensors/
  git commit -m "refactor: extract SensorsRepository interface for testability"
  ```

---

### Task 3: Create test properties file

**Files:**
- Create: `app/src/test/resources/avaje-test.properties`

- [ ] **Step 1: Create the file**

  Create `app/src/test/resources/avaje-test.properties`:

  ```properties
  sensors.channels=Living Room, Bedroom
  updater.mintemp=60
  updater.maxtemp=85
  server.contextPath=/
  ```

  Avaje Config auto-loads any classpath file matching `avaje*.properties`, so this file is picked up automatically by `Configuration.create()` / `Config.asConfiguration()` in tests.

- [ ] **Step 2: Commit**

  ```bash
  git add app/src/test/resources/avaje-test.properties
  git commit -m "test: add avaje-test.properties for test configuration"
  ```

---

## Chunk 2: Test fakes

### Task 4: `FakeKumoJsRepository`

**Files:**
- Create: `app/src/test/java/org/ethelred/temperature4/kumojs/FakeKumoJsRepository.java`

- [ ] **Step 1: Write the fake**

  ```java
  // (C) Edward Harman 2026
  package org.ethelred.temperature4.kumojs;

  import java.util.ArrayList;
  import java.util.LinkedHashMap;
  import java.util.List;
  import java.util.Map;
  import org.ethelred.temperature4.NamedResult;
  import org.ethelred.temperature4.RoomView;
  import org.ethelred.temperature4.Temperature;

  public class FakeKumoJsRepository implements KumoJsRepository {
      private final Map<String, RoomStatus> statuses = new LinkedHashMap<>();
      public final List<String> modeCallArgs = new ArrayList<>();
      public final List<String> tempCallArgs = new ArrayList<>();

      public void addRoom(String name, RoomStatus status) {
          statuses.put(name, status);
      }

      @Override
      public List<String> getRoomList() {
          return List.copyOf(statuses.keySet());
      }

      @Override
      public RoomStatus getRoomStatus(String name) {
          return statuses.get(name);
      }

      @Override
      public List<NamedResult<RoomView>> getRoomStatuses() {
          return statuses.entrySet().stream()
                  .map(e -> (NamedResult<RoomView>) new NamedRoomStatus(e.getKey(), e.getValue()))
                  .toList();
      }

      @Override
      public void setMode(String name, String mode) {
          modeCallArgs.add(name + ":" + mode);
      }

      @Override
      public void setTemperature(String name, String mode, int t) {
          tempCallArgs.add(name + ":" + mode + ":" + t);
          var old = statuses.get(name);
          if (old != null) {
              var newTemp = Temperature.fromFahrenheit(t);
              if ("cool".equalsIgnoreCase(mode)) {
                  statuses.put(name, new RoomStatus(old.roomTemp(), old.mode(), newTemp, old.spHeat()));
              } else {
                  statuses.put(name, new RoomStatus(old.roomTemp(), old.mode(), old.spCool(), newTemp));
              }
          }
      }
  }
  ```

- [ ] **Step 2: Compile to verify**

  ```bash
  ./gradlew :app:compileTestJava
  ```
  Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/test/java/org/ethelred/temperature4/kumojs/FakeKumoJsRepository.java
  git commit -m "test: add FakeKumoJsRepository"
  ```

---

### Task 5: `FakeSensorsRepository`

**Files:**
- Create: `app/src/test/java/org/ethelred/temperature4/sensors/FakeSensorsRepository.java`

- [ ] **Step 1: Write the fake**

  ```java
  // (C) Edward Harman 2026
  package org.ethelred.temperature4.sensors;

  import java.util.List;

  public class FakeSensorsRepository implements SensorsRepository {
      private List<SensorResult> results = List.of();

      public void setResults(List<SensorResult> results) {
          this.results = results;
      }

      @Override
      public List<SensorResult> getSensorResults() {
          return results;
      }
  }
  ```

- [ ] **Step 2: Compile**

  ```bash
  ./gradlew :app:compileTestJava
  ```
  Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/test/java/org/ethelred/temperature4/sensors/FakeSensorsRepository.java
  git commit -m "test: add FakeSensorsRepository"
  ```

---

### Task 6: `FakeSensorMapping`, `FakeRoomService`, `FakeOpenWeatherClient`

**Files:**
- Create: `app/src/test/java/org/ethelred/temperature4/FakeSensorMapping.java`
- Create: `app/src/test/java/org/ethelred/temperature4/FakeRoomService.java`
- Create: `app/src/test/java/org/ethelred/temperature4/openweather/FakeOpenWeatherClient.java`

- [ ] **Step 1: Write `FakeSensorMapping`**

  Two `addSensorRoom` overloads: one that takes a temperature (for tests that need sensor readings) and one that registers a room with no view (for tests that only need `hasSensor()` to return true).

  ```java
  // (C) Edward Harman 2026
  package org.ethelred.temperature4;

  import java.time.OffsetDateTime;
  import java.util.Collection;
  import java.util.HashMap;
  import java.util.LinkedHashSet;
  import java.util.List;
  import java.util.Map;
  import java.util.Set;
  import org.ethelred.temperature4.sensors.SensorResult;
  import org.jspecify.annotations.Nullable;

  class FakeSensorMapping implements SensorMapping {
      private final Set<String> rooms = new LinkedHashSet<>();
      private final Map<String, SensorView> channelMap = new HashMap<>();

      /** Register room with a sensor reading at the given Fahrenheit temperature. */
      void addSensorRoom(String name, double fahrenheit) {
          rooms.add(name);
          var result = new SensorResult(OffsetDateTime.now(), "1",
                  Temperature.fromFahrenheit(fahrenheit), true);
          channelMap.put(name, new DefaultSensorMapping.MappedSensorView(name, result));
      }

      /** Register room as sensor-equipped but with no reading (hasSensor=true, channelForRoom=null). */
      void addSensorRoom(String name) {
          rooms.add(name);
      }

      @Override
      public SensorView channelToRoom(SensorResult sensorResult) {
          throw new UnsupportedOperationException("not used in tests");
      }

      @Override
      public @Nullable SensorView channelForRoom(String room, Collection<SensorResult> sensorResults) {
          return channelMap.get(room);
      }

      @Override
      public List<SensorView> allChannels(Collection<SensorResult> sensorResults) {
          return List.copyOf(channelMap.values());
      }

      @Override
      public boolean hasSensor(String room) {
          return rooms.contains(room);
      }
  }
  ```

- [ ] **Step 2: Write `FakeRoomService`**

  ```java
  // (C) Edward Harman 2026
  package org.ethelred.temperature4;

  import java.util.ArrayList;
  import java.util.LinkedHashMap;
  import java.util.List;
  import java.util.Map;
  import java.util.Optional;

  class FakeRoomService implements RoomService {
      private RoomsAndSensors roomsAndSensors = new RoomsAndSensors(List.of(), List.of());
      private final Map<String, RoomView> rooms = new LinkedHashMap<>();
      final List<String> updateRoomCalls = new ArrayList<>();

      void addRoom(RoomView view) {
          rooms.put(view.name(), view);
          List<NamedResult<RoomView>> namedRooms =
                  rooms.values().stream().map(r -> (NamedResult<RoomView>) r).toList();
          roomsAndSensors = new RoomsAndSensors(namedRooms, List.of());
      }

      @Override
      public RoomsAndSensors getRoomsAndSensors() {
          return roomsAndSensors;
      }

      @Override
      public Optional<RoomView> getRoom(String name) {
          return Optional.ofNullable(rooms.get(name));
      }

      @Override
      public void updateRoom(String name, Mode mode, TemperatureSettingAction action) {
          updateRoomCalls.add(name + ":" + mode + ":" + action);
      }
  }
  ```

- [ ] **Step 3: Write `FakeOpenWeatherClient`**

  ```java
  // (C) Edward Harman 2026
  package org.ethelred.temperature4.openweather;

  import java.time.LocalDateTime;
  import java.util.List;
  import org.ethelred.temperature4.Temperature;

  public class FakeOpenWeatherClient implements OpenWeatherClient {
      private final OpenWeatherResult result = new OpenWeatherResult(
              new Current(LocalDateTime.now(), Temperature.fromFahrenheit(0.0), List.of()),
              List.of());

      @Override
      public OpenWeatherResult getWeather() {
          return result;
      }
  }
  ```

- [ ] **Step 4: Compile**

  ```bash
  ./gradlew :app:compileTestJava
  ```
  Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

  ```bash
  git add app/src/test/java/org/ethelred/temperature4/FakeSensorMapping.java
  git add app/src/test/java/org/ethelred/temperature4/FakeRoomService.java
  git add app/src/test/java/org/ethelred/temperature4/openweather/FakeOpenWeatherClient.java
  git commit -m "test: add FakeSensorMapping, FakeRoomService, FakeOpenWeatherClient"
  ```

---

## Chunk 3: Unit tests

### Task 7: `TemperatureTest`

**Files:**
- Create: `app/src/test/java/org/ethelred/temperature4/TemperatureTest.java`

- [ ] **Step 1: Write the tests**

  ```java
  // (C) Edward Harman 2026
  package org.ethelred.temperature4;

  import static com.google.common.truth.Truth.assertThat;
  import static com.google.common.truth.Truth.assertWithMessage;

  import org.junit.jupiter.api.Test;

  class TemperatureTest {

      @Test
      void freezingPoint() {
          var t = new Temperature(0.0);
          assertThat(t.fahrenheit()).isWithin(0.001).of(32.0);
      }

      @Test
      void boilingPoint() {
          var t = new Temperature(100.0);
          assertThat(t.fahrenheit()).isWithin(0.001).of(212.0);
      }

      @Test
      void fromFahrenheit_roundTrip() {
          var t = Temperature.fromFahrenheit(72.0);
          assertThat(t.fahrenheit()).isWithin(0.001).of(72.0);
      }

      @Test
      void fromScaledInt_celsius() {
          var t = Temperature.fromScaledInt(2150, 2, Temperature.Unit.CELSIUS);
          assertThat(t.celsius()).isWithin(0.001).of(21.5);
      }

      @Test
      void display_roundsToNearestFahrenheit() {
          // 21.3°C = 70.34°F → rounds to 70
          var t = new Temperature(21.3);
          assertThat(t.display()).isEqualTo("70");
      }
  }
  ```

- [ ] **Step 2: Run the tests**

  ```bash
  ./gradlew :app:test --tests "org.ethelred.temperature4.TemperatureTest"
  ```
  Expected: 5 tests pass.

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/test/java/org/ethelred/temperature4/TemperatureTest.java
  git commit -m "test: add TemperatureTest"
  ```

---

### Task 8: `DefaultSensorMappingTest`

**Files:**
- Create: `app/src/test/java/org/ethelred/temperature4/DefaultSensorMappingTest.java`

`DefaultSensorMapping` takes `Configuration`. Use `io.avaje.config.Config.asConfiguration()` which reads `avaje-test.properties` from the test classpath automatically.

- [ ] **Step 1: Write the tests**

  ```java
  // (C) Edward Harman 2026
  package org.ethelred.temperature4;

  import static com.google.common.truth.Truth.assertThat;

  import io.avaje.config.Config;
  import java.time.OffsetDateTime;
  import java.util.List;
  import org.ethelred.temperature4.sensors.SensorResult;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;

  class DefaultSensorMappingTest {

      DefaultSensorMapping mapping;

      @BeforeEach
      void setUp() {
          // avaje-test.properties on test classpath provides sensors.channels=Living Room, Bedroom
          mapping = new DefaultSensorMapping(Config.asConfiguration());
      }

      private SensorResult freshResult(String channel) {
          return new SensorResult(OffsetDateTime.now().minusMinutes(5), channel,
                  Temperature.fromFahrenheit(68.0), true);
      }

      private SensorResult staleResult(String channel) {
          return new SensorResult(OffsetDateTime.now().minusHours(7), channel,
                  Temperature.fromFahrenheit(65.0), true);
      }

      @Test
      void channelForRoom_mapsChannelOneToFirstName() {
          var result = mapping.channelForRoom("Living Room", List.of(freshResult("1")));
          assertThat(result).isNotNull();
          assertThat(result.name()).isEqualTo("Living Room");
      }

      @Test
      void channelForRoom_mapsChannelTwoToSecondName() {
          var result = mapping.channelForRoom("Bedroom", List.of(freshResult("2")));
          assertThat(result).isNotNull();
          assertThat(result.name()).isEqualTo("Bedroom");
      }

      @Test
      void channelForRoom_filtersStaleReadings() {
          var result = mapping.channelForRoom("Living Room", List.of(staleResult("1")));
          assertThat(result).isNull();
      }

      @Test
      void channelForRoom_acceptsFreshReadings() {
          var result = mapping.channelForRoom("Living Room", List.of(freshResult("1")));
          assertThat(result).isNotNull();
      }

      @Test
      void allChannels_deduplicatesByChannel_returnsNewest() {
          var older = new SensorResult(OffsetDateTime.now().minusHours(2), "1",
                  Temperature.fromFahrenheit(65.0), true);
          var newer = new SensorResult(OffsetDateTime.now().minusMinutes(5), "1",
                  Temperature.fromFahrenheit(70.0), true);
          var views = mapping.allChannels(List.of(older, newer));
          assertThat(views).hasSize(1);
          assertThat(views.get(0).temperature().fahrenheit()).isWithin(0.1).of(70.0);
      }

      @Test
      void hasSensor_trueForConfiguredRoom() {
          assertThat(mapping.hasSensor("Living Room")).isTrue();
      }

      @Test
      void hasSensor_falseForUnknownRoom() {
          assertThat(mapping.hasSensor("Garage")).isFalse();
      }
  }
  ```

- [ ] **Step 2: Run the tests**

  ```bash
  ./gradlew :app:test --tests "org.ethelred.temperature4.DefaultSensorMappingTest"
  ```
  Expected: 7 tests pass.

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/test/java/org/ethelred/temperature4/DefaultSensorMappingTest.java
  git commit -m "test: add DefaultSensorMappingTest"
  ```

---

### Task 9: `DefaultSettingUpdaterTest`

**Files:**
- Create: `app/src/test/java/org/ethelred/temperature4/DefaultSettingUpdaterTest.java`

Key: `DefaultSettingUpdater` must be instantiated via `new` (bypassing `@RequiresProperty` which is DI-only). The `avaje-test.properties` file supplies `updater.mintemp=60` and `updater.maxtemp=85`.

- [ ] **Step 1: Write the tests**

  ```java
  // (C) Edward Harman 2026
  package org.ethelred.temperature4;

  import static com.google.common.truth.Truth.assertThat;

  import io.avaje.config.Config;
  import java.time.OffsetDateTime;
  import java.util.List;
  import org.ethelred.temperature4.kumojs.FakeKumoJsRepository;
  import org.ethelred.temperature4.kumojs.RoomStatus;
  import org.ethelred.temperature4.sensors.FakeSensorsRepository;
  import org.ethelred.temperature4.sensors.SensorResult;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;

  class DefaultSettingUpdaterTest {

      FakeKumoJsRepository fakeKumo;
      FakeSensorsRepository fakeSensors;
      InMemorySettingRepository settingRepo;
      FakeSensorMapping sensorMapping;
      DefaultSettingUpdater updater;

      @BeforeEach
      void setUp() {
          fakeKumo = new FakeKumoJsRepository();
          fakeSensors = new FakeSensorsRepository();
          settingRepo = new InMemorySettingRepository();
          sensorMapping = new FakeSensorMapping();
          updater = new DefaultSettingUpdater(
                  Config.asConfiguration(), settingRepo, fakeKumo, fakeSensors, sensorMapping);
      }

      private SensorResult sensorAt(double fahrenheit) {
          return new SensorResult(OffsetDateTime.now(), "1", Temperature.fromFahrenheit(fahrenheit), true);
      }

      private RoomStatus kumoAt(int fahrenheit, String mode) {
          return new RoomStatus(Temperature.fromFahrenheit(fahrenheit), mode,
                  null, Temperature.fromFahrenheit(fahrenheit));
      }

      @Test
      void incrementsKumoTemp_whenSensorBelowSetting() {
          settingRepo.update(new Setting("TestRoom", 70, Mode.heat));
          fakeKumo.addRoom("TestRoom", kumoAt(70, "heat"));
          sensorMapping.addSensorRoom("TestRoom", 68.0); // sensor reads 68°F < setting 70°F

          updater.checkForUpdates(true);

          assertThat(fakeKumo.tempCallArgs).contains("TestRoom:heat:71");
      }

      @Test
      void decrementsKumoTemp_whenSensorAboveSetting() {
          settingRepo.update(new Setting("TestRoom", 70, Mode.heat));
          fakeKumo.addRoom("TestRoom", kumoAt(70, "heat"));
          sensorMapping.addSensorRoom("TestRoom", 72.0); // sensor reads 72°F > setting 70°F

          updater.checkForUpdates(true);

          assertThat(fakeKumo.tempCallArgs).contains("TestRoom:heat:69");
      }

      @Test
      void doesNotExceedMax() {
          // maxtemp=85, kumo already at 85
          settingRepo.update(new Setting("TestRoom", 86, Mode.heat));
          fakeKumo.addRoom("TestRoom", kumoAt(85, "heat"));
          sensorMapping.addSensorRoom("TestRoom", 68.0);

          updater.checkForUpdates(true);

          assertThat(fakeKumo.tempCallArgs).isEmpty();
      }

      @Test
      void doesNotGoBelowMin() {
          // mintemp=60, kumo already at 60
          settingRepo.update(new Setting("TestRoom", 59, Mode.heat));
          fakeKumo.addRoom("TestRoom", kumoAt(60, "heat"));
          sensorMapping.addSensorRoom("TestRoom", 72.0);

          updater.checkForUpdates(true);

          assertThat(fakeKumo.tempCallArgs).isEmpty();
      }

      @Test
      void setsMode_whenModesDiffer() {
          settingRepo.update(new Setting("TestRoom", 70, Mode.heat));
          fakeKumo.addRoom("TestRoom", kumoAt(70, "cool"));
          sensorMapping.addSensorRoom("TestRoom", 70.0);

          updater.checkForUpdates(true);

          assertThat(fakeKumo.modeCallArgs).contains("TestRoom:heat");
      }

      @Test
      void skipsTemperature_whenModeIsOff() {
          settingRepo.update(new Setting("TestRoom", 70, Mode.off));
          fakeKumo.addRoom("TestRoom", new RoomStatus(
                  Temperature.fromFahrenheit(70), "off", null, null));
          sensorMapping.addSensorRoom("TestRoom", 65.0);

          updater.checkForUpdates(true);

          assertThat(fakeKumo.tempCallArgs).isEmpty();
      }

      @Test
      void noUpdate_whenNoSettings() {
          fakeKumo.addRoom("TestRoom", kumoAt(70, "heat"));
          sensorMapping.addSensorRoom("TestRoom", 68.0);
          // no settings in settingRepo

          updater.checkForUpdates(true);

          assertThat(fakeKumo.tempCallArgs).isEmpty();
          assertThat(fakeKumo.modeCallArgs).isEmpty();
      }

      @Test
      void rateLimitRespected() {
          settingRepo.update(new Setting("TestRoom", 70, Mode.heat));
          fakeKumo.addRoom("TestRoom", kumoAt(70, "heat"));
          sensorMapping.addSensorRoom("TestRoom", 68.0);

          updater.checkForUpdates(true);  // triggers, records call
          int callsAfterFirst = fakeKumo.tempCallArgs.size();

          updater.checkForUpdates(false); // should be rate-limited, no new call
          assertThat(fakeKumo.tempCallArgs).hasSize(callsAfterFirst);
      }
  }
  ```

- [ ] **Step 2: Run the tests, fix any failures**

  ```bash
  ./gradlew :app:test --tests "org.ethelred.temperature4.DefaultSettingUpdaterTest"
  ```
  Expected: 8 tests pass. If `channelForRoom` returns null (sensor not found), `checkForUpdate` is skipped — fix the fake's addSensorRoom to store a real SensorView.

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/test/java/org/ethelred/temperature4/DefaultSettingUpdaterTest.java
  git commit -m "test: add DefaultSettingUpdaterTest"
  ```

---

### Task 10: `DefaultRoomServiceTest`

**Files:**
- Create: `app/src/test/java/org/ethelred/temperature4/DefaultRoomServiceTest.java`

- [ ] **Step 1: Write the tests**

  ```java
  // (C) Edward Harman 2026
  package org.ethelred.temperature4;

  import static com.google.common.truth.Truth.assertThat;

  import java.time.OffsetDateTime;
  import java.util.List;
  import org.ethelred.temperature4.kumojs.FakeKumoJsRepository;
  import org.ethelred.temperature4.kumojs.RoomStatus;
  import org.ethelred.temperature4.sensors.FakeSensorsRepository;
  import org.ethelred.temperature4.sensors.SensorResult;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;

  class DefaultRoomServiceTest {

      FakeKumoJsRepository fakeKumo;
      FakeSensorsRepository fakeSensors;
      InMemorySettingRepository settingRepo;
      FakeSensorMapping sensorMapping;
      NoOpSettingUpdater noOpUpdater;
      DefaultRoomService service;

      @BeforeEach
      void setUp() {
          fakeKumo = new FakeKumoJsRepository();
          fakeSensors = new FakeSensorsRepository();
          settingRepo = new InMemorySettingRepository();
          sensorMapping = new FakeSensorMapping();
          noOpUpdater = new NoOpSettingUpdater();
          service = new DefaultRoomService(fakeSensors, settingRepo, sensorMapping, noOpUpdater, fakeKumo);
      }

      private RoomStatus kumoAt(int fahrenheit, String mode) {
          return new RoomStatus(Temperature.fromFahrenheit(fahrenheit), mode,
                  null, Temperature.fromFahrenheit(fahrenheit));
      }

      @Test
      void getRoom_returnsEmpty_forUnknownRoom() {
          var result = service.getRoom("NonExistent");
          assertThat(result).isEmpty();
      }

      @Test
      void getRoom_returnsRoom_forKnownRoom() {
          fakeKumo.addRoom("TestRoom", kumoAt(70, "heat"));
          var result = service.getRoom("TestRoom");
          assertThat(result).isPresent();
          assertThat(result.get().name()).isEqualTo("TestRoom");
      }

      @Test
      void combine_withoutSensor_showsKumoDisplaySetting() {
          fakeKumo.addRoom("TestRoom", kumoAt(70, "heat"));
          var result = service.getRoom("TestRoom");
          assertThat(result).isPresent();
          // No sensor → displaySetting comes from kumo setpoint only, no HTML span wrapper
          assertThat(result.get().displaySetting()).doesNotContain("<span");
      }

      @Test
      void combine_withSensorAndSetting_showsBoth() {
          fakeKumo.addRoom("TestRoom", kumoAt(70, "heat"));
          settingRepo.update(new Setting("TestRoom", 70, Mode.heat));
          sensorMapping.addSensorRoom("TestRoom", 68.0); // sensor at 68°F

          var result = service.getRoom("TestRoom");
          assertThat(result).isPresent();
          // With sensor + setting, roomTemp includes sensor reading AND kumo reading
          assertThat(result.get().roomTemp()).contains("68");
          assertThat(result.get().roomTemp()).contains("70");
      }

      @Test
      void updateRoom_savesSetting_whenSensorPresent() {
          fakeKumo.addRoom("TestRoom", kumoAt(70, "heat"));
          sensorMapping.addSensorRoom("TestRoom"); // hasSensor = true, no reading needed

          service.updateRoom("TestRoom", Mode.heat, TemperatureSettingAction.NONE);

          var saved = settingRepo.findByRoom("TestRoom");
          assertThat(saved).isNotNull();
          assertThat(saved.mode()).isEqualTo(Mode.heat);
      }

      @Test
      void updateRoom_callsKumoDirect_whenNoSensor() {
          fakeKumo.addRoom("TestRoom", kumoAt(70, "cool"));
          // sensorMapping has no entry for TestRoom → hasSensor = false

          service.updateRoom("TestRoom", Mode.heat, TemperatureSettingAction.NONE);

          assertThat(fakeKumo.modeCallArgs).contains("TestRoom:heat");
      }

      @Test
      void updateRoom_noOp_whenModeAndActionUnchanged() {
          fakeKumo.addRoom("TestRoom", kumoAt(70, "heat"));
          // no sensor → direct kumo path; mode already matches, NONE action → guard skips body

          service.updateRoom("TestRoom", Mode.heat, TemperatureSettingAction.NONE);

          assertThat(fakeKumo.modeCallArgs).isEmpty();
          assertThat(fakeKumo.tempCallArgs).isEmpty();
      }
  }
  ```

- [ ] **Step 2: Run the tests, fix any failures**

  ```bash
  ./gradlew :app:test --tests "org.ethelred.temperature4.DefaultRoomServiceTest"
  ```
  Expected: 6 tests pass.

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/test/java/org/ethelred/temperature4/DefaultRoomServiceTest.java
  git commit -m "test: add DefaultRoomServiceTest"
  ```

---

## Chunk 4: HTTP integration tests

### Task 11: `UIControllerTest`

**Files:**
- Create: `app/src/test/java/org/ethelred/temperature4/UIControllerTest.java`

Key setup facts:
- `UIController$Route(UIController controller)` — one-arg constructor
- `UIController(Configuration, Templates, OpenWeatherRepository, RoomService)` — four-arg constructor
- `Templates` = `new StaticTemplates()` (JTE generated, in package `org.ethelred.temperature4.template`)
- `Configuration` = `Config.asConfiguration()` (reads `avaje-test.properties` with `server.contextPath=/`)
- Don't follow redirects in `HttpClient` — use `.followRedirects(HttpClient.Redirect.NEVER)`

- [ ] **Step 1: Write the tests**

  ```java
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
          var controller = new UIController(
                  Config.asConfiguration(),
                  new StaticTemplates(),
                  weatherRepo,
                  fakeRoomService);
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
          assertThat(response.headers().firstValue("content-type").orElse(""))
                  .contains("text/html");
      }

      @Test
      void getRoom_returns200_forKnownRoom() throws Exception {
          fakeRoomService.addRoom(new SimpleRoomView("TestRoom"));
          var response = http.send(
                  HttpRequest.newBuilder(URI.create(baseUrl() + "/room/TestRoom")).GET().build(),
                  HttpResponse.BodyHandlers.ofString());
          assertThat(response.statusCode()).isEqualTo(200);
      }

      @Test
      void getRoom_returns404_forUnknownRoom() throws Exception {
          var response = http.send(
                  HttpRequest.newBuilder(URI.create(baseUrl() + "/room/NoSuchRoom")).GET().build(),
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
          assertThat(response.headers().firstValue("location").orElse(""))
                  .contains("/room/TestRoom");
      }

      // Minimal RoomView for test setup
      private record SimpleRoomView(String name) implements RoomView {
          @Override public String roomTemp() { return "70"; }
          @Override public String mode() { return "heat"; }
          @Override public String displaySetting() { return "70"; }
      }
  }
  ```

- [ ] **Step 2: Run the tests, fix any failures**

  ```bash
  ./gradlew :app:test --tests "org.ethelred.temperature4.UIControllerTest"
  ```
  Expected: 4 tests pass.

  **Common issues:**
  - `StaticTemplates` class-not-found: run `./gradlew :app:generateJte` first, then re-run tests.
  - `getIndex_returns200` gets 404: the generated route registers path `""` (empty string). Javalin 7 normalises `""` and `"/"` to the same route when no contextPath is set. If the test fails with 404, change the test URL from `"/"` to `""` or add `cfg.router.contextPath = "/"` to the test's `Javalin.create(...)` call.

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/test/java/org/ethelred/temperature4/UIControllerTest.java
  git commit -m "test: add UIControllerTest"
  ```

---

### Task 12: `SettingControllerTest`

**Files:**
- Create: `app/src/test/java/org/ethelred/temperature4/SettingControllerTest.java`

Key facts:
- `SettingController$Route(SettingController controller, Jsonb jsonb)` — two-arg constructor
- Create `Jsonb` via `Jsonb.builder().build()` — ServiceLoader auto-discovers `GeneratedJsonComponent` which registers `SettingJsonAdapter`
- POST to `/api/settings` returns 201; `checkForUpdates` also returns 201 (from generated route)

- [ ] **Step 1: Write the tests**

  ```java
  // (C) Edward Harman 2026
  package org.ethelred.temperature4;

  import static com.google.common.truth.Truth.assertThat;

  import io.avaje.jsonb.Jsonb;
  import io.javalin.Javalin;
  import java.net.URI;
  import java.net.http.HttpClient;
  import java.net.http.HttpRequest;
  import java.net.http.HttpResponse;
  import java.util.List;
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
          var jsonb = Jsonb.builder().build(); // auto-discovers GeneratedJsonComponent via ServiceLoader
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
                  HttpRequest.newBuilder(URI.create(baseUrl() + "/api/settings")).GET().build(),
                  HttpResponse.BodyHandlers.ofString());
          assertThat(response.statusCode()).isEqualTo(200);
          assertThat(response.body()).isEqualTo("[]");
      }

      @Test
      void update_thenGetAll_returnsSetting() throws Exception {
          http.send(
                  HttpRequest.newBuilder(URI.create(baseUrl() + "/api/settings"))
                          .header("Content-Type", "application/json")
                          .POST(HttpRequest.BodyPublishers.ofString(
                                  "{\"room\":\"TestRoom\",\"settingFahrenheit\":70,\"mode\":\"heat\"}"))
                          .build(),
                  HttpResponse.BodyHandlers.ofString());

          var response = http.send(
                  HttpRequest.newBuilder(URI.create(baseUrl() + "/api/settings")).GET().build(),
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
          // Generated SettingController$Route sets ctx.status(201) for all POST endpoints
          assertThat(response.statusCode()).isEqualTo(201);
      }
  }
  ```

- [ ] **Step 2: Run the tests, fix any failures**

  ```bash
  ./gradlew :app:test --tests "org.ethelred.temperature4.SettingControllerTest"
  ```
  Expected: 3 tests pass.

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/test/java/org/ethelred/temperature4/SettingControllerTest.java
  git commit -m "test: add SettingControllerTest"
  ```

---

## Final verification

- [ ] **Run all app tests**

  ```bash
  ./gradlew :app:test
  ```
  Expected: all new tests pass (≥ 29 tests).

- [ ] **Run kumo tests to check nothing regressed**

  ```bash
  ./gradlew :kumo:test
  ```
  Expected: 7 tests pass.

- [ ] **Run full check (includes Spotless formatting)**

  ```bash
  ./gradlew :app:check
  ```
  Expected: BUILD SUCCESSFUL. If Spotless fails, run `./gradlew :app:spotlessApply` then re-run.

- [ ] **Final commit**

  If any formatting fixes were needed:
  ```bash
  git add -u
  git commit -m "style: apply Spotless formatting to test files"
  ```
