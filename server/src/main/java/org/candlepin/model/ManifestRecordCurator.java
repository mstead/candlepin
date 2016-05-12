package org.candlepin.model;

import java.util.Date;
import java.util.List;

import javax.persistence.Query;

import org.candlepin.model.ManifestRecord.ManifestRecordType;

import com.google.inject.persist.Transactional;

public class ManifestRecordCurator extends AbstractHibernateCurator<ManifestRecord> {

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

}
