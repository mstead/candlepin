package org.candlepin.model;

import java.sql.Blob;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "cp_stored_file")
public class DbStoredFile extends AbstractHibernateObject {

    public enum CandlepinFileType {
        MANIFEST_IMPORT,
        MANIFEST_EXPORT
    }

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(length = 32)
    @NotNull
    private String id;

    /**
     * The ID of the file owner. This could be something like a Consumer UUID or
     * Owner ID. This is generally who the file belongs to and provides a way
     * to query for files by file owner.
     */
    @Column(name = "meta_id")
    private String metaId;

    /**
     * The operation type that triggered the file storage.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "cp_file_type")
    private CandlepinFileType cpFileType;

    private String filename;

    @Lob
    @Basic(fetch=FetchType.LAZY)
    private Blob fileData;

    public DbStoredFile() {
        // Default constructor required by hibernate.
    }

    public DbStoredFile(String name, String metaId, Blob data, CandlepinFileType type) {
        this.filename = name;
        this.fileData = data;
        this.metaId = metaId;
        this.cpFileType = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return filename;
    }

    public void setFileName(String fileName) {
        this.filename = fileName;
    }

    public Blob getFileData() {
        return fileData;
    }

    public void setFileData(Blob fileData) {
        this.fileData = fileData;
    }

    public String getMetaId() {
        return metaId;
    }

    public void setMetaId(String metaId) {
        this.metaId = metaId;
    }

    public CandlepinFileType getCandlepinFileType() {
        return cpFileType;
    }

    public void setCandlepinFileType(CandlepinFileType fileType) {
        this.cpFileType = fileType;
    }

}
