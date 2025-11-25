package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.sun.jdi.event.BreakpointEvent;

public interface BreakpointEventProvider {
	
	BreakpointEvent getCurrentBreakpointEvent();

}
