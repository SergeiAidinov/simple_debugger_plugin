package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEvent;

public interface DebugEventProvider {
	
	void sendDebugEvent(SimpleDebugEvent debugEvent);

}
