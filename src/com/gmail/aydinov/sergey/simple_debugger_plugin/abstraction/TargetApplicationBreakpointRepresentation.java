package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;

import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;

/**
 * Representation of breakpoints in the target application.
 *
 * • single collection • pending = BreakpointRequest == null • proper VM cleanup
 * • ClassPrepareEvent support
 * <p>
 * Author: Sergei Aidinov <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class TargetApplicationBreakpointRepresentation implements BreakpointSubscriber {

	private static TargetApplicationBreakpointRepresentation INSTANCE;

	private final IBreakpointManager breakpointManager;
	private final Set<BreakpointWrapper> breakpoints = ConcurrentHashMap.newKeySet();

	private TargetApplicationBreakpointRepresentation(IBreakpointManager breakpointManager) {
		this.breakpointManager = Objects.requireNonNull(breakpointManager, "IBreakpointManager не может быть null");
	}

	/** Создаём синглтон с IBreakpointManager */
	public static synchronized TargetApplicationBreakpointRepresentation getInstanceFor(
			IBreakpointManager breakpointManager) {
		if (INSTANCE != null) {
			throw new IllegalStateException("TargetApplicationBreakpointRepresentation уже создан");
		}
		INSTANCE = new TargetApplicationBreakpointRepresentation(breakpointManager);
		return INSTANCE;
	}

	/** Получаем уже созданный синглтон */
	public static TargetApplicationBreakpointRepresentation getInstance() {
		if (INSTANCE == null) {
			throw new IllegalStateException("TargetApplicationBreakpointRepresentation ещё не создан");
		}
		return INSTANCE;
	}

	// ======================================================================
	// BreakpointSubscriber
	// ======================================================================

	@Override
	public synchronized void addBreakepoint(IBreakpoint iBreakpoint) {
		if (Objects.isNull(iBreakpoint))
			return;
		if (breakpoints.stream().anyMatch(b -> b.getBreakpoint().equals(iBreakpoint)))
			return;

		Optional<Location> locationOptional = findLocation(iBreakpoint);

		if (locationOptional.isPresent()) {
			BreakpointRequest breakpointRequest = TargetVirtualMachineRepresentation.getInstance().getVirtualMachine()
					.eventRequestManager().createBreakpointRequest(locationOptional.get());
			breakpointRequest.enable();
			breakpoints.add(new BreakpointWrapper(iBreakpoint, breakpointRequest));
		} else {
			// Class is not loaded yet
			breakpoints.add(new BreakpointWrapper(iBreakpoint, null));
		}
	}

	@Override
	public synchronized void deleteBreakepoint(IBreakpoint iBreakpoint) {
		BreakpointWrapper toDelete = null;

		for (BreakpointWrapper wrapper : breakpoints) {
			if (wrapper.getBreakpoint().equals(iBreakpoint)) {
				toDelete = wrapper;

				try {
					iBreakpoint.setEnabled(false);
				} catch (CoreException e) {
					e.printStackTrace();
				}

				EventRequestManager erm = TargetVirtualMachineRepresentation.getInstance().getVirtualMachine()
						.eventRequestManager();

				for (BreakpointRequest br : erm.breakpointRequests()) {
					if (br.equals(wrapper.getBreakpointRequest())) {
						erm.deleteEventRequest(br);
					}
				}
			}
		}

		if (toDelete != null)
			breakpoints.remove(toDelete);
	}

	@Override
	public synchronized void changeBreakpoint(IBreakpoint iBreakpoint) {
		deleteBreakepoint(iBreakpoint);
		addBreakepoint(iBreakpoint);
	}

	// ======================================================================
	// Lifecycle
	// ======================================================================

	public synchronized void onClassPrepared(ReferenceType referenceType) {
		for (BreakpointWrapper wrapper : breakpoints) {
			if (wrapper.getBreakpointRequest() != null)
				continue;

			IBreakpoint iBreakpoint = wrapper.getBreakpoint();
			String typeName = getTypeName(iBreakpoint);
			if (!Objects.equals(typeName, referenceType.name()))
				continue;

			Optional<Location> locationOptional = findLocation(iBreakpoint);
			if (locationOptional.isEmpty())
				continue;

			BreakpointRequest br = TargetVirtualMachineRepresentation.getInstance().getVirtualMachine()
					.eventRequestManager().createBreakpointRequest(locationOptional.get());
			br.enable();
			wrapper.setBreakpointRequest(br);
		}
	}

	public synchronized void refreshBreakpoints() {
		for (BreakpointWrapper wrapper : breakpoints) {
			deleteJdiRequest(wrapper);
		}
		breakpoints.clear();

		Arrays.stream(breakpointManager.getBreakpoints()).forEach(this::addBreakepoint);
	}

	// ======================================================================
	// Helpers
	// ======================================================================

	private void deleteJdiRequest(BreakpointWrapper wrapper) {
		BreakpointRequest br = wrapper.getBreakpointRequest();
		if (br != null) {
			try {
				br.disable();
				TargetVirtualMachineRepresentation.getInstance().getVirtualMachine().eventRequestManager()
						.deleteEventRequest(br);
			} catch (Exception ignored) {
			}
			wrapper.setBreakpointRequest(null);
		}
	}

	private Optional<Location> findLocation(IBreakpoint iBreakpoint) {
		String className = getTypeName(iBreakpoint);
		int line = getLineNumber(iBreakpoint);

		if (className == null || line < 0)
			return Optional.empty();

		List<ReferenceType> classes = TargetVirtualMachineRepresentation.getInstance().getVirtualMachine()
				.classesByName(className);
		if (classes.isEmpty())
			return Optional.empty();

		ReferenceType referenceType = classes.get(0);
		try {
			return referenceType.locationsOfLine(line).stream().findFirst();
		} catch (AbsentInformationException e) {
			return Optional.empty();
		}
	}

	// Marker helpers
	private String getTypeName(IBreakpoint iBreakpoint) {
		try {
			IMarker marker = iBreakpoint.getMarker();
			if (marker != null)
				return marker.getAttribute("org.eclipse.jdt.debug.core.typeName", (String) null);
		} catch (Exception ignored) {
		}
		return null;
	}

	private int getLineNumber(IBreakpoint iBreakpoint) {
		try {
			IMarker marker = iBreakpoint.getMarker();
			if (marker != null)
				return marker.getAttribute(IMarker.LINE_NUMBER, -1);
		} catch (Exception ignored) {
		}
		return -1;
	}

}