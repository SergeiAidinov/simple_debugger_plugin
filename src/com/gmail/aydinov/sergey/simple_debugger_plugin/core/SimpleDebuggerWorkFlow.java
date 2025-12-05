package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.swt.widgets.Display;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.StatusesHolder;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.StatusesHolder.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.StatusesHolder.TargetApplicationStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.BreakpointSubscriberRegistrar;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DebugSession;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.OnWorkflowReadyListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowManager;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.StackFrame;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.EventRequestManager;

public class SimpleDebuggerWorkFlow {

	private final TargetVirtualMachineRepresentation targetVirtualMachineRepresentation;
	private final TargetApplicationRepresentation targetApplicationRepresentation;
	private final IBreakpointManager iBreakpointManager; // do NOT remove!!!
	private final BreakpointSubscriberRegistrar breakpointListener; // do NOT remove!!!
	private final CurrentLineHighlighter highlighter = new CurrentLineHighlighter();
	public static final AtomicBoolean running = new AtomicBoolean();

	public SimpleDebuggerWorkFlow(TargetVirtualMachineRepresentation targetVirtualMachineRepresentation,
			IBreakpointManager iBreakpointManager, BreakpointSubscriberRegistrar breakpointListener) {
		this.targetVirtualMachineRepresentation = targetVirtualMachineRepresentation;
		this.iBreakpointManager = iBreakpointManager;
		EventRequestManager eventRequestManager = targetVirtualMachineRepresentation.getVirtualMachine()
				.eventRequestManager();
		this.breakpointListener = breakpointListener;
		this.targetApplicationRepresentation = new TargetApplicationRepresentation(iBreakpointManager,
				eventRequestManager, targetVirtualMachineRepresentation.getVirtualMachine(), breakpointListener);
		running.set(true);
	}

/** Запуск дебага */
	public void debug() {
		System.out.println("DEBUG");
		openDebugWindow();
		refreshBreakpoints();
		targetApplicationRepresentation
				.refreshReferencesToClassesOfTargetApplication(targetVirtualMachineRepresentation.getVirtualMachine());
		
		while (running.get()) {
			EventQueue queue = targetVirtualMachineRepresentation.getVirtualMachine().eventQueue();
			EventSet eventSet = null;
			try {
				eventSet = queue.remove();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (Objects.isNull(eventSet))
				continue;
			
			DebugSession debugSession = new DebugSessionImpl(targetVirtualMachineRepresentation,
					targetApplicationRepresentation, eventSet, highlighter);
			Thread debugSessionThread = new Thread(debugSession);
			debugSessionThread.setDaemon(true);
			debugSessionThread.start();
			try {
				debugSessionThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			refreshBreakpoints();
			// eventSet.resume();
		}
	}

	private void refreshBreakpoints() {
		targetApplicationRepresentation.getTargetApplicationBreakepointRepresentation().refreshBreakePoints();
	}

	private void openDebugWindow() {
		Display.getDefault().asyncExec(() -> {
			DebugWindow window = DebugWindowManager.instance().getOrCreateWindow();
			if (window != null && !window.isOpen())
				window.open();
		});
	}

	public static class Factory {

		private static SimpleDebuggerWorkFlow instance = null;

		public static SimpleDebuggerWorkFlow getSimpleDebuggerWorkFlow() {
			return instance;
		}

		public static void create(String host, int port, OnWorkflowReadyListener listener) {
			//StatusesHolder.simpleDebuggerStatus = SimpleDebuggerStatus.STARTING;

			CompletableFuture.runAsync(() -> {
				VirtualMachine vm = attachToVm(host, port);
				IBreakpointManager bpm = waitForBreakpointManager();

				BreakePointListener breakpointListener = new BreakePointListener();
				bpm.setEnabled(true);
				bpm.addBreakpointListener(breakpointListener);
				//StatusesHolder.simpleDebuggerStatus = SimpleDebuggerStatus.STARTING;
				instance = new SimpleDebuggerWorkFlow(new TargetVirtualMachineRepresentation(host, port, vm), bpm,
						breakpointListener);
				if (listener != null) {
					Display.getDefault().asyncExec(() -> listener.onReady(instance));
				}
			});
		}

		// -------------------
		private static VirtualMachine attachToVm(String host, int port) {
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
					//simpleDebuggerStatus = SimpleDebuggerStatus.VM_CONNECTED;
					StatusesHolder.simpleDebuggerStatus = SimpleDebuggerStatus.VM_CONNECTED;
					System.out.println("Successfully connected to VM.");
					return vm;
				} catch (Exception ignored) {
					StatusesHolder.simpleDebuggerStatus = SimpleDebuggerStatus.VM_AWAITING_CONNECTION;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ignored2) {
					}
				}
			}
		}

		// -------------------
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
	}
}
