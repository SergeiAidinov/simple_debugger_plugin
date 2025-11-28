package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.VariableDTO;

public class VarEntry {

    private final String name;
    private final String type;
    private final String value;
    private Object newValue;

    public VarEntry(VariableDTO dto) {
        this.name = dto.getName();
        this.type = dto.getType();
        this.value = dto.getValue();
    }

    public VarEntry(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }
}