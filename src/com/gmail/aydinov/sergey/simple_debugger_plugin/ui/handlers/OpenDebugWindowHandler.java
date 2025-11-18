package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;

import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindow;

public class OpenDebugWindowHandler extends AbstractHandler {

    private static DebugWindow window;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        Display display = Display.getDefault();

        if (window == null || window.getShell().isDisposed()) {
            window = new DebugWindow(display);
            window.open();
        } else {
            window.getShell().setActive();
        }

        return null;
    }
}
