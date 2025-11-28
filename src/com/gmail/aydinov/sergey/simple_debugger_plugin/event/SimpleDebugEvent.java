package com.gmail.aydinov.sergey.simple_debugger_plugin.event;

import java.util.Map;

import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Value;

/**
 * 
 */
public class SimpleDebugEvent {

	private final SimpleDebugEventType simpleDebugEventType;
	private final String className;
	private final String methodName;
	private final int lineNumber;
	private final Map<LocalVariable, Value> localVariables;
	private final Map<Field, Value> fields;
	private final String stackDescription;

	public SimpleDebugEvent(SimpleDebugEventType simpleDebugEventType, String className, String methodName,
			int lineNumber, Map<Field, Value> fields, Map<LocalVariable, Value> localVariables,
			String stackDescription) {
		this.simpleDebugEventType = simpleDebugEventType;
		this.className = className;
		this.methodName = methodName;
		this.lineNumber = lineNumber;
		this.fields = fields;
		this.localVariables = localVariables;
		this.stackDescription = stackDescription;
	}

	public SimpleDebugEventType getSimpleDebugEventType() {
		return simpleDebugEventType;
	}

	public String getClassName() {
		return className;
	}

	public String getMethodName() {
		return methodName;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public Map<Field, Value> getFields() {
		return fields;
	}

	public Map<LocalVariable, Value> getLocalVariables() {
		return localVariables;
	}

	public String getStackDescription() {
		return stackDescription;
	}

}
