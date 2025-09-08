package org.desingpatterns.questions.loggingsystem;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🎯 Problem Statement: Low-Level Design - Logging System
 *
 * Design a logging system that supports multiple log levels, formatters, and appenders.
 * The system should be configurable, use chain of responsibility for log handling, and support various outputs.
 *
 * ✅ Requirements:
 * - Support log levels (debug, info, warning, error, fatal) with severity-based filtering.
 * - Allow different log formatters (simple, JSON) for output formatting.
 * - Support multiple appenders (console, file) for log output.
 * - Use chain of responsibility to handle log messages based on level.
 * - Provide configuration for log level, formatter, and appender.
 *
 * 📦 Key Components:
 * - LogLevel enum for severity levels.
 * - LogMessage class to encapsulate log data.
 * - LogFormatter interface and implementations (SimpleLogFormatter, JsonLogFormatter).
 * - LogAppender interface and implementations (ConsoleAppender, FileAppender).
 * - LogHandler abstract class and concrete handlers for each level.
 * - Logger singleton for logging operations.
 *
 * 🚀 Example Flow:
 * 1. Logger configured with log level INFO and JSON formatter to file.
 * 2. Application logs debug message → ignored due to level.
 * 3. Application logs error message → formatted as JSON → written to file.
 * 4. Chain of responsibility handles message based on severity.
 */

// Enum representing different log severity levels
enum LogLevel {
    DEBUG(1),   // Debug-level messages (least severe)
    INFO(2),    // Informational messages
    WARNING(3), // Warning messages
    ERROR(4),   // Error messages indicating failures
    FATAL(5);   // Fatal error messages (most severe)

    // Numeric value associated with each log level
    private final int value;
    LogLevel(int value) {
        this.value = value;
    }

    // Getter method to retrieve the numeric value of a log level
    public int getValue() {
        return value;
    }

    // Method to compare log levels based on severity
    public boolean isGreaterOrEqual(LogLevel other) {
        return this.value >= other.value;
    }
}

// Class representing a log message with all its attributes
class LogMessage {
    // Log level of the message (e.g., INFO, DEBUG, ERROR)
    private final LogLevel level;
    // The actual log message content
    private final String message;
    // Timestamp when the log message was created
    private final long timestamp;
    // Context data for additional information
    private final Map<String, Object> context;

    // Constructor to initialize log level, message, and context
    public LogMessage(LogLevel level, String message, Map<String, Object> context) {
        this.level = level;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.context = context;
    }

    // Returns the log level of the message
    public LogLevel getLevel() {
        return level;
    }

    // Returns the log message content
    public String getMessage() {
        return message;
    }

    // Returns the timestamp of the log creation
    public long getTimestamp() {
        return timestamp;
    }

    // Returns the context data
    public Map<String, Object> getContext() {
        return context;
    }

    // Formats the log message as a string with level, timestamp, and message
    @Override
    public String toString() {
        return "[" + level + "] " + new Date(timestamp) + " - " + message;
    }
}

// Interface for log formatters (Strategy Pattern)
interface LogFormatter {
    String format(LogMessage logMessage);
}

// Simple log formatter implementation
class SimpleLogFormatter implements LogFormatter {
    @Override
    public String format(LogMessage logMessage) {
        return String.format("[%s] %s: %s",
                new Date(logMessage.getTimestamp()),
                logMessage.getLevel(),
                logMessage.getMessage());
    }
}

// JSON log formatter implementation
class JsonLogFormatter implements LogFormatter {
    @Override
    public String format(LogMessage logMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"timestamp\": \"").append(new Date(logMessage.getTimestamp())).append("\",\n");
        sb.append("  \"level\": \"").append(logMessage.getLevel()).append("\",\n");
        sb.append("  \"message\": \"").append(logMessage.getMessage()).append("\",\n");
        sb.append("  \"context\": ").append(formatContext(logMessage.getContext())).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String formatContext(Map<String, Object> context) {
        if (context.isEmpty()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        boolean isFirstEntry = true;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (!isFirstEntry) {
                sb.append(",\n");
            }
            isFirstEntry = false;
            sb.append("    \"").append(entry.getKey()).append("\": ");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
        }
        sb.append("\n  }");
        return sb.toString();
    }
}

