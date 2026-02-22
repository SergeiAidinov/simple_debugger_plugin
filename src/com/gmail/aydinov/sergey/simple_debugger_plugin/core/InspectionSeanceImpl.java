package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.InspectionSeance;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.FieldOrVariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.SetInspectionWindowStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractInspectionUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserEndedInspectionSessionForElement;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event_collectors.SimpleDebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event_collectors.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event_collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;

public class InspectionSeanceImpl implements InspectionSeance {
	
	private final UiEventCollector uiEventCollector = SimpleDebuggerEventQueue.instance();
	private final SimpleDebugEventCollector debugCollector = SimpleDebuggerEventQueue.instance();
	private final FieldOrVariableDTO anchor;

	public InspectionSeanceImpl(FieldOrVariableDTO anchor) {
		super();
		this.anchor = anchor;
	}
	
	public FieldOrVariableDTO getAnchor() {
		return anchor;
	}


	@Override
	public void run() {
		System.out.println(Thread.currentThread() + " started for " + anchor.toString());
		try {
			startInspectionSeanceForAnchor(anchor);
		} finally {
			System.out.println("===> Inspection finished");
		}
	}

	private void startInspectionSeanceForAnchor(FieldOrVariableDTO anchor) {
		AbstractUIEvent uiEvent = null;
		try {
			uiEvent = uiEventCollector.takeUiEvent();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!(uiEvent instanceof AbstractInspectionUIEvent)) {
			ignoreUiEvent(uiEvent);
		} else if (uiEvent instanceof UserEndedInspectionSessionForElement userEndedInspectionSessionForElement) {
			System.out.println("CAUGHT: " + userEndedInspectionSessionForElement);
			debugCollector.collectDebugEvent(new SetInspectionWindowStatus(false));
		}
		
	}

	private void ignoreUiEvent(AbstractUIEvent uiEvent) {
		SimpleDebuggerLogger.info("Ignored UI-event " + uiEvent + " because inspection session has alredy started");
		
	}

}
