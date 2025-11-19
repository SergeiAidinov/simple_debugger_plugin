package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugEvent;

public interface DebugEventProvider {
	
	void sendDebugEvent(DebugEvent debugEvent);

}
