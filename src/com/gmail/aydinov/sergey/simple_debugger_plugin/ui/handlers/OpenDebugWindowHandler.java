package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.handlers;

import java.util.Objects;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindow;

/**
 * Handler to open the Simple Debugger window.
 * If the window is already open, it will bring it to the front.
 */
public class OpenDebugWindowHandler extends AbstractHandler {

    /** Singleton instance of the debug window */
    private static DebugWindow window;

    /**
     * Executes the command to open the debugger window.
     *
     * @param event the execution event
     * @return null
     * @throws ExecutionException if execution fails
     */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        // If the window does not exist or has been disposed, create a new one
        if (Objects.isNull(window) || window.getShell().isDisposed()) {
            window = new DebugWindow();
            window.open();
        } else {
            // Bring existing window to the front
            window.getShell().setActive();
        }

        return null;
    }
}
