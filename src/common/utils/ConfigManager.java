package common.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Configuration manager để quản lý các thiết lập của hệ thống
 */
public class ConfigManager {
    private static final Logger logger = Logger.getLogger(ConfigManager.class.getName());
    private static ConfigManager instance;
    private Properties properties;
    
    // Default configuration values
    private static final String DEFAULT_SERVER_HOST = "localhost";
    private static final int DEFAULT_TCP_PORT = 8080;
    private static final int DEFAULT_UDP_PORT = 8081;
    private static final int DEFAULT_HEARTBEAT_INTERVAL = 30000; // 30 seconds
    private static final int DEFAULT_CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int DEFAULT_MAX_CLIENTS = 100;
    
    private ConfigManager() {
        loadDefaultConfig();
        loadConfigFile();
    }
    
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    private void loadDefaultConfig() {
        properties = new Properties();
        properties.setProperty("server.host", DEFAULT_SERVER_HOST);
        properties.setProperty("server.tcp.port", String.valueOf(DEFAULT_TCP_PORT));
        properties.setProperty("server.udp.port", String.valueOf(DEFAULT_UDP_PORT));
        properties.setProperty("network.heartbeat.interval", String.valueOf(DEFAULT_HEARTBEAT_INTERVAL));
        properties.setProperty("network.connection.timeout", String.valueOf(DEFAULT_CONNECTION_TIMEOUT));
        properties.setProperty("server.max.clients", String.valueOf(DEFAULT_MAX_CLIENTS));
        properties.setProperty("client.auto.reconnect", "true");
        properties.setProperty("logging.level", "INFO");
        properties.setProperty("sync.batch.size", "10");
    }
    
    private void loadConfigFile() {
        try {
            FileInputStream fis = new FileInputStream("config.properties");
            properties.load(fis);
            fis.close();
            logger.info("Configuration loaded from config.properties");
        } catch (IOException e) {
            logger.info("Config file not found, using default configuration");
        }
    }
    
    public String getServerHost() {
        return properties.getProperty("server.host", DEFAULT_SERVER_HOST);
    }
    
    public int getTcpPort() {
        return Integer.parseInt(properties.getProperty("server.tcp.port", String.valueOf(DEFAULT_TCP_PORT)));
    }
    
    public int getUdpPort() {
        return Integer.parseInt(properties.getProperty("server.udp.port", String.valueOf(DEFAULT_UDP_PORT)));
    }
    
    public int getHeartbeatInterval() {
        return Integer.parseInt(properties.getProperty("network.heartbeat.interval", String.valueOf(DEFAULT_HEARTBEAT_INTERVAL)));
    }
    
    public int getConnectionTimeout() {
        return Integer.parseInt(properties.getProperty("network.connection.timeout", String.valueOf(DEFAULT_CONNECTION_TIMEOUT)));
    }
    
    public int getMaxClients() {
        return Integer.parseInt(properties.getProperty("server.max.clients", String.valueOf(DEFAULT_MAX_CLIENTS)));
    }
    
    public boolean isAutoReconnectEnabled() {
        return Boolean.parseBoolean(properties.getProperty("client.auto.reconnect", "true"));
    }
    
    public String getLoggingLevel() {
        return properties.getProperty("logging.level", "INFO");
    }
    
    public int getSyncBatchSize() {
        return Integer.parseInt(properties.getProperty("sync.batch.size", "10"));
    }
    
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
}