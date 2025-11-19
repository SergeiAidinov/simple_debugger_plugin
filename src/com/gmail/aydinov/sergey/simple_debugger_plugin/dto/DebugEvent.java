package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sun.jdi.LocalVariable;
import com.sun.jdi.Value;

public class DebugEvent {
	
	private final Map<LocalVariable, Value> localVariables;

	public DebugEvent(Map<LocalVariable, Value> localVariables) {
		this.localVariables = localVariables;
		
	}
	
	public Map<LocalVariable, Value> getLocalVariables() {
		return localVariables;
	}
	
	

}
