package com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.SimpleDebugEventDTO;

public interface DebugEventProvider {
	
	void sendDebugEvent(SimpleDebugEventDTO debugEvent);

}
