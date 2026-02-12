package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.gmail.aydinov.sergey.simple_debugger_plugin.DebugConfiguration;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.BreakpointSubscriberRegistrar;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DebugSession;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.OnWorkflowReadyListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowManager;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;

/**
 * Coordinates the execution flow of a JDI-based debugging process.
 * <p>
 * This class is responsible for:
 * <ul>
 *   <li>launching and attaching to the target JVM,</li>
 *   <li>preparing initial breakpoints (e.g. at {@code main}),</li>
 *   <li>running the JDI event loop,</li>
 *   <li>starting a {@link DebugSession} when a breakpoint is hit.</li>
 * </ul>
 *
 * <p>
 * This class is <strong>not</strong> a debug session.
 * A debug session is represented by {@link DebugSession} and is created
 * per breakpoint hit.
 * </p>
 *
 * <p>
 * No UI state is managed directly by this class.
 * Any UI-related operations are delegated to other components and executed
 * asynchronously on the UI thread.
 * </p>
 *
 * <p>
 * Global debugger state transitions are tracked via {@link DebuggerContext}
 * and {@link DebuggerContext.SimpleDebuggerStatus}.
 * </p>
 *
 * @author Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 * @see DebugSession
 * @see TargetVirtualMachineRepresentation
 * @see TargetApplicationRepresentation
 */
public class SimpleDebuggerWorkFlow {

	private final TargetVirtualMachineRepresentation targetVirtualMachineRepresentation;
	private final TargetApplicationRepresentation targetApplicationRepresentation;
	private final IBreakpointManager breakpointManager; // do NOT remove
	private final BreakpointSubscriberRegistrar breakpointListener; // do NOT remove
	private final CurrentLineHighlighter highlighter = new CurrentLineHighlighter();

	public SimpleDebuggerWorkFlow(TargetVirtualMachineRepresentation targetVirtualMachineRepresentation,
			IBreakpointManager breakpointManager, BreakpointSubscriberRegistrar breakpointListener,
			DebugConfiguration debugConfiguration) {
		this.targetVirtualMachineRepresentation = targetVirtualMachineRepresentation;
		this.breakpointManager = breakpointManager;
		this.breakpointListener = breakpointListener;

		EventRequestManager eventRequestManager = targetVirtualMachineRepresentation.getVirtualMachine().eventRequestManager();
		this.targetApplicationRepresentation = new TargetApplicationRepresentation(breakpointManager, eventRequestManager,
				targetVirtualMachineRepresentation.getVirtualMachine(), breakpointListener, debugConfiguration);
	}

	/** Starts the debug workflow */
	public void debug(String mainClassName) {
		prepareDebug(targetVirtualMachineRepresentation.getVirtualMachine().eventQueue(), mainClassName);
		if (!DebuggerContext.context().getStatus().equals(SimpleDebuggerStatus.PREPARED)) {
			Display.getDefault().asyncExec(() -> {
				Shell shell = Display.getDefault().getActiveShell();
				MessageDialog.openError(
				        shell,
				        "Debug Startup Failed",
				        "Failed to start the debug session."
				);
			});
			DebuggerContext.context().setStatus(SimpleDebuggerStatus.WILL_NOT_START);
			return;
		}
		SimpleDebuggerLogger.info("DEBUGGER STARTED");
		DebuggerContext.context().setStatus(SimpleDebuggerStatus.RUNNING);
		openDebugWindow();
		targetVirtualMachineRepresentation.getVirtualMachine().resume();

		while (DebuggerContext.context().isRunning()) {
			targetApplicationRepresentation
					.refreshReferencesToClassesOfTargetApplication(targetVirtualMachineRepresentation.getVirtualMachine());
			targetApplicationRepresentation.getTargetApplicationBreakepointRepresentation().refreshBreakpoints();

			EventSet eventSet = null;
			try {
				eventSet = targetVirtualMachineRepresentation.getVirtualMachine().eventQueue().remove();
			} catch (InterruptedException ignored) {
			} catch (com.sun.jdi.VMDisconnectedException e) {
				SimpleDebuggerLogger.info("VM disconnected, exiting debug loop.");
				break;
			}

			if (Objects.isNull(eventSet))
				continue;

			for (Event event : eventSet) {
				if (DebuggerContext.context().getStatus().equals(SimpleDebuggerStatus.STOPPED)) break;
				if (event instanceof ClassPrepareEvent classPrepareEvent) {
					targetApplicationRepresentation.getTargetApplicationBreakepointRepresentation()
							.onClassPrepared(classPrepareEvent.referenceType());
				} else if (event instanceof BreakpointEvent) {
					DebugSession debugSession = new DebugSessionImpl(targetVirtualMachineRepresentation, targetApplicationRepresentation,
							eventSet, highlighter);
					Thread sessionThread = new Thread(debugSession);
					sessionThread.setDaemon(true);
					sessionThread.start();
					try {
						sessionThread.join();
					} catch (InterruptedException ignored) {
					}
				}
			}
			eventSet.resume();
		}
	}

