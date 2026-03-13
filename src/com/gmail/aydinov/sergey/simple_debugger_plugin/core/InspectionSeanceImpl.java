package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.Objects;
import java.util.stream.Stream;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.InspectionSeance;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TopLevelElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;

public class InspectionSeanceImpl implements InspectionSeance {
	
	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private final DebugEventCollector debugCollector = SimpleDebuggerEventCollector.instance();
	private final UniversalElementRepresentation anchorElement;
	private final TargetApplicationRepresentation targetApplicationRepresentation;

	public InspectionSeanceImpl(UniversalElementRepresentation topLevelElement, TargetApplicationRepresentation targetApplicationRepresentation) {
		super();
		this.anchorElement = topLevelElement;
		this.targetApplicationRepresentation = targetApplicationRepresentation;
	}
	
	public UniversalElementRepresentation getAnchor() {
		return anchorElement;
	}

	@Override
	public void run() {
		System.out.println(Thread.currentThread() + " started for " + anchorElement.toString());
		try {
			startInspectionSeanceForAnchor(anchorElement);
		} finally {
			System.out.println("===> Inspection finished");
			DebuggerContext.context().setStatus(SimpleDebuggerStatus.INSPECTION_SEANCE_STOPPED);
		}
	}

	private void startInspectionSeanceForAnchor(UniversalElementRepresentation anchorElement2) {
		//debugCollector.collectDebugEvent(new DebugEvent<TopLevelElementRepresentationDTO>(SimpleDebuggerEventType.SHOW_ANCHOR_ELEMENT, anchorElement2));
		while (DebuggerContext.context().isInspectionSeanceActive()) {
			AbstractUIEvent abstractUIEvent = null;
			try {
				abstractUIEvent = uiEventCollector.takeUiEvent();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (Objects.isNull(abstractUIEvent)) continue;
		//	if (SimpleDebuggerEventTypes.isMainWindowEvent(abstractUIEvent.getType())) ignoreUiEvent(abstractUIEvent); 
			if (DebuggerContext.context().getStatus().equals(SimpleDebuggerStatus.INSPECTION_SEANCE_CLOSING)) break;
			
		}
	}

	private void ignoreUiEvent(AbstractUIEvent uiEvent) {
		SimpleDebuggerLogger.info("Ignored UI-event " + uiEvent + " because inspection session has alredy started");
		
	}

}
