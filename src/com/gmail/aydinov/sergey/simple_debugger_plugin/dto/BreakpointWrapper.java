package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import org.eclipse.core.resources.IMarker;
import org.eclipse.debug.core.model.IBreakpoint;

import java.util.Optional;

public class BreakpointWrapper implements Comparable<BreakpointWrapper> {

    private final IBreakpoint bp;
    private final String path;
    private final int line;

    public BreakpointWrapper(IBreakpoint bp) {
        this.bp = bp;
        this.path = Optional.ofNullable(bp)
                .map(IBreakpoint::getMarker)
                .map(IMarker::getResource)
                .map(r -> r.getFullPath().toString())
                .orElse("");
        this.line = Optional.ofNullable(bp)
                .map(IBreakpoint::getMarker)
                .map(marker -> marker.getAttribute(IMarker.LINE_NUMBER, -1))
                .orElse(-1);
    }

    public Optional<IBreakpoint> get() {
        return Optional.ofNullable(bp);
    }

    @Override
    public int compareTo(BreakpointWrapper o) {
        if (o == null) return 1;
        int cmp = path.compareTo(o.path);
        return cmp != 0 ? cmp : Integer.compare(line, o.line);
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
