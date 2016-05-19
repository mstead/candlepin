package org.candlepin.sync.file;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.candlepin.model.Consumer;
import org.candlepin.model.ManifestRecord;
import org.candlepin.model.ManifestRecord.ManifestRecordType;
import org.candlepin.model.ManifestRecordCurator;
import org.candlepin.model.Owner;
import org.candlepin.sync.ManifestFileService;
import org.candlepin.sync.ManifestServiceException;
import org.candlepin.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class DBManifestService implements ManifestFileService {

    private static Logger log = LoggerFactory.getLogger(DBManifestService.class);

    private ManifestRecordCurator curator;

    @Inject
    public DBManifestService(ManifestRecordCurator curator) {
        this.curator = curator;
    }

    @Override
    public ManifestFile get(String id) throws ManifestServiceException {
        return curator.findFile(id);
    }

    @Override
    public boolean delete(String id) throws ManifestServiceException {
        return curator.deleteById(id);
    }

    @Override
    public String store(ManifestRecordType type, File fileToStore, String principalName, String targetId)
        throws ManifestServiceException {
        // TODO: Can I return a reference to the object now that I have fixed the transaction bits?
        try {
            ManifestRecord stored = curator.createFile(type, fileToStore, principalName, targetId);
            return stored.getId();
        }
        catch (IOException e) {
            throw new ManifestServiceException(e);
        }
    }

    @Override
    public int deleteExpired(Date expiryDate) {
        return curator.deleteExpired(expiryDate);
    }

}
