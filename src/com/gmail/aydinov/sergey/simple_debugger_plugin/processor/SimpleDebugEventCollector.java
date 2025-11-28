package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventDTO;

public interface SimpleDebugEventCollector {
	
	void collectDebugEvent(SimpleDebugEventDTO event);

	// Получить событие для обработки (Worker поток)
	SimpleDebugEventDTO takeDebugEvent() throws InterruptedException;
}
