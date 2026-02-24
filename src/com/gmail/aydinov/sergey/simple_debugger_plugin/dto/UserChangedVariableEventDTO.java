package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

public class UserChangedVariableEventDTO{

    private final String name;      // the variable name
    private final String type;      // the variable type (informational)
    private final Object newValue;  // the new value assigned by the user

    public UserChangedVariableEventDTO(String name, String type, Object newValue) {
        this.name = name;
        this.type = type;
        this.newValue = newValue;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Object getNewValue() {
        return newValue;
    }
}
