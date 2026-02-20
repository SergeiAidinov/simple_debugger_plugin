package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.FieldOrVariableType;

/**
 * DTO representing a variable in the target application.
 * Stores the variable's name, type, and value as strings.
 * <p>
 * Author: Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class FieldOrVariableDTO {

    private final String name;   // the variable name
    private final String type;   // the variable type
    private final String value;  // the variable value
    private final FieldOrVariableType fieldOrVariableType;

    /**
     * Constructs a VariableDTO.
     *
     * @param name  the name of the variable
     * @param type  the type of the variable
     * @param value the string representation of the variable's value
     */
    public FieldOrVariableDTO(String name, String type, String value, FieldOrVariableType fieldOrVariableType) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.fieldOrVariableType = fieldOrVariableType;
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

	public FieldOrVariableType getFieldOrVariableType() {
		return fieldOrVariableType;
	}

	@Override
	public String toString() {
		return "FieldOrVariableDTO [name=" + name + ", type=" + type + ", value=" + value + ", fieldOrVariableType="
				+ fieldOrVariableType + "]";
	}
}
