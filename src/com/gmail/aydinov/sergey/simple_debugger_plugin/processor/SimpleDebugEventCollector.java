package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractSimpleDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugStoppedAtBreakepointEvent;

public interface SimpleDebugEventCollector {
	
	void collectDebugEvent(AbstractSimpleDebugEvent event);

	// Получить событие для обработки (Worker поток)
	AbstractSimpleDebugEvent takeDebugEvent() throws InterruptedException;
}
