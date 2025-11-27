package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.eclipse.core.resources.IMarker;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.BreakpointRequestWrapper;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.BreakpointWrapper;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;

public class TargetApplicationBreakepointRepresentation implements BreakpointSubscriber  {

	private final IBreakpointManager iBreakpointManager;
	private final EventRequestManager eventRequestManager;
	private final VirtualMachine virtualMachine;

	public TargetApplicationBreakepointRepresentation(IBreakpointManager iBreakpointManager,
			EventRequestManager eventRequestManager, VirtualMachine virtualMachine) {
		this.iBreakpointManager = iBreakpointManager;
		this.eventRequestManager = eventRequestManager;
		this.virtualMachine = virtualMachine;
	}

	private final Set<BreakpointRequestWrapper> breakpointRequestWrappers = new ConcurrentHashMap().newKeySet();
	private final ConcurrentLinkedDeque<Location> locations = new ConcurrentLinkedDeque<>();


	public Set<BreakpointRequestWrapper> getBreakpointRequests() {
		return breakpointRequestWrappers;
	}
	
	

	public ConcurrentLinkedDeque<Location> getLocations() {
		return locations;
	}

	@Override
	public synchronized void addBreakepoint(BreakpointWrapper breakpointWrapper) {
		Optional<Method> methodOptional = getMethodForBreakpoint(breakpointWrapper.get(), virtualMachine);
		if (methodOptional.isPresent()) {
			int line = getLineNumber(breakpointWrapper.get());
			Optional<Location> location = findLocation(methodOptional.get(), line);
			if (location.isPresent()) {
				BreakpointRequest breakpointRequest = eventRequestManager.createBreakpointRequest(location.get());
				breakpointRequest.enable();
				//breakpointWrappers.add(breakpointWrapper);
				locations.offer(location.get());
				breakpointRequestWrappers.add(new BreakpointRequestWrapper(breakpointRequest, breakpointWrapper));
			}
		}
	}

	@Override
	public void deleteBreakepoint(BreakpointWrapper breakpointWrapper) {
		Optional<BreakpointRequestWrapper> wrapperOpt = breakpointRequestWrappers.stream()
	            .filter(w -> w.getBreakpointWrapper().equals(breakpointWrapper))
	            .findFirst();

	    wrapperOpt.ifPresent(wrapper -> {
	        BreakpointRequest request = wrapper.getRequest();
	        request.disable();
	        eventRequestManager.deleteEventRequest(request);

	        breakpointRequestWrappers.remove(wrapper);
	        //breakpointWrappers.remove(breakpointWrapper);
	        locations.remove(request.location());
	    });
	}

	@Override
	public void changeBreakpoint(BreakpointWrapper breakpointWrapper) {

	}

	public Set<BreakpointWrapper> getBreakpoints() {
		Set<BreakpointWrapper> breakpointWrappers = new HashSet<BreakpointWrapper>();
		breakpointWrappers.addAll(breakpointWrappers);
		return breakpointWrappers;
	}

	public synchronized void refreshBreakePoints() {
		breakpointRequestWrappers.clear();
		Arrays.asList(iBreakpointManager.getBreakpoints()).stream()
				.forEach(bp -> addBreakepoint(new BreakpointWrapper(bp)));
		breakpointRequestWrappers.stream().forEach(bpw -> System.out.println("===> " + bpw.getBreakpointRequest()));
	}

	private Optional<Location> findLocation(Method method, int sourceLine) {
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

	public String getTypeName(IBreakpoint bp) {
		try {
			IMarker marker = bp.getMarker();
			if (marker != null) {
				return marker.getAttribute("org.eclipse.jdt.debug.core.typeName", (String) null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public int getLineNumber(IBreakpoint bp) {
		try {
			IMarker marker = bp.getMarker();
			if (marker != null) {
				return marker.getAttribute(IMarker.LINE_NUMBER, -1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	public Optional<Method> getMethodForBreakpoint(IBreakpoint bp, VirtualMachine vm) {
		String className = getTypeName(bp);
		int lineNumber = getLineNumber(bp);

		if (className == null || lineNumber < 0)
			return Optional.empty();

		List<ReferenceType> classes = vm.classesByName(className);
		if (classes.isEmpty())
			return Optional.empty();
		ReferenceType refType = classes.get(0);

		try {
			for (Method m : refType.allMethods()) {
				for (Location loc : m.allLineLocations()) {
					if (loc.lineNumber() == lineNumber) {
						return Optional.of(m);
					}
				}
			}
		} catch (AbsentInformationException e) {
			e.printStackTrace();
		}
		return Optional.empty();
	}

}