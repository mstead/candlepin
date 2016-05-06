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

import org.candlepin.model.Consumer;
import org.candlepin.model.Owner;
import org.candlepin.sync.file.Manifest;

/**
 * A service providing access to manifest files that have been
 * imported or exported.
 *
 */
public interface ManifestService {

    /**
     * Gets a manifest matching the specified id. A files id should be
     * a unique identifier such as a database entity ID, or a file path.
     *
     * @param id the id of the target manifest.
     * @return a {@link Manifest} matching the id, null otherwise.
     */
    Manifest get(String id) throws ManifestServiceException;

    /**
     * Deletes a manifest matching the specified id.
     *
     * @param id the id of the target manifest file.
     */
    void delete(String id) throws ManifestServiceException;

    /**
     * Stores the specified manifest import file.
     *
     * @param the manifest import {@link File} to store
     * @return the id of the stored manifest file.
     */
    String storeImport(File importFile, Owner targetOwner) throws ManifestServiceException;

    /**
     * Stores the specified manifest export file.
     *
     * @param exportFile the manifest export {@link File} to store.
     * @return the id of the stored manifest file.
     * @throws ManifestServiceException
     */
    String storeExport(File exportFile, Consumer distributor) throws ManifestServiceException;

}
