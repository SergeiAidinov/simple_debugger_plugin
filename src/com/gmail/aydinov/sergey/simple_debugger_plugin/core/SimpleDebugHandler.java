package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow.Factory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.OnWorkflowReadyListener;

public class SimpleDebugHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);

        // Можно показать окно сразу
        DebugWindow window = DebugWindowManager.instance().getOrCreateWindow();
        if (!window.isOpen()) {
            window.open();
        }

        // Предлагаем хост/порт для подключения к таргет-приложению
        String host = "localhost"; // либо через диалог
        int port = 5005;           // либо через диалог

        // Создаём workflow асинхронно
        Factory.create(host, port, new OnWorkflowReadyListener() {
            @Override
            public void onReady(SimpleDebuggerWorkFlow workflow) {
                // Подписываем DebugWindow на workflow
               // window.setWorkflow(workflow);

                // Можно сразу стартовать debug
                new Thread(workflow::debug).start();
            }
        });

        MessageDialog.openInformation(shell, "Simple Debugger", "Debug workflow is starting...");
        return null;
    }
}
