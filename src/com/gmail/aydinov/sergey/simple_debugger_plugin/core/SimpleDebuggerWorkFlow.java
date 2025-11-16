package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.EventRequestManager;


public class SimpleDebuggerWorkFlow {
	
	private final TargetVirtualMachineRepresentation targetVirtualMachineRepresentation;
    private final TargetApplicationRepresentation targetApplicationRepresentation;
    //private final IBreakpointManager manager;
    private final DebugPlugin debugPlugin;  // новое поле

    public SimpleDebuggerWorkFlow(TargetVirtualMachineRepresentation targetVirtualMachineRepresentation,
                                  IBreakpointManager iBreakpointManager,
                                  DebugPlugin debugPlugin) {
        this.targetVirtualMachineRepresentation = targetVirtualMachineRepresentation;
        EventRequestManager eventRequestManager = targetVirtualMachineRepresentation.getVirtualMachine().eventRequestManager();
        this.targetApplicationRepresentation = new TargetApplicationRepresentation(iBreakpointManager, eventRequestManager, targetVirtualMachineRepresentation.getVirtualMachine());
        //this.manager = manager;
        this.debugPlugin = debugPlugin;
    }
	
	public List<ReferenceType> getClassesOfTargetApplication() {
		return targetVirtualMachineRepresentation.getVirtualMachine().allClasses();
	}

	public void debug() throws IOException, AbsentInformationException {
		System.out.println("DEBUG");
		targetApplicationRepresentation.refreshReferencesToClassesOfTargetApplication(targetVirtualMachineRepresentation.getVirtualMachine());
		targetApplicationRepresentation.getTargetApplicationBreakepointRepresentation().refreshBreakePoints();
		targetApplicationRepresentation.getTargetApplicationBreakepointRepresentation().getBreakpoints().stream().forEach(bp -> System.out.println(bp));
		//EventRequestManager eventRequestManager = targetVirtualMachineRepresentation.getVirtualMachine().eventRequestManager();
		EventQueue queue = targetVirtualMachineRepresentation.getVirtualMachine().eventQueue();
		
		while (true) {
			System.out.println("hi-hi!");
			try {
				Thread.currentThread().sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

//		while (true) {
//		    EventSet eventSet = null;
//			try {
//				eventSet = queue.remove();
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		    for (Event event : eventSet) {
//		        if (event instanceof BreakpointEvent bpEvent) {
//		            Location loc = bpEvent.location();
//		            Method method = loc.method();
//
//		            System.out.println("Breakpoint hit at:");
//		            System.out.println("Class:  " + loc.declaringType().name());
//		            System.out.println("Method: " + method.name());
//		            System.out.println("Line:   " + loc.lineNumber());
//
//		            // Получение текущего фрейма
//		            StackFrame frame = null;
//					try {
//						frame = bpEvent.thread().frame(0);
//					} catch (IncompatibleThreadStateException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//		            Map<LocalVariable, Value> vars = frame.getValues(frame.visibleVariables());
//		            vars.forEach((v, val) -> System.out.println(v.name() + " = " + val));
//
//		            // Продолжить выполнение
//		            targetVirtualMachineRepresentation.getVirtualMachine().resume();
//		        }
//		    }
//		}

	}

	public Optional<Location> findLocation(Method method, int sourceLine) {
		try {
			for (Location l : method.allLineLocations()) {
				if (l.lineNumber() == sourceLine) {
					return Optional.of(l);
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

	        // 1️⃣ Асинхронное подключение к JVM через JDI
	        CompletableFuture<VirtualMachine> vmFuture = CompletableFuture.supplyAsync(() -> configureVirtualMachine(host, port));

	        // 2️⃣ Асинхронное получение DebugPlugin и BPM
	        CompletableFuture<IBreakpointManager> bpmFuture = getDebugPluginAndBreakpointManager()
	                .thenApply(manager -> {
	                    registerBreakpointListener(manager);
	                    return manager;
	                });

	        // 3️⃣ Когда оба готовы, создаём workflow
	        vmFuture.thenCombine(bpmFuture, (vm, bpManager) -> {
	            DebugPlugin plugin = DebugPlugin.getDefault(); // уже гарантированно готов

	            return new SimpleDebuggerWorkFlow(
	                    new TargetVirtualMachineRepresentation(host, port, vm),
	                    bpManager,
	                    plugin
	            );
	        }).thenAccept(workflow -> {
	            if (listener != null) listener.onReady(workflow);
	        });
	    }

	    // -------------------
	    // Асинхронное ожидание DebugPlugin и IBreakpointManager
	    private static CompletableFuture<IBreakpointManager> getDebugPluginAndBreakpointManager() {
	        CompletableFuture<IBreakpointManager> future = new CompletableFuture<>();

	        Runnable check = new Runnable() {
	            @Override
	            public void run() {
	                DebugPlugin plugin = DebugPlugin.getDefault();
	                if (plugin != null && plugin.getBreakpointManager() != null) {
	                    future.complete(plugin.getBreakpointManager());
	                } else {
	                    // Планируем повторную проверку через 500 мс
	                    Display.getDefault().timerExec(500, this);
	                }
	            }
	        };

	        Display.getDefault().asyncExec(check);
	        return future;
	    }

	    // -------------------
	    // Регистрация собственного BreakpointListener
	    private static void registerBreakpointListener(IBreakpointManager manager) {
	        manager.setEnabled(true);
	        BreakePointListener listener = new BreakePointListener();
	        manager.addBreakpointListener(listener);
	        System.out.println("[Factory] Breakpoint listener registered!");
	    }

	    // -------------------
	    // Подключение к JVM через JDI
	    private static VirtualMachine configureVirtualMachine(String host, int port) {
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
	                System.out.println("Successfully connected to VM.");
	                return vm;
	            } catch (Exception ignored) {
	                try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException ignored2) {}
	            }
	        }
	    }
	}



}
