package com.haystack.storage;

import com.haystack.server.model.Photo;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.io.*;
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

        for (String host : hosts) {
            connectionPoints.add(HostAndPort.parseString(host));
        }

        this.cluster = new JedisCluster(connectionPoints);
    }


    /**
     * Reads photo from redis cache.
     * Deserialization logic based on this https://stackoverflow.com/a/2836659/272787
     */
    public Photo getFile(UUID uuid) {
        byte[] bytes = this.cluster.get(uuid.toString().getBytes());

        if (bytes == null) {
            return null;
        }

        ObjectInput in = null;

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(
                    bytes);
            in = new ObjectInputStream(bis);
            Object o = in.readObject();
            return (Photo) o;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }

        return null;
    }

    /**
     * Saves photo to redis cache.
     * Serialization logic based on this https://stackoverflow.com/a/2836659/272787
     */
    public void saveFile(Photo photo) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;

        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(photo);
            out.flush();
            byte[] serialized = bos.toByteArray();

            this.cluster.set(photo.getId().toString().getBytes(), serialized);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
            }
        }
    }
}
