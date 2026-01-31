package com.gmail.aydinov.sergey.simple_debugger_plugin.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

public class SimpleDebuggerLogger {

    private static final String PLUGIN_ID = "com.gmail.aydinov.sergey.simpledebugger";
    private static volatile ILog LOG = null;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static int attempt = 0;

    static {
        // Инициализация LOG в отдельном потоке
        Thread initializer = new Thread(() -> {
            try {
                Bundle bundle = null;
                while (LOG == null) {
                	attempt++;
                    try {
                        bundle = Platform.getBundle(PLUGIN_ID);
                        if (bundle != null) {
                            LOG = Platform.getLog(bundle);
                            System.out.println("Log obtained. Attempt: " + LOG.toString());
                        } else {
                        	System.out.println("Log NOT obtained: " + attempt);
                            Thread.sleep(200); // ждем пока bundle появится
                        }
                    } catch (Exception ignored) {
                        Thread.sleep(200);
                    }
                }
            } catch (InterruptedException ignored) {
                // прерывание потока – безопасно игнорируем
            }
        }, "SimpleDebuggerLogger-Init");
        initializer.setDaemon(true);
        initializer.start();
    }

    private SimpleDebuggerLogger() { }

    private static String addTimestamp(String message) {
        return "[" + LocalDateTime.now().format(formatter) + "] " + message;
    }

    public static void info(String message) {
        String msg = addTimestamp(message);
        System.out.println("[INFO] " + msg); // всегда пишем в консоль для раннего запуска
        if (LOG != null) {
            LOG.log(new Status(Status.INFO, PLUGIN_ID, msg));
        }
    }

    public static void warn(String message) {
        String msg = addTimestamp(message);
        System.out.println("[WARN] " + msg);
        if (LOG != null) {
            LOG.log(new Status(Status.WARNING, PLUGIN_ID, msg));
        }
    }

    public static void error(String message, Throwable t) {
        String msg = addTimestamp(message);
        System.err.println("[ERROR] " + msg);
        if (t != null) t.printStackTrace(System.err);
        if (LOG != null) {
            LOG.log(new Status(Status.ERROR, PLUGIN_ID, msg, t));
        }
    }
}
