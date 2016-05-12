package org.candlepin.controller;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.candlepin.guice.PrincipalProvider;
import org.candlepin.model.Consumer;
import org.candlepin.model.ManifestRecord;
import org.candlepin.model.ManifestRecordCurator;
import org.candlepin.model.ManifestRecord.ManifestRecordType;
import org.candlepin.model.Owner;
import org.candlepin.sync.ManifestFileService;
import org.candlepin.sync.ManifestServiceException;
import org.candlepin.sync.file.ManifestFile;
import org.candlepin.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class ManifestManager {

    private static Logger log = LoggerFactory.getLogger(ManifestManager.class);
    private ManifestRecordCurator manifestRecordCurator;
    private ManifestFileService manifestFileService;
    private PrincipalProvider principalProvider;

    @Inject
    public ManifestManager(ManifestRecordCurator manifestRecordCurator, ManifestFileService manifestFileService,
        PrincipalProvider principalProvider) {
        this.manifestRecordCurator = manifestRecordCurator;
        this.manifestFileService = manifestFileService;
        this.principalProvider = principalProvider;
    }

    /**
     * Gets a manifest matching the specified id. A files id should be
     * a unique identifier such as a database entity ID, or a file path.
     *
     * @param id the id of the target manifest.
     * @return a {@link ManifestFile} matching the id, null otherwise.
     */
    public ManifestRecord get(String id) throws ManifestServiceException {
        return manifestRecordCurator.find(id);
    }

    public ManifestFile getFile(ManifestRecord record) {
        return manifestFileService.get(record.getFileId());
    }

    /**
     * Deletes a manifest matching the specified id.
     *
     * @param id the id of the target manifest file.
     * @return true if the record was deleted, false otherwise.
     */
    @Transactional
    public boolean delete(String id) throws ManifestServiceException {
        ManifestRecord record = manifestRecordCurator.find(id);
        if (record == null) {
            // nothing to do
            return true;
        }
        if (!manifestFileService.delete(record.getFileId())) {
            return false;
        }

        // If this fails, we will end up with manifest records that have broken
        // file references, to the service, since they were deleted above. There isn't
        // much we can do about this here since the file service could be on another
        // system and we can't really roll it back. For this reason, the ExportCleaner
        // job will delete any rogue ManifestRecords for us.
        return manifestRecordCurator.deleteById(record.getId());
    }

    /**
     * Stores the specified manifest import file.
     *
     * @param the manifest import {@link File} to store
     * @return the id of the stored manifest file.
     */
    @Transactional
    public ManifestRecord storeImport(File importFile, Owner targetOwner) throws ManifestServiceException {
        // Store the manifest record, and then store the file.
        // TODO: Check to see if we are allowed to do this based on the principal.
        return storeFile(importFile, ManifestRecordType.IMPORT, targetOwner.getKey());
    }

    /**
     * Stores the specified manifest export file.
     *
     * @param exportFile the manifest export {@link File} to store.
     * @return the id of the stored manifest file.
     * @throws ManifestServiceException
     */
    @Transactional
    public ManifestRecord storeExport(File exportFile, Consumer distributor) throws ManifestServiceException {
        // Store the manifest record, and then store the file.
        // TODO: Check to see if we are allowed to do this based on the principal.
        return storeFile(exportFile, ManifestRecordType.EXPORT, distributor.getUuid());
    }

    /**
     * Performs a cleanup of the manifest records. It will remove records that
     * are of the specified age (in minutes) and will clean up all related files.
     * Because the uploaded/downloaded manifest files are provided by a service
     * any rogue records are removed as well.
     *
     * @param maxAge the maximum age of the file in minutes. A negative value
     *               indicates no expiry.
     * @return the number of expired exports that were deleted.
     */
    @Transactional
    public int cleanup(int maxAgeInMinutes) throws ManifestServiceException {
        if (maxAgeInMinutes < 0) {
            return 0;
        }

        List<ManifestRecord> expired =
            manifestRecordCurator.getExpired(Util.addMinutesToDt(maxAgeInMinutes * -1),
                ManifestRecordType.EXPORT);
        Set<String> fileIds = new HashSet<String>();
        for (ManifestRecord record : expired) {
            fileIds.add(record.getFileId());
        }

        List<String> deletedFileIds = manifestFileService.delete(fileIds);
        if (log.isDebugEnabled()) {
            for (String deleted : deletedFileIds) {
                log.debug("Deleted file: {}", deleted);
            }
        }

        // Make sure that we only delete the manifest records who's file
        // was also deleted.
        List<ManifestRecord> toDelete = new LinkedList<ManifestRecord>();
        for (ManifestRecord record: expired) {
            if (deletedFileIds.contains(record.getFileId())) {
                toDelete.add(record);
            }
        }

        // If this fails, we will end up with manifest records that have broken
        // file references, to the service, since they were deleted above. There isn't
        // much we can do about this here since the file service could be on another
        // system and we can't really roll it back. For this reason, the ExportCleaner
        // job will delete any rogue ManifestRecords for us.
        // TODO Implement this.
        manifestRecordCurator.bulkDelete(toDelete);
        return toDelete.size();
    }

    private ManifestRecord storeFile(File targetFile, ManifestRecordType type, String targetId)
        throws ManifestServiceException {
        // Store the manifest record, and then store the file.
        String fileId = manifestFileService.store(targetFile);
        ManifestRecord manifestRecord = manifestRecordCurator.create(
            new ManifestRecord(type, fileId, principalProvider.get().getName(), targetId));
        return manifestRecord;
    }
}
