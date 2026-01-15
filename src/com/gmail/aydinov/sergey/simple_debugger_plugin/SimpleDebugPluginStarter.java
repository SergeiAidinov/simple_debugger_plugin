package com.gmail.aydinov.sergey.simple_debugger_plugin;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow.SimpleDebuggerWorkFlowFactory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.TargetLauncher;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.OnWorkflowReadyListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowManager;

public class SimpleDebugPluginStarter extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		TargetLauncher targetLauncher = new TargetLauncher(null, null);
		new Thread(targetLauncher).start();
		Shell shell = HandlerUtil.getActiveShell(event);

		try {
			// -------------------------
			// 1️⃣ Выбор конфигурации
			// -------------------------
			ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations();

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
				return null; // пользователь отменил выбор
			}

			ILaunchConfiguration selectedConfig = (ILaunchConfiguration) dialog.getFirstResult();

			// -------------------------
			// 2️⃣ Получаем порт (если есть) и VM args
			// -------------------------
			String vmArgs = selectedConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "");
			Integer port = getPortFromConfiguration(vmArgs);
			if (port == null) {
				port = 5005;
				ILaunchConfigurationWorkingCopy wc = selectedConfig.getWorkingCopy();
				if (!vmArgs.isEmpty())
					vmArgs += " ";
				vmArgs += "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005";
				wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
				wc.doSave();
			}

			// -------------------------
			// 3️⃣ Основная точка входа — создание workflow
			// -------------------------
			String mainClass = "com.example.TargetMain"; // TODO: заменить на свой main
			List<String> options = List.of("-Xmx512m");

			SimpleDebuggerWorkFlowFactory.createLaunched(mainClass, options, workflow -> {
				// -------------------------
				// Создаём окно DebugWindow в UI-потоке
				// -------------------------
//                Display.getDefault().asyncExec(() -> {
//                    DebugWindow debugWindow = DebugWindowManager.instance().getOrCreateWindow();
//                    debugWindow.open();
//                });

				// -------------------------
				// Сразу запускаем workflow.debug() в отдельном потоке
				// -------------------------
				new Thread(() -> {
					try {
						System.out.println("Starting workflow...");
						DebuggerContext.context().setRunning(true);
						workflow.debug();
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

	private Integer getPortFromConfiguration(String vmArgs) {
		if (vmArgs == null || vmArgs.isEmpty())
			return null;
		String[] parts = vmArgs.split(",");
		for (String part : parts) {
			part = part.trim();
			if (part.startsWith("address=")) {
				try {
					return Integer.parseInt(part.substring("address=".length()));
				} catch (NumberFormatException e) {
					return null;
				}
			}
		}
		return null;
	}
}
