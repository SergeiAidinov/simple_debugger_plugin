package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.BreakpointWrapper;

public interface BreakpointSubscriber {

	void addBreakepoint(BreakpointWrapper breakpointWrapper);

	void deleteBreakepoint(BreakpointWrapper breakpointWrapper);

	void changeBreakpoint(BreakpointWrapper breakpointWrapper);

}