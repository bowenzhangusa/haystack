package com.haystack.storage;

import com.datastax.driver.core.ResultSet;
import com.haystack.server.model.Photo;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * This test is based on http://www.baeldung.com/cassandra-with-java
 */
public class DbTest {
    private Db db;

    @Before
    public void connect() {
        this.db = Service.getService().getDb();
    }

    @Test
    public void checkKeyspaceCreation() {
        this.db.ensureKeyspaceExists();

        ResultSet result =
                this.db.getSession().execute("SELECT * FROM system_schema.keyspaces;");

        List<String> matchedKeyspaces = result.all()
                                              .stream()
                                              .filter(r -> r.getString(0).equals(Db.FILES_KEYSPACE.toLowerCase()))
                                              .map(r -> r.getString(0))
                                              .collect(Collectors.toList());

        assertEquals(matchedKeyspaces.size(), 1);
        assertTrue(matchedKeyspaces.get(0).equals(Db.FILES_KEYSPACE.toLowerCase()));
    }

    @Test
    public void checkTableCreation() {
        this.db.ensureTableExists();

        ResultSet result = this.db.getSession().execute("SELECT * FROM storage.files;");

        List<String> columnNames =
                result.getColumnDefinitions().asList().stream()
                      .map(cl -> cl.getName())
                      .collect(Collectors.toList());

        assertEquals(columnNames.size(), 3);
        assertTrue(columnNames.contains("id"));
        // cassandra keeps columns in lowercase
        assertTrue(columnNames.contains("contenttype"));
        assertTrue(columnNames.contains("data"));
    }

    @Test
    public void checkFileSave() {
        Photo photo = new Photo();
        photo.setId(UUID.randomUUID());

        try {
            byte[] fileBytes = IOUtils.toByteArray(this.getClass().getResourceAsStream("cat.jpg"));
            photo.setContent(fileBytes);
            photo.setContentType("image/jpeg");
            this.db.saveFile(photo);
            Photo savedFile = this.db.getFile(photo.getId());
            assertNotNull(savedFile);
            assertArrayEquals("Saved file must match the original", fileBytes, savedFile.getContent());
            assertEquals("Saved content-type must match the original", "image/jpeg", savedFile.getContentType());
            this.db.deleteFile(photo.getId());

            Photo deleted = this.db.getFile(photo.getId());
            assertNull(deleted);
        } catch (IOException e) {
            fail("Unexpected exception");
        }
    }
}