package org.candlepin.model;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Date;

import org.candlepin.model.DbStoredFile.CandlepinFileType;
import org.candlepin.test.DatabaseTestFixture;
import org.candlepin.util.Util;
import org.junit.Test;

public class DbFileStoreCuratorTest extends DatabaseTestFixture {

    @Test
    public void deleteOlderThan() throws Exception {
        File tmp = null;
        try {
            tmp = File.createTempFile("just_testing", "");
            DbStoredFile stored = fileStoreCurator.createFile(tmp, CandlepinFileType.MANIFEST_EXPORT, "abc");
            assertNotNull(stored.getId());
            assertNotNull(fileStoreCurator.findFile(stored.getId()));
            int deleted = fileStoreCurator.deleteOlderThan(new Date(), CandlepinFileType.MANIFEST_EXPORT);
            assertEquals(1, deleted);
        }
        finally {
            if (tmp != null) {
                tmp.delete();
            }
        }
    }

}