	private void prepareDebug(EventQueue queue, String mainClassName) {
		SimpleDebuggerLogger.info("Debug preparation...");
		DebuggerContext.context().setStatus(SimpleDebuggerStatus.PREPARING);
		openDebugWindow();
		EventRequestManager eventRequestManager = targetVirtualMachineRepresentation.getVirtualMachine().eventRequestManager();
		ClassPrepareRequest classPrepareRequest = eventRequestManager.createClassPrepareRequest();
		classPrepareRequest.addClassFilter(mainClassName);
		classPrepareRequest.enable();

		boolean preparing = true;
		while (preparing) {
			EventSet eventSet = null;
			try {
				eventSet = queue.remove();
			} catch (InterruptedException ignored) {
			}

			if (Objects.nonNull(eventSet)) {
				for (Event event : eventSet) {
					if (event instanceof VMStartEvent) {
						SimpleDebuggerLogger.info("VMStartEvent received");
					} else if (event instanceof ClassPrepareEvent classPrepareEvent) {
						SimpleDebuggerLogger.info("ClassPrepareEvent received");
						ReferenceType referenceType = classPrepareEvent.referenceType();
						Method mainMethod = referenceType.methodsByName("main").get(0);
						Location firstLine = null;
						try {
							firstLine = mainMethod.allLineLocations().get(0);
						} catch (AbsentInformationException ignored) {
						}

						BreakpointRequest breakpointRequest = eventRequestManager.createBreakpointRequest(firstLine);
						breakpointRequest.enable();
						try {
							SimpleDebuggerLogger
									.info("Breakpoint set at " + firstLine.sourceName() + ":" + firstLine.lineNumber());
							preparing = false;
							break;
						} catch (AbsentInformationException ignored) {
						}
					}
					eventSet.resume();
				}
			}
			SimpleDebuggerLogger.info("Debug preparation complete");
			DebuggerContext.context().setStatus(SimpleDebuggerStatus.PREPARED);
		}
	}

	private void openDebugWindow() {
		Display.getDefault().asyncExec(() -> {
			DebugWindow debugWindow = DebugWindowManager.instance().getOrCreateWindow();
			if (Objects.nonNull(debugWindow) && !debugWindow.isOpen())
				debugWindow.open();
		});
	}

	/** Factory for creating a debug workflow asynchronously */
	public static class SimpleDebuggerWorkFlowFactory {

