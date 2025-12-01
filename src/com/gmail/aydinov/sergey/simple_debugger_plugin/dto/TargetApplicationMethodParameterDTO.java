package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import com.sun.jdi.Type;

public class TargetApplicationMethodParameterDTO {
    private final String name;
    private final Type type;

    public TargetApplicationMethodParameterDTO(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        String typeName;
        try {
            typeName = type.name();
        } catch (Exception e) {
            typeName = e.getMessage();
        }

        // УБИРАЕМ только "no class loader"
        if (typeName != null && typeName.contains("no class loader")) {
            typeName = "";
        }

        return typeName.isEmpty() ? name : name + ": " + typeName;
    }
}
