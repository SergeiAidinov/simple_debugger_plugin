package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

import org.eclipse.e4.ui.internal.workbench.swt.handlers.ThemeUtil;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InterfaceType;
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
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationClassOrInterfaceRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.exception.VirtualMachineConfigurationException;

public class SimpleDebuggerWorkFlow {

	private static SimpleDebuggerWorkFlow instance = null;

	private VirtualMachine virtualMachine = null;
	private final Map<ReferenceType, TargetApplicationElementRepresentation> referencesAtClassesAndInterfaces = new HashMap<>();
	private String host;
	private Integer port;
	private Method method = null;
	// private static final Map<SimpleDebuggerWorkFlowIdentifier,
	// SimpleDebuggerWorkFlow> CACHE = new WeakHashMap<>();

	private SimpleDebuggerWorkFlow(String host, int port) throws IllegalStateException {
		this.host = host;
		this.port = port;
	}

	public static SimpleDebuggerWorkFlow instance(String host, int port) {
		if (Objects.isNull(instance))
			instance = new SimpleDebuggerWorkFlow(host, port);
		return instance;
	}

	public void debug() throws IOException, AbsentInformationException {
		EventRequestManager eventRequestManager = virtualMachine.eventRequestManager();
		System.out.println("referencesAtClassesAndInterfaces.size: " + referencesAtClassesAndInterfaces.size());

		for (TargetApplicationElementRepresentation targetApplicationElementRepresentation : referencesAtClassesAndInterfaces
				.values()) {
			System.out.println("==> " + targetApplicationElementRepresentation.prettyPrint());
			if (targetApplicationElementRepresentation.getTargetApplicationElementType()
					.equals(TargetApplicationElementType.CLASS) && Objects.isNull(method)) {
				method = targetApplicationElementRepresentation.getMethods().stream()
						.filter(m -> m.name().contains("sayHello")).findAny().orElse(null);
			}
		}
		/*
		 * Location location = method.location(); BreakpointRequest bpReq =
		 * eventRequestManager.createBreakpointRequest(location); bpReq.enable();
		 */
		Optional<Location> loc = findLocation(method, 29);
		loc.ifPresent(l -> {
			BreakpointRequest bp = eventRequestManager.createBreakpointRequest(l);
			bp.enable();
		});

		EventQueue queue = virtualMachine.eventQueue();
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
					virtualMachine.resume();
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

	public List<? extends TargetApplicationElementRepresentation> getTargetApplicationStatus() {
		return referencesAtClassesAndInterfaces.values().stream().collect(Collectors.toList());
	}

	private void createReferencesToClassesOfTargetApplication() {
		System.out.println("Target class not loaded yet. Waiting...");
		List<ReferenceType> loadedClassesAndInterfaces = new ArrayList<ReferenceType>();
		while (loadedClassesAndInterfaces.isEmpty()) {
			loadedClassesAndInterfaces.addAll(virtualMachine.allClasses());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				continue;
			}
		}
		loadedClassesAndInterfaces = loadedClassesAndInterfaces.stream().filter(lci -> lci.name().contains("target"))
				.toList();
		System.out.println("Loaded " + loadedClassesAndInterfaces.size() + " classes.");
		Set<ReferenceType> references = loadedClassesAndInterfaces.stream().filter(clr -> Objects.nonNull(clr))
				.map(clr -> clr.classLoader()).filter(clr -> Objects.nonNull(clr))
				.flatMap(rt -> rt.definedClasses().stream()).collect(Collectors.toSet());
		Optional<TargetApplicationElementType> targetApplicationElementTypeOptional;
		for (ReferenceType referenceType : references) {
			targetApplicationElementTypeOptional = Optional.empty();
			if (referenceType instanceof ClassType) {
				targetApplicationElementTypeOptional = Optional.of(TargetApplicationElementType.CLASS);
			} else if (referenceType instanceof InterfaceType) {
				targetApplicationElementTypeOptional = Optional.of(TargetApplicationElementType.INTERFACE);
			}
			targetApplicationElementTypeOptional.ifPresent(type -> referencesAtClassesAndInterfaces.put(referenceType,
					new TargetApplicationClassOrInterfaceRepresentation(referenceType.name(), type,
							referenceType.allMethods().stream().collect(Collectors.toSet()),
							referenceType.allFields().stream().collect(Collectors.toSet()))));
		}
		System.out.println("referencesAtClasses: " + referencesAtClassesAndInterfaces.size());
	}

	public boolean configureVirtualMachine() {
		try {
			this.virtualMachine = doConfigureVirtualMachine();
		} catch (VirtualMachineConfigurationException virtualMachineConfigurationException) {
			try {
				Thread.currentThread().sleep(2000);
				configureVirtualMachine();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Connected to VM: " + virtualMachine.name());
		return true;
	}

	private VirtualMachine doConfigureVirtualMachine() {
		FutureTask<VirtualMachine> futureTaskVirtualMachine = null;
		try {
			futureTaskVirtualMachine = new FutureTask<VirtualMachine>(() -> {
				VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
				AttachingConnector connector = virtualMachineManager.attachingConnectors().stream()
						.filter(c -> c.name().equals("com.sun.jdi.SocketAttach")).findAny().orElseThrow();
				Map<String, Connector.Argument> arguments = connector.defaultArguments();
				arguments.get("hostname").setValue(host);
				arguments.get("port").setValue(String.valueOf(port));
				System.out.println("Connecting to " + host + ":" + port + "...");
				VirtualMachine virtualMachine = null;
				virtualMachine = connector.attach(arguments);
				return virtualMachine;
			});
		} catch (Exception e) {
			throw new VirtualMachineConfigurationException();
		}
		new Thread(futureTaskVirtualMachine).start();
		try {
			if (Objects.nonNull(futureTaskVirtualMachine.get())) {
				return futureTaskVirtualMachine.get();
			} else
				throw new VirtualMachineConfigurationException();
		} catch (Exception e) {
			throw new VirtualMachineConfigurationException();
		}
	}

	@Override
	public String toString() {
		return "SimpleDebuggerWorkFlow [virtualMachine=" + virtualMachine + ", referencesAtClasses="
				+ referencesAtClassesAndInterfaces + ", host=" + host + ", port=" + port + "]";
	}
}
