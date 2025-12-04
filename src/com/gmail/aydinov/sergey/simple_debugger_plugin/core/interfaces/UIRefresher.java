package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.sun.jdi.event.BreakpointEvent;

@FunctionalInterface
public interface UIRefresher {
	
	boolean refreshUI(BreakpointEvent breakpointEvent);

}
