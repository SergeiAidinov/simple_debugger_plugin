package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import org.eclipse.core.resources.IMarker;
import org.eclipse.debug.core.model.IBreakpoint;

public class BreakpointWrapper implements Comparable<BreakpointWrapper> {

    private final IBreakpoint bp;
    private final String path;
    private final int line;

    public BreakpointWrapper(IBreakpoint bp) {
        this.bp = bp;
        IMarker marker = bp != null ? bp.getMarker() : null;
        this.path = (marker != null && marker.getResource() != null)
                        ? marker.getResource().getFullPath().toString()
                        : "";
        this.line = (marker != null) ? marker.getAttribute(IMarker.LINE_NUMBER, -1) : -1;
    }

    public IBreakpoint get() {
        return bp;
    }

    @Override
    public int compareTo(BreakpointWrapper o) {
        if (o == null) return 1;
        int cmp = path.compareTo(o.path);
        if (cmp != 0) return cmp;
        return Integer.compare(line, o.line);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BreakpointWrapper)) return false;
        BreakpointWrapper other = (BreakpointWrapper) obj;
        return line == other.line && path.equals(other.path);
    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + line;
        return result;
    }

    @Override
    public String toString() {
        return "Breakpoint at " + path + ":" + line;
    }
}
