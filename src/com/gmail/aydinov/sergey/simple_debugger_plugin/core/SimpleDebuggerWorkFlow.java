package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.swt.widgets.Display;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.TargetApplicationStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.BreakpointSubscriberRegistrar;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DebugSession;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.OnWorkflowReadyListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowManager;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
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
		vm.resume();
//		Process process = null;
//		try {
//			process = new ProcessBuilder(
//				    "java",
//				    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005",
//				    "-cp", "/home/sergei/eclipse-commiters-workspace/target_debug/bin",
//				    "target_debug.Main"
//				).start();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//			new Thread(new ConsoleWriter(process), "stdout").start();
			//new Thread(new ConsoleWriterErr(process), "stderr").start();

			// потом
			//VirtualMachine vm = attachTo(5005);
		refreshBreakpoints();
		targetApplicationRepresentation.refreshReferencesToClassesOfTargetApplication(vm);
		boolean running = true;
		while (running) {
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
		
//		public static VirtualMachine attach(String host, int port) {
//	        try {
//	            VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
//	            AttachingConnector connector = vmm.attachingConnectors().stream()
//	                    .filter(c -> c.name().equals("com.sun.jdi.SocketAttach")).findAny().orElseThrow();
//
//	            Map<String, Connector.Argument> args = connector.defaultArguments();
//	            args.get("hostname").setValue(host);
//	            args.get("port").setValue(String.valueOf(port));
//
//	            VirtualMachine vm;
//	            while (true) {
//	                try {
//	                    vm = connector.attach(args);
//	                    vm.suspend(); // останавливаем сразу после attach
//	                    break;
//	                } catch (Exception ignored) {
//	                    Thread.sleep(500);
//	                }
//	            }
//	            return vm;
//	        } catch (Exception e) {
//	            throw new RuntimeException("Cannot attach VM", e);
//	        }
//	    }

		public static VirtualMachine launchVirtualMachine() {
		    try {
		        LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
		        Map<String, Connector.Argument> args = connector.defaultArguments();

		        // Main class и classpath
		        args.get("main").setValue("target_debug.Main");
		        args.get("options").setValue("-cp /home/sergei/eclipse-commiters-workspace/target_debug/bin");

		        // Не приостанавливать JVM
		        args.get("suspend").setValue("false");

		        // Запуск таргета
		        VirtualMachine vm = connector.launch(args);
		        System.out.println("==> VM LAUNCHED: " + vm.description());

		        // Запускаем потоки для консоли
		        attachConsoleReaders(vm.process());

		        return vm;

		    } catch (Exception e) {
		        throw new RuntimeException("Cannot launch VM", e);
		    }
		}

		private static void attachConsoleReaders(Process process) {
		    // stdout
		    new Thread(() -> {
		        try (InputStream in = process.getInputStream();
		             InputStreamReader isr = new InputStreamReader(in);
		             BufferedReader reader = new BufferedReader(isr)) {
		            String line;
		            while ((line = reader.readLine()) != null) {
		                System.out.println("[TARGET] " + line);
		            }
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
		    }, "Console-stdout").start();

		    // stderr
		    new Thread(() -> {
		        try (InputStream err = process.getErrorStream();
		             InputStreamReader isr = new InputStreamReader(err);
		             BufferedReader reader = new BufferedReader(isr)) {
		            String line;
		            while ((line = reader.readLine()) != null) {
		                System.err.println("[TARGET-ERR] " + line);
		            }
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
		    }, "Console-stderr").start();
		}


	}
}
