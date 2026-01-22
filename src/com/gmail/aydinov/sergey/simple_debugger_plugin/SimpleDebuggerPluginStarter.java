package com.gmail.aydinov.sergey.simple_debugger_plugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.TargetApplicationStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow.SimpleDebuggerWorkFlowFactory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugConfigurationEditDialog;

public class SimpleDebuggerPluginStarter extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        Shell shell = HandlerUtil.getActiveShell(event);

        try {
            // ----------------------------
            // 1️⃣ Выбор конфигурации
            // ----------------------------
            ILaunchConfiguration[] configs = DebugPlugin.getDefault()
                    .getLaunchManager()
                    .getLaunchConfigurations();

            ElementListSelectionDialog selectionDialog = new ElementListSelectionDialog(shell, new LabelProvider() {
                @Override
                public String getText(Object element) {
                    if (element instanceof ILaunchConfiguration) {
                        return ((ILaunchConfiguration) element).getName();
                    }
                    return super.getText(element);
                }
            });

            selectionDialog.setTitle("Select Java Launch Configuration");
            selectionDialog.setMessage("Select the target application for debugging:");
            selectionDialog.setElements(configs);
            selectionDialog.setMultipleSelection(false);

            if (selectionDialog.open() != Window.OK) {
                return null; // пользователь отменил
            }

            ILaunchConfiguration selectedConfig = (ILaunchConfiguration) selectionDialog.getFirstResult();

            // ----------------------------
            // 2️⃣ Строим DebugConfiguration
            // ----------------------------
            DebugConfiguration debugConfiguration = buildDebugConfiguration(selectedConfig);
            System.out.println(debugConfiguration.prettyPrint());

            // ----------------------------
            // 3️⃣ Диалог редактирования VM options и порта
            // ----------------------------
            DebugConfigurationEditDialog editDialog =
                    new DebugConfigurationEditDialog(Display.getDefault().getActiveShell(), debugConfiguration);

            if (editDialog.open() != Window.OK) {
                System.out.println("Debug launch cancelled by user.");
                return null;
            }

            // ----------------------------
            // 4️⃣ Запуск workflow с обновлённой конфигурацией
            // ----------------------------
            SimpleDebuggerWorkFlowFactory.createWorkFlow(debugConfiguration, workflow -> {
                DebuggerContext.context().setTargetApplicationStatus(TargetApplicationStatus.STARTING);
                new Thread(() -> {
                    try {
                        System.out.println("Starting workflow...");
                        DebuggerContext.context().setRunning(true);
                        workflow.debug(debugConfiguration.getMainClassName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, "Workflow-Thread").start();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        return shell;
    }

    // ===========================
    // Методы buildDebugConfiguration и getPortFromConfigurationOrSetDefault оставляем как есть
    // ===========================

    private DebugConfiguration buildDebugConfiguration(ILaunchConfiguration selectedConfig) throws Exception {

        // 1️⃣ VM arguments и порт
        String vmArgs = selectedConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "");
        int port = getPortFromConfigurationOrSetDefault(vmArgs);

        ILaunchConfigurationWorkingCopy wc = selectedConfig.getWorkingCopy();
        if (!vmArgs.contains("-agentlib:jdwp")) {
            if (!vmArgs.isEmpty()) vmArgs += " ";
            vmArgs += "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + port;
            wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
            wc.doSave();
        }

        List<String> options = Arrays.stream(vmArgs.split("\\s+"))
                                     .filter(s -> !s.isBlank())
                                     .toList();

        // 2️⃣ Main класс
        String mainClassName = selectedConfig.getAttribute(
                IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "<null>"
        );

        // 3️⃣ Проект
        IJavaProject javaProject = JavaRuntime.getJavaProject(selectedConfig);
        IType mainType = javaProject.findType(mainClassName);

        Path workingDirPath = null;
        Path outputFolderPath = null;
        List<Path> additionalClasspath = new ArrayList<>();

        if (mainType != null && mainType.getCompilationUnit() != null) {
            IProject project = mainType.getCompilationUnit().getResource().getProject();

            // Рабочая директория — корень проекта
            workingDirPath = project.getLocation().toFile().toPath();

            // Output folder — только bin
            outputFolderPath = workingDirPath.resolve("bin");

            // JAR зависимости
            for (IClasspathEntry entry : javaProject.getRawClasspath()) {
                if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                    IPath path = entry.getPath();
                    if (path.toFile().exists()) {
                        additionalClasspath.add(path.toFile().toPath());
                    }
                }
            }
        }

        // 4️⃣ Создаём DebugConfiguration
        return new DebugConfiguration(
                mainClassName,
                options,
                port,
                workingDirPath,
                outputFolderPath,
                additionalClasspath
        );
    }

    private Integer getPortFromConfigurationOrSetDefault(String vmArgs) {
        Integer port = 5005;
        if (vmArgs == null || vmArgs.isEmpty())
            return port;
        String[] parts = vmArgs.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("address=")) {
                try {
                    port = Integer.parseInt(part.substring("address=".length()));
                } catch (NumberFormatException e) {
                }
            }
        }
        return port;
    }
}
