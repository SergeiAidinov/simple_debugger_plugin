package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import java.util.Objects;

import org.eclipse.swt.widgets.Display;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;

/**
 * Singleton managing the debugger window.
 * Provides access to the current window or creates a new one if needed.
 * <p>
 * Author: Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class DebugWindowManager {

    private static DebugWindowManager INSTANCE;

    private DebugWindow debugWindow;

    private DebugWindowManager() {
        // private constructor for singleton
    }

    /**
     * Returns the singleton instance of DebugWindowManager.
     */
    public static synchronized DebugWindowManager instance() {
        if (Objects.isNull(INSTANCE)) {
            INSTANCE = new DebugWindowManager();
        }
        return INSTANCE;
    }

    /**
     * Returns the current debugger window, creating it if it doesn't exist or is closed.
     *
     * @return the DebugWindow instance, or null if debugging is not running
     */
    public DebugWindow getOrCreateWindow() {
        if (!DebuggerContext.context().isRunning()) {
            return null;
        }
        if (Objects.isNull(debugWindow) || !debugWindow.isOpen()) {
            debugWindow = new DebugWindow();

            // Open window in the UI thread
            Display.getDefault().asyncExec(() -> debugWindow.open());
        }
        return debugWindow;
    }
}
