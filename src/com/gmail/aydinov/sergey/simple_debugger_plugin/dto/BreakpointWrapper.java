package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import org.eclipse.core.resources.IMarker;
import org.eclipse.debug.core.model.IBreakpoint;

public class BreakpointWrapper implements Comparable<BreakpointWrapper> {

    private final IBreakpoint bp;
    private final String path;
    private final int line;

    public BreakpointWrapper(IBreakpoint bp) {
        this.bp = bp;
        this.path = bp.getMarker().getResource().getFullPath().toString();
        this.line = bp.getMarker().getAttribute(IMarker.LINE_NUMBER, -1);
    }

    public IBreakpoint get() {
        return bp;
    }

    @Override
    public int compareTo(BreakpointWrapper o) {
        int c = path.compareTo(o.path);
        if (c != 0) return c;
        return Integer.compare(line, o.line);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BreakpointWrapper)) return false;
        BreakpointWrapper o = (BreakpointWrapper) obj;
        return path.equals(o.path) && line == o.line;
    }

    @Override
    public int hashCode() {
        return 31 * path.hashCode() + line;
    }
}

