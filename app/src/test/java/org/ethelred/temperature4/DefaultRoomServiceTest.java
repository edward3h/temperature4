// (C) Edward Harman 2026
package org.ethelred.temperature4;

import static com.google.common.truth.Truth.assertThat;

import org.ethelred.temperature4.kumojs.FakeKumoJsRepository;
import org.ethelred.temperature4.kumojs.RoomStatus;
import org.ethelred.temperature4.sensors.FakeSensorsRepository;
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
        fakeSensors = new FakeSensorsRepository(); // wired but unused: FakeSensorMapping.allChannels bypasses it
        settingRepo = new InMemorySettingRepository();
        sensorMapping = new FakeSensorMapping();
        noOpUpdater = new NoOpSettingUpdater();
        service = new DefaultRoomService(fakeSensors, settingRepo, sensorMapping, noOpUpdater, fakeKumo);
    }

    private RoomStatus kumoAt(int fahrenheit, String mode) {
        return new RoomStatus(
                Temperature.fromFahrenheit(fahrenheit), mode, null, Temperature.fromFahrenheit(fahrenheit));
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
        assertThat(result.get().displaySetting()).doesNotContain("<span");
    }

    @Test
    void combine_withSensorAndSetting_showsBoth() {
        fakeKumo.addRoom("TestRoom", kumoAt(70, "heat"));
        settingRepo.update(new Setting("TestRoom", 70, Mode.heat));
        sensorMapping.addSensorRoom("TestRoom", 68.0);

        var result = service.getRoom("TestRoom");
        assertThat(result).isPresent();
        assertThat(result.get().roomTemp()).contains("68");
        assertThat(result.get().roomTemp()).contains("70");
    }

    @Test
    void updateRoom_savesSetting_whenSensorPresent() {
        fakeKumo.addRoom("TestRoom", kumoAt(70, "cool")); // start in cool mode
        sensorMapping.addSensorRoom("TestRoom");

        service.updateRoom("TestRoom", Mode.heat, TemperatureSettingAction.NONE); // change to heat

        var saved = settingRepo.findByRoom("TestRoom");
        assertThat(saved).isNotNull();
        assertThat(saved.mode()).isEqualTo(Mode.heat);
    }

    @Test
    void updateRoom_callsKumoDirect_whenNoSensor() {
        fakeKumo.addRoom("TestRoom", kumoAt(70, "cool"));

        service.updateRoom("TestRoom", Mode.heat, TemperatureSettingAction.NONE);

        assertThat(fakeKumo.modeCallArgs).contains("TestRoom:heat");
    }

    @Test
    void updateRoom_noOp_whenModeAndActionUnchanged() {
        fakeKumo.addRoom("TestRoom", kumoAt(70, "heat"));

        service.updateRoom("TestRoom", Mode.heat, TemperatureSettingAction.NONE);

        assertThat(fakeKumo.modeCallArgs).isEmpty();
        assertThat(fakeKumo.tempCallArgs).isEmpty();
    }

    @Test
    void updateRoom_optimisticResponse_reflectsNewTemp_whenNoSensor() {
        fakeKumo.addRoom("TestRoom", kumoAt(70, "heat"));

        var result = service.updateRoom("TestRoom", Mode.heat, TemperatureSettingAction.INCREMENT);

        assertThat(result).isPresent();
        assertThat(result.get().displaySetting()).isEqualTo("72");
    }
}
