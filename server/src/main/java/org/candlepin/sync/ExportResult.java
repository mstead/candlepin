package org.candlepin.sync;

import java.io.Serializable;

/**
 * Represents the result from an async export job. This class simply defines
 * the appropriate meta data to create a link to download the manifest.
 *
 */
public class ExportResult implements Serializable {

    // FIXME This will prevent correcting paths if it changes.
    //       Store the essentials that are required to formulate the path.
    private String exportedConsumer;
    private String exportId;
    private String href;

    public ExportResult(String exportedConsumer, String exportId) {
        this.exportedConsumer = exportedConsumer;
        this.exportId = exportId;
        this.href = String.format("/consumers/%s/export/download?export_id=%s", this.exportedConsumer,
            this.exportId);
    }

    public String getExportedConsumer() {
        return exportedConsumer;
    }

    public String getExportId() {
        return exportId;
    }

    public String getHref() {
        return this.href;
    }

}
