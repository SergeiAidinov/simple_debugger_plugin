package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventDTO;

@FunctionalInterface
public interface DebugEventProvider {
	
	void sendDebugEvent(SimpleDebugEventDTO debugEvent);

}
