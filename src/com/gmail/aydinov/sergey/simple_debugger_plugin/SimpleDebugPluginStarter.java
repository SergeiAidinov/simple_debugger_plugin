package com.gmail.aydinov.sergey.simple_debugger_plugin;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

public class SimpleDebugPluginStarter extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);

        try {
            // 1. Получаем все Java конфигурации таргета
            ILaunchConfiguration[] configs = DebugPlugin.getDefault()
                    .getLaunchManager()
                    .getLaunchConfigurations();

            // 2. Диалог для выбора таргет-приложения
            ElementListSelectionDialog dialog = new ElementListSelectionDialog(shell, new LabelProvider() {
                @Override
                public String getText(Object element) {
                    if (element instanceof ILaunchConfiguration config) {
                        return config.getName();
                    }
                    return super.getText(element);
                }
            });

            dialog.setTitle("Select Target App");
            dialog.setMessage("Choose target application to debug:");
            dialog.setElements(configs);
            dialog.setMultipleSelection(false);

            if (dialog.open() != Window.OK) return null;
            ILaunchConfiguration selectedConfig = (ILaunchConfiguration) dialog.getFirstResult();

            // 3. Задаем параметры для wrapper
            String targetClass = "target_debug.Main"; // имя таргет-класса
            String targetBin = "/home/sergei/eclipse-commiters-workspace/target_debug/bin"; // путь к bin

            // 4. Формируем команду для запуска TargetAppWrapper через ProcessBuilder
            String wrapperClass = "com.gmail.aydinov.sergey.simple_debugger_plugin.wrapper.TargetAppWrapper";
            String pluginBin = "/home/sergei/eclipse-commiters-workspace/simple_debugger_plugin/bin"; // путь к вашему плагину

            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-cp", pluginBin,
                    "-Dtarget.classpath=" + targetBin,
                    wrapperClass,
                    targetClass
            );

            pb.inheritIO(); // вывод таргет-приложения в консоль wrapper

            System.out.println("Launching wrapper for target: " + targetClass);
            Process process = pb.start();
            System.out.println("Wrapper started. Target: " + targetClass);

            // Ждем завершения таргет-приложения
            int exitCode = process.waitFor();
            System.out.println("Wrapper finished. Exit code: " + exitCode);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
