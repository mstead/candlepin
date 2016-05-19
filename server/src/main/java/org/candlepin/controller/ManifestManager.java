package org.candlepin.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.candlepin.common.exceptions.BadRequestException;
import org.candlepin.common.exceptions.NotFoundException;
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
import org.hibernate.tool.hbm2x.ExporterException;
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
    public ManifestFile getFile(String id) throws ManifestServiceException {
        return manifestFileService.get(id);
    }

    /**
     * Deletes a manifest matching the specified id.
     *
     * @param id the id of the target manifest file.
     * @return true if the record was deleted, false otherwise.
     */
    @Transactional
    public boolean delete(String id) throws ManifestServiceException {
        return manifestFileService.delete(id);
    }

    /**
     * Stores the specified manifest import file.
     *
     * @param the manifest import {@link File} to store
     * @return the id of the stored manifest file.
     */
    @Transactional
    public ManifestFile storeImport(File importFile, Owner targetOwner) throws ManifestServiceException {
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
    public ManifestFile storeExport(File exportFile, Consumer distributor) throws ManifestServiceException {
        // TODO: Check to see if we are allowed to do this based on the principal.
        // Only allow a single export for a consumer at a time. Delete all others before
        // storing the new one.
        int count = manifestFileService.delete(ManifestRecordType.EXPORT, distributor.getUuid());
        log.debug("Deleted {} existing export files for distributor {}.", count, distributor.getUuid());
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

        return manifestFileService.deleteExpired(Util.addMinutesToDt(maxAgeInMinutes * -1));
    }

    @Transactional
    public void readStoredExport(String exportId, Consumer exportedConsumer, HttpServletResponse response) {

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

    private ManifestFile storeFile(File targetFile, ManifestRecordType type, String targetId)
        throws ManifestServiceException {
        // Store the manifest record, and then store the file.
        return manifestFileService.store(type, targetFile, principalProvider.get().getName(), targetId);
    }
}
