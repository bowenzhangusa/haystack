package com.haystack.storage;

import com.datastax.driver.core.ResultSet;
import com.haystack.Config;
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
        this.db = new Db();
        this.db.connect(Config.CASSANDRA_HOST, Config.CASSANDRA_PORT);
    }

    @Test
    public void checkKeyspaceCreation() {
        this.db.createKeyspace();

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
        this.db.createTable();

        ResultSet result = this.db.getSession().execute("SELECT * FROM storage.files;");

        List<String> columnNames =
                result.getColumnDefinitions().asList().stream()
                      .map(cl -> cl.getName())
                      .collect(Collectors.toList());

        assertEquals(columnNames.size(), 3);
        assertTrue(columnNames.contains("id"));
        assertTrue(columnNames.contains("name"));
        assertTrue(columnNames.contains("data"));
    }

    @Test
    public void checkFileSave() {
        try {
            byte[] file = IOUtils.toByteArray(this.getClass().getResourceAsStream("cat.jpg"));
            UUID id = this.db.saveFile("cat.jpg", file);
            assertNotNull("ID should be assigned to file", id);
            byte[] savedFile = this.db.getFile(id);
            assertNotNull(savedFile);
            assertArrayEquals("Saved file must match the original", file, savedFile);
            this.db.deleteFile(id);

            byte[] deleted = this.db.getFile(id);
            assertNull(deleted);
        } catch (IOException e) {
            fail("Unexpected exception");
        }
    }
}