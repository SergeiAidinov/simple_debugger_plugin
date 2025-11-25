package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.events.SimpleDebugEvent;

public interface SimpleDebugEventCollector {
	
	void collectDebugEvent(SimpleDebugEvent event);

	// Получить событие для обработки (Worker поток)
	SimpleDebugEvent takeDebugEvent() throws InterruptedException;
}
