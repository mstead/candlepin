package org.candlepin.sync;

public class ManifestServiceException extends RuntimeException {

    public ManifestServiceException(String message) {
        super(message);
    }

    public ManifestServiceException(String message, Throwable e) {
        super(message, e);
    }

    public ManifestServiceException(Throwable e) {
        super(e);
    }
}
