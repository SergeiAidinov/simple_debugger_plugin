package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.AbstractSimpleDebugEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventDTO;

public interface SimpleDebugEventCollector {
	
	void collectDebugEvent(AbstractSimpleDebugEventDTO event);

	// Получить событие для обработки (Worker поток)
	AbstractSimpleDebugEventDTO takeDebugEvent() throws InterruptedException;
}
