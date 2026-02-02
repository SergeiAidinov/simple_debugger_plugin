package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;

/**
 * DTO representing a user-initiated change of a variable in the UI.
 * Contains the variable's name, type, and the new value entered by the user.
 */
public class UserChangedVariableDTO extends AbstractUIEvent {

    private final String name;      // the variable name
    private final String type;      // the variable type (informational)
    private final Object newValue;  // the new value assigned by the user

    /**
     * Constructs a UserChangedVariableDTO.
     *
     * @param name the name of the variable
     * @param type the type of the variable
     * @param newValue the new value assigned to the variable
     */
    public UserChangedVariableDTO(String name, String type, Object newValue) {
        this.name = name;
        this.type = type;
        this.newValue = newValue;
    }

    /** @return the variable name */
    public String getName() {
        return name;
    }

    /** @return the variable type */
    public String getType() {
        return type;
    }

    /** @return the new value assigned to the variable */
    public Object getNewValue() {
        return newValue;
    }
}
