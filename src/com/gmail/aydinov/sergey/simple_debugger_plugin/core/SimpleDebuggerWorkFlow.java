package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

	/*
	 * private String host; private Integer port; private VirtualMachine
	 * virtualMachine;
	 */

	// private static SimpleDebuggerWorkFlow instance = null;
	// private TargetVirtualMachineRepresentation
	// targetVirtualMachineRepresentation;

	// private Method method = null;
	// private static final Map<SimpleDebuggerWorkFlowIdentifier,
	// SimpleDebuggerWorkFlow> CACHE = new WeakHashMap<>();

	/*
	 * private SimpleDebuggerWorkFlow(String host, int port) throws
	 * IllegalStateException { targetVirtualMachineRepresentation =
	 * TargetVirtualMachineRepresentation.instance(host, port); }
	 */

	/*
	 * public static SimpleDebuggerWorkFlow instance(String host, int port) { if
	 * (Objects.isNull(instance)) instance = new SimpleDebuggerWorkFlow(host, port);
	 * return instance; }
	 * 
	 * public List<ReferenceType> getClassesOfTargetApplication() { return
	 * targetVirtualMachineRepresentation.getVirtualMachine().allClasses(); }
	 */

	
	
	public List<ReferenceType> getClassesOfTargetApplication() {
		return targetVirtualMachineRepresentation.getVirtualMachine().allClasses();
	}

	public SimpleDebuggerWorkFlow(TargetVirtualMachineRepresentation targetVirtualMachineRepresentation) {
		this.targetVirtualMachineRepresentation = targetVirtualMachineRepresentation;
	}


	public void debug() throws IOException, AbsentInformationException {
		System.out.println("DEBUG");
		EventRequestManager eventRequestManager = targetVirtualMachineRepresentation.getVirtualMachine().eventRequestManager();
//		 System.out.println("referencesAtClassesAndInterfaces.size: " +
//		 referencesAtClassesAndInterfaces.size());

//		for (TargetApplicationElementRepresentation targetApplicationElementRepresentation : referencesAtClassesAndInterfaces
//				.values()) {
//			System.out.println("==> " + targetApplicationElementRepresentation.prettyPrint());
//			if (targetApplicationElementRepresentation.getTargetApplicationElementType()
//					.equals(TargetApplicationElementType.CLASS) && Objects.isNull(method)) {
//				method = targetApplicationElementRepresentation.getMethods().stream()
//						.filter(m -> m.name().contains("sayHello")).findAny().orElse(null);
//			}
		// }
		/*
		 * Location location = method.location(); BreakpointRequest bpReq =
		 * eventRequestManager.createBreakpointRequest(location); bpReq.enable();
		 */
//		Optional<Location> loc = findLocation(method, 29);
//		loc.ifPresent(l -> {
//			BreakpointRequest bp = eventRequestManager.createBreakpointRequest(l);
//			bp.enable();
//		});

		EventQueue queue = targetVirtualMachineRepresentation.getVirtualMachine().eventQueue();
		System.out.println("Waiting for events...");

		while (true) {
			EventSet eventSet = null;
			try {
				eventSet = queue.remove();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			for (Event event : eventSet) {
				if (event instanceof BreakpointEvent breakpointEvent) {
					System.out.println("Breakpoint hit at method: " + breakpointEvent.location().method().name());
					BreakpointEvent bp = (BreakpointEvent) event;
					StackFrame frame = null;
					try {
						frame = bp.thread().frame(0);
					} catch (IncompatibleThreadStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Map<LocalVariable, Value> values = frame.getValues(frame.visibleVariables());
					values.values().stream().forEach(v -> System.out.println(v));
					targetVirtualMachineRepresentation.getVirtualMachine().resume();
				}
			}
		}
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

	    private static SimpleDebuggerWorkFlow instance;

	    public static synchronized SimpleDebuggerWorkFlow create(String host, int port, OnWorkflowReadyListener listener) {
	    	AtomicReference<VirtualMachine> vmRef = new AtomicReference<>();
	        if (Objects.nonNull(instance)) {
	            throw new IllegalStateException("SimpleDebuggerWorkFlow already created");
	        }

	        // Запуск асинхронного потока для инициализации
	       // new Thread(() -> {
	            CountDownLatch latch = new CountDownLatch(2);

	            // 1. Асинхронная регистрация listener'a
	            Display.getDefault().asyncExec(() -> {
	                try {
	                    DebugPlugin plugin = DebugPlugin.getDefault();
	                    if (Objects.nonNull(plugin) && Objects.nonNull(plugin.getBreakpointManager())) {
	                        registerListener();
	                    } else {
	                        scheduleRetry(); // если UI ещё не готов
	                    }
	                } catch (Exception e) {
	                    e.printStackTrace();
	                } finally {
	                    latch.countDown();
	                }
	            });

	            // 2. Асинхронное подключение к VM
//	            FutureTask<VirtualMachine> futureTask = new FutureTask<>(() -> configureVirtualMachine(host, port));
//	            new Thread(futureTask).start();
	           
	            CompletableFuture<VirtualMachine> completableFutureVirtualMachine = 
	            		CompletableFuture.supplyAsync(() -> { return configureVirtualMachine(host, port);}
	            		);
	            completableFutureVirtualMachine.thenRun(latch::countDown);
	            completableFutureVirtualMachine.thenAccept(r -> vmRef.set(r));
	           
	            try {
					/*
					 * vm = futureTask.get(); // ждем бесконечно, как ты и хотел latch.countDown();
					 */

	                // Ждем обе задачи
	                latch.await();

	                // Создаем workflow
	                
	                instance = new SimpleDebuggerWorkFlow(new TargetVirtualMachineRepresentation(host, port, vmRef.get()));

	                // Вызываем callback
	                if (Objects.nonNull(listener)) {
	                    listener.onReady(instance);
	                }

	            } catch (Exception e) {
	                e.printStackTrace();
	                
	                //return instance;
	            }
	        //}).start();
				return instance;
	    }

	    private static void scheduleRetry() {
	        Display.getDefault().timerExec(1000, () -> {
	            DebugPlugin plugin = DebugPlugin.getDefault();
	            if (Objects.nonNull(plugin) && Objects.nonNull(plugin.getBreakpointManager())) {
	                registerListener();
	            } else {
	                scheduleRetry();
	            }
	        });
	    }

	    private static void registerListener() {
	        IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
	        manager.setEnabled(true);
	        BreakePointListener listener = new BreakePointListener();
	        manager.addBreakpointListener(listener);
	        System.out.println("[Factory] Breakpoint listener registered!");
	    }

	    private static VirtualMachine configureVirtualMachine(String host, int port) {
	        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
	        AttachingConnector connector = vmm.attachingConnectors().stream()
	                .filter(c -> c.name().equals("com.sun.jdi.SocketAttach")).findAny().orElseThrow();
	        Map<String, Connector.Argument> args = connector.defaultArguments();
	        args.get("hostname").setValue(host);
	        args.get("port").setValue(String.valueOf(port));

	        VirtualMachine vm = null;
	        while (true) {
	            try {
	                System.out.println("Connecting to " + host + ":" + port + "...");
	                vm = connector.attach(args);
	                System.out.println("Successfully connected to VM at " + host + ":" + port + ".");
	                return vm;
	            } catch (Exception e) {
	                try {
	                    TimeUnit.SECONDS.sleep(2);
	                } catch (InterruptedException ignored) {}
	            }
	        }
	    }
	}


}
