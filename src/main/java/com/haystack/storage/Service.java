package com.haystack.storage;

import com.haystack.Config;

import java.util.UUID;

/**
 * Storage service that works with database and cache to save and retrieve files
 */
public class Service {
    protected Cache cache;
    protected Db db;
    private static Service singleton;

    public static Service getService() {
        if (singleton != null) {
            return singleton;
        }

        singleton = new Service(new Cache(Config.REDIS_HOST, Config.REDIS_PORT), new Db());
        return singleton;
    }

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
