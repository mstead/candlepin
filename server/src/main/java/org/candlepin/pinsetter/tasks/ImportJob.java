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

import static org.quartz.JobBuilder.newJob;

import java.io.IOException;
import java.util.HashMap;

import org.candlepin.common.exceptions.BadRequestException;
import org.candlepin.common.exceptions.IseException;
import org.candlepin.model.ImportRecord;
import org.candlepin.model.Owner;
import org.candlepin.model.OwnerCurator;
import org.candlepin.pinsetter.core.model.JobStatus;
import org.candlepin.sync.ConflictOverrides;
import org.candlepin.sync.Importer;
import org.candlepin.sync.ImporterException;
import org.candlepin.sync.ManifestFileService;
import org.candlepin.sync.ManifestServiceException;
import org.candlepin.sync.SyncDataFormatException;
import org.candlepin.util.Util;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Runs an asynchronous manifest import. This job expects that the manifest file
 * was already uploaded.
 */
public class ImportJob extends UniqueByEntityJob {

    protected static final String STORED_FILE_ID = "stored_manifest_file_id";
    protected static final String CONFLICT_OVERRIDES = "conflict_overrides";
    protected static final String UPLOADED_FILE_NAME = "uploaded_file_name";

    private static Logger log = LoggerFactory.getLogger(ImportJob.class);

    private Importer importer;
    private OwnerCurator ownerCurator;
    private ManifestFileService manifestService;

    @Inject
    public ImportJob(Importer importer, OwnerCurator ownerCurator, ManifestFileService manifestService) {
        this.importer = importer;
        this.ownerCurator = ownerCurator;
        this.manifestService = manifestService;
    }

    @Override
    public void toExecute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap map = context.getMergedJobDataMap();
        String ownerKey = (String) map.get(JobStatus.TARGET_ID);
        ConflictOverrides overrides = new ConflictOverrides((String[]) map.get(CONFLICT_OVERRIDES));
        String storedFileId = (String) map.get(STORED_FILE_ID);
        String uploadedFileName = (String) map.get(UPLOADED_FILE_NAME);

        Throwable caught = null;
        Owner targetOwner = null;
        try {
            targetOwner = ownerCurator.lookupByKey(ownerKey);
            if (targetOwner == null) {
                context.setResult("Nothing to do. Owner no longer exists.");
                return;
            }

            ImportRecord importRecord =
                importer.loadStoredExport(targetOwner, storedFileId, overrides, uploadedFileName);
            context.setResult(importRecord);
        }
        // TODO We wrap the exceptions in CandlepinException so that we can get a
        //      more detailed exception data such as Conflicts, but this really doesn't
        //      feel right. Investigate if there's a better way have the JobDetail capture
        //      the extra data.
        // TODO Is the exception catching below required? Or is it put there for test purposes?

        // Catch and handle SyncDataFormatException and ImporterExceptions
        // as the OwnerResource.importManifest does which will provide a little more
        // info about the exception that was thrown (CandlepinException).
        catch (SyncDataFormatException e) {
            caught = new BadRequestException(e.getMessage(), e);
        }
        catch (ImporterException e) {
            caught = new IseException(e.getMessage(), e);
        }
        catch (Exception e) {
            caught = e;
        }

        if (caught != null) {
            log.error("ImportJob encountered a problem.", caught);
            importer.recordImportFailure(targetOwner, new HashMap<String, Object>(), caught,
                uploadedFileName);
            context.setResult(caught.getMessage());
            // If an exception was thrown, the importer's transaction was rolled
            // back. We want to make sure that the file gets deleted so that it
            // doesn't take up disk space. It may be possible that the file was
            // already deleted, but we attempt it anyway.
            importer.deleteStoredManifest(storedFileId);
            throw new JobExecutionException(caught.getMessage(), caught, false);
        }
    }

    public static JobDetail scheduleImport(Owner owner, String storedFileId, String uploadedFileName,
        ConflictOverrides overrides) {
        JobDataMap map = new JobDataMap();
        map.put(JobStatus.OWNER_ID, owner.getKey());
        map.put(JobStatus.TARGET_TYPE, JobStatus.TargetType.OWNER);
        map.put(JobStatus.TARGET_ID, owner.getKey());
        map.put(STORED_FILE_ID, storedFileId);
        map.put(UPLOADED_FILE_NAME, uploadedFileName);
        map.put(CONFLICT_OVERRIDES, overrides.asStringArray());

        JobDetail detail = newJob(ImportJob.class)
            .withIdentity("import_" + Util.generateUUID())
            .usingJobData(map)
            .build();
        return detail;
    }
}
