package com.haystack.server.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Class representing uploaded photo
 */
public class Photo implements Serializable {
    private UUID id;
    private byte[] content;
    private String contentType;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
