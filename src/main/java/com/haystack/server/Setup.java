package com.haystack.server;

import com.haystack.storage.Service;

/**
 * Performs cassandra table creation
 */
public class Setup {
    public static void main(String[] args) {
        Service s = Service.getService();
        s.getDb().ensureKeyspaceExists();
        s.getDb().ensureTableExists();

        System.out.println("Cassandra keyspace and table initialized");
    }
}
