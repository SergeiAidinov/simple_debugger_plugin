package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.swt.widgets.Display;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationBreakepointRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.BreakpointSubscriberRegistrar;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DebugSession;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.OnWorkflowReadyListener;
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
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;

public class SimpleDebuggerWorkFlow {

	private final TargetVirtualMachineRepresentation targetVirtualMachineRepresentation;
	private final TargetApplicationRepresentation targetApplicationRepresentation;
	private final IBreakpointManager iBreakpointManager;
	private final BreakpointSubscriberRegistrar breakpointListener;
	private final CurrentLineHighlighter highlighter = new CurrentLineHighlighter();

	public SimpleDebuggerWorkFlow(TargetVirtualMachineRepresentation targetVirtualMachineRepresentation,
			IBreakpointManager iBreakpointManager, BreakpointSubscriberRegistrar breakpointListener) {
		this.targetVirtualMachineRepresentation = targetVirtualMachineRepresentation;
		this.iBreakpointManager = iBreakpointManager;
		EventRequestManager eventRequestManager = targetVirtualMachineRepresentation.getVirtualMachine()
				.eventRequestManager();
		this.breakpointListener = breakpointListener;
		this.targetApplicationRepresentation = new TargetApplicationRepresentation(iBreakpointManager,
				eventRequestManager, targetVirtualMachineRepresentation.getVirtualMachine(), breakpointListener);
		DebuggerContext.context().setRunning(true);
	}

	/** Запуск дебага */
	public void debug() {
		System.out.println("DEBUG");
		openDebugWindow();
		VirtualMachine vm = targetVirtualMachineRepresentation.getVirtualMachine();
		prepareDebug(vm.eventQueue());
		refreshBreakpoints();
		System.out.println("BEFORE CYCLE: " + targetApplicationRepresentation.getTargetApplicationBreakepointRepresentation()
				.prettyPrintBreakpoints());
		targetApplicationRepresentation.refreshReferencesToClassesOfTargetApplication(vm);
		vm.resume();
		boolean running = true;
		while (running) {
			targetApplicationRepresentation.refreshReferencesToClassesOfTargetApplication(vm);
			refreshBreakpoints();
			System.out.println("===>");
			System.out.println(targetApplicationRepresentation.getTargetApplicationBreakepointRepresentation()
					.prettyPrintBreakpoints());
			System.out.println("<===");
			EventQueue queue = vm.eventQueue();
			EventSet eventSet = null;
			try {
				eventSet = queue.remove();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (com.sun.jdi.VMDisconnectedException e) {
				System.out.println("VM disconnected, finishing debug loop.");
				break; // корректно выходим из цикла
			}

			if (eventSet == null)
				continue;

			DebugSession debugSession = new DebugSessionImpl(targetVirtualMachineRepresentation,
					targetApplicationRepresentation, eventSet, highlighter);
			Thread debugSessionThread = new Thread(debugSession);
			debugSessionThread.setDaemon(true);
			debugSessionThread.start();

			try {
				debugSessionThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			eventSet.resume();

			running = DebuggerContext.context().isRunning();
		}
	}

	private void prepareDebug(EventQueue queue) {
		openDebugWindow();
		VirtualMachine vm = targetVirtualMachineRepresentation.getVirtualMachine();
		EventRequestManager erm = vm.eventRequestManager();
		ClassPrepareRequest cpr = erm.createClassPrepareRequest();
		cpr.addClassFilter("target_debug.Main");
		cpr.enable();
		boolean preparing = true;
		while (preparing) {
			EventSet eventSet = null;
			try {
				 eventSet = queue.remove();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for (Event event : eventSet) {
				if (event instanceof VMStartEvent) {
					System.out.println("VMStartEvent");
				} else if (event instanceof ClassPrepareEvent cpe) {
					System.out.println("ClassPrepareEvent");
					ReferenceType ref = cpe.referenceType();
					Method main = ref.methodsByName("main").get(0);
					Location firstLine = null;
					try {
						firstLine = main.allLineLocations().get(0);
					} catch (AbsentInformationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					BreakpointRequest bp = erm.createBreakpointRequest(firstLine);
					bp.enable();
					try {
						System.out
								.println("Breakpoint set at " + firstLine.sourceName() + ":" + firstLine.lineNumber());
						preparing = false;
						break;
					} catch (AbsentInformationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
//				
			 eventSet.resume();
		}
		System.out.println("Debug prepared");
		}
	}

	private void refreshBreakpoints() {
		targetApplicationRepresentation.getTargetApplicationBreakepointRepresentation().refreshBreakpoints();
	}

	private void openDebugWindow() {
		Display.getDefault().asyncExec(() -> {
			DebugWindow window = DebugWindowManager.instance().getOrCreateWindow();
			if (Objects.nonNull(window) && !window.isOpen())
				window.open();
		});
	}

	public static class SimpleDebuggerWorkFlowFactory {

		private static SimpleDebuggerWorkFlow instance = null;

		public static SimpleDebuggerWorkFlow getInstance() {
			return instance;
		}

		public static void createWorkFlow(String mainClass, List<String> options, OnWorkflowReadyListener listener) {
			CompletableFuture.runAsync(() -> {
				VirtualMachine virtualMachine = launchVirtualMachine();
				IBreakpointManager breakpointManager = waitForBreakpointManager();
				BreakePointListener breakePointListener = new BreakePointListener();
				breakpointManager.setEnabled(true);
				breakpointManager.addBreakpointListener(breakePointListener);
				TargetVirtualMachineRepresentation targetVirtualMachineRepresentation = new TargetVirtualMachineRepresentation(
						"localhost", 5005, virtualMachine);

				instance = new SimpleDebuggerWorkFlow(targetVirtualMachineRepresentation, breakpointManager,
						breakePointListener);
				if (Objects.nonNull(listener)) {
					Display.getDefault().asyncExec(() -> listener.onReady(instance));
				}
			});
		}

		private static IBreakpointManager waitForBreakpointManager() {
			CompletableFuture<IBreakpointManager> future = new CompletableFuture<>();
			Runnable check = new Runnable() {
				@Override
				public void run() {
					DebugPlugin plugin = DebugPlugin.getDefault();
					if (plugin != null && plugin.getBreakpointManager() != null) {
						future.complete(plugin.getBreakpointManager());
					} else {
						Display.getDefault().timerExec(500, this);
					}
				}
			};
			Display.getDefault().asyncExec(check);
			return future.join();
		}

		public static VirtualMachine launchVirtualMachine() {
			try {
				LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();

				Map<String, Connector.Argument> args = connector.defaultArguments();

				args.get("main").setValue("target_debug.Main");
				args.get("options").setValue("-cp /home/sergei/eclipse-commiters-workspace/target_debug/bin");

				// 🔴 ВАЖНО: JVM стартует ОСТАНОВЛЕННОЙ
				args.get("suspend").setValue("true");

				VirtualMachine vm = connector.launch(args);

				System.out.println("==> VM LAUNCHED (SUSPENDED): " + vm.description());

				attachConsoleReaders(vm.process());

				return vm;

			} catch (Exception e) {
				throw new RuntimeException("Cannot launch VM", e);
			}
		}

		private static void attachConsoleReaders(Process process) {
			new Thread(new ConsoleWriter(process.getInputStream(), "[TARGET]")).start();
			new Thread(new ConsoleWriter(process.getErrorStream(), "[TARGET-ERR]")).start();
		}
	}
}
