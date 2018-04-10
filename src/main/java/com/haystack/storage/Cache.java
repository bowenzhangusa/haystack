package com.haystack.storage;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * This is a file cache using Redis
 */
public class Cache {
    protected JedisCluster cluster;

    public Cache(String[] hosts) {
        Set<HostAndPort> connectionPoints = new HashSet<HostAndPort>();

        for (String host: hosts) {
            connectionPoints.add(HostAndPort.parseString(host));
        }

        this.cluster = new JedisCluster(connectionPoints);
    }

    public byte[] getFile(UUID uuid) {
        return this.cluster.get(uuid.toString().getBytes());
    }

    public void saveFile(UUID uuid, byte[] data) {
        this.cluster.set(uuid.toString().getBytes(), data);
    }
}
