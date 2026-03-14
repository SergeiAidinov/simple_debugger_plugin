package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.AbstractDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.SimpleDebugerWindowsManager;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class InspectionSeanceForCollectionHandler implements UIEventHandler {

	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private final DebugEventCollector debugEventCollector = SimpleDebuggerEventCollector.instance();

	@Override
	public boolean handle(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		System.out.println("COLLECT. INSP. STARTED");
//		simpleDebugEventCollector.collectDebugEvent(
//				new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_COLLECTION_INSPECT_WINDOW_STATE, true));
		debugEventCollector
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
			DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
			debugEventCollector
					.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
//			simpleDebugEventCollector.collectDebugEvent(
//					new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_COLLECTION_INSPECT_WINDOW_STATE, false));
			SimpleDebugerWindowsManager.instance().getManageableCollectionInspectorWindow().close();
			
		}

	}

	private class CollectionInspectionSeance implements Runnable {
		@Override
		public void run() {
			collectionInspection();
		}

		private void collectionInspection() {

			
			while (true) {
				AbstractUIEvent uiEvent = null;
				try {
					uiEvent = uiEventCollector.takeUiEvent();
					System.out.println("EVENT IN SEANCE: " + uiEvent);
				} catch (InterruptedException e) {}
				if (!SimpleDebuggerEventTypes.isCollectionInspectionWindowEvent(uiEvent.getType()))
					ignoreEvent(uiEvent);
				if (uiEvent.getType().equals(SimpleDebuggerEventType.USER_CLOSED_INSPECTION_SEANCE_FOR_COLLECTION)) {
					break;
				}
					
				

			}

		}

		private void ignoreEvent(AbstractUIEvent debugEvent) {
			SimpleDebuggerLogger.info("Intentionally ignored: " + debugEvent);
		}

	}
}
