package common.utils;

import java.io.IOException;
import java.util.logging.*;

/**
 * Logger utility để cấu hình và quản lý logging cho toàn bộ hệ thống
 */
public class LoggerUtil {
    private static boolean initialized = false;
    
    public static void initializeLogging() {
        if (initialized) return;
        
        try {
            // Get root logger
            Logger rootLogger = Logger.getLogger("");
            
            // Remove default handlers
            Handler[] handlers = rootLogger.getHandlers();
            for (Handler handler : handlers) {
                rootLogger.removeHandler(handler);
            }
            
            // Create console handler
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);
            consoleHandler.setFormatter(new SimpleFormatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("[%1$tF %1$tT] [%2$s] %3$s: %4$s%n",
                        record.getMillis(),
                        record.getLevel(),
                        record.getLoggerName(),
                        record.getMessage()
                    );
                }
            });
            
            // Create file handler
            FileHandler fileHandler = new FileHandler("notesync.log", true);
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new SimpleFormatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("[%1$tF %1$tT] [%2$s] [%3$s] %4$s%n",
                        record.getMillis(),
                        record.getLevel(),
                        record.getLoggerName(),
                        record.getMessage()
                    );
                }
            });
            
            // Add handlers to root logger
            rootLogger.addHandler(consoleHandler);
            rootLogger.addHandler(fileHandler);
            
            // Set level based on configuration
            ConfigManager config = ConfigManager.getInstance();
            Level logLevel = Level.parse(config.getLoggingLevel());
            rootLogger.setLevel(logLevel);
            
            initialized = true;
            
        } catch (IOException e) {
            System.err.println("Failed to initialize file logging: " + e.getMessage());
        }
    }
    
    public static Logger getLogger(Class<?> clazz) {
        if (!initialized) {
            initializeLogging();
        }
        return Logger.getLogger(clazz.getName());
    }
    
    public static Logger getLogger(String name) {
        if (!initialized) {
            initializeLogging();
        }
        return Logger.getLogger(name);
    }
    
    public static void setLogLevel(Level level) {
        Logger.getLogger("").setLevel(level);
    }
    
    public static void logException(Logger logger, String message, Exception e) {
        logger.log(Level.SEVERE, message, e);
    }
}