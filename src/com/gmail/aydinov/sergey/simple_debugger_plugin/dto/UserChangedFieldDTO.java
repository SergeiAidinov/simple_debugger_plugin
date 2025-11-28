package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UIEvent;

/**
 * DTO события изменения поля объекта пользователем через UI.
 */
public class UserChangedFieldDTO extends UIEvent {

    private final String fieldName;
    private final String fieldType;
    private final String newValue;

    public UserChangedFieldDTO(String fieldName, String fieldType, Object newValue) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.newValue = newValue != null ? newValue.toString() : null;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public String getNewValue() {
        return newValue;
    }

    @Override
    public String toString() {
        return fieldName + " : " + fieldType + " = " + newValue;
    }
}
