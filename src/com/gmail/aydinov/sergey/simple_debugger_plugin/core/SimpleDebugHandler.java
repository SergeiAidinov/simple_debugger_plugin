package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.dialogs.PortSelectionDialog;

public class SimpleDebugHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        Shell shell = HandlerUtil.getActiveShell(event);

        // Show port selection dialog
        PortSelectionDialog dlg = new PortSelectionDialog(shell);
        dlg.create();
        if (dlg.open() != org.eclipse.jface.window.Window.OK) {
            return null; // user cancelled
        }

        Integer port = dlg.getPort();
        String host = "localhost";

        // Open debugger window
        var window = DebugWindowManager.instance().getOrCreateWindow();
        if (!window.isOpen()) {
            window.open();
        }

        // Create workflow and start debugging
        SimpleDebuggerWorkFlow.Factory.create(host, port, workflow ->
                new Thread(() -> {
                    try {
                        workflow.debug();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start()
        );

        return null;
    }
}
