package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.Objects;

import org.eclipse.core.resources.IMarker;
import org.eclipse.debug.core.model.IBreakpoint;

import com.sun.jdi.request.BreakpointRequest;

/**
 * Wrapper for Eclipse IBreakpoint and JDI BreakpointRequest, with associated
 * path and line information.
 * <p>
 * Author: Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class BreakpointWrapper {

    private BreakpointRequest breakpointRequest;
    private final IBreakpoint breakpoint;
    private final String path;
    private final int line;

    /**
     * Constructs a BreakpointWrapper.
     *
     * @param breakpoint        Eclipse IBreakpoint
     * @param breakpointRequest JDI BreakpointRequest
     */
    public BreakpointWrapper(IBreakpoint breakpoint, BreakpointRequest breakpointRequest) {
        this.breakpoint = breakpoint;

        IMarker marker = Objects.nonNull(breakpoint) ? breakpoint.getMarker() : null;
        this.path = (Objects.nonNull(marker) && Objects.nonNull(marker.getResource()))
                ? marker.getResource().getFullPath().toString()
                : "";
        this.line = Objects.nonNull(marker) ? marker.getAttribute(IMarker.LINE_NUMBER, -1) : -1;

        this.breakpointRequest = breakpointRequest;
    }

    public IBreakpoint getBreakpoint() {
        return breakpoint;
    }

    public BreakpointRequest getBreakpointRequest() {
        return breakpointRequest;
    }

    public void setBreakpointRequest(BreakpointRequest breakpointRequest) {
        this.breakpointRequest = breakpointRequest;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BreakpointWrapper other)) return false;
        return line == other.line && Objects.equals(path, other.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, line);
    }

    @Override
    public String toString() {
        return "Breakpoint at " + path + ":" + line;
    }

    /**
     * Returns a pretty-printed multi-line string for debugging.
     */
    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        sb.append("BreakpointWrapper {\n");
        sb.append("  path: ").append(path.isEmpty() ? "<unknown>" : path).append("\n");
        sb.append("  line: ").append(line >= 0 ? line : "<unknown>").append("\n");
        sb.append("  breakpoint: ")
          .append(Objects.nonNull(breakpoint) ? breakpoint.getClass().getSimpleName() : "<null>")
          .append("\n");
        sb.append("  breakpointRequest: ")
          .append(Objects.nonNull(breakpointRequest) ? breakpointRequest.getClass().getSimpleName() : "<null>")
          .append("\n");
        sb.append("}");
        return sb.toString();
    }
}
