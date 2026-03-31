// (C) Edward Harman 2026
package org.ethelred.kumo;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

class KumoCryptoTest {

    @Test
    void computeHash_returnsHexString() {
        var device = new KumoDeviceConfig(
                "Test Room",
                "192.168.1.100",
                new byte[] {1, 2, 3, 4},
                new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8},
                0,
                new byte[32]);
        var crypto = new KumoCrypto();
        var hash = crypto.computeHash(device, "{\"c\":{\"indoorUnit\":{\"status\":{}}}}");
        assertThat(hash).hasLength(64);
        assertThat(hash).matches("[0-9a-f]+");
    }

    @Test
    void computeHash_isDeterministic() {
        var device = new KumoDeviceConfig(
                "Test Room",
                "192.168.1.100",
                new byte[] {1, 2, 3, 4},
                new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8},
                2,
                new byte[32]);
        var crypto = new KumoCrypto();
        var payload = "{\"c\":{\"indoorUnit\":{\"status\":{}}}}";
        var hash1 = crypto.computeHash(device, payload);
        var hash2 = crypto.computeHash(device, payload);
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void computeHash_differsForDifferentPayloads() {
        var device = new KumoDeviceConfig(
                "Test Room",
                "192.168.1.100",
                new byte[] {1, 2, 3, 4},
                new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8},
                0,
                new byte[32]);
        var crypto = new KumoCrypto();
        var hash1 = crypto.computeHash(device, "{\"c\":{\"indoorUnit\":{\"status\":{}}}}");
        var hash2 = crypto.computeHash(device, "{\"c\":{\"indoorUnit\":{\"status\":{\"mode\":\"heat\"}}}}");
        assertThat(hash1).isNotEqualTo(hash2);
    }
}
