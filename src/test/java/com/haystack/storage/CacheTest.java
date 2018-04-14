package com.haystack.storage;

import com.haystack.Config;
import com.haystack.server.model.Photo;
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
        Cache c = Service.getService().getCache();
        Photo photo = new Photo();
        UUID nonExistingId = UUID.randomUUID();
        photo.setId(nonExistingId);


        assertNull("Null must be returned for non-existing file", c.getFile(nonExistingId));

        try {
            byte[] fileBytes = IOUtils.toByteArray(this.getClass().getResourceAsStream("cat.jpg"));
            photo.setContent(fileBytes);
            photo.setContentType("image/jpeg");
            c.saveFile(photo);

            Photo saved = c.getFile(photo.getId());
            assertNotNull("A photo must be returned after its been saved", saved);
            assertArrayEquals("Saved file must match the original", photo.getContent(), saved.getContent());
            assertEquals("Saved content-type must match the original", "image/jpeg", saved.getContentType());
        } catch (IOException e) {
            fail("Unexpected exception");
        }
    }
}