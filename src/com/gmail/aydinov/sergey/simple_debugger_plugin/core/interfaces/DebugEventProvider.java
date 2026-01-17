package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugStoppedAtBreakepointEvent;

@FunctionalInterface
public interface DebugEventProvider {
	
	void sendDebugEvent(DebugStoppedAtBreakepointEvent debugEvent);

}
