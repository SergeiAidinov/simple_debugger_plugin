package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.io.IOException;
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
 * Main workflow for controlling the debug session. Handles VM launch,
 * breakpoint setup, event loop, and UI updates.
 */
public class SimpleDebuggerWorkFlow {

	private final TargetVirtualMachineRepresentation targetVmRepresentation;
	private final TargetApplicationRepresentation targetAppRepresentation;
	private final IBreakpointManager breakpointManager; // do NOT remove
	private final BreakpointSubscriberRegistrar breakpointListener; // do NOT remove
	private final CurrentLineHighlighter highlighter = new CurrentLineHighlighter();

	public SimpleDebuggerWorkFlow(TargetVirtualMachineRepresentation targetVmRepresentation,
			IBreakpointManager breakpointManager, BreakpointSubscriberRegistrar breakpointListener,
			DebugConfiguration debugConfiguration) {
		this.targetVmRepresentation = targetVmRepresentation;
		this.breakpointManager = breakpointManager;
		this.breakpointListener = breakpointListener;

		EventRequestManager eventRequestManager = targetVmRepresentation.getVirtualMachine().eventRequestManager();
		this.targetAppRepresentation = new TargetApplicationRepresentation(breakpointManager, eventRequestManager,
				targetVmRepresentation.getVirtualMachine(), breakpointListener, debugConfiguration);
	}

	/** Starts the debug workflow */
	public void debug(String mainClassName) {
		prepareDebug(targetVmRepresentation.getVirtualMachine().eventQueue(), mainClassName);

		SimpleDebuggerLogger.info("DEBUGGER STARTED");
		DebuggerContext.context().setStatus(SimpleDebuggerStatus.RUNNING);
		openDebugWindow();
		targetVmRepresentation.getVirtualMachine().resume();

		while (DebuggerContext.context().isRunning()) {
			targetAppRepresentation
					.refreshReferencesToClassesOfTargetApplication(targetVmRepresentation.getVirtualMachine());
			targetAppRepresentation.getTargetApplicationBreakepointRepresentation().refreshBreakpoints();

			EventSet eventSet = null;
			try {
				eventSet = targetVmRepresentation.getVirtualMachine().eventQueue().remove();
			} catch (InterruptedException ignored) {
			} catch (com.sun.jdi.VMDisconnectedException e) {
				SimpleDebuggerLogger.info("VM disconnected, exiting debug loop.");
				break;
			}

			if (Objects.isNull(eventSet))
				continue;

			for (Event event : eventSet) {
				if (event instanceof ClassPrepareEvent classPrepareEvent) {
					targetAppRepresentation.getTargetApplicationBreakepointRepresentation()
							.onClassPrepared(classPrepareEvent.referenceType());
				} else if (event instanceof BreakpointEvent) {
					DebugSession debugSession = new DebugSessionImpl(targetVmRepresentation, targetAppRepresentation,
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
		openDebugWindow();
		EventRequestManager eventRequestManager = targetVmRepresentation.getVirtualMachine().eventRequestManager();
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
			DebugWindow window = DebugWindowManager.instance().getOrCreateWindow();
			if (Objects.nonNull(window) && !window.isOpen())
				window.open();
		});
	}

	/** Factory for creating a debug workflow asynchronously */
	public static class SimpleDebuggerWorkFlowFactory {

		public static void createWorkflow(DebugConfiguration debugConfiguration, OnWorkflowReadyListener listener) {
			SimpleDebuggerLogger.info("Starting debug workflow");

			CompletableFuture.runAsync(() -> {
				if (isDebugPortBusy(debugConfiguration.getPort())) {
					DebuggerContext.context().setStatus(SimpleDebuggerStatus.NOT_STARTED);
					SimpleDebuggerLogger.warn("Debug port " + debugConfiguration.getPort() + " is already in use");
					notifyAlreadyRunning(debugConfiguration);
					return;
				}

				DebuggerContext.context().setStatus(SimpleDebuggerStatus.STARTING);
				VirtualMachine vm = launchVirtualMachine(debugConfiguration);

				IBreakpointManager breakpointManager = waitForBreakpointManager();
				BreakePointListener breakpointListener = new BreakePointListener();
				breakpointManager.setEnabled(true);
				breakpointManager.addBreakpointListener(breakpointListener);

				TargetVirtualMachineRepresentation vmRepresentation = new TargetVirtualMachineRepresentation(
						"localhost", debugConfiguration.getPort(), vm);

				if (Objects.nonNull(listener)) {
					Display.getDefault().asyncExec(() -> listener.onReady(new SimpleDebuggerWorkFlow(vmRepresentation,
							breakpointManager, breakpointListener, debugConfiguration)));
				}

				SimpleDebuggerLogger.info("VM launched: " + vm.description());
			});
		}

		private static boolean isDebugPortBusy(int port) {
			try (var socket = new java.net.Socket("localhost", port)) {
				return true;
			} catch (IOException e) {
				return false;
			}
		}

		private static void notifyAlreadyRunning(DebugConfiguration config) {
			Display.getDefault().asyncExec(() -> {
				Shell shell = Display.getDefault().getActiveShell();
				MessageDialog.openError(shell, "Debug session already running",
						"Application is already running on port " + config.getPort());
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

		private static VirtualMachine launchVirtualMachine(DebugConfiguration debugConfig) {
			VirtualMachine vm = null;
			DebuggerContext.context().setStatus(SimpleDebuggerStatus.VM_AWAITING_CONNECTION);

			try {
				LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
				Map<String, Connector.Argument> args = connector.defaultArguments();

				args.get("main").setValue(debugConfig.getMainClassName());
				String options = "-cp " + debugConfig.getOutputFolder() + " "
						+ debugConfig.getvirtualMachineOptionsStringWithoutJDWP();
				args.get("options").setValue(options);
				args.get("suspend").setValue("true");

				vm = connector.launch(args);
				SimpleDebuggerLogger.info("==> VM LAUNCHED (SUSPENDED): " + vm.description());

				attachConsoleWriters(vm.process());
				DebuggerContext.context().setStatus(SimpleDebuggerStatus.VM_CONNECTED);
				return vm;

			} catch (Exception ex) {
				SimpleDebuggerLogger.error("Cannot launch VM", ex);
				Display.getDefault().asyncExec(() -> {
					DebugWindow window = DebugWindowManager.instance().getOrCreateWindow();
					window.showError("Cannot launch VM", ex.getMessage());
				});
			}

			return vm;
		}

		private static void attachConsoleWriters(Process process) {
			new Thread(new ConsoleWriter(process.getInputStream(), "[TARGET]")).start();
			new Thread(new ConsoleWriter(process.getErrorStream(), "[TARGET-ERR]")).start();
		}
	}
}
