package com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event;

import java.util.Objects;

/**
 * Represents a UI event where a user changes a field of an object.
 * Stores the field name, type, and the new value assigned by the user.
 */
public class UserChangedFieldEvent extends AbstractUIEvent {

    private final String fieldName;
    private final String fieldType;
    private final String newValue;

    /**
     * Constructs a UserChangedFieldDTO.
     *
     * @param fieldName the name of the field that was changed
     * @param fieldType the type of the field
     * @param newValue the new value assigned to the field (converted to String)
     */
    public UserChangedFieldEvent(String fieldName, String fieldType, Object newValue) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.newValue = Objects.nonNull(newValue) ? newValue.toString() : null;
    }

    /** @return the name of the field */
    public String getFieldName() {
        return fieldName;
    }

    /** @return the type of the field */
    public String getFieldType() {
        return fieldType;
    }

    /** @return the new value assigned to the field */
    public String getNewValue() {
        return newValue;
    }

    /**
     * Returns a human-readable string representation of this event.
     * Format: "fieldName : fieldType = newValue"
     */
    @Override
    public String toString() {
        return (Objects.nonNull(fieldName) ? fieldName : "<unknown>")
                + " : " + (Objects.nonNull(fieldType) ? fieldType : "<unknown>")
                + " = " + (Objects.nonNull(newValue) ? newValue : "<null>");
    }
}
