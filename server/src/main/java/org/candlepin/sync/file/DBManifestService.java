package org.candlepin.sync.file;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.candlepin.model.Consumer;
import org.candlepin.model.DbFileStoreCurator;
import org.candlepin.model.DbStoredFile;
import org.candlepin.model.ManifestRecord.ManifestRecordType;
import org.candlepin.model.Owner;
import org.candlepin.sync.ManifestFileService;
import org.candlepin.sync.ManifestServiceException;
import org.candlepin.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class DBManifestService implements ManifestFileService {

    private static Logger log = LoggerFactory.getLogger(DBManifestService.class);

    private DbFileStoreCurator curator;

    @Inject
    public DBManifestService(DbFileStoreCurator curator) {
        this.curator = curator;
    }

    @Override
    public ManifestFile get(String id) throws ManifestServiceException {
        try {
            DbStoredFile file = curator.findFile(id);
            if (file == null) {
                return null;
            }
            return new ManifestFile(file.getId(), file.getFileName(), file.getFileData().getBinaryStream());
        }
        catch (SQLException e) {
            throw new ManifestServiceException("Unable to load manifest data", e);
        }
    }

    @Override
    public boolean delete(String id) throws ManifestServiceException {
        return curator.deleteById(id);
    }

    @Override
    public String store(File toStore) throws ManifestServiceException {
        try {
            DbStoredFile stored = curator.createFile(toStore);
            return stored.getId();
        }
        catch (IOException e) {
            throw new ManifestServiceException(e);
        }
    }

    @Override
    public List<String> delete(Set<String> fileIds) {
        return curator.deleteByIds(fileIds);
    }

}
