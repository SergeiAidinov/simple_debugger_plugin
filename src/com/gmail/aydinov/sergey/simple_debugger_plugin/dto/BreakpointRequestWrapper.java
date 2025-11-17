package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import com.sun.jdi.request.BreakpointRequest;

public class BreakpointRequestWrapper {
    private final BreakpointRequest breakpointRequest;
    private final BreakpointWrapper breakpointWrapper;

    public BreakpointRequestWrapper(BreakpointRequest request, BreakpointWrapper wrapper) {
        this.breakpointRequest = request;
        this.breakpointWrapper = wrapper;
    }

    public BreakpointRequest getBreakpointRequest() {
        return breakpointRequest;
    }

    public BreakpointWrapper getBreakpointWrapper() {
        return breakpointWrapper;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BreakpointRequestWrapper)) return false;
        BreakpointRequestWrapper o = (BreakpointRequestWrapper) obj;
        return breakpointWrapper.equals(o.breakpointWrapper);
    }

    @Override
    public int hashCode() {
        return breakpointWrapper.hashCode();
    }

	public BreakpointRequest getRequest() {
		return breakpointRequest;
	}
}
