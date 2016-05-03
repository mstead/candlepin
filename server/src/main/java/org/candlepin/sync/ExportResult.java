package org.candlepin.sync;

import java.io.Serializable;

import org.candlepin.model.Consumer;

/**
 * Represents the result from an async export job. This class simply defines
 * the appropriate link back to the generated export file
 *
 */
public class ExportResult implements Serializable {

    private String manifestHref;

    public ExportResult(Consumer target, String exportId) {
        manifestHref = String.format("/consumers/%s/export/%s", target.getUuid(), exportId);
    }

    public String getManifestHref() {
        return manifestHref;
    }

    public void setManifestHref(String manifestHref) {
        this.manifestHref = manifestHref;
    }

}
