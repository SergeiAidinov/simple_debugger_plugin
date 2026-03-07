package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.Objects;

public class UserChangedFieldEventDTO {

    private final String fieldName;
    private final String fieldType;
    private final String newValue;

    public UserChangedFieldEventDTO(String fieldName, String fieldType, Object newValue) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.newValue = Objects.nonNull(newValue) ? newValue.toString() : null;
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
        return (Objects.nonNull(fieldName) ? fieldName : "<unknown>")
                + " : " + (Objects.nonNull(fieldType) ? fieldType : "<unknown>")
                + " = " + (Objects.nonNull(newValue) ? newValue : "<null>");
    }
}
