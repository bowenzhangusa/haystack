package com.haystack.storage;

import com.datastax.driver.core.*;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * This is a primary file storage using Cassandra
 */
public class Db {
    private Cluster cluster;
    private Session session;

    public static final String FILES_TABLE = "files";
    public static final String FILES_KEYSPACE = "storage";

    public void connect(String node, Integer port) {
        Cluster.Builder b = Cluster.builder().addContactPoint(node);
        b.withPort(port);
        cluster = b.build();

        session = cluster.connect();
    }

    public Session getSession() {
        return this.session;
    }

    public UUID saveFile(String name, byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        PreparedStatement ps = session.prepare("insert into " + getTableKeyspace() + " (id, name, data) values(?,?,?)");
        BoundStatement boundStatement = new BoundStatement(ps);

        UUID id = UUID.randomUUID();
        session.execute(boundStatement.bind(id, name, buffer));

        return id;
    }

    public byte[] getFile(UUID id) {
        PreparedStatement ps = session.prepare("SELECT data FROM " + getTableKeyspace() + " WHERE id=?");
        BoundStatement boundStatement = new BoundStatement(ps);
        ResultSet result = session.execute(boundStatement.bind(id));
        Row r = result.one();

        if (r == null) {
            return null;
        }

        return com.datastax.driver.core.utils.Bytes.getArray(r.getBytes("data"));
    }

    public void deleteFile(UUID id) {
        PreparedStatement ps = session.prepare("delete from " + getTableKeyspace() + " WHERE id=?");
        BoundStatement boundStatement = new BoundStatement(ps);
        session.execute(boundStatement.bind(id));
    }

    public void close() {
        session.close();
        cluster.close();
    }

    public void ensureKeyspaceExists() {
        StringBuilder sb =
                new StringBuilder("CREATE KEYSPACE IF NOT EXISTS ")
                        .append(FILES_KEYSPACE).append(" WITH replication = {")
                        .append("'class':'").append("SimpleStrategy")
                        .append("','replication_factor':").append(1)
                        .append("};");

        String query = sb.toString();
        session.execute(query);
    }

    public void ensureTableExists() {
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(getTableKeyspace()).append("(")
                .append("id uuid PRIMARY KEY, ")
                .append("name text,")
                .append("data blob);");

        String query = sb.toString();
        session.execute(query);
    }

    private String getTableKeyspace() {
        return FILES_KEYSPACE + "." + FILES_TABLE;
    }
}
