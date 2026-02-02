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

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.BreakpointWrapper;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;

/**
 * Representation of breakpoints in the target application.
 *
 * • single collection
 * • pending = BreakpointRequest == null
 * • proper VM cleanup
 * • ClassPrepareEvent support
 */
public class TargetApplicationBreakpointRepresentation implements BreakpointSubscriber {

    private final IBreakpointManager breakpointManager;
    private final VirtualMachine virtualMachine;
    private final EventRequestManager eventRequestManager;

    /** All breakpoints (active + pending) */
    private final Set<BreakpointWrapper> breakpoints = ConcurrentHashMap.newKeySet();

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
    public synchronized void addBreakepoint(IBreakpoint iBreakpoint) {
        if (Objects.isNull(iBreakpoint)) {
            return;
        }

        if (breakpoints.stream()
                .anyMatch(b -> b.getBreakpoint().equals(iBreakpoint))) {
            return;
        }

        Optional<Location> locationOptional = findLocation(iBreakpoint);

        if (locationOptional.isPresent()) {
            BreakpointRequest breakpointRequest = eventRequestManager.createBreakpointRequest(locationOptional.get());
            breakpointRequest.enable();
            breakpoints.add(new BreakpointWrapper(iBreakpoint, breakpointRequest));
        } else {
            // Class is not loaded yet
            breakpoints.add(new BreakpointWrapper(iBreakpoint, null));
        }
    }

    @Override
    public synchronized void deleteBreakepoint(IBreakpoint iBreakpoint) {
        BreakpointWrapper breakpointWrapperToBeDeleted = null;

        for (BreakpointWrapper breakpointWrapper : breakpoints) {
            if (breakpointWrapper.getBreakpoint().equals(iBreakpoint)) {
                breakpointWrapperToBeDeleted = breakpointWrapper;

                try {
                    iBreakpoint.setEnabled(false);
                } catch (CoreException e) {
                    e.printStackTrace();
                }

                for (BreakpointRequest breakpointRequest : eventRequestManager.breakpointRequests()) {
                    if (breakpointRequest.equals(breakpointWrapper.getBreakpointRequest())) {
                        eventRequestManager.deleteEventRequest(breakpointRequest);
                    }
                }
            }
        }

        if (Objects.nonNull(breakpointWrapperToBeDeleted)) {
            breakpoints.remove(breakpointWrapperToBeDeleted);
        }
    }

    @Override
    public synchronized void changeBreakpoint(IBreakpoint iBreakpoint) {
        deleteBreakepoint(iBreakpoint);
        addBreakepoint(iBreakpoint);
    }

    // ======================================================================
    // Lifecycle
    // ======================================================================

    /** Called on ClassPrepareEvent */
    public synchronized void onClassPrepared(ReferenceType referenceType) {
        for (BreakpointWrapper breakpointWrapper : breakpoints) {
            if (Objects.nonNull(breakpointWrapper.getBreakpointRequest())) {
                continue;
            }

            IBreakpoint iBreakpoint = breakpointWrapper.getBreakpoint();
            String typeName = getTypeName(iBreakpoint);

            if (Objects.isNull(typeName) || !typeName.equals(referenceType.name())) {
                continue;
            }

            Optional<Location> location = findLocation(iBreakpoint);
            if (location.isEmpty()) continue;

            BreakpointRequest breakpointRequest = eventRequestManager.createBreakpointRequest(location.get());
            breakpointRequest.enable();
            breakpointWrapper.setBreakpointRequest(breakpointRequest);
        }
    }

    /** Full resynchronization with Eclipse */
    public synchronized void refreshBreakpoints() {
        // Remove all JDI breakpoints from VM
        for (BreakpointWrapper breakpointWrapper : breakpoints) {
            deleteJdiRequest(breakpointWrapper);
        }
        breakpoints.clear();

        Arrays.stream(breakpointManager.getBreakpoints())
                .forEach(this::addBreakepoint);
    }

    // ======================================================================
    // Helpers
    // ======================================================================

    private void deleteJdiRequest(BreakpointWrapper breakpointWrapper) {
        BreakpointRequest breakpointRequest = breakpointWrapper.getBreakpointRequest();

        if (Objects.nonNull(breakpointRequest)) {
            try {
                breakpointRequest.disable();
                eventRequestManager.deleteEventRequest(breakpointRequest);
            } catch (Exception ignored) {
            }
            breakpointWrapper.setBreakpointRequest(null);
        }
    }

    private Optional<Location> findLocation(IBreakpoint iBreakpoint) {
        String className = getTypeName(iBreakpoint);
        int line = getLineNumber(iBreakpoint);

        if (Objects.isNull(className) || line < 0) {
            return Optional.empty();
        }

        List<ReferenceType> classes = virtualMachine.classesByName(className);
        if (classes.isEmpty()) return Optional.empty();

        ReferenceType referenceType = classes.get(0);

        try {
            return referenceType.locationsOfLine(line).stream().findFirst();
        } catch (AbsentInformationException e) {
            return Optional.empty();
        }
    }

    // ======================================================================
    // Marker helpers
    // ======================================================================

    private String getTypeName(IBreakpoint iBreakpoint) {
        try {
            IMarker marker = iBreakpoint.getMarker();
            if (Objects.nonNull(marker)) {
                return marker.getAttribute("org.eclipse.jdt.debug.core.typeName", (String) null);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private int getLineNumber(IBreakpoint iBreakpoint) {
        try {
            IMarker marker = iBreakpoint.getMarker();
            if (Objects.nonNull(marker)) {
                return marker.getAttribute(IMarker.LINE_NUMBER, -1);
            }
        } catch (Exception ignored) {
        }
        return -1;
    }
}
