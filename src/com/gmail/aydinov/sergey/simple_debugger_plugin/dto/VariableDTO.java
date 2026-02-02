package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

/**
 * DTO representing a variable in the target application.
 * Stores the variable's name, type, and value as strings.
 */
public class VariableDTO {

    private final String name;   // the variable name
    private final String type;   // the variable type
    private final String value;  // the variable value

    /**
     * Constructs a VariableDTO.
     *
     * @param name  the name of the variable
     * @param type  the type of the variable
     * @param value the string representation of the variable's value
     */
    public VariableDTO(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    /** @return the variable name */
    public String getName() {
        return name;
    }

    /** @return the variable type */
    public String getType() {
        return type;
    }

    /** @return the variable value */
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name + ": " + type + " = " + value;
    }
}
