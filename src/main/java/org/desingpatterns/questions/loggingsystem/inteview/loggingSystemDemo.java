package org.desingpatterns.questions.loggingsystem.inteview;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Question
 * Implement logging framework with following requirement
 * 1. Log level support : debug, info, error,
 * 2. Log appenders : file , database, blob storage
 * 3. Ability to write to multiple appenders
 * 4. Configure different set of appenders for each log level / fallback to default appender if log level specific appender is not configured
 * 5. Ability to Modify Loglevel at runtime
 *
 * In this logging system, we are using the following types of classes and objects:
 *
 * 1. Enum Class:
 * - LogLevel: Represents different severity levels for logging.
 * - Helps with comparing levels and organizing logs by importance.
 *
 * 2. Data Class:
 * - LogMessage: Stores the actual log details like level, message, timestamp, and context.
 * - Immutable design to ensure thread safety.
 *
 * 3. Strategy Pattern:
 * - LogFormatter (interface) with implementations:
 * • SimpleLogFormatter – formats logs in a simple readable format.
 * • JsonLogFormatter – formats logs in JSON format for better integration with tools.
 * - LogAppender (interface) with implementations:
 * • ConsoleAppender – prints logs to the console.
 * • FileAppender – writes logs to a file.
 * ➤ This makes the system flexible and extendable for other output formats or destinations.
 *
 * 4. Chain of Responsibility Pattern:
 * - LogHandler (abstract class) with concrete handlers for each log level (DebugHandler, InfoHandler, etc.).
 * ➤ Allows the log message to pass through the chain based on its severity level.
 *
 * 5. Singleton Pattern:
 * - Logger class ensures a single instance per configuration.
 * ➤ Improves resource management and avoids unnecessary object creation.
 *
 * 6. Configuration Class:
 * - LoggerConfig holds the logger settings like level, appender, and formatter.
 * ➤ Provides flexibility to change logger behavior at runtime.
 *
 * ➤ Objects used:
 * - Immutable objects (LogMessage).
 * - Shared singleton objects (Logger instances).
 * - Strategy objects (formatter and appender).
 * - Chain objects (handler chain).
 *
 * ✅ Future action items / improvements:
 * - Add support for asynchronous logging to avoid blocking the main thread.
 * - Add support for network-based appenders (e.g., sending logs to a server).
 * - Implement log rotation and archiving for large log files.
 * - Add support for structured search and filtering on logs (using indexing or external services).
 * - Improve error handling in case of file writing failures.
 * - Provide dynamic configuration reload without restarting the application.
 * - Add unit tests to cover edge cases, especially for concurrency scenarios.
 */
/*
❓ Interview Question: Why use an enum instead of constants for log levels?
💡 Answer: An enum provides type-safety and a fixed set of named constants. It prevents using an invalid value, unlike int constants, and can have behavior (like the isGreaterOrEqual method), making the code cleaner and less error-prone.
*/
/*
❓ Interview Question: Why associate a numeric value with each level?
💡 Answer: The numeric value allows for easy comparison of log levels. This is essential for filtering logs based on a configured minimum level, such as only logging messages with a level greater than or equal to INFO.
*/
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

    /* 💡 Modified Logic: Checks if the current level is LESS THAN OR EQUAL to the other level */
    public boolean isLessOrEqual(LogLevel other) {
        return this.value <= other.value;
    }
}

