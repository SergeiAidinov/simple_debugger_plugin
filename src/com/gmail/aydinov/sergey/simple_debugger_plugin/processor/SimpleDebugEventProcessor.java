package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractSimpleDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugStoppedAtBreakpointEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindow;

public class SimpleDebugEventProcessor implements Runnable {

//	private final SimpleDebuggerEventQueue simpleDebuggerEventQueue;
	private final DebugWindow debugWindow;

	public SimpleDebugEventProcessor(DebugWindow debugWindow) {
		// this.simpleDebuggerEventQueue = simpleDebuggerEventQueue;
		this.debugWindow = debugWindow;
	}

	@Override
	public void run() {
		while (true) {
			try {
				AbstractSimpleDebugEvent event = SimpleDebuggerEventQueue.instance().takeDebugEvent();
				SimpleDebuggerLogger.info("SimpleDebugEvent: " + event);
				debugWindow.handleDebugEvent(event);
				// handleEvent(event);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

}
