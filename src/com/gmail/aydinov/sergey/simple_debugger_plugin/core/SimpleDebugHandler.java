package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow.Factory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.OnWorkflowReadyListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowManager;

public class SimpleDebugHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);

        // Открываем окно DebugWindow
//        DebugWindow window = DebugWindowManager.instance().getOrCreateWindow();
//        if (!window.isOpen()) {
//            window.open();
//        }

        try {
            // Получаем все Java конфигурации запуска
            ILaunchConfiguration[] configs = DebugPlugin.getDefault()
                    .getLaunchManager()
                    .getLaunchConfigurations();

            // Создаём диалог для выбора конфигурации
            ElementListSelectionDialog dialog = new ElementListSelectionDialog(shell, new LabelProvider() {
                @Override
                public String getText(Object element) {
                    if (element instanceof ILaunchConfiguration) {
                        return ((ILaunchConfiguration) element).getName();
                    }
                    return super.getText(element);
                }
            });

            dialog.setTitle("Select Java Launch Configuration");
            dialog.setMessage("Select the target application for debugging:");
            dialog.setElements(configs);
            dialog.setMultipleSelection(false);

            if (dialog.open() != Window.OK) {
                return null; // Пользователь отменил выбор
            }

            ILaunchConfiguration selectedConfig = (ILaunchConfiguration) dialog.getFirstResult();

            // Получаем порт из конфигурации, если есть
            Integer port = getPortFromConfiguration(selectedConfig);
            if (port == null) {
                port = 5005; // дефолтный порт
            }

            // Запускаем workflow
            String host = "localhost"; // локальный хост
            Factory.create(host, port, new OnWorkflowReadyListener() {
                @Override
                public void onReady(SimpleDebuggerWorkFlow workflow) {
                    new Thread(workflow::debug).start();
                }
            });
            
            ILaunch launch = selectedConfig.launch(ILaunchManager.RUN_MODE, null);
            System.out.println("Application launched: " + selectedConfig.getName());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private Integer getPortFromConfiguration(ILaunchConfiguration config) {
        try {
            // Ищем атрибут JDWP-порта
            String portStr = config.getAttribute("org.eclipse.jdt.launching.CONNECT_MAP", "5005");
            return Integer.parseInt(portStr);
        } catch (Exception e) {
            return null;
        }
    }
}
