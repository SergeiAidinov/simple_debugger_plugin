package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import com.sun.jdi.LocalVariable;
import com.sun.jdi.Value;

public class VarEntry {
	
    private final LocalVariable localVar;
    private final String type;
    private final String value;
    private Object newValue;
    
    VarEntry(LocalVariable localVar, Value val) {
        this.localVar = localVar;
        this.type = localVar.typeName();
        this.value = valueToString(val);
    }
    
    public Object getNewValue() {
		return newValue;
	}

	public void setNewValue(Object newValue) {
		this.newValue = newValue;
	}

	public LocalVariable getLocalVar() {
		return localVar;
	}

	public String getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}

	private String valueToString(Value v) {
        if (v == null) return "null";
        try {
            return v.toString();
        } catch (Exception e) {
            return "<error>";
        }
    }
}