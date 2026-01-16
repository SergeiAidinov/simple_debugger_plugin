package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.io.IOException;
import java.io.InputStream;
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
		Process process = vm.process();
		new Thread(() -> {
			try (InputStream in = process.getInputStream()) {
				in.transferTo(System.out);
			} catch (IOException ignored) {
			}
		}).start();

		// stderr
		new Thread(() -> {
			try (InputStream err = process.getErrorStream()) {
				err.transferTo(System.err);
			} catch (IOException ignored) {
			}
		}).start();
		//try {
			// Подождём, пока VM реально запустится
//			int attempts = 0;
//			while (targetApplicationRepresentation.getReferencesAtClassesAndInterfaces().size() == 0) {
//				refreshBreakpoints();
//				targetApplicationRepresentation.refreshReferencesToClassesOfTargetApplication(vm);
//				Thread.sleep(1000);
//				attempts++;
//				System.out.println("ATTEMPTS: " + attempts);
//
//			}
//
//			// vm.resume(); // запускаем VM
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		refreshBreakpoints();
		targetApplicationRepresentation.refreshReferencesToClassesOfTargetApplication(vm);

		boolean running = true;
		while (running) {
//			if (DebuggerContext.context().getTargetApplicationStatus().equals(TargetApplicationStatus.STARTING))
//				continue;
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
	            // Получаем стандартный LaunchingConnector
	            LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
	            Map<String, Connector.Argument> args = connector.defaultArguments();

	            // Полностью квалифицированное имя main-класса
	            args.get("main").setValue("target_debug.Main");

	            // Classpath к скомпилированным классам (не исходники)
	            args.get("options").setValue("-cp /home/sergei/eclipse-commiters-workspace/target_debug/bin"); 

	            // Не останавливать сразу JVM
	            args.get("suspend").setValue("false");

	            // Запускаем target JVM
	            VirtualMachine vm = connector.launch(args);

	            System.out.println("==> VM LAUNCHED: " + vm.description());

	            // Логируем stdout и stderr target-приложения
	            Process process = vm.process();

	            new Thread(() -> {
	                try (InputStream in = process.getInputStream()) {
	                    in.transferTo(System.out);
	                } catch (Exception ignored) {}
	            }).start();

	            new Thread(() -> {
	                try (InputStream err = process.getErrorStream()) {
	                    err.transferTo(System.err);
	                } catch (Exception ignored) {}
	            }).start();

	            return vm;

	        } catch (Exception e) {
	            throw new RuntimeException("Cannot launch VM", e);
	        }
	    }

	}
}
