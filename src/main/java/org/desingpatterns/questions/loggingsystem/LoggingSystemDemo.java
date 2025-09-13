package org.desingpatterns.questions.loggingsystem;
/**
 * Question
 * Implement logging framework with following requirement
 *     1. Log level support : debug, info, error,
 *     2. Log appenders : file , database, blob storage
 *     3. Ability to write to multiple appenders
 *     4. Configure different set of appenders for each log level / fallback to default appender if log level specific appender is not configured
 *     5. Ability to Modify Loglevel at runtime
 *
 * In this logging system, we are using the following types of classes and objects:
 *
 * 1. Enum Class:
 *    - LogLevel: Represents different severity levels for logging.
 *    - Helps with comparing levels and organizing logs by importance.
 *
 * 2. Data Class:
 *    - LogMessage: Stores the actual log details like level, message, timestamp, and context.
 *    - Immutable design to ensure thread safety.
 *
 * 3. Strategy Pattern:
 *    - LogFormatter (interface) with implementations:
 *        • SimpleLogFormatter – formats logs in a simple readable format.
 *        • JsonLogFormatter – formats logs in JSON format for better integration with tools.
 *    - LogAppender (interface) with implementations:
 *        • ConsoleAppender – prints logs to the console.
 *        • FileAppender – writes logs to a file.
 *    ➤ This makes the system flexible and extendable for other output formats or destinations.
 *
 * 4. Chain of Responsibility Pattern:
 *    - LogHandler (abstract class) with concrete handlers for each log level (DebugHandler, InfoHandler, etc.).
 *    ➤ Allows the log message to pass through the chain based on its severity level.
 *
 * 5. Singleton Pattern:
 *    - Logger class ensures a single instance per configuration.
 *    ➤ Improves resource management and avoids unnecessary object creation.
 *
 * 6. Configuration Class:
 *    - LoggerConfig holds the logger settings like level, appender, and formatter.
 *    ➤ Provides flexibility to change logger behavior at runtime.
 *
 * ➤ Objects used:
 *    - Immutable objects (LogMessage).
 *    - Shared singleton objects (Logger instances).
 *    - Strategy objects (formatter and appender).
 *    - Chain objects (handler chain).
 *
 * ✅ Future action items / improvements:
 *    - Add support for asynchronous logging to avoid blocking the main thread.
 *    - Add support for network-based appenders (e.g., sending logs to a server).
 *    - Implement log rotation and archiving for large log files.
 *    - Add support for structured search and filtering on logs (using indexing or external services).
 *    - Improve error handling in case of file writing failures.
 *    - Provide dynamic configuration reload without restarting the application.
 *    - Add unit tests to cover edge cases, especially for concurrency scenarios.
 */

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Enum representing different log severity levels
// ❓ Cross Questions:
// Q1. Why use an enum instead of constants for log levels?
// ➤ Enums are type-safe and make it easier to manage and understand the different log levels.

// Q2. Why associate a numeric value with each level?
// ➤ It helps compare log levels to decide which messages should be logged based on severity.

enum LogLevel {
    DEBUG(1),
    INFO(2),
    WARNING(3),
    ERROR(4),
    FATAL(5);

    private final int value;

    LogLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public boolean isGreaterOrEqual(LogLevel other) {
        return this.value >= other.value;
    }
}

// Class representing a log message with all its details
// ❓ Cross Questions:
// Q1. Why use final fields?
// ➤ It makes the object immutable and ensures thread safety, preventing accidental changes after creation.

// Q2. Why store timestamp as long instead of Date object?
// ➤ It is lightweight and easier to process or serialize when needed.

class LogMessage {
    private final LogLevel level;
    private final String message;
    private final long timestamp;
    private final Map<String, Object> context;

    public LogMessage(LogLevel level, String message, Map<String, Object> context) {
        this.level = level;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.context = context;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    @Override
    public String toString() {
        return "[" + level + "] " + new Date(timestamp) + " - " + message;
    }
}

// Interface for formatting logs
// ❓ Cross Questions:
// Q1. Why define an interface here?
// ➤ It allows different ways to format the log message without changing other parts of the code.

interface LogFormatter {
    String format(LogMessage logMessage);
}

// Simple formatter that prints message in plain text
// ❓ Cross Questions:
// Q1. Why use String.format() instead of concatenation?
// ➤ It improves readability and makes it easier to format the message consistently.

class SimpleLogFormatter implements LogFormatter {
    @Override
    public String format(LogMessage logMessage) {
        return String.format("[%s] %s: %s",
                new Date(logMessage.getTimestamp()),
                logMessage.getLevel(),
                logMessage.getMessage());
    }
}

// JSON formatter implementation
// ❓ Cross Questions:
// Q1. Why would someone want a JSON format?
// ➤ JSON is widely used and can be easily processed by other systems or tools.

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
        boolean isFirst = true;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (!isFirst) {
                sb.append(",\n");
            }
            isFirst = false;
            sb.append("    \"").append(entry.getKey()).append("\": ");
            if (entry.getValue() instanceof String) {
                sb.append("\"").append(entry.getValue()).append("\"");
            } else {
                sb.append(entry.getValue());
            }
        }
        sb.append("\n  }");
        return sb.toString();
    }
}

