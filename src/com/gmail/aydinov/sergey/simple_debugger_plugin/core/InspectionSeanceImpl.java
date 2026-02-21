package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.InspectionSeance;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.FieldOrVariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.UiEventCollector;

public class InspectionSeanceImpl implements InspectionSeance {
	
	UiEventCollector uiEventCollector = SimpleDebuggerEventQueue.instance();
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

	private void startInspectionSeanceForAnchor(FieldOrVariableDTO anchor2) {
		
		
	}

}
