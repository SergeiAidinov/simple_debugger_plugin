package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow.Factory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.OnWorkflowReadyListener;

public class SimpleDebugHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);

        // Создаём или открываем окно дебаггера
        DebugWindow window = DebugWindowManager.instance().getOrCreateWindow();
        if (!window.isOpen()) {
            window.open();
        }

        // Получаем все конфигурации запуска
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfiguration[] allConfigs = null;
		try {
			allConfigs = launchManager.getLaunchConfigurations();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        Map<String, ILaunchConfiguration> configMap = new LinkedHashMap<>();
        for (ILaunchConfiguration config : allConfigs) {
            try {
                // Фильтруем только Java Application
                if ("org.eclipse.jdt.launching.localJavaApplication".equals(config.getType().getIdentifier())) {
                    configMap.put(config.getName(), config);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (configMap.isEmpty()) {
            MessageDialog.openInformation(shell, "Simple Debugger", "No Java launch configurations found.");
            return null;
        }

        // Диалог выбора конфигурации
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(shell, new LabelProvider());
        dialog.setElements(configMap.keySet().toArray());
        dialog.setTitle("Select Debug Configuration");
        dialog.setMessage("Choose a configuration to attach:");
        if (dialog.open() != Window.OK) return null;

        String selectedName = (String) dialog.getFirstResult();
        ILaunchConfiguration selectedConfig = configMap.get(selectedName);

        // Определяем host/port из конфигурации
        String host = "localhost";
        int port = 5005; // стандартный JDWP порт

        try {
            // Пример: если конфигурация содержит аргумент -agentlib:jdwp=...
            String vmArgs = selectedConfig.getAttribute("org.eclipse.jdt.launching.VM_ARGUMENTS", "");
            if (vmArgs.contains("address=")) {
                String addr = vmArgs.split("address=")[1].split(",")[0].trim();
                port = Integer.parseInt(addr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        MessageDialog.openInformation(shell, "Simple Debugger", "Connecting to " + host + ":" + port + "...");

        // Создаём workflow асинхронно
        Factory.create(host, port, new OnWorkflowReadyListener() {
            @Override
            public void onReady(com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow workflow) {
                // Можно подписать DebugWindow на workflow
                // window.setWorkflow(workflow);

                // Стартуем debug loop
                new Thread(workflow::debug).start();
            }
        });

        return null;
    }
}