// Interface for appending logs to outputs
// ❓ Cross Questions:
// Q1. Why separate formatting from appending?
// ➤ It follows the single responsibility principle, making it easier to change one part without affecting the other.

interface LogAppender {
    void append(LogMessage logMessage);
}

// Console appender that outputs to the console
// ❓ Cross Questions:
// Q1. Why default to simple formatting?
// ➤ It provides an easy-to-use setup without needing extra configuration.

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

// File appender that writes logs to a file
// ❓ Cross Questions:
// Q1. Why use try-with-resources for file writing?
// ➤ It ensures that resources are properly closed after use, avoiding memory leaks.

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

// Abstract class defining how handlers work
// ❓ Cross Questions:
// Q1. Why use an abstract class here instead of an interface?
// ➤ It allows sharing common code like level checking and chaining logic between handlers.

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

// Handlers for each log level (DEBUG, INFO, etc.)
// ❓ Cross Questions:
// Q1. Why create separate handler classes instead of using one?
// ➤ It keeps the design simple and allows customization for specific levels if needed in the future.

class DebugHandler extends LogHandler {
    public DebugHandler(LogLevel level, LogAppender appender) {
        super(level, appender);
    }
}

class InfoHandler extends LogHandler {
    public InfoHandler(LogLevel level, LogAppender appender) {
        super(level, appender);
    }
}

class WarningHandler extends LogHandler {
    public WarningHandler(LogLevel level, LogAppender appender) {
        super(level, appender);
    }
}

class ErrorHandler extends LogHandler {
    public ErrorHandler(LogLevel level, LogAppender appender) {
        super(level, appender);
    }
}

class FatalHandler extends LogHandler {
    public FatalHandler(LogLevel level, LogAppender appender) {
        super(level, appender);
    }
}

// Configuration class to hold logger settings
// ❓ Cross Questions:
// Q1. Why use getters and setters instead of public fields?
// ➤ It encapsulates the data, allowing changes later without affecting other parts of the code.

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

// Logger class following the singleton pattern
// ❓ Cross Questions:
// Q1. Why use a map for instances instead of a single object?
// ➤ Different configurations can be reused without creating new objects every time.

// Q2. Why synchronize setConfig?
// ➤ To ensure thread safety when changing configuration.

class Logger {
    private static final ConcurrentHashMap<String, Logger> instances = new ConcurrentHashMap<>();
    private LoggerConfig config;
    private LogHandler handlerChain;

    private Logger(LoggerConfig config) {
        this.config = config;
        buildHandlerChain();
    }

    public static Logger getInstance(LoggerConfig config) {
        String key = config.getLogLevel() + "_" +
                config.getLogAppender().getClass().getName() + "_" +
                config.getLogFormatter().getClass().getName();

        return instances.computeIfAbsent(key, k -> new Logger(config));
    }

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

    public void setConfig(LoggerConfig config) {
        synchronized (Logger.class) {
            this.config = config;
            buildHandlerChain();
        }
    }

    public void log(LogLevel level, String message, Map<String, Object> context) {
        LogMessage logMessage = new LogMessage(level, message, context);
        handlerChain.handle(logMessage);
    }

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

// Demo class to show how logger works
// ❓ Cross Questions:
// Q1. Why demonstrate both console and file outputs?
// ➤ To show that the system is flexible and can be configured differently for different needs.

public class LoggingSystemDemo {
    public static void main(String[] args) {
        LogFormatter simpleFormatter = new SimpleLogFormatter();
        LogFormatter jsonFormatter = new JsonLogFormatter();

        LogAppender consoleAppender = new ConsoleAppender(simpleFormatter);
        LogAppender fileAppender = new FileAppender("application.log", jsonFormatter);

        LoggerConfig config = new LoggerConfig(LogLevel.DEBUG, consoleAppender, simpleFormatter);
        Logger logger = Logger.getInstance(config);

        logger.debug("This is a debug message");
        logger.info("This is an info message");
        logger.warning("This is a warning message");

        logger.error("This is an error message", Map.of("user", "john_doe", "action", "login"));
        logger.fatal("This is a fatal message", Map.of("error_code", 500, "component", "auth_service"));

        LoggerConfig fileConfig = new LoggerConfig(LogLevel.INFO, fileAppender, jsonFormatter);
        logger.setConfig(fileConfig);

        logger.info("This info message goes to the file");
        logger.warning("This warning message goes to the file");

        System.out.println("Logging demonstration completed. Check application.log for JSON formatted logs.");
    }
}
