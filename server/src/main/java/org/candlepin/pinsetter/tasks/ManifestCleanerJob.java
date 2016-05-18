package org.candlepin.pinsetter.tasks;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.candlepin.common.config.Configuration;
import org.candlepin.config.ConfigProperties;
import org.candlepin.controller.ManifestManager;
import org.candlepin.model.ManifestRecord;
import org.candlepin.sync.ManifestFileService;
import org.candlepin.util.Util;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
* This pinsetter task is responsible for cleaning up all artifacts that may result
* from importing/exporting manifest files. It has three key responsibilities:
*
* 1) It examines the scratch directory where the importer/exporter writes temp
*    files and deletes any that are older than a configured age (default 24 hours).
*
* 2) Deletes expired files that live on the {@link ManifestFileService} (default 24 hours).
*
* 3) Deletes any rogue {@link ManifestRecord}s that exist without a file in the service. This case should
*    only happen if the {@link ManifestFileService} implementation is remote, has already deleted the file,
*    and deleting of the {@link ManifestRecord} fails for some reason.
*
*/
public class ManifestCleanerJob extends KingpinJob {

    public static final String DEFAULT_SCHEDULE = "0 0 12 * * ?";
    private static Logger log = LoggerFactory.getLogger(ManifestCleanerJob.class);

    private Configuration config;
    private ManifestManager manifestManager;

    @Inject
    public ManifestCleanerJob(Configuration config, ManifestManager manifestService) {
        this.config = config;
        this.manifestManager = manifestService;
    }

    @Override
    public void toExecute(JobExecutionContext arg0) throws JobExecutionException {
        File baseDir = new File(config.getString(ConfigProperties.SYNC_WORK_DIR));
        int maxAgeInMinutes = config.getInt(ConfigProperties.EXPORT_CLEANER_JOB_MAX_AGE_IN_MINUTES);

        log.info("Manifest cleanup started:");
        log.info("Max Age: {} mins ({} hours)", maxAgeInMinutes, maxAgeInMinutes / 60);
        cleanupExportWorkDirs(baseDir, maxAgeInMinutes);
        manifestServiceCleanup(maxAgeInMinutes);
    }

    private void cleanupExportWorkDirs(File baseDir, int maxAgeInMinutes) {
        long dirCount = 0;
        long delCount = 0;
        long leftCount = 0;

        Date cutOff = Util.addMinutesToDt(maxAgeInMinutes * -1);

        if (baseDir.listFiles() != null) {
            dirCount =  baseDir.listFiles().length;
            for (File f : baseDir.listFiles()) {
                if (f.lastModified() < cutOff.getTime()) {
                    try {
                        FileUtils.deleteDirectory(f);
                        delCount++;
                    }
                    catch (IOException io) {
                        log.error("Unable to delete export directory that is old enough " +
                            "to delete", io);
                    }
                }
                else {
                    leftCount++;
                }
            }
        }
        log.info("Begining directory count: " + dirCount);
        log.info("Directories deleted: " + delCount);
        log.info("Directories remaining: " + leftCount);
    }

    private void manifestServiceCleanup(int maxAgeInMinutes) {
        int deleted = manifestManager.cleanup(maxAgeInMinutes);
        log.info("Deleted from file service: {}", deleted);
    }
}
