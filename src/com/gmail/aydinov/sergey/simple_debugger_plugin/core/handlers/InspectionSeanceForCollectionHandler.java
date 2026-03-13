package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class InspectionSeanceForCollectionHandler implements UIEventHandler {

	private final DebugEventCollector simpleDebugEventCollector = SimpleDebuggerEventCollector.instance();

	@Override
	public boolean handle(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		System.out.println("COLLECT. INSP. STARTED");
		simpleDebugEventCollector
				.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, false));
		DebuggerContext.context().setStatus(SimpleDebuggerStatus.COLLECTION_INSPECTION_SEANCE_RUNNING);
		Thread collectionInspectionThread = new Thread(new CollectionInspectionSeance());
		collectionInspectionThread.setDaemon(false);
		try {
			collectionInspectionThread.start();
			try {
				collectionInspectionThread.join();
			} catch (InterruptedException e) {
			}
			return true;
		} finally {
			simpleDebugEventCollector
					.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
			simpleDebugEventCollector.collectDebugEvent(
					new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_COLLECTION_INSPECT_WINDOW_STATE, false));
			DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
		}

	}

	private class CollectionInspectionSeance implements Runnable {
		@Override
		public void run() {
			collectionInspection();
		}

		private void collectionInspection() {

			simpleDebugEventCollector.collectDebugEvent(
					new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_COLLECTION_INSPECT_WINDOW_STATE, true));
			while (true) {
				AbstractDebugEvent debugEvent = null;
				try {
					debugEvent = simpleDebugEventCollector.takeDebugEvent();
				} catch (InterruptedException e) {}
				if (!SimpleDebuggerEventTypes.isCollectionInspectionWindowEvent(debugEvent.getType()))
					ignoreEvent(debugEvent);
				if (debugEvent.getType().equals(SimpleDebuggerEventType.USER_CLOSED_INSPECTION_SEANCE_FOR_COLLECTION)) break;
				

			}

		}

		private void ignoreEvent(AbstractDebugEvent debugEvent) {
			SimpleDebuggerLogger.info("Intentionally ignored: " + debugEvent);
		}

	}
}
