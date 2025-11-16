package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArraySet;

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

public class TargetApplicationBreakepointRepresentation {

    private final IBreakpointManager iBreakpointManager;
    private final EventRequestManager eventRequestManager;
    private final VirtualMachine virtualMachine;

    public TargetApplicationBreakepointRepresentation(IBreakpointManager iBreakpointManager,
                                                      EventRequestManager eventRequestManager,
                                                      VirtualMachine virtualMachine) {
        this.iBreakpointManager = iBreakpointManager;
        this.eventRequestManager = eventRequestManager;
        this.virtualMachine = virtualMachine;
    }

    // -------------------
    // Основные коллекции
    private final Set<BreakpointWrapper> breakpoints = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedDeque<BreakpointRequest> breakpointRequests = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Location> locations = new ConcurrentLinkedDeque<>();

    // -------------------
    // Слушатели
    private final Set<BreakpointHitListener> listeners = new CopyOnWriteArraySet<>();

    public void addListener(BreakpointHitListener breakpointListener) {
        listeners.add(breakpointListener);
    }

    public void removeListener(BreakpointHitListener listener) {
        listeners.remove(listener);
    }

    // -------------------
    // Геттеры коллекций
    public ConcurrentLinkedDeque<BreakpointRequest> getBreakpointRequests() {
        return breakpointRequests;
    }

    public ConcurrentLinkedDeque<Location> getLocations() {
        return locations;
    }

    public Set<BreakpointWrapper> getBreakpoints() {
        return new HashSet<>(breakpoints);
    }

    // -------------------
    // Добавление брейкпойнта
    public synchronized boolean addBreakepoint(BreakpointWrapper breakpointWrapper) {
        Optional<Method> methodOptional = getMethodForBreakpoint(breakpointWrapper.get(), virtualMachine);
        if (methodOptional.isPresent()) {
            int line = getLineNumber(breakpointWrapper.get());
            Optional<Location> location = findLocation(methodOptional.get(), line);
            if (location.isPresent()) {
                BreakpointRequest breakpointRequest = eventRequestManager.createBreakpointRequest(location.get());
                breakpointRequest.enable();

                breakpointRequests.offer(breakpointRequest);
                locations.offer(location.get());
                boolean added = breakpoints.add(breakpointWrapper);

                // -------------------
                // Уведомляем всех слушателей
                for (BreakpointHitListener listener : listeners) {
                    listener.onBreakpointHit(breakpointRequest);
                }

                return added;
            }
        }
        return false;
    }

    // -------------------
    // Обновление списка брейкпойнтов из Eclipse
    public synchronized void refreshBreakePoints() {
        breakpoints.clear();
        Arrays.stream(iBreakpointManager.getBreakpoints())
                .forEach(bp -> addBreakepoint(new BreakpointWrapper(bp)));
        breakpoints.forEach(bpw -> System.out.println("===> " + bpw.get()));
    }

    // -------------------
    private Optional<Location> findLocation(Method method, int sourceLine) {
        try {
            for (Location l : method.allLineLocations()) {
                if (l.lineNumber() == sourceLine) {
                    return Optional.of(l);
                }
            }
        } catch (AbsentInformationException e) {
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
