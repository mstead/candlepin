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

package org.candlepin.sync;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.candlepin.model.Consumer;
import org.candlepin.model.Owner;
import org.candlepin.model.ManifestRecord.ManifestRecordType;
import org.candlepin.sync.file.ManifestFile;

/**
 * A service providing access to manifest files that have been
 * imported or exported.
 *
 */
public interface ManifestFileService {

    /**
     * Gets a manifest matching the specified id. A files id should be
     * a unique identifier such as a database entity ID, or a file path.
     *
     * @param id the id of the target manifest.
     * @return a {@link ManifestFile} matching the id, null otherwise.
     */
    ManifestFile get(String id) throws ManifestServiceException;

    /**
     * Stores the specified file.
     *
     * @param type the type of operation the file is being stored for (Import/Export)
     * @param fileToStore the {@link File} to store.
     * @param principalName the name of the principal who uploaded the file.
     * @param targetId the id of the target entity (will change based on operation). Import: Owner.id
     *                 Export: Consumer.uuid
     * @return the id of the stored file.
     * @throws ManifestServiceException
     */
    ManifestFile store(ManifestRecordType type, File fileToStore, String principalName, String targetId)
        throws ManifestServiceException;

    /**
     * Deletes a manifest matching the specified id.
     *
     * @param id the id of the target manifest file.
     * @return true if the file was deleted, false otherwise.
     */
    boolean delete(String id) throws ManifestServiceException;

    /**
     * Deletes any manifests files that are older than the specified expiry date.
     * @param expiryDate the target expiry date.
     * @return the number of files deleted.
     */
    int deleteExpired(Date expiryDate);

//    /**
//     * Delete all the files identified by the collection of IDs. The IDs of all
//     * files that were deleted should be returned.
//     *
//     * @param fileIds the ids of the files to delete.
//     * @return the IDs of all the files that were deleted.
//     */
//    List<String> delete(Set<String> fileIds);

}