/*
❓ Interview Question: Why is this class immutable?
💡 Answer: Making LogMessage immutable ensures that its state cannot be changed after creation. This is critical for thread safety, as multiple threads can log messages without needing synchronization, preventing data corruption.
*/
/*
❓ Interview Question: Why is context stored as a Map<String, Object>?
💡 Answer: Using a map allows for structured logging. It enables the inclusion of arbitrary, key-value pairs that provide rich, machine-readable context (e.g., user ID, request ID). This is more powerful than a simple string message and allows for advanced log analysis and searching.
*/
final class LogMessage {
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

/*
❓ Interview Question: Why use an interface for LogFormatter?
💡 Answer: An interface defines a contract for formatting behavior without dictating implementation details. This allows for the Strategy pattern, where different formatting strategies (simple, JSON) can be plugged in and swapped without changing the Appender or Logger classes.
*/
interface LogFormatter {
    String format(LogMessage logMessage);
}

/*
❓ Interview Question: What is the benefit of this simple formatter?
💡 Answer: It provides a human-readable format that is easy to understand for debugging. It serves as a good default or a simple option when machine-readable logs are not required.
*/
class SimpleLogFormatter implements LogFormatter {
    @Override
    public String format(LogMessage logMessage) {
        String contextStr = logMessage.getContext().isEmpty() ? "" : " | Context: " + logMessage.getContext();
        return String.format("[%s] %s: %s%s",
                new Date(logMessage.getTimestamp()),
                logMessage.getLevel(),
                logMessage.getMessage(),
                contextStr);
    }
}


/*
❓ Interview Question: Why would you choose a JSON formatter over a simple one?
💡 Answer: JSON is a structured, machine-readable format. It's ideal for centralized logging systems and data analysis tools (like Splunk or the ELK stack) that can parse and index the data. This makes it much easier to search, filter, and visualize logs based on fields like 'level' or 'context' keys.
*/
class JsonLogFormatter implements LogFormatter {
    @Override
    public String format(LogMessage logMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"timestamp\": \"").append(new Date(logMessage.getTimestamp())).append("\",\n");
        sb.append("  \"level\": \"").append(logMessage.getLevel()).append("\",\n");
        sb.append("  \"message\": \"").append(escapeJson(logMessage.getMessage())).append("\",\n");
        sb.append("  \"context\": ").append(formatContext(logMessage.getContext())).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String formatContext(Map<String, Object> context) {
        if (context.isEmpty()) {
            return "{}";
        }
        return context.entrySet().stream()
                .map(entry -> String.format("\"%s\": %s", escapeJson(entry.getKey()), toJsonValue(entry.getValue())))
                .collect(Collectors.joining(",\n  ", "{\n  ", "\n}"));
    }

    private String toJsonValue(Object value) {
        if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        }
        return String.valueOf(value);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

/*
❓ Interview Question: What is the purpose of the LogAppender interface?
💡 Answer: It decouples the act of writing a log from the destination (console, file, etc.). This allows the system to be easily extended with new log destinations without changing the core logging logic.
*/
interface LogAppender {
    void append(LogMessage logMessage);
}

/*
❓ Interview Question: How do you handle file I/O errors in the FileAppender?
💡 Answer: A try-with-resources block is used to ensure the FileWriter is automatically closed, preventing resource leaks. If an IOException occurs, an error message is printed to the console (System.err) to notify the user of the failure. In a production system, this error might be logged to a fallback system or re-thrown.
*/
class ConsoleAppender implements LogAppender {
    private final LogFormatter formatter;

    public ConsoleAppender(LogFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void append(LogMessage logMessage) {
        System.out.println(formatter.format(logMessage));
    }
}

class FileAppender implements LogAppender {
    private final String filePath;
    private final LogFormatter formatter;

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

/*
❓ Interview Question: This is a single handler, not a Chain of Responsibility. Why this design choice?
💡 Answer: A single handler is simpler and more efficient for this use case. The core logic is to check if a log message's level is high enough to be processed. A single handler performs this check and appends the message, avoiding the overhead and complexity of passing the message through a chain of identical handlers.
*/
class LoggerHandler {
    private final List<LogAppender> appenders;

    public LoggerHandler(List<LogAppender> appender) {
        this.appenders = appender;
    }

    public void handle(LogMessage logMessage) {
        appenders.forEach((appender)-> appender.append(logMessage));
    }
}

/*
❓ Interview Question: Why use a dedicated configuration class?
💡 Answer: A separate configuration class encapsulates logger settings, promoting a clean separation of concerns. It makes it easy to pass a complete configuration object around and allows the logger to be reconfigured at runtime by simply providing a new LoggerConfig instance.
*/
final class LoggerConfig {
    private final LogLevel logLevel;
    private final List<LogAppender> logAppenders;
    private final LogFormatter logFormatter;

    public LoggerConfig(LogLevel logLevel, List<LogAppender> logAppenders, LogFormatter logFormatter) {
        this.logLevel = logLevel;
        this.logAppenders = logAppenders;
        this.logFormatter = logFormatter;
    }
    public LogLevel getLogLevel() { return logLevel; }
    public List<LogAppender> getLogAppenders() { return logAppenders; }
    public LogFormatter getLogFormatter() { return logFormatter; }
}

/*
❓ Interview Question: How is the Singleton pattern implemented here?
💡 Answer: This is a true singleton, ensuring only one instance of the Logger exists in the application. All logging calls go through this single instance, which can be reconfigured at runtime.
*/
/*
❓ Interview Question: How does this logger handle concurrency?
💡 Answer: The `instance` is a static field, and `getInstance` is synchronized to ensure thread-safe initialization. The `configRef` and `handlerRef` are `AtomicReference` objects, ensuring that `setConfig` operations are atomic. This means reading and writing the configuration and handler references are safe, preventing other threads from seeing an inconsistent state during a configuration change.
*/
class Logger {
    private static volatile Logger instance;
    private final AtomicReference<LoggerConfig> configRef;
    private final AtomicReference<LoggerHandler> handlerRef;

