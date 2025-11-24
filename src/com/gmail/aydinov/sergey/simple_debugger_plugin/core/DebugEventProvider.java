package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.events.SimpleDebugEvent;

public interface DebugEventProvider {
	
	void sendDebugEvent(SimpleDebugEvent debugEvent);

}
