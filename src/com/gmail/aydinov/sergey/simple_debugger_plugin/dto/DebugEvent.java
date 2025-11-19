package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.Map;

import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Value;

public class DebugEvent {
	
	private final Map<LocalVariable, Value> localVariables;
	private final String className;
	private final Map<Field, Value> fields;

	public DebugEvent(String className, Map<Field, Value> fields, Map<LocalVariable, Value> localVariables) {
		this.className = className;
		this.fields = fields;
		this.localVariables = localVariables;
		
		
	}
	
	public String getClassName() {
		return className;
	}
	
	
	public Map<Field, Value> getFields() {
		return fields;
	}

	public Map<LocalVariable, Value> getLocalVariables() {
		return localVariables;
	}

}