// Interface for log appenders (Strategy Pattern)
interface LogAppender {
    void append(LogMessage logMessage);
}

// Console appender implementation
class ConsoleAppender implements LogAppender {
    private LogFormatter formatter;

    public ConsoleAppender() {
        this.formatter = new SimpleLogFormatter();
    }

    public ConsoleAppender(LogFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void append(LogMessage logMessage) {
        System.out.println(formatter.format(logMessage));
    }
}

// File appender implementation
class FileAppender implements LogAppender {
    private final String filePath;
    private LogFormatter formatter;

    public FileAppender(String filePath) {
        this.filePath = filePath;
        this.formatter = new SimpleLogFormatter();
    }

    public FileAppender(String filePath, LogFormatter formatter) {
        this.filePath = filePath;
        this.formatter = formatter;
    }

    @Override
    public void append(LogMessage logMessage) {
        try (FileWriter writer = new FileWriter(filePath, true)) {
            writer.write(formatter.format(logMessage) + "\n");
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }
}

// Abstract logger defining the chain behavior (Chain of Responsibility Pattern)
abstract class LogHandler {
    protected LogLevel level;
    protected LogHandler nextHandler;
    protected LogAppender appender;

    public LogHandler(LogLevel level, LogAppender appender) {
        this.level = level;
        this.appender = appender;
    }

    public void setNextHandler(LogHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    public void handle(LogMessage logMessage) {
        if (logMessage.getLevel().getValue() >= this.level.getValue()) {
            if (appender != null) {
                appender.append(logMessage);
            }
        }

        if (nextHandler != null) {
            nextHandler.handle(logMessage);
        }
    }
}

// Concrete handler for DEBUG level
class DebugHandler extends LogHandler {
    public DebugHandler(LogLevel level, LogAppender appender) {
        super(level, appender);
    }
}

// Concrete handler for INFO level
class InfoHandler extends LogHandler {
    public InfoHandler(LogLevel level, LogAppender appender) {
        super(level, appender);
    }
}

// Concrete handler for WARNING level
class WarningHandler extends LogHandler {
    public WarningHandler(LogLevel level, LogAppender appender) {
        super(level, appender);
    }
}

// Concrete handler for ERROR level
class ErrorHandler extends LogHandler {
    public ErrorHandler(LogLevel level, LogAppender appender) {
        super(level, appender);
    }
}

// Concrete handler for FATAL level
class FatalHandler extends LogHandler {
    public FatalHandler(LogLevel level, LogAppender appender) {
        super(level, appender);
    }
}

// Configuration class for logger settings
class LoggerConfig {
    private LogLevel logLevel;
    private LogAppender logAppender;
    private LogFormatter logFormatter;

    public LoggerConfig(LogLevel logLevel, LogAppender logAppender, LogFormatter logFormatter) {
        this.logLevel = logLevel;
        this.logAppender = logAppender;
        this.logFormatter = logFormatter;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public LogAppender getLogAppender() {
        return logAppender;
    }

    public void setLogAppender(LogAppender logAppender) {
        this.logAppender = logAppender;
    }

    public LogFormatter getLogFormatter() {
        return logFormatter;
    }

    public void setLogFormatter(LogFormatter logFormatter) {
        this.logFormatter = logFormatter;
    }
}

// Main logger class (Singleton Pattern)
class Logger {
    private static final ConcurrentHashMap<String, Logger> instances = new ConcurrentHashMap<>();
    private LoggerConfig config;
    private LogHandler handlerChain;

    // Private constructor to enforce singleton pattern
    private Logger(LoggerConfig config) {
        this.config = config;
        buildHandlerChain();
    }

