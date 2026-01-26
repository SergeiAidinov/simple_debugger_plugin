package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import org.eclipse.debug.core.model.IBreakpoint;

public interface BreakpointSubscriber {

	void addBreakepoint(IBreakpoint breakpoint);

	void deleteBreakepoint(IBreakpoint breakpoint);

	void changeBreakpoint(IBreakpoint breakpoint);

}