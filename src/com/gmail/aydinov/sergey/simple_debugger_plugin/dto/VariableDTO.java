package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

public class VariableDTO {

    private final String name;
    private final String type;
    private final String value;

    public VariableDTO(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public String getValue() { return value; }

    @Override
    public String toString() {
        return name + ": " + type + " = " + value;
    }
    
}