package com.gmail.aydinov.sergey.simple_debugger_plugin.ui;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.VariableDTO;

/**
 * Represents a variable entry in the debugger UI.
 * Stores the variable's name, type, original value, and optionally a new value.
 */
public class VarEntry {

    /** Variable name */
    private final String name;

    /** Variable type */
    private final String type;

    /** Original value of the variable */
    private final String value;

    /** New value set by user or evaluation */
    private Object newValue;

    /**
     * Constructs a VarEntry from a VariableDTO object.
     *
     * @param dto Data transfer object containing variable information
     */
    public VarEntry(VariableDTO dto) {
        this.name = dto.getName();
        this.type = dto.getType();
        this.value = dto.getValue();
    }

    /**
     * Constructs a VarEntry with explicit name, type, and value.
     *
     * @param name  variable name
     * @param type  variable type
     * @param value variable value
     */
    public VarEntry(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    /**
     * @return the variable name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the variable type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the original value of the variable
     */
    public String getValue() {
        return value;
    }

    /**
     * @return the new value of the variable (if set)
     */
    public Object getNewValue() {
        return newValue;
    }

    /**
     * Sets a new value for the variable (used for evaluation or editing in UI).
     *
     * @param newValue the new value to assign
     */
    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }
}
