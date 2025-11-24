package com.gmail.aydinov.sergey.simple_debugger_plugin.processor.events;

import com.sun.jdi.LocalVariable;
import com.sun.jdi.StackFrame;

public class UIEventUpdateVariable extends UIEvent {
	
	private final StackFrame stackFrame;
	private final LocalVariable localVariable;
	private final String newValue;
	
	public UIEventUpdateVariable(StackFrame stackFrame, LocalVariable localVariable, String newValue) {
		this.stackFrame = stackFrame;
		this.localVariable = localVariable;
		this.newValue = newValue;
	}
	
	public StackFrame getStackFrame() {
		return stackFrame;
	}

	public LocalVariable getLocalVariable() {
		return localVariable;
	}

	public String getNewValue() {
		return newValue;
	}
	
}
