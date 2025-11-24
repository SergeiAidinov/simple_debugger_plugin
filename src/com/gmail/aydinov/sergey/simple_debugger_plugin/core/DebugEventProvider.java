package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.SimpleDebugEvent;

public interface DebugEventProvider {
	
	void sendDebugEvent(SimpleDebugEvent debugEvent);

}
