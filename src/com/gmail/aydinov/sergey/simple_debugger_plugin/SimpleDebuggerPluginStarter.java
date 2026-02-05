package com.gmail.aydinov.sergey.simple_debugger_plugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.SimpleDebuggerWorkFlow.SimpleDebuggerWorkFlowFactory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugConfigurationEditDialog;

/**
 * Handler for starting the Simple Debugger plugin from Eclipse UI. Launches the
 * target application in debug mode and starts the workflow.
 */
public class SimpleDebuggerPluginStarter extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		Shell shell = HandlerUtil.getActiveShell(event);

		try {
			// Launch configuration selection
			ILaunchConfiguration[] iLaunchConfigurations = DebugPlugin.getDefault().getLaunchManager()
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
			selectionDialog.setElements(iLaunchConfigurations);
			selectionDialog.setMultipleSelection(false);

			if (selectionDialog.open() != Window.OK) {
				return null;
			}

			ILaunchConfiguration selectedConfiguration = (ILaunchConfiguration) selectionDialog.getFirstResult();

			// Build DebugConfiguration
			DebugConfiguration debugConfiguration = buildDebugConfiguration(selectedConfiguration);

			// Edit VM options and port
			DebugConfigurationEditDialog editDialog = new DebugConfigurationEditDialog(
					Display.getDefault().getActiveShell(), debugConfiguration);

			if (editDialog.open() != Window.OK) {
				SimpleDebuggerLogger.info("Debug launch cancelled by user.");
				return null;
			}

			// Start workflow with updated configuration
			SimpleDebuggerWorkFlowFactory.createWorkflow(debugConfiguration, workflow -> new Thread(() -> {
				try {
					SimpleDebuggerLogger.info("Starting workflow...");
					workflow.debug(debugConfiguration.getMainClassName());
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}, "Workflow-Thread").start());

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return shell;
	}

	/**
	 * Builds a DebugConfiguration object from the selected Eclipse launch
	 * configuration.
	 *
	 * @param selectedConfig selected ILaunchConfiguration
	 * @return DebugConfiguration
	 * @throws Exception if project or main class cannot be resolved
	 */
	private DebugConfiguration buildDebugConfiguration(ILaunchConfiguration selectedConfig) throws Exception {

		// VM arguments and debug port
		String virtualMachineArgs = selectedConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
				"");
		int port = extractPortFromVmArgsOrDefault(virtualMachineArgs);

		ILaunchConfigurationWorkingCopy launchConfigurationCopy = selectedConfig.getWorkingCopy();

		if (!virtualMachineArgs.contains("-agentlib:jdwp")) {
			if (!virtualMachineArgs.isEmpty()) {
				virtualMachineArgs += " ";
			}
			virtualMachineArgs += "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + port;
			launchConfigurationCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
					virtualMachineArgs);
			launchConfigurationCopy.doSave();
		}

		List<String> options = Arrays.stream(virtualMachineArgs.split("\\s+")).filter(s -> !s.isBlank()).toList();

		// Main class
		String mainClassName = selectedConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
				"<null>");

		// Project paths
		IJavaProject javaProject = JavaRuntime.getJavaProject(selectedConfig);
		IType mainType = javaProject.findType(mainClassName);

		Path workingDirPath = null;
		Path outputFolderPath = null;
		List<Path> additionalClasspath = new ArrayList<>();

		if (Objects.nonNull(mainType) && Objects.nonNull(mainType.getCompilationUnit())) {

			IProject project = mainType.getCompilationUnit().getResource().getProject();

			// Working directory is the project root
			workingDirPath = project.getLocation().toFile().toPath();

			// Output folder (bin)
			outputFolderPath = workingDirPath.resolve("bin");

			// Additional JAR dependencies
			for (IClasspathEntry entry : javaProject.getRawClasspath()) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					IPath path = entry.getPath();
					if (path.toFile().exists()) {
						additionalClasspath.add(path.toFile().toPath());
					}
				}
			}
		}

		// Create DebugConfiguration
		return new DebugConfiguration(mainClassName, options, port, workingDirPath, outputFolderPath,
				additionalClasspath);
	}

	/**
	 * Extracts debug port from VM arguments or returns default (5005).
	 *
	 * @param vmArgs VM arguments string
	 * @return port number
	 */
	private int extractPortFromVmArgsOrDefault(String vmArgs) {
		int port = 5005;

		if (Objects.isNull(vmArgs) || vmArgs.isEmpty()) {
			return port;
		}

		String[] parts = vmArgs.split(",");

		for (String part : parts) {
			part = part.trim();
			if (part.startsWith("address=")) {
				try {
					port = Integer.parseInt(part.substring("address=".length()));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return port;
	}
}
