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
