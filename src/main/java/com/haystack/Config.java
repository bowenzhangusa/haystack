package com.haystack;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

public class Config {
    public static String[] REDIS_HOSTS;
    public static String[] CASSANDRA_HOSTS;
    public static int CASSANDRA_PORT;

    public static void initialize() {
        String home = System.getenv("HAYSTACK_HOME");
        try {
            XMLConfiguration configRead = new XMLConfiguration(home + "/config.xml");
            Config.REDIS_HOSTS = configRead.getStringArray("redis_hosts");
            Config.CASSANDRA_HOSTS = configRead.getStringArray("cassandra_hosts");
            Config.CASSANDRA_PORT = configRead.getInt("cassandra_port", 9042);
        } catch (ConfigurationException ex) {
            ex.printStackTrace();
            // TODO: should we ignore this?
        }
    }
}
