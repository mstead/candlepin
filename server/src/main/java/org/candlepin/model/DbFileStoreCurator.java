package org.candlepin.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.persist.Transactional;

public class DbFileStoreCurator extends AbstractHibernateCurator<DbStoredFile> {

    private static Logger log = LoggerFactory.getLogger(DbFileStoreCurator.class);

    public DbFileStoreCurator() {
        super(DbStoredFile.class);
    }

    public DbStoredFile findFile(String id) {
        return (DbStoredFile) currentSession().get(DbStoredFile.class, id);
    }

    @Transactional
    public DbStoredFile createFile(File fileToStore) throws IOException {
        Blob data = currentSession().getLobHelper().createBlob(new FileInputStream(fileToStore),
            fileToStore.length());
        return create(new DbStoredFile(fileToStore.getName(), data));
    }

    @Transactional
    public boolean deleteById(String id) {
        Query q = getEntityManager().createQuery("delete from DbStoredFile where id=:id");
        q.setParameter("id", id);
        return q.executeUpdate() > 0;
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

        Query q = getEntityManager().createQuery("delete from DbStoredFile d where d.id in :ids");
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
        Query q = getEntityManager().createQuery("select d.id from DbStoredFile d where d.id in :ids");
        q.setParameter("ids", fileIds);
        return (List<String>) q.getResultList();
    }

}
