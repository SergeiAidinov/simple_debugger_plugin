package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.swt.widgets.Display;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
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
		refreshBreakpoints();
		targetApplicationRepresentation
				.refreshReferencesToClassesOfTargetApplication(targetVirtualMachineRepresentation.getVirtualMachine());

		while (DebuggerContext.context().isRunning()) {
			EventQueue queue = targetVirtualMachineRepresentation.getVirtualMachine().eventQueue();
			EventSet eventSet = null;
			try {
				eventSet = queue.remove();
			} catch (InterruptedException e) {
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
				e.printStackTrace();
			}
			eventSet.resume();
		}
	}

	private void refreshBreakpoints() {
		targetApplicationRepresentation.getTargetApplicationBreakepointRepresentation().refreshBreakePoints();
	}

	private void openDebugWindow() {
		Display.getDefault().asyncExec(() -> {
			DebugWindow window = DebugWindowManager.instance().getOrCreateWindow();
			if (Objects.nonNull(window) && !window.isOpen())
				window.open();
		});
	}

	public static class Factory {

	    private static SimpleDebuggerWorkFlow instance = null;

	    public static SimpleDebuggerWorkFlow getSimpleDebuggerWorkFlow() {
	        return instance;
	    }

	    /** Синхронное создание workflow */
	    public static SimpleDebuggerWorkFlow createSync(String host, int port) {
	        // 1. Подключаемся к VM
	        VirtualMachine vm = attachToVm(host, port);

	        // 2. Получаем менеджер брейкпойнтов синхронно
	        IBreakpointManager bpm = waitForBreakpointManagerSync();

	        // 3. Создаём listener для брейкпойнтов
	        BreakePointListener breakpointListener = new BreakePointListener();
	        bpm.setEnabled(true);
	        bpm.addBreakpointListener(breakpointListener);

	        // 4. Создаём workflow
	        instance = new SimpleDebuggerWorkFlow(
	                new TargetVirtualMachineRepresentation(host, port, vm),
	                bpm,
	                breakpointListener
	        );

	        // 5. Возвращаем workflow
	        return instance;
	    }

	    // -------------------
	    private static VirtualMachine attachToVm(String host, int port) {
	        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
	        AttachingConnector connector = vmm.attachingConnectors().stream()
	                .filter(c -> c.name().equals("com.sun.jdi.SocketAttach"))
	                .findAny()
	                .orElseThrow();

	        Map<String, Connector.Argument> args = connector.defaultArguments();
	        args.get("hostname").setValue(host);
	        args.get("port").setValue(String.valueOf(port));

	        while (true) {
	            try {
	                System.out.println("Connecting to " + host + ":" + port + "...");
	                VirtualMachine vm = connector.attach(args);
	                DebuggerContext.context().setSimpleDebuggerStatus(
	                        DebuggerContext.SimpleDebuggerStatus.VM_CONNECTED
	                );
	                System.out.println("Successfully connected to VM.");
	                return vm;
	            } catch (Exception ignored) {
	                DebuggerContext.context().setSimpleDebuggerStatus(
	                        DebuggerContext.SimpleDebuggerStatus.VM_AWAITING_CONNECTION
	                );
	                try { Thread.sleep(1000); } catch (InterruptedException ignored2) {}
	            }
	        }
	    }

	    // -------------------
	    private static IBreakpointManager waitForBreakpointManagerSync() {
	        while (true) {
	            DebugPlugin plugin = DebugPlugin.getDefault();
	            if (plugin != null && plugin.getBreakpointManager() != null) {
	                return plugin.getBreakpointManager();
	            }
	            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
	        }
	    }
	
	
	public static SimpleDebuggerWorkFlow createSync(String host, int port, OnWorkflowReadyListener listener) {
	    // 1. Подключаемся к VM
	    VirtualMachine vm = attachToVm(host, port);

	    // 2. Получаем менеджер брейкпойнтов синхронно
	    IBreakpointManager bpm = waitForBreakpointManagerSync();

	    // 3. Создаём listener для брейкпойнтов
	    BreakePointListener breakpointListener = new BreakePointListener();
	    bpm.setEnabled(true);
	    bpm.addBreakpointListener(breakpointListener);

	    // 4. Создаём workflow
	    instance = new SimpleDebuggerWorkFlow(
	            new TargetVirtualMachineRepresentation(host, port, vm),
	            bpm,
	            breakpointListener
	    );

	    // 5. Вызываем колбэк, если передан
	    if (listener != null) {
	        listener.onReady(instance);
	    }

	    // 6. Возвращаем workflow
	    return instance;
	}

	}
}
