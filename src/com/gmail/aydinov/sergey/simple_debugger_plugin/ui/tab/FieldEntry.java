package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import com.sun.jdi.Field;
import com.sun.jdi.Value;

import java.util.Objects;

/**
 * Represents a field in a debugged class along with its original and possibly updated value.
 */
public class FieldEntry {

    /** JDI field reference */
    private final Field field;

    /** Original value retrieved from the debugged VM */
    private final Value originalValue;

    /** Current (possibly updated) value as string */
    private String newValue;

    /**
     * Constructs a FieldEntry for a given field and its value.
     *
     * @param field the JDI field
     * @param value the initial value of the field
     */
    public FieldEntry(Field field, Value value) {
        this.field = field;
        this.originalValue = value;
        this.newValue = (Objects.isNull(value) ? "null" : value.toString());
    }

    /**
     * Returns the JDI Field object.
     *
     * @return the field
     */
    public Field getField() {
        return field;
    }

    /**
     * Returns the current value of the field as string.
     *
     * @return the current value or empty string if null
     */
    public String getValue() {
        return Objects.toString(newValue, "");
    }

    /**
     * Updates the value of the field.
     *
     * @param newValue the new value as string
     */
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    /**
     * Returns the updated value of the field as string.
     *
     * @return the new value
     */
    public String getNewValue() {
        return newValue;
    }

    /**
     * Returns the original value retrieved from the VM.
     *
     * @return the original JDI value
     */
    public Value getOriginalValue() {
        return originalValue;
    }
}
