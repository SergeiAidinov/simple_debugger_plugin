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
 * <p>
 * Author: Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class SimpleDebuggerLogger {

    private static final String PLUGIN_ID = "com.gmail.aydinov.sergey.simpledebugger";
    private static volatile ILog LOG = null;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        Thread initializer = new Thread(() -> {
            try {
                Bundle bundle = null;
                while (Objects.isNull(LOG)) {
                    try {
                        bundle = Platform.getBundle(PLUGIN_ID);
                        if (Objects.nonNull(bundle)) {
                            LOG = Platform.getLog(bundle);
                        } else {
                            Thread.sleep(200);
                        }
                    } catch (Exception ignored) {
                        Thread.sleep(200);
                    }
                }
            } catch (InterruptedException ignored) {
                // Safe to ignore
            }
        }, "SimpleDebuggerLogger-Init");
        initializer.setDaemon(true);
        initializer.start();
    }

    private SimpleDebuggerLogger() { }

    /** Adds timestamp to a message */
    private static String addTimestamp(String message) {
        return "[" + LocalDateTime.now().format(FORMATTER) + "] " + message;
    }

    /** Logs info-level message */
    public static void info(String message) {
        if (Objects.nonNull(LOG)) {
            LOG.log(new Status(Status.INFO, PLUGIN_ID, addTimestamp(message)));
        }
    }

    /** Logs warning-level message */
    public static void warn(String message) {
        if (Objects.nonNull(LOG)) {
            LOG.log(new Status(Status.WARNING, PLUGIN_ID, addTimestamp(message)));
        }
    }

    /**
     * Logs an error message with an optional throwable.
     *
     * @param message the error message to log
     * @param t       the throwable associated with the error (can be null)
     */
    public static void error(String message, Throwable t) {
        if (Objects.nonNull(t)) t.printStackTrace(System.err);
        if (Objects.nonNull(LOG)) {
            LOG.log(new Status(Status.ERROR, PLUGIN_ID, addTimestamp(message), t));
        }
    }
}
