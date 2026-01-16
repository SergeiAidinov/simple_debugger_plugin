package com.gmail.aydinov.sergey.simple_debugger_plugin;

import java.util.Arrays;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.TargetApplicationStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow.SimpleDebuggerWorkFlowFactory;

public class SimpleDebuggerPluginStarter extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		Shell shell = HandlerUtil.getActiveShell(event);

		try {
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
				return null; 
			}

			ILaunchConfiguration selectedConfig = (ILaunchConfiguration) dialog.getFirstResult();
			String vmArgs = selectedConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "");
			Integer port = getPortFromConfigurationOrSetDefault(vmArgs);
			ILaunchConfigurationWorkingCopy wc = selectedConfig.getWorkingCopy();
			if (!vmArgs.contains("-agentlib:jdwp")) {
			    if (!vmArgs.isEmpty()) vmArgs += " ";
			    vmArgs += "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + port;
			    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
			    wc.doSave();
			}
			
			String mainClass = selectedConfig.getAttribute(
				    IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
				    "target_debug.Main"
				);
			List<String> options = Arrays.stream(vmArgs.split("\\s+"))
                    .filter(s -> !s.isBlank())
                    .toList();
			SimpleDebuggerWorkFlowFactory.createWorkFlow(mainClass, options, workflow -> {
				DebuggerContext.context().setTargetApplicationStatus(TargetApplicationStatus.STARTING);
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
