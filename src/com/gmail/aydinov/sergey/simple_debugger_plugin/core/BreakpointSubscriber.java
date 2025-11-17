package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.BreakpointWrapper;

public interface BreakpointSubscriber {

	boolean addBreakepoint(BreakpointWrapper breakpointWrapper);

	void deleteBreakepoint(BreakpointWrapper breakpointWrapper);

	void changeBreakpoint(BreakpointWrapper breakpointWrapper);

}