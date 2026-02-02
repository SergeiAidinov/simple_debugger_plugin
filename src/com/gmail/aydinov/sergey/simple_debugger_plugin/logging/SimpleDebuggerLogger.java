package com.gmail.aydinov.sergey.simple_debugger_plugin.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

/**
 * Logger utility for the Simple Debugger plugin.
 * Wraps Eclipse ILog and adds timestamp to messages.
 */
public class SimpleDebuggerLogger {

    private static final String PLUGIN_ID = "com.gmail.aydinov.sergey.simpledebugger";
    private static volatile ILog LOG = null;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        // Initialize LOG asynchronously
        Thread initializer = new Thread(() -> {
            try {
                Bundle bundle = null;
                while (Objects.isNull(LOG)) {
                    try {
                        bundle = Platform.getBundle(PLUGIN_ID);
                        if (Objects.nonNull(bundle)) {
                            LOG = Platform.getLog(bundle);
                        } else {
                            Thread.sleep(200); // wait until bundle is available
                        }
                    } catch (Exception ignored) {
                        Thread.sleep(200);
                    }
                }
            } catch (InterruptedException ignored) {
                // Thread interrupted; safe to ignore
            }
        }, "SimpleDebuggerLogger-Init");
        initializer.setDaemon(true);
        initializer.start();
    }

    private SimpleDebuggerLogger() { }

    /**
     * Adds timestamp to a message.
     *
     * @param message message to format
     * @return message with timestamp prepended
     */
    private static String addTimestamp(String message) {
        return "[" + LocalDateTime.now().format(FORMATTER) + "] " + message;
    }

    /**
     * Logs an info-level message.
     *
     * @param message message to log
     */
    public static void info(String message) {
        String msg = addTimestamp(message);
        if (LOG != null) {
            LOG.log(new Status(Status.INFO, PLUGIN_ID, msg));
        }
    }

    /**
     * Logs a warning-level message.
     *
     * @param message message to log
     */
    public static void warn(String message) {
        String msg = addTimestamp(message);
        if (LOG != null) {
            LOG.log(new Status(Status.WARNING, PLUGIN_ID, msg));
        }
    }

    /**
     * Logs an error-level message with optional Throwable.
     *
     * @param message message to log
     * @param t       throwable to log; may be null
     */
    public static void error(String message, Throwable t) {
        String msg = addTimestamp(message);
        if (t != null) t.printStackTrace(System.err);
        if (LOG != null) {
            LOG.log(new Status(Status.ERROR, PLUGIN_ID, msg, t));
        }
    }
}
