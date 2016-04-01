package org.candlepin.sync.file;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.candlepin.model.Consumer;
import org.candlepin.model.DbFileStoreCurator;
import org.candlepin.model.DbStoredFile;
import org.candlepin.model.DbStoredFile.CandlepinFileType;
import org.candlepin.model.Owner;
import org.candlepin.sync.ManifestService;
import org.candlepin.sync.ManifestServiceException;

import com.google.inject.Inject;

public class DBManifestService implements ManifestService {

    private DbFileStoreCurator curator;

    @Inject
    public DBManifestService(DbFileStoreCurator curator) {
        this.curator = curator;
    }

    @Override
    public Manifest get(String id) throws ManifestServiceException {
        try {
            DbStoredFile file = curator.findFile(id);
            if (file == null) {
                return null;
            }
            return new Manifest(file.getId(), file.getFileData().getBinaryStream());
        }
        catch (SQLException e) {
            throw new ManifestServiceException("Unable to load manifest data", e);
        }
    }

    @Override
    public void delete(String id) throws ManifestServiceException {
        curator.deleteById(id);
    }

    @Override
    public String storeImport(File manifestFile, Owner owner) throws ManifestServiceException {
        return store(manifestFile, CandlepinFileType.MANIFEST_IMPORT, owner.getKey());
    }

    @Override
    public String storeExport(File exportFile, Consumer distributor) throws ManifestServiceException {
        return store(exportFile, CandlepinFileType.MANIFEST_EXPORT, distributor.getUuid());
    }

    private String store(File toStore, CandlepinFileType type, String metaId) throws ManifestServiceException {
        try {
            DbStoredFile stored = curator.createFile(toStore, type, metaId);
            return stored.getId();
        }
        catch (IOException e) {
            throw new ManifestServiceException(e);
        }
    }

}
