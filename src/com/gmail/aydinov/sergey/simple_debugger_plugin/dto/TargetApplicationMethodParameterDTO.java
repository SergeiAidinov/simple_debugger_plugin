package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;


public final class TargetApplicationMethodParameterDTO {

    private final String name;
    private final String typeName;

    public TargetApplicationMethodParameterDTO(String name, String typeName) {
        this.name = name;
        this.typeName = typeName;
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    @Override
    public String toString() {
        return typeName == null || typeName.isEmpty()
                ? name
                : name + ": " + typeName;
    }
}

