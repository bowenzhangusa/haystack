package com.haystack.storage;

import com.haystack.Config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

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

        Config.initialize();

        Db db = new Db();
        db.connect(Config.CASSANDRA_HOSTS, Config.CASSANDRA_PORT);
        singleton = new Service(new Cache(Config.REDIS_HOSTS), db);
        return singleton;
    }

    public Cache getCache() {
        return cache;
    }

    public Db getDb() {
        return db;
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

    public UUID saveFile(UUID id, byte[] data) {
        this.db.saveFile(id, data);
        this.cache.saveFile(id, data);

        return id;
    }
}
