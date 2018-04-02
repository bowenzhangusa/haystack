package com.haystack.storage;

import com.haystack.Config;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Testing interaction with Redis
 */
public class CacheTest {
    @Test
    public void canGetAndSet() {
        Cache c = new Cache(Config.REDIS_HOST, Config.REDIS_PORT);
        UUID nonExistingId = UUID.randomUUID();
        assertNull(c.getFile(nonExistingId));

        try {
            byte[] file = IOUtils.toByteArray(this.getClass().getResourceAsStream("cat.jpg"));
            c.saveFile(nonExistingId, file);
            assertNotNull(c.getFile(nonExistingId));
        } catch (IOException e) {
            fail("Unexpected exception");
        }
    }
}