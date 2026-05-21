// (C) Edward Harman 2026
package org.ethelred.temperature4;

import static com.google.common.truth.Truth.assertThat;

import io.avaje.config.Config;
import java.util.List;
import org.ethelred.temperature4.kumojs.FakeKumoJsRepository;
import org.ethelred.temperature4.kumojs.RoomStatus;
import org.ethelred.temperature4.sensors.FakeSensorsRepository;
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

    private RoomStatus kumoAt(int fahrenheit, String mode) {
        return new RoomStatus(Temperature.fromFahrenheit(fahrenheit), mode,
                null, Temperature.fromFahrenheit(fahrenheit));
    }

    @Test
    void incrementsKumoTemp_whenSensorBelowSetting() {
        settingRepo.update(new Setting("TestRoom", 70, Mode.heat));
        fakeKumo.addRoom("TestRoom", kumoAt(70, "heat"));
        sensorMapping.addSensorRoom("TestRoom", 68.0);

        updater.checkForUpdates(true);

        assertThat(fakeKumo.tempCallArgs).contains("TestRoom:heat:71");
    }

    @Test
    void decrementsKumoTemp_whenSensorAboveSetting() {
        settingRepo.update(new Setting("TestRoom", 70, Mode.heat));
        fakeKumo.addRoom("TestRoom", kumoAt(70, "heat"));
        sensorMapping.addSensorRoom("TestRoom", 72.0);

        updater.checkForUpdates(true);

        assertThat(fakeKumo.tempCallArgs).contains("TestRoom:heat:69");
    }

    @Test
    void doesNotExceedMax() {
        settingRepo.update(new Setting("TestRoom", 86, Mode.heat));
        fakeKumo.addRoom("TestRoom", kumoAt(85, "heat"));
        sensorMapping.addSensorRoom("TestRoom", 68.0);

        updater.checkForUpdates(true);

        assertThat(fakeKumo.tempCallArgs).isEmpty();
    }

    @Test
    void doesNotGoBelowMin() {
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

        updater.checkForUpdates(true);

        assertThat(fakeKumo.tempCallArgs).isEmpty();
        assertThat(fakeKumo.modeCallArgs).isEmpty();
    }

    @Test
    void rateLimitRespected() {
        settingRepo.update(new Setting("TestRoom", 70, Mode.heat));
        fakeKumo.addRoom("TestRoom", kumoAt(70, "heat"));
        sensorMapping.addSensorRoom("TestRoom", 68.0);

        updater.checkForUpdates(true);
        int callsAfterFirst = fakeKumo.tempCallArgs.size();

        updater.checkForUpdates(false);
        assertThat(fakeKumo.tempCallArgs).hasSize(callsAfterFirst);
    }
}
