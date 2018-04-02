package com.haystack.storage;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.UUID;

/**
 * This is a file cache using Redis
 */
public class Cache {
    protected JedisPool pool;

    public Cache(String host, int port) {
        this.pool = new JedisPool(new JedisPoolConfig(), host, port);
    }

    public byte[] getFile(UUID uuid) {
        return this.pool.getResource().get(uuid.toString().getBytes());
    }

    public void saveFile(UUID uuid, byte[] data) {
        this.pool.getResource().set(uuid.toString().getBytes(), data);
    }
}
