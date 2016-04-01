package org.candlepin.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Blob;

import javax.persistence.Query;

import org.candlepin.model.DbStoredFile.CandlepinFileType;

import com.google.inject.persist.Transactional;

public class DbFileStoreCurator extends AbstractHibernateCurator<DbStoredFile> {

    public DbFileStoreCurator() {
        super(DbStoredFile.class);
    }

    public DbStoredFile findFile(String id) {
        return (DbStoredFile) currentSession().get(DbStoredFile.class, id);
    }

    @Transactional
    public DbStoredFile createFile(File fileToStore, CandlepinFileType type, String metaId) throws IOException {
        Blob data = currentSession().getLobHelper().createBlob(new FileInputStream(fileToStore),
            fileToStore.length());
        return create(new DbStoredFile(fileToStore.getName(), metaId, data, type));
    }

    @Transactional
    public void deleteById(String id) {
        Query q = getEntityManager().createQuery("delete from DbStoredFile where id=:id");
        q.setParameter("id", id);
        q.executeUpdate();
        // TODO Is this needed?
        flush();
    }

}
