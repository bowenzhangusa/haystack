package com.haystack.storage;

import com.datastax.driver.core.*;
import com.haystack.server.model.Photo;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * This is a primary file storage using Cassandra
 */
public class Db {
    private Cluster cluster;
    private Session session;
    private int nodeCount;

    public static final String FILES_TABLE = "files";
    public static final String FILES_KEYSPACE = "storage";

    public void connect(String[] hosts, int port) {
        Cluster.Builder b = Cluster.builder().addContactPoints(hosts);
        b.withPort(port);
        cluster = b.build();

        this.nodeCount = hosts.length;
        session = cluster.connect();
    }

    public Session getSession() {
        return this.session;
    }

    public void saveFile(Photo photo) {
        ByteBuffer buffer = ByteBuffer.wrap(photo.getContent());
        PreparedStatement ps = session.prepare(
                "insert into " + getTableKeyspace() + " (id, contentType, data) values(?,?,?)");
        BoundStatement boundStatement = new BoundStatement(ps);
        session.execute(boundStatement.bind(photo.getId(), photo.getContentType(), buffer));
    }

    public Photo getFile(UUID id) {
        PreparedStatement ps = session.prepare(
                "SELECT contentType, data FROM " + getTableKeyspace() + " WHERE id=?");
        BoundStatement boundStatement = new BoundStatement(ps);
        ResultSet result = session.execute(boundStatement.bind(id));
        Row r = result.one();

        if (r == null) {
            return null;
        }

        Photo photo = new Photo();
        photo.setId(id);
        photo.setContent(com.datastax.driver.core.utils.Bytes.getArray(r.getBytes("data")));
        System.out.println("file len " + photo.getContent().length);
        photo.setContentType(r.getString("contentType"));

        return photo;
    }

    public void deleteFile(UUID id) {
        PreparedStatement ps = session.prepare("delete from " + getTableKeyspace() + " WHERE id=?");
        BoundStatement boundStatement = new BoundStatement(ps);
        session.execute(boundStatement.bind(id));
    }

    /**
     * Creates keyspace in cassandra if it wasnt created yet
     * with replication factor of 3 (or less, if there are less nodes available)
     */
    public void ensureKeyspaceExists() {
        StringBuilder sb =
                new StringBuilder("CREATE KEYSPACE IF NOT EXISTS ")
                        .append(FILES_KEYSPACE).append(" WITH replication = {")
                        .append("'class':'").append("SimpleStrategy")
                        .append("','replication_factor':").append(Math.min(3, this.nodeCount))
                        .append("};");

        String query = sb.toString();
        session.execute(query);
    }

    /**
     * Creates table in cassandra if it wasnt created yet
     */
    public void ensureTableExists() {
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(getTableKeyspace()).append("(")
                .append("id uuid PRIMARY KEY, ")
                .append("contentType text,")
                .append("data blob);");

        String query = sb.toString();
        session.execute(query);
    }

    private String getTableKeyspace() {
        return FILES_KEYSPACE + "." + FILES_TABLE;
    }
}
