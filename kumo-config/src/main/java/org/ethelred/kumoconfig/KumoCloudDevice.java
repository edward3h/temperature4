// (C) Edward Harman 2026
package org.ethelred.kumoconfig;

public record KumoCloudDevice(
        String serial, String label, String cryptoSerial, String cryptoKeySet, String password, String address) {}
