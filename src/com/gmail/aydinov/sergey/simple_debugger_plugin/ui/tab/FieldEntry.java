package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import com.sun.jdi.Field;
import com.sun.jdi.Value;

public class FieldEntry {

    private final Field field;
    private final Value originalValue;
    private String newValue;

    public FieldEntry(Field field, Value value) {
        this.field = field;
        this.originalValue = value;
        this.newValue = (value == null ? "null" : value.toString());
    }

    public Field getField() {
        return field;
    }

    public String getValue() {
        return newValue != null ? newValue.toString() : "";
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public Value getOriginalValue() {
        return originalValue;
    }
}
