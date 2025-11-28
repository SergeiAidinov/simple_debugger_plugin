package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.List;
import java.util.Map;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventType;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Value;

public class SimpleDebugEventDTO {

    private final SimpleDebugEventType type;
    private final String className;
    private final String methodName;
    private final int lineNumber;
    private final List<VariableDTO> locals;
    private final List<VariableDTO> fields;
    private final String stackTrace;

    public SimpleDebugEventDTO(SimpleDebugEventType type,
                               String className,
                               String methodName,
                               int lineNumber,
                               List<VariableDTO> fields,
                               List<VariableDTO> locals,
                               String stackTrace) {
        this.type = type;
        this.className = className;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
        this.fields = fields;
        this.locals = locals;
        this.stackTrace = stackTrace;
    }

    public SimpleDebugEventType getType() { return type; }
    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    public int getLineNumber() { return lineNumber; }
    public List<VariableDTO> getLocals() { return locals; }
    public List<VariableDTO> getFields() { return fields; }
    public String getStackTrace() { return stackTrace; }

	public Map<LocalVariable, Value> getLocalVariablesDTO() {
		// TODO Auto-generated method stub
		return null;
	}
}

