package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.List;
import java.util.Map;

import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;

/**
 * 
 */
public class DebugEvent {
	
	private final String className;
	private final String methodName;
	private final int lineNumber;
	private final Map<LocalVariable, Value> localVariables;
	private final Map<Field, Value> fields;
	private final List<StackFrame> frames;
	private final String stackDescription;

	public DebugEvent(String className, String methodName, int lineNumber, Map<Field, Value> fields, Map<LocalVariable, Value> localVariables,
			List<StackFrame> frames, String stackDescription) {
		this.className = className;
		this.methodName = methodName;
		this.lineNumber = lineNumber;
		this.fields = fields;
		this.localVariables = localVariables;
		this.frames = frames;
		this.stackDescription = stackDescription;
		
		
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

	public List<StackFrame> getFrames() {
		return frames;
	}

	public String getStackDescription() {
		return stackDescription;
	}
	
}
