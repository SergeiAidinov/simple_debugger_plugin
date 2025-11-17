package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import com.sun.jdi.Location;
import com.sun.jdi.request.BreakpointRequest;

public final class BreakpointRequestWrapper implements Comparable<BreakpointRequestWrapper> {

    private final BreakpointRequest request;
    private final Location location;

    public BreakpointRequestWrapper(BreakpointRequest request) {
        if (request == null) throw new IllegalArgumentException("Request is null");
        this.request = request;
        this.location = request.location();
    }

    public BreakpointRequest getRequest() {
        return request;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BreakpointRequestWrapper)) return false;
        BreakpointRequestWrapper other = (BreakpointRequestWrapper) obj;

        // Location has correct equals/hashCode in JDI
        return location.equals(other.location);
    }

    @Override
    public int hashCode() {
        return location.hashCode();
    }

    @Override
    public int compareTo(BreakpointRequestWrapper o) {
        // Сортировка сначала по FQN класса
        int c = location.declaringType().name()
                .compareTo(o.location.declaringType().name());
        if (c != 0) return c;

        // Потом по номеру строки (быстрее и понятнее)
        c = Integer.compare(location.lineNumber(), o.location.lineNumber());
        if (c != 0) return c;

        // Потом по codeIndex (на случай нескольких байт-кодов в строке)
        return Long.compare(location.codeIndex(), o.location.codeIndex());
    }

    @Override
    public String toString() {
        return location.declaringType().name() + ":" +
               location.lineNumber() + " @" +
               location.codeIndex();
    }
}
