package com.haystack.storage;

import java.util.UUID;

/**
 * Storage service that works with database and cache to save and retrieve files
 */
public class Service {
    protected Cache cache;
    protected Db db;

    public Service(Cache cache, Db db) {
        this.cache = cache;
        this.db = db;
    }

    public byte[] getFile(UUID id) {
        byte[] result = this.cache.getFile(id);

        if (result == null) {
            result = this.db.getFile(id);
        }

        return result;
    }

    public UUID saveFile(String name, byte[] data) {
        UUID id = this.db.saveFile(name, data);
        this.cache.saveFile(id, data);

        return id;
    }
}
