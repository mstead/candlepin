package org.candlepin.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.candlepin.sync.ManifestFileService;
import org.hibernate.annotations.GenericGenerator;

/**
 * A class representing the storage of a manifest file and the meta-data associated
 * with it. A ManifestRecord object is the persistent bridge between candlepin and
 * the implemented {@link ManifestFileService}.
 */
@Entity
@Table(name = "cp_manifest_record")
public class ManifestRecord extends AbstractHibernateObject {

    public enum ManifestRecordType {
        IMPORT,
        EXPORT
    }

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(length = 32)
    @NotNull
    private String id;

    /**
     * The type of manifest record - IMPORT/EXPORT.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ManifestRecordType type;

    @Column(name = "file_id")
    private String fileId;

    @Column(name = "principal_name")
    private String principalName;

    /**
     * The tartgetId is the unique ID of the entity being targeted in during
     * an import/export operation. During export the target would be the consumer.
     * During import, the target would be the owner.
     */
    @Column(name = "target_id")
    private String targetId;

    public ManifestRecord() {
        // For hibernate.
    }

    public ManifestRecord(ManifestRecordType type, String fileId, String principalName, String targetId) {
        this.type = type;
        this.fileId = fileId;
        this.principalName = principalName;
        this.targetId = targetId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ManifestRecordType getType() {
        return type;
    }

    public void setType(ManifestRecordType type) {
        this.type = type;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }
}
