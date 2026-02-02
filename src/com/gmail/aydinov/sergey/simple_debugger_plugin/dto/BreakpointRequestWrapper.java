package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import com.sun.jdi.request.BreakpointRequest;

/**
 * Wrapper class for a JDI BreakpointRequest and its corresponding BreakpointWrapper.
 */
public class BreakpointRequestWrapper {

    private final BreakpointRequest breakpointRequest;
    private final BreakpointWrapper breakpointWrapper;

    /**
     * Constructs a BreakpointRequestWrapper.
     *
     * @param request the JDI BreakpointRequest
     * @param wrapper the associated BreakpointWrapper
     */
    public BreakpointRequestWrapper(BreakpointRequest request, BreakpointWrapper wrapper) {
        this.breakpointRequest = request;
        this.breakpointWrapper = wrapper;
    }

    /**
     * Returns the JDI BreakpointRequest.
     */
    public BreakpointRequest getBreakpointRequest() {
        return breakpointRequest;
    }

    /**
     * Returns the associated BreakpointWrapper.
     */
    public BreakpointWrapper getBreakpointWrapper() {
        return breakpointWrapper;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BreakpointRequestWrapper other)) return false;
        return breakpointWrapper.equals(other.breakpointWrapper);
    }

    @Override
    public int hashCode() {
        return breakpointWrapper.hashCode();
    }

    /**
     * Alias for {@link #getBreakpointRequest()}.
     */
    public BreakpointRequest getRequest() {
        return breakpointRequest;
    }
}
