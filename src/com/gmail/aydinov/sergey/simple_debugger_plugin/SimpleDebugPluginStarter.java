package com.gmail.aydinov.sergey.simple_debugger_plugin;

import java.util.Objects;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow.Factory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.OnWorkflowReadyListener;

public class SimpleDebugPluginStarter extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShell(event);

		try {
			// Получаем все Java конфигурации запуска
			ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations();

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
			String vmArgs = selectedConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "");

			Integer port = getPortFromConfiguration(vmArgs);
			if (port == null) {
				port = 5005; // дефолтный порт
				ILaunchConfigurationWorkingCopy wc = selectedConfig.getWorkingCopy();
				if (!vmArgs.isEmpty())
					vmArgs += " ";
				vmArgs += "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005";
				wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
				ILaunchConfiguration newConfig = wc.doSave();
			}

			// Запускаем workflow
			String host = "localhost"; // локальный хост
			Factory.create(host, port, new OnWorkflowReadyListener() {
				@Override
				public void onReady(SimpleDebuggerWorkFlow workflow) {
					new Thread(workflow::debug).start();
				}
			});

			boolean ready = false;
			while (!ready) {
				ready = DebuggerContext.context().getSimpleDebuggerStatus() == DebuggerContext.SimpleDebuggerStatus.VM_AWAITING_CONNECTION;
				System.out.println("READY: " + ready);
				if (!ready) {
					Thread.currentThread().sleep(200);
				}
			}

			ILaunch launch = selectedConfig.launch(ILaunchManager.RUN_MODE, null);
			System.out.println("Application launched: " + selectedConfig.getName());

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
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
