package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.BreakpointWrapper;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;

/**
 * Представление брейкпоинтов таргет-приложения.
 *
 * • одна коллекция
 * • pending = BreakpointRequest == null
 * • корректная очистка VM
 * • поддержка ClassPrepareEvent
 */
public class TargetApplicationBreakpointRepresentation implements BreakpointSubscriber {

    private final IBreakpointManager breakpointManager;
    private final VirtualMachine virtualMachine;
    private final EventRequestManager eventRequestManager;

    /** Все брейкпоинты (active + pending) */
    private final Set<BreakpointWrapper> breakpoints =
            ConcurrentHashMap.newKeySet();

    public TargetApplicationBreakpointRepresentation(
            IBreakpointManager breakpointManager,
            VirtualMachine virtualMachine
    ) {
        this.breakpointManager = breakpointManager;
        this.virtualMachine = virtualMachine;
        this.eventRequestManager = virtualMachine.eventRequestManager();
    }

    // ======================================================================
    // BreakpointSubscriber
    // ======================================================================

    @Override
    public synchronized void addBreakepoint(IBreakpoint breakpoint) {
        if (breakpoint == null) return;

        // не добавляем дубликаты
        if (breakpoints.stream().anyMatch(bw -> bw.getBreakpoint().equals(breakpoint)))
            return;

        Optional<Location> location = findLocation(breakpoint);

        if (location.isPresent()) {
            BreakpointRequest request =
                    eventRequestManager.createBreakpointRequest(location.get());
            request.enable();
            breakpoints.add(new BreakpointWrapper(breakpoint, request));
        } else {
            // класс ещё не загружен
            breakpoints.add(new BreakpointWrapper(breakpoint, null));
        }
    }

    @Override
    public synchronized void deleteBreakepoint(IBreakpoint breakpoint) {
    	System.out.println("BEFORE DELETION: ");
    	breakpoints.stream().forEach(bw -> System.out.println(bw.prettyPrint()));
    	BreakpointWrapper breakpointWrapperToBeDeleted = null;
        for (BreakpointWrapper breakpointWrapper : breakpoints) {
        	if (breakpointWrapper.getBreakpoint().equals(breakpoint)) {
        		breakpointWrapperToBeDeleted = breakpointWrapper;
        		try {
					breakpoint.setEnabled(false);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        }
        
        if (Objects.nonNull(breakpointWrapperToBeDeleted)) breakpoints.remove(breakpointWrapperToBeDeleted);
        System.out.println("AFTER DELETION: ");
    	breakpoints.stream().forEach(bw -> System.out.println(bw.prettyPrint()));
    }

    @Override
    public synchronized void changeBreakpoint(IBreakpoint breakpoint) {
        deleteBreakepoint(breakpoint);
        addBreakepoint(breakpoint);
    }

    // ======================================================================
    // Lifecycle
    // ======================================================================

    /** Вызывается при ClassPrepareEvent */
    public synchronized void onClassPrepared(ReferenceType refType) {
        for (BreakpointWrapper bw : breakpoints) {
            if (bw.getBreakpointRequest() != null)
                continue;

            IBreakpoint eclipseBp = bw.getBreakpoint();
            String typeName = getTypeName(eclipseBp);

            if (typeName == null || !typeName.equals(refType.name()))
                continue;

            Optional<Location> location = findLocation(eclipseBp);
            if (location.isEmpty())
                continue;

            BreakpointRequest request =
                    eventRequestManager.createBreakpointRequest(location.get());
            request.enable();
            bw.setBreakpointRequest(request);
        }
    }

    /** Полная пересинхронизация с Eclipse */
    public synchronized void refreshBreakpoints() {
        // удалить ВСЕ JDI-брейкпоинты из VM
        for (BreakpointWrapper bw : breakpoints) {
            deleteJdiRequest(bw);
        }
        breakpoints.clear();

        Arrays.stream(breakpointManager.getBreakpoints())
                .forEach(this::addBreakepoint);
    }

    // ======================================================================
    // Helpers
    // ======================================================================

    private void deleteJdiRequest(BreakpointWrapper bw) {
        BreakpointRequest req = bw.getBreakpointRequest();
        if (req != null) {
            try {
                req.disable();
                eventRequestManager.deleteEventRequest(req);
            } catch (Exception ignored) {
            }
            bw.setBreakpointRequest(null);
        }
    }

    private Optional<Location> findLocation(IBreakpoint bp) {
        String className = getTypeName(bp);
        int line = getLineNumber(bp);

        if (className == null || line < 0)
            return Optional.empty();

        var classes = virtualMachine.classesByName(className);
        if (classes.isEmpty())
            return Optional.empty();

        ReferenceType refType = classes.get(0);

        try {
            return refType.locationsOfLine(line).stream().findFirst();
        } catch (AbsentInformationException e) {
            return Optional.empty();
        }
    }

    // ======================================================================
    // Marker helpers
    // ======================================================================

    private String getTypeName(IBreakpoint bp) {
        try {
            IMarker marker = bp.getMarker();
            if (marker != null) {
                return marker.getAttribute(
                        "org.eclipse.jdt.debug.core.typeName",
                        (String) null
                );
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
