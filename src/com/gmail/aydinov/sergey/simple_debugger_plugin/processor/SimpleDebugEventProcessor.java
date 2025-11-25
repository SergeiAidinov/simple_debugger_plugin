package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.events.SimpleDebugEvent;
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
		System.out.println(Thread.currentThread() + " started.");
		while (true) {
			try {
				SimpleDebugEvent event = SimpleDebuggerEventQueue.instance().takeDebugEvent();
				System.out.println("SimpleDebugEvent: " + event);
				debugWindow.handleDebugEvent(event);
				// handleEvent(event);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

}
