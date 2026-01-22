package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IMarker;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.BreakpointWrapper;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;

/**
 * Представление брейкпоинтов таргет-приложения.
 *
 * Поддерживает: • установку брейкпоинтов ДО загрузки классов • корректную
 * обработку ClassPrepareEvent • удаление и переустановку
 */
public class TargetApplicationBreakepointRepresentation implements BreakpointSubscriber {

	private final IBreakpointManager breakpointManager;
	private final EventRequestManager eventRequestManager;
	private final VirtualMachine virtualMachine;

	/** Активные BreakpointRequest */
//    private final Set<BreakpointRequestWrapper> activeRequests =
//            ConcurrentHashMap.newKeySet();

	/** Локации для подсветки текущей строки */
	private final ConcurrentLinkedDeque<Location> hitLocations = new ConcurrentLinkedDeque<>();

	/** Брейкпоинты, ожидающие загрузки класса */
	private final Set<BreakpointWrapper> breakpoints = ConcurrentHashMap.newKeySet();

	public TargetApplicationBreakepointRepresentation(IBreakpointManager breakpointManager,
			// EventRequestManager eventRequestManager,
			VirtualMachine virtualMachine) {
		this.breakpointManager = breakpointManager;
		this.eventRequestManager = virtualMachine.eventRequestManager();
		this.virtualMachine = virtualMachine;
	}

	// ========================================================================
	// API
	// ========================================================================

	@Override
	public synchronized void addBreakepoint(IBreakpoint breakpoint) {

		Optional<Method> method = findMethodForBreakpoint(breakpoint);

		if (method.isPresent()) {
			addBreakepointWrapper(breakpoint, method.get());
		} else {
			
			// класс ещё не загружен
			breakpoints.add(new BreakpointWrapper(breakpoint, null));
		}
	}

	@Override
	public synchronized void deleteBreakepoint(IBreakpoint breakpoint) {
		for (BreakpointWrapper breakpointWrapper : breakpoints) {
			if (breakpoint.equals(breakpointWrapper.get())) {
				breakpoints.remove(breakpointWrapper);
				System.out.println("REMOVED FROM SET: " + breakpointWrapper);
			}
		}

	}

	@Override
	public void changeBreakpoint(IBreakpoint breakpoint) {
	}

	/**
	 * Вызывается при получении ClassPrepareEvent
	 */
	public synchronized void onClassPrepared(ReferenceType refType) {
		
		System.out.println("onClassPrepared(ReferenceType refType): ClassPrepareEvent");
//
//		String loadedClassName = refType.name();
//
//		Set<BreakpointWrapper> toActivate = breakpoints.stream()
//				.filter(bp -> loadedClassName.equals(getTypeName(bp.get()))).collect(Collectors.toSet());
//
//		for (BreakpointWrapper bp : toActivate) {
//			findMethodForBreakpoint(bp.get()).ifPresent(m -> createBreakpointRequest(bp, m));
//			breakpoints.remove(bp);
//		}
	}

	/**
	 * Перечитать все брейкпоинты Eclipse (при старте)
	 */
	public synchronized void refreshBreakpoints() {
		// activeRequests.clear();
		breakpoints.clear();
		Arrays.stream(breakpointManager.getBreakpoints()).forEach(this::addBreakepoint);
	}

	// ========================================================================
	// Internal helpers
	// ========================================================================
	public String prettyPrintBreakpoints() {
		StringBuilder sb = new StringBuilder();

		sb.append("Breakpoints:\n");
//
//        if (activeRequests.isEmpty() && pendingBreakpoints.isEmpty()) {
//            sb.append("  <none>\n");
//            return sb.toString();
//        }

		// --------- Active ---------
//        if (!activeRequests.isEmpty()) {
//            sb.append("  Active:\n");
//            for (BreakpointRequestWrapper wrapper : activeRequests) {
//                IBreakpoint breakpoint = wrapper.getBreakpointWrapper().get();
//                sb.append("    ")
//                  .append(formatBreakpoint(breakpoint))
//                  .append("\n");
//            }
//        }

		// --------- Pending ---------
		if (!breakpoints.isEmpty()) {
			sb.append("  Pending (class not loaded):\n");
			for (BreakpointWrapper wrapper : breakpoints) {
				sb.append("    ").append(formatBreakpoint(wrapper.get())).append("\n");
			}
		}

		return sb.toString();
	}

	private String formatBreakpoint(IBreakpoint breakpoint) {
		if (breakpoint == null) {
			return "<invalid breakpoint>";
		}

		String typeName = getTypeName(breakpoint);
		int lineNumber = getLineNumber(breakpoint);

		if (typeName == null) {
			return "<unknown location>";
		}

		return typeName + ":" + lineNumber;
	}

	private void addBreakepointWrapper(IBreakpoint breakpoint, Method method) {
		int line = getLineNumber(breakpoint);

		Optional<Location> location = findLocation(method, line);

		if (location.isEmpty())
			return;
		BreakpointRequest breakpointRequest = eventRequestManager.createBreakpointRequest(location.get());
		breakpointRequest.enable();
		hitLocations.offer(location.get());
		breakpoints.add(new BreakpointWrapper(breakpoint, breakpointRequest));
	}

	private Optional<Method> findMethodForBreakpoint(IBreakpoint bp) {

		String className = getTypeName(bp);
		int lineNumber = getLineNumber(bp);

		if (className == null || lineNumber < 0)
			return Optional.empty();

		var classes = virtualMachine.classesByName(className);
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
		} catch (AbsentInformationException ignored) {
		}

		return Optional.empty();
	}

	private Optional<Location> findLocation(Method method, int line) {
		try {
			return method.allLineLocations().stream().filter(l -> l.lineNumber() == line).findFirst();
		} catch (AbsentInformationException e) {
			return Optional.empty();
		}
	}

	// ========================================================================
	// Marker helpers
	// ========================================================================

	private String getTypeName(IBreakpoint bp) {
		try {
			IMarker marker = bp.getMarker();
			if (marker != null) {
				return marker.getAttribute("org.eclipse.jdt.debug.core.typeName", (String) null);
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private int getLineNumber(IBreakpoint bp) {
		try {
			IMarker marker = bp.getMarker();
			if (marker != null) {
				return marker.getAttribute(IMarker.LINE_NUMBER, -1);
			}
		} catch (Exception ignored) {
		}
		return -1;
	}
}
