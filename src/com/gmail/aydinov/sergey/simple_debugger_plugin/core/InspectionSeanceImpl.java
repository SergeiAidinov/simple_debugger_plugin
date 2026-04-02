package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.InspectionSeance;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class InspectionSeanceImpl implements InspectionSeance{

	@Override
	public void startInspectionSeanceForAnchorElement(AbstractUIEvent abstractSimpleDebuggerUIEvent,
			StackFrame currentFrame, BreakpointEvent breakpointEvent) {
		Inspection inspection = new Inspection();
		inspection.run();
		
	}
	
	private class Inspection implements Runnable {

		@Override
		public void run() {
			for (int i = 0; i < 5; i++) {
				System.out.println("INSPECTION: " + Thread.currentThread());
				try {
					Thread.currentThread().sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
	}
	
	

}
