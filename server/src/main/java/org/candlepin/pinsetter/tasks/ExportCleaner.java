/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.pinsetter.tasks;

import org.candlepin.common.config.Configuration;
import org.candlepin.config.ConfigProperties;
import org.candlepin.controller.ManifestManager;
import org.candlepin.util.Util;

import com.google.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * ExportCleaner
 *
 * This pinsetter task examines the directory where the exporter compiles its
 * information and resultant zip file. Data that is more that a day old will
 * be expunged.
 *
 */
public class ExportCleaner extends KingpinJob {

    public static final String DEFAULT_SCHEDULE = "0 0 12 * * ?";
    private static Logger log = LoggerFactory.getLogger(ExportCleaner.class);

    private Configuration config;
    private ManifestManager manifestManager;

    @Inject
    public ExportCleaner(Configuration config, ManifestManager manifestService) {
        this.config = config;
        this.manifestManager = manifestService;
    }

    @Override
    public void toExecute(JobExecutionContext arg0) throws JobExecutionException {
        File baseDir = new File(config.getString(ConfigProperties.SYNC_WORK_DIR));
        int maxAgeInMinutes = config.getInt(ConfigProperties.EXPORT_CLEANER_JOB_MAX_AGE_IN_MINUTES);

        log.info("Export Data Cleaner run:");
        log.info("Max Age: {} mins", maxAgeInMinutes);
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
        // TODO: Delete rogue ManifestRecords
    }
}
