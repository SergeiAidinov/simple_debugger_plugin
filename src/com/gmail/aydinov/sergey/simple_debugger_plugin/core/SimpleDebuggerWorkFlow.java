package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.swt.widgets.Display;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

public class SimpleDebuggerWorkFlow {

	private final TargetVirtualMachineRepresentation targetVirtualMachineRepresentation;
	private final TargetApplicationRepresentation targetApplicationRepresentation;
	// private final IBreakpointManager manager;
	private final DebugPlugin debugPlugin; // новое поле

	public SimpleDebuggerWorkFlow(TargetVirtualMachineRepresentation targetVirtualMachineRepresentation,
			IBreakpointManager iBreakpointManager, DebugPlugin debugPlugin, BreakpointHitListener breakpointListener) {
		this.targetVirtualMachineRepresentation = targetVirtualMachineRepresentation;
		EventRequestManager eventRequestManager = targetVirtualMachineRepresentation.getVirtualMachine()
				.eventRequestManager();
		this.targetApplicationRepresentation = new TargetApplicationRepresentation(iBreakpointManager,
				eventRequestManager, targetVirtualMachineRepresentation.getVirtualMachine(), breakpointListener);
		// this.manager = manager;
		this.debugPlugin = debugPlugin;
	}

	public List<ReferenceType> getClassesOfTargetApplication() {
		return targetVirtualMachineRepresentation.getVirtualMachine().allClasses();
	}

	public void debug() throws IOException, AbsentInformationException {
		System.out.println("DEBUG");

		// Обновляем данные о target приложении
		targetApplicationRepresentation
				.refreshReferencesToClassesOfTargetApplication(targetVirtualMachineRepresentation.getVirtualMachine());
		targetApplicationRepresentation.getTargetApplicationBreakepointRepresentation().refreshBreakePoints();

		// Получаем JDI EventRequestManager
		EventRequestManager eventRequestManager = targetVirtualMachineRepresentation.getVirtualMachine()
				.eventRequestManager();

		// Создаём BreakpointRequest один раз для всех Location
		ConcurrentLinkedDeque<Location> locationsQueue = targetApplicationRepresentation
				.getTargetApplicationBreakepointRepresentation().getLocations();

		for (Location location : locationsQueue) {
			BreakpointRequest breakpointRequest = eventRequestManager.createBreakpointRequest(location);
			breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD); // или SUSPEND_ALL
			breakpointRequest.enable();
		}

		System.out.println("Waiting for events...");
		EventQueue queue;

		while (true) {
			queue = targetVirtualMachineRepresentation.getVirtualMachine().eventQueue();
			try {
				EventSet eventSet = queue.remove(); // блокирует до события

				for (Event event : eventSet) {
					if (event instanceof BreakpointEvent bpEvent) {
						handleBreakpointEvent(bpEvent);
					} else if (event instanceof VMDisconnectEvent || event instanceof VMDeathEvent) {
						System.out.println("Target VM stopped");
						return;
					}
				}

				// После обработки всех событий нужно их резюмировать
				eventSet.resume();

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// Отдельный метод для обработки события
	private void handleBreakpointEvent(BreakpointEvent bpEvent) {
		ThreadReference thread = bpEvent.thread();
		Location loc = bpEvent.location();
		try {
			StackFrame frame = thread.frame(0);
			System.out.println("Breakpoint hit at " + loc.declaringType().name() + "."
					+ frame.location().method().name() + " line " + loc.lineNumber());

			// Локальные переменные
			// Локальные переменные
			for (LocalVariable var : frame.visibleVariables()) {
				Value val = frame.getValue(var);
				System.out.println(var.name() + " = " + val);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Optional<Location> findLocation(Method method, int sourceLine) {
		try {
			for (Location location : method.allLineLocations()) {
				if (location.lineNumber() == sourceLine) {
					return Optional.of(location);
				}
			}
		} catch (AbsentInformationException e) {
			// в этом случае исходники не доступны: метод скомпилирован без -g
			return Optional.empty();
		}
		return Optional.empty();
	}

	public static class Factory {

		public static void create(String host, int port, OnWorkflowReadyListener listener) {

			// 1️⃣ Подключение к JVM асинхронно
			CompletableFuture<VirtualMachine> vmFuture = CompletableFuture
					.supplyAsync(() -> configureVirtualMachine(host, port));

			// 2️⃣ Асинхронное ожидание DebugPlugin и BreakpointManager
			CompletableFuture<IBreakpointManager> bpmFuture = getDebugPluginAndBreakpointManager();

			// 3️⃣ Когда оба готовы — создаём workflow с listener
			vmFuture.thenCombine(bpmFuture, (vm, bpManager) -> {

				DebugPlugin plugin = DebugPlugin.getDefault();

				// 🔹 создаём и регистрируем listener
				BreakePointListener breakpointListener = new BreakePointListener();
				bpManager.setEnabled(true);
				bpManager.addBreakpointListener(breakpointListener);
				System.out.println("[Factory] Breakpoint listener registered!");

				// 🔹 создаём workflow с listener
				return new SimpleDebuggerWorkFlow(new TargetVirtualMachineRepresentation(host, port, vm), bpManager,
						plugin, breakpointListener);

			}).thenAccept(workflow -> {
				if (listener != null)
					listener.onReady(workflow);
			});
		}

		// -------------------
		private static CompletableFuture<IBreakpointManager> getDebugPluginAndBreakpointManager() {
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
			return future;
		}

		// -------------------
		private static VirtualMachine configureVirtualMachine(String host, int port) {
			VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
			AttachingConnector connector = vmm.attachingConnectors().stream()
					.filter(c -> c.name().equals("com.sun.jdi.SocketAttach")).findAny().orElseThrow();

			Map<String, Connector.Argument> args = connector.defaultArguments();
			args.get("hostname").setValue(host);
			args.get("port").setValue(String.valueOf(port));

			while (true) {
				try {
					System.out.println("Connecting to " + host + ":" + port + "...");
					VirtualMachine vm = connector.attach(args);
					System.out.println("Successfully connected to VM.");
					return vm;
				} catch (Exception ignored) {
					try {
						TimeUnit.SECONDS.sleep(1);
					} catch (InterruptedException ignored2) {
					}
				}
			}
		}

		// -------------------
		public interface OnWorkflowReadyListener {
			void onReady(SimpleDebuggerWorkFlow workflow);
		}
	}

}