    // Get instance based on configuration
    public static Logger getInstance(LoggerConfig config) {
        String key = config.getLogLevel() + "_" +
                config.getLogAppender().getClass().getName() + "_" +
                config.getLogFormatter().getClass().getName();

        return instances.computeIfAbsent(key, k -> new Logger(config));
    }

    // Build the chain of handlers
    private void buildHandlerChain() {
        LogAppender appender = config.getLogAppender();

        DebugHandler debugHandler = new DebugHandler(LogLevel.DEBUG, appender);
        InfoHandler infoHandler = new InfoHandler(LogLevel.INFO, appender);
        WarningHandler warningHandler = new WarningHandler(LogLevel.WARNING, appender);
        ErrorHandler errorHandler = new ErrorHandler(LogLevel.ERROR, appender);
        FatalHandler fatalHandler = new FatalHandler(LogLevel.FATAL, appender);

        debugHandler.setNextHandler(infoHandler);
        infoHandler.setNextHandler(warningHandler);
        warningHandler.setNextHandler(errorHandler);
        errorHandler.setNextHandler(fatalHandler);

        this.handlerChain = debugHandler;
    }

    // Update the logger configuration
    public void setConfig(LoggerConfig config) {
        synchronized (Logger.class) {
            this.config = config;
            buildHandlerChain();
        }
    }

    // Log a message with level and context
    public void log(LogLevel level, String message, Map<String, Object> context) {
        LogMessage logMessage = new LogMessage(level, message, context);
        handlerChain.handle(logMessage);
    }

    // Convenience methods for different log levels
    public void debug(String message) {
        debug(message, Map.of());
    }

    public void debug(String message, Map<String, Object> context) {
        log(LogLevel.DEBUG, message, context);
    }

    public void info(String message) {
        info(message, Map.of());
    }

    public void info(String message, Map<String, Object> context) {
        log(LogLevel.INFO, message, context);
    }

    public void warning(String message) {
        warning(message, Map.of());
    }

    public void warning(String message, Map<String, Object> context) {
        log(LogLevel.WARNING, message, context);
    }

    public void error(String message) {
        error(message, Map.of());
    }

    public void error(String message, Map<String, Object> context) {
        log(LogLevel.ERROR, message, context);
    }

    public void fatal(String message) {
        fatal(message, Map.of());
    }

    public void fatal(String message, Map<String, Object> context) {
        log(LogLevel.FATAL, message, context);
    }
}

// Demo class to showcase the logging system
public class LoggingSystemDemo {
    public static void main(String[] args) {
        // Create formatters
        LogFormatter simpleFormatter = new SimpleLogFormatter();
        LogFormatter jsonFormatter = new JsonLogFormatter();

        // Create appenders with different formatters
        LogAppender consoleAppender = new ConsoleAppender(simpleFormatter);
        LogAppender fileAppender = new FileAppender("application.log", jsonFormatter);

        // Create configuration
        LoggerConfig config = new LoggerConfig(LogLevel.DEBUG, consoleAppender, simpleFormatter);

        // Get logger instance
        Logger logger = Logger.getInstance(config);

        // Log messages at different levels
        logger.debug("This is a debug message");
        logger.info("This is an info message");
        logger.warning("This is a warning message");

        // Log with context
        logger.error("This is an error message", Map.of("user", "john_doe", "action", "login"));
        logger.fatal("This is a fatal message", Map.of("error_code", 500, "component", "auth_service"));

        // Change configuration to use file appender
        LoggerConfig fileConfig = new LoggerConfig(LogLevel.INFO, fileAppender, jsonFormatter);
        logger.setConfig(fileConfig);

        // These messages will go to the file
        logger.info("This info message goes to the file");
        logger.warning("This warning message goes to the file");

        System.out.println("Logging demonstration completed. Check application.log for JSON formatted logs.");
    }
}
