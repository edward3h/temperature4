// (C) Edward Harman 2026
package org.ethelred.kumo;

public record KumoDeviceConfig(String label, String address, byte[] password, byte[] cryptoSerial, int s, byte[] w) {}