    private Logger(LoggerConfig config) {
        this.configRef = new AtomicReference<>(config);
        this.handlerRef = new AtomicReference<>(new LoggerHandler(config.getLogAppenders()));
    }

    public static Logger getInstance(LoggerConfig config) {
        if (instance == null) {
            synchronized (Logger.class) {
                if (instance == null) {
                    instance = new Logger(config);
                }
            }
        }
        return instance;
    }

    public void setConfig(LoggerConfig newConfig) {
        this.configRef.set(newConfig);
        this.handlerRef.set(new LoggerHandler(newConfig.getLogAppenders()));
    }

    public void log(LogLevel level, String message, Map<String, Object> context) {
        LogLevel configuredLevel = configRef.get().getLogLevel();
        if (level.isLessOrEqual(configuredLevel)) {
            LogMessage logMessage = new LogMessage(level, message, context);
            handlerRef.get().handle(logMessage);
        }
    }

    public void debug(String message, Map<String, Object> context) { log(LogLevel.DEBUG, message, context); }
    public void debug(String message) { debug(message, Map.of()); }
    public void info(String message, Map<String, Object> context) { log(LogLevel.INFO, message, context); }
    public void info(String message) { info(message, Map.of()); }
    public void warning(String message, Map<String, Object> context) { log(LogLevel.WARNING, message, context); }
    public void warning(String message) { warning(message, Map.of()); }
    public void error(String message, Map<String, Object> context) { log(LogLevel.ERROR, message, context); }
    public void error(String message) { error(message, Map.of()); }
    public void fatal(String message, Map<String, Object> context) { log(LogLevel.FATAL, message, context); }
    public void fatal(String message) { fatal(message, Map.of()); }
}

/*
❓ Interview Question: Why is a demo class useful for this project?
💡 Answer: A demo class serves as a client to showcase how the logging framework works end-to-end. It demonstrates how to configure different loggers, change settings at runtime, and confirms that the logging behavior (like filtering by level) is correct.
*/
public class loggingSystemDemo {
    public static void main(String[] args) {
        LogFormatter simpleFormatter = new SimpleLogFormatter();
        LogFormatter jsonFormatter = new JsonLogFormatter();

        LogAppender consoleAppender = new ConsoleAppender(simpleFormatter);

        String currentDir = System.getProperty("user.dir");
        //please clean "application.log" file before running if already exist
        // this is windows dir format use your os specific path
        String logFilePath = currentDir + "\\src\\main\\java\\org\\desingpatterns\\questions\\loggingsystem\\inteview\\application.log";
        LogAppender fileAppender = new FileAppender(logFilePath, jsonFormatter);

        /*
         ✅ Test 1: Console Logging with DEBUG level and multiple appenders
         */
        System.out.println("--- Console Logging (DEBUG level set) ---");
        LoggerConfig consoleConfig = new LoggerConfig(LogLevel.DEBUG, List.of(consoleAppender), simpleFormatter);
        Logger logger = Logger.getInstance(consoleConfig);

        logger.debug("Debug message", Map.of("module", "console-test"));
        logger.info("Info message", Map.of("module", "console-test"));
        logger.warning("Warning message", Map.of("module", "console-test"));
        logger.error("Error message", Map.of("user", "john_doe", "action", "login"));

        /*
         ✅ Test 2: File Logging with INFO level and multiple appenders
         */
        System.out.println("\n--- Switching to File Logging (INFO level set) ---");
        LoggerConfig fileConfig = new LoggerConfig(LogLevel.INFO, List.of(fileAppender, consoleAppender), jsonFormatter);
        logger.setConfig(fileConfig);

        logger.debug("Debug message: should be logged", Map.of("module", "file-test"));
        logger.info("Info message: should be logged", Map.of("user", "alice", "operation", "file-write"));
        logger.warning("Warning message: should not be logged", Map.of("module", "file-test"));

        /*
         ✅ Test 3: Empty context handling
         */
        logger.info("Info message with empty context", Map.of());

        /*
         ✅ Test 4: Runtime reconfiguration
         */
        System.out.println("\n--- Changing to DEBUG level at runtime ---");
        LoggerConfig debugConfig = new LoggerConfig(LogLevel.DEBUG, List.of(consoleAppender), simpleFormatter);
        logger.setConfig(debugConfig);

        logger.debug("Debug message after reconfiguration", Map.of("test", "runtime-config"));
        logger.info("Info message after reconfiguration and it will not be logged", Map.of("test", "runtime-config"));

        System.out.println("\nLogging demonstration completed. Check application.log for JSON formatted logs.");
    }

}
