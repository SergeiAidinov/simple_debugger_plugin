package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.InspectionSeance;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractSimpleDebuggerUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event_collectors.SimpleDebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event_collectors.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event_collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;

public class InspectionSeanceImpl implements InspectionSeance {
	
	private final UiEventCollector uiEventCollector = SimpleDebuggerEventQueue.instance();
	private final SimpleDebugEventCollector debugCollector = SimpleDebuggerEventQueue.instance();
	private final TargetApplicationElementRepresentation anchorElement;
	private final TargetApplicationRepresentation targetApplicationRepresentation;

	public InspectionSeanceImpl(TargetApplicationElementRepresentation targetApplicationElementRepresentation, TargetApplicationRepresentation targetApplicationRepresentation) {
		super();
		this.anchorElement = targetApplicationElementRepresentation;
		this.targetApplicationRepresentation = targetApplicationRepresentation;
	}
	
	public TargetApplicationElementRepresentation getAnchor() {
		return anchorElement;
	}

	@Override
	public void run() {
		System.out.println(Thread.currentThread() + " started for " + anchorElement.toString());
		try {
			startInspectionSeanceForAnchor(anchorElement);
		} finally {
			System.out.println("===> Inspection finished");
		}
	}

	private void startInspectionSeanceForAnchor(TargetApplicationElementRepresentation anchorElement) {
		debugCollector.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventTypes.SimpleDebuggerEventType.DISPLAY_INSPECTION_WINDOW, false));
		debugCollector.collectDebugEvent(new DebugEvent<TargetApplicationElementRepresentation>(SimpleDebuggerEventType.SHOW_ANCHOR_ELEMENT, anchorElement));
		try {
			Thread.currentThread().sleep(60_000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	private void ignoreUiEvent(AbstractSimpleDebuggerUIEvent uiEvent) {
		SimpleDebuggerLogger.info("Ignored UI-event " + uiEvent + " because inspection session has alredy started");
		
	}

}
