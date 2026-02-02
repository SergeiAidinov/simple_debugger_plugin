package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import org.eclipse.core.resources.IMarker;
import org.eclipse.debug.core.model.IBreakpoint;

import com.sun.jdi.request.BreakpointRequest;

/**
 * Wrapper for Eclipse IBreakpoint and JDI BreakpointRequest, with associated
 * path and line information.
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
		IMarker marker = breakpoint != null ? breakpoint.getMarker() : null;
		this.path = (marker != null && marker.getResource() != null) ? marker.getResource().getFullPath().toString()
				: "";
		this.line = (marker != null) ? marker.getAttribute(IMarker.LINE_NUMBER, -1) : -1;
		this.breakpointRequest = breakpointRequest;
	}

	/**
	 * Returns the Eclipse IBreakpoint.
	 */
	public IBreakpoint getBreakpoint() {
		return breakpoint;
	}

	/**
	 * Returns the JDI BreakpointRequest.
	 */
	public BreakpointRequest getBreakpointRequest() {
		return breakpointRequest;
	}

	/**
	 * Sets the JDI BreakpointRequest.
	 */
	public void setBreakpointRequest(BreakpointRequest breakpointRequest) {
		this.breakpointRequest = breakpointRequest;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof BreakpointWrapper other))
			return false;
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

	/**
	 * Returns a pretty-printed multi-line string for debugging.
	 */
	public String prettyPrint() {
		StringBuilder sb = new StringBuilder();
		sb.append("BreakpointWrapper {\n");
		sb.append("  path: ").append(path.isEmpty() ? "<unknown>" : path).append("\n");
		sb.append("  line: ").append(line >= 0 ? line : "<unknown>").append("\n");
		sb.append("  breakpoint: ");
		sb.append(breakpoint != null ? breakpoint.getClass().getSimpleName() : "<null>").append("\n");
		sb.append("  breakpointRequest: ");
		sb.append(breakpointRequest != null ? breakpointRequest.getClass().getSimpleName() : "<null>").append("\n");
		sb.append("}");
		return sb.toString();
	}
}