		public static void createWorkflow(DebugConfiguration debugConfiguration,
				OnWorkflowReadyListener onWorkflowReadyListener) {
			SimpleDebuggerLogger.info("Starting debug workflow");

			CompletableFuture.runAsync(() -> {
				if (isDebugPortBusy(debugConfiguration.getPort())) {
					DebuggerContext.context().setStatus(SimpleDebuggerStatus.WILL_NOT_START);
					SimpleDebuggerLogger.warn("Debug port " + debugConfiguration.getPort() + " is already in use");
					notifyAlreadyRunning(debugConfiguration);
					return;
				}

				DebuggerContext.context().setStatus(SimpleDebuggerStatus.STARTING);
				VirtualMachine virtualMachine = launchVirtualMachine(debugConfiguration);

				IBreakpointManager breakpointManager = waitForBreakpointManager();
				BreakePointListener breakpointListener = new BreakePointListener();
				breakpointManager.setEnabled(true);
				breakpointManager.addBreakpointListener(breakpointListener);

				TargetVirtualMachineRepresentation targetVirtualMachineRepresentation = new TargetVirtualMachineRepresentation(
						"localhost", debugConfiguration.getPort(), virtualMachine);

				if (Objects.nonNull(onWorkflowReadyListener)) {
					Display.getDefault()
							.asyncExec(() -> onWorkflowReadyListener
									.onReady(new SimpleDebuggerWorkFlow(targetVirtualMachineRepresentation,
											breakpointManager, breakpointListener, debugConfiguration)));
				}
				SimpleDebuggerLogger.info("VM launched: " + virtualMachine.description());
			});
		}

		private static boolean isDebugPortBusy(int port) {
			try (Socket socket = new java.net.Socket("localhost", port)) {
				return true;
			} catch (IOException e) {
				return false;
			}
		}

		private static void notifyAlreadyRunning(DebugConfiguration debugConfiguration) {
			Display.getDefault().asyncExec(() -> {
				Shell shell = Display.getDefault().getActiveShell();
				MessageDialog.openError(shell, "Debug session already running",
						"Application is already running on port " + debugConfiguration.getPort());
			});
		}

		private static IBreakpointManager waitForBreakpointManager() {
			CompletableFuture<IBreakpointManager> future = new CompletableFuture<>();
			Runnable check = new Runnable() {
				@Override
				public void run() {
					DebugPlugin plugin = DebugPlugin.getDefault();
					if (Objects.nonNull(plugin) && Objects.nonNull(plugin.getBreakpointManager())) {
						future.complete(plugin.getBreakpointManager());
					} else {
						Display.getDefault().timerExec(500, this);
					}
				}
			};
			Display.getDefault().asyncExec(check);
			return future.join();
		}

		private static VirtualMachine launchVirtualMachine(DebugConfiguration debugConfiguration) {
			VirtualMachine virtualMachine = null;
			DebuggerContext.context().setStatus(SimpleDebuggerStatus.VM_AWAITING_CONNECTION);
			try {
				LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
				Map<String, Connector.Argument> launchArguments = connector.defaultArguments();
				launchArguments.get("main").setValue(debugConfiguration.getMainClassName());
				String options = "-cp " + debugConfiguration.getOutputFolder() + " "
						+ debugConfiguration.getvirtualMachineOptionsStringWithoutJDWP();
				launchArguments.get("options").setValue(options);
				launchArguments.get("suspend").setValue("true");
				virtualMachine = connector.launch(launchArguments);
				SimpleDebuggerLogger.info("==> VM LAUNCHED (SUSPENDED): " + virtualMachine.description());
				attachConsoleWriters(virtualMachine.process());
				DebuggerContext.context().setStatus(SimpleDebuggerStatus.VM_CONNECTED);
				return virtualMachine;
			} catch (Exception ex) {
				SimpleDebuggerLogger.error("Cannot launch VM", ex);
				Display.getDefault().asyncExec(() -> {
					DebugWindow debugWindow = DebugWindowManager.instance().getOrCreateWindow();
					debugWindow.showError("Cannot launch VM", ex.getMessage());
				});
			}
			return virtualMachine;
		}

		private static void attachConsoleWriters(Process process) {
			new Thread(new ConsoleWriter(process.getInputStream(), "[TARGET]")).start();
			new Thread(new ConsoleWriter(process.getErrorStream(), "[TARGET-ERR]")).start();
		}
	}
}
