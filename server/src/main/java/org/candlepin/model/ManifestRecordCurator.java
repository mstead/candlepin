package org.candlepin.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.Query;

import org.candlepin.model.ManifestRecord.ManifestRecordType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.persist.Transactional;

public class ManifestRecordCurator extends AbstractHibernateCurator<ManifestRecord> {

    private static Logger log = LoggerFactory.getLogger(ManifestRecordCurator.class);

    public ManifestRecordCurator() {
        super(ManifestRecord.class);
    }

    public List<ManifestRecord> getExpired(Date maxAge, ManifestRecordType type) {
        Query q = getEntityManager().createQuery(
            "select r from ManifestRecord r where r.type = :report_type and r.created < :max_age");
        q.setParameter("report_type", type);
        q.setParameter("max_age", maxAge);
        return q.getResultList();
    }

    @Transactional
    public boolean deleteById(String id) {
        Query q = getEntityManager().createQuery("delete from ManifestRecord where id=:id");
        q.setParameter("id", id);
        return q.executeUpdate() > 0;
    }

    // Need the caller to start the transaction since large object
    // streaming must be done in the same transaction as the object
    // was looked up in.
    public ManifestRecord findFile(String id) {
        return id == null ? null : ManifestRecord.class.cast(currentSession().get(ManifestRecord.class, id));
    }

    @Transactional
    public ManifestRecord createFile(ManifestRecordType type, File fileToStore, String principalName,
        String targetId) throws IOException {
        Blob data = currentSession().getLobHelper().createBlob(new FileInputStream(fileToStore),
            fileToStore.length());

        return create(new ManifestRecord(type, fileToStore.getName(), principalName, targetId, data));
    }

    /**
     * Deletes all file records matching the specified IDs and returns a list
     * of all file IDs that were deleted.
     *
     * @param fileIds the file IDs to match.
     * @return a {@link List} of IDs of all the files that were deleted.
     */
    @Transactional
    public List<String> deleteByIds(Set<String> fileIds) {
        List<String> deletedIds = new LinkedList<String>(fileIds);

        log.info("Deleting DB stored files:");
        for (String id: fileIds) {
            log.info(id);
        }

        if (fileIds == null || fileIds.isEmpty()) {
            log.info("No files to delete");
            return deletedIds;
        }

        Query q = getEntityManager().createQuery("delete from ManifestRecord r where r.id in :ids");
        q.setParameter("ids", fileIds);
        int numDeleted = q.executeUpdate();
        if (numDeleted != fileIds.size()) {
            deletedIds.removeAll(findExistingFileIds(new LinkedList<String>(fileIds)));
        }
        return deletedIds;
    }

    private List<String> findExistingFileIds(List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return new ArrayList<String>();
        }
        Query q = getEntityManager().createQuery("select d.id from ManifestRecord r where r.id in :ids");
        q.setParameter("ids", fileIds);
        return (List<String>) q.getResultList();
    }

    public int deleteExpired(Date expiryDate) {
        Query q = getEntityManager().createQuery(
            "delete from ManifestRecord r where r.created < :expiry");
        q.setParameter("expiry", expiryDate);
        return q.executeUpdate();
    }
}
