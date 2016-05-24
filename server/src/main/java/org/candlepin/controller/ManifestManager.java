package org.candlepin.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.candlepin.common.exceptions.BadRequestException;
import org.candlepin.common.exceptions.NotFoundException;
import org.candlepin.guice.PrincipalProvider;
import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerCurator;
import org.candlepin.model.EntitlementCurator;
import org.candlepin.model.ImportRecord;
import org.candlepin.model.ManifestRecord.ManifestRecordType;
import org.candlepin.pinsetter.tasks.ExportJob;
import org.candlepin.pinsetter.tasks.ImportJob;
import org.candlepin.model.Owner;
import org.candlepin.sync.ConflictOverrides;
import org.candlepin.sync.ExportCreationException;
import org.candlepin.sync.ExportResult;
import org.candlepin.sync.Exporter;
import org.candlepin.sync.ImportExtractionException;
import org.candlepin.sync.Importer;
import org.candlepin.sync.ImporterException;
import org.candlepin.sync.ManifestFileService;
import org.candlepin.sync.ManifestServiceException;
import org.candlepin.sync.file.ManifestFile;
import org.candlepin.util.Util;
import org.hibernate.tool.hbm2x.ExporterException;
import org.quartz.JobDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class ManifestManager {

    private static Logger log = LoggerFactory.getLogger(ManifestManager.class);
    private ManifestFileService manifestFileService;
    private Exporter exporter;
    private Importer importer;
    private EntitlementCurator entitlementCurator;
    private PoolManager poolManager;
    private ConsumerCurator consumerCurator;
    private PrincipalProvider principalProvider;

    @Inject
    public ManifestManager(ManifestFileService manifestFileService, Exporter exporter, Importer importer,
        ConsumerCurator consumerCurator, EntitlementCurator entitlementCurator, PoolManager poolManager,
        PrincipalProvider principalProvider) {
        this.manifestFileService = manifestFileService;
        this.exporter = exporter;
        this.importer = importer;
        this.consumerCurator = consumerCurator;
        this.entitlementCurator = entitlementCurator;
        this.poolManager = poolManager;
        this.principalProvider = principalProvider;
    }

    public JobDetail generateManifestAsync(Consumer consumer, String cdnLabel, String webAppPrefix,
        String apiUrl) {
        log.info("Scheduling Async Export for consumer {}", consumer.getUuid());
        return ExportJob.scheduleExport(consumer, cdnLabel, webAppPrefix, apiUrl);
    }

    public File generateManifest(Consumer consumer, String cdnLabel, String webAppPrefix, String apiUrl)
        throws ExportCreationException {
        return exporter.getFullExport(consumer, cdnLabel, webAppPrefix, apiUrl);
    }

    public JobDetail importManifestAsync(Owner owner, File archive, String uploadedFileName,
        ConflictOverrides overrides) throws ImportExtractionException, IOException, ImporterException {
        try {
            ManifestFile manifestRecordId = storeImport(archive, owner);
            return ImportJob.scheduleImport(owner, manifestRecordId.getId(), uploadedFileName, overrides);
        }
        catch (ManifestServiceException mse) {
            throw new ImporterException("Unable to store manifest file", mse);
        }
    }

    public ImportRecord importManifest(Owner owner, File archive, String uploadedFileName,
        ConflictOverrides overrides) throws ImporterException {
        return importer.loadExport(owner, archive, overrides, uploadedFileName);
    }

    public void recordImportFailure(Owner owner, Map data, Throwable error, String filename) {
        importer.recordImportFailure(owner, data, error, filename);
    }

    @Transactional
    public ImportRecord importStoredManifest(Owner targetOwner, String fileId, ConflictOverrides overrides,
        String uploadedFileName) throws ImporterException {
        ManifestFile manifest = getFile(fileId);
        if (manifest == null) {
            throw new ImporterException("The requested manifest was not found: " + fileId);
        }
        ImportRecord importResult = importer.loadStoredExport(manifest, targetOwner, overrides,
            uploadedFileName);
        deleteStoredManifest(manifest.getId());
        return importResult;
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
        return manifestFileService.deleteExpired(Util.addMinutesToDt(maxAgeInMinutes * -1));
    }

    @Transactional
    public void readStoredExportToResponse(String exportId, Consumer exportedConsumer, HttpServletResponse response) {
        // In order to stream the results from the DB to the client
        // we write the file contents directly to the response output stream.
        //
        // NOTE: Passing the database input stream to the response builder seems
        //       like it would be a correct approach here, but large object streaming
        //       can only be done inside a single transaction, so we have to stream it
        //       manually.
        BufferedOutputStream output = null;
        InputStream input = null;
        try {
            ManifestFile manifest = getFile(exportId);
            if (manifest == null) {
                throw new NotFoundException("Unable to find specified manifest by id: " + exportId);
            }

            if (!exportedConsumer.getUuid().equals(manifest.getTargetId())) {
                throw new BadRequestException("Could not validate export against specifed consumer: " +
                    exportedConsumer.getUuid());
            }

            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=" + manifest.getName());

            // NOTE: Input and output streams are expected to be closed by their creators.
            input = manifest.getInputStream();
            output = new BufferedOutputStream(response.getOutputStream());
            int data = input.read();
            while (data != -1)
            {
                output.write(data);
                data = input.read();
            }
            output.flush();
        }
        catch (ManifestServiceException e) {
            throw new ExporterException("Unable to find manifest by id: " + exportId, e);
        }
        catch (IOException e) {
            throw new ExporterException("Unable to get manifest: " + exportId, e);
        }
    }

    public ExportResult generateAndStoreExport(Consumer consumer, String cdnKey, String webAppPrefix,
        String apiUrl) throws ExportCreationException {
        File export = null;
        try {
            export = exporter.getFullExport(consumer, cdnKey, webAppPrefix, apiUrl);
            ManifestFile manifestFile = storeExport(export, consumer);
            return new ExportResult(consumer, manifestFile.getId());
        }
        catch (ManifestServiceException e) {
            throw new ExportCreationException("Unable to create export archive", e);
        }
        finally {
            // We no longer need the export work directory since the archive has been saved in the DB.
            if (export != null) {
                File workDir = export.getParentFile();
                try {
                    FileUtils.deleteDirectory(workDir);
                }
                catch (IOException ioe) {
                    // It'll get cleaned up by the ManifestCleanerJob if it couldn't
                    // be deleted for some reason.
                }
            }
        }
    }

    /**
     * Deletes the manifest file stored by the {@link ManifestFileService}.
     *
     * @param manifestFileId the ID of the manifest to be deleted.
     */
    public void deleteStoredManifest(String manifestFileId) {
        try {
            log.info("Deleting stored manifest file: {}", manifestFileId);
            delete(manifestFileId);
        }
        catch (Exception e) {
            // Just log any exception here. This will eventually get cleaned up by
            // a cleaner job.
            log.warn("Could not delete import file by id: {}", manifestFileId, e);
        }
    }

    public File generateEntitlementArchive(String consumerUuid, Set<Long> serials)
        throws ExportCreationException {
        log.debug("Getting client certificate zip file for consumer: {}", consumerUuid);
        Consumer consumer = consumerCurator.verifyAndLookupConsumer(consumerUuid);
        poolManager.regenerateDirtyEntitlements(
            entitlementCurator.listByConsumer(consumer));

        return exporter.getEntitlementExport(consumer, serials);
    }

    /**
     * Gets a manifest matching the specified id. A files id should be
     * a unique identifier such as a database entity ID, or a file path.
     *
     * @param id the id of the target manifest.
     * @return a {@link ManifestFile} matching the id, null otherwise.
     */
    private ManifestFile getFile(String id) throws ManifestServiceException {
        return manifestFileService.get(id);
    }

    /**
     * Deletes a manifest matching the specified id.
     *
     * @param id the id of the target manifest file.
     * @return true if the record was deleted, false otherwise.
     */
    @Transactional
    private boolean delete(String id) throws ManifestServiceException {
        return manifestFileService.delete(id);
    }

    /**
     * Stores the specified manifest import file.
     *
     * @param the manifest import {@link File} to store
     * @return the id of the stored manifest file.
     */
    @Transactional
    private ManifestFile storeImport(File importFile, Owner targetOwner) throws ManifestServiceException {
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
    private ManifestFile storeExport(File exportFile, Consumer distributor) throws ManifestServiceException {
        // TODO: Check to see if we are allowed to do this based on the principal.
        // Only allow a single export for a consumer at a time. Delete all others before
        // storing the new one.
        int count = manifestFileService.delete(ManifestRecordType.EXPORT, distributor.getUuid());
        log.debug("Deleted {} existing export files for distributor {}.", count, distributor.getUuid());
        return storeFile(exportFile, ManifestRecordType.EXPORT, distributor.getUuid());
    }

    private ManifestFile storeFile(File targetFile, ManifestRecordType type, String targetId)
        throws ManifestServiceException {
        return manifestFileService.store(type, targetFile, principalProvider.get().getName(), targetId);
    }

}
