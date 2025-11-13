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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.e4.ui.internal.workbench.swt.handlers.ThemeUtil;
import org.eclipse.swt.widgets.Display;

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
	private TargetVirtualMachineRepresentation targetVirtualMachineRepresentation;

	
	//private Method method = null;
	// private static final Map<SimpleDebuggerWorkFlowIdentifier,
	// SimpleDebuggerWorkFlow> CACHE = new WeakHashMap<>();

	private SimpleDebuggerWorkFlow(String host, int port) throws IllegalStateException {
		targetVirtualMachineRepresentation = TargetVirtualMachineRepresentation.instance(host, port);
	}

	public static SimpleDebuggerWorkFlow instance(String host, int port) {
		if (Objects.isNull(instance))
			instance = new SimpleDebuggerWorkFlow(host, port);
		return instance;
	}
	
	public List<ReferenceType> getClassesOfTargetApplication() {
		return targetVirtualMachineRepresentation.getVirtualMachine().allClasses();
	}

	public void debug() throws IOException, AbsentInformationException {
		EventRequestManager eventRequestManager = targetVirtualMachineRepresentation.getVirtualMachine().eventRequestManager();
		Display.getDefault();
		//System.out.println("referencesAtClassesAndInterfaces.size: " + referencesAtClassesAndInterfaces.size());

//		for (TargetApplicationElementRepresentation targetApplicationElementRepresentation : referencesAtClassesAndInterfaces
//				.values()) {
//			System.out.println("==> " + targetApplicationElementRepresentation.prettyPrint());
//			if (targetApplicationElementRepresentation.getTargetApplicationElementType()
//					.equals(TargetApplicationElementType.CLASS) && Objects.isNull(method)) {
//				method = targetApplicationElementRepresentation.getMethods().stream()
//						.filter(m -> m.name().contains("sayHello")).findAny().orElse(null);
//			}
		//}
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

	

	

}
