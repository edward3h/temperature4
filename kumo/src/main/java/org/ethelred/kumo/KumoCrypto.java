// (C) Edward Harman 2026
package org.ethelred.kumo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class KumoCrypto {

    public String computeHash(KumoDeviceConfig device, String jsonPayload) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");

            // Step 1: SHA256(password + payload)
            var payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
            digest.update(device.password());
            digest.update(payloadBytes);
            var dt1 = digest.digest();

            // Step 2: Build 88-byte buffer
            var buf = new byte[88];
            // bytes 0-31: device.w() (32 bytes)
            System.arraycopy(device.w(), 0, buf, 0, 32);
            // bytes 32-63: dt1 (32 bytes)
            System.arraycopy(dt1, 0, buf, 32, 32);
            // fixed bytes
            buf[64] = 8;
            buf[65] = 64;
            buf[66] = (byte) device.s();
            // bytes 79-87: cryptoSerial reordered [8,4,5,6,7,0,1,2,3]
            var cs = device.cryptoSerial();
            buf[79] = cs[8];
            buf[80] = cs[4];
            buf[81] = cs[5];
            buf[82] = cs[6];
            buf[83] = cs[7];
            buf[84] = cs[0];
            buf[85] = cs[1];
            buf[86] = cs[2];
            buf[87] = cs[3];

            // Step 3: SHA256(buf)
            digest.reset();
            var result = digest.digest(buf);

            // Step 4: hex encode
            var sb = new StringBuilder(64);
            for (var b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
