package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractDebugEvent;

public interface ManageableWindow {
	
	boolean isOpen();

	void handleDebugEvent(AbstractDebugEvent event);

	void open();
	
	void close();

}
