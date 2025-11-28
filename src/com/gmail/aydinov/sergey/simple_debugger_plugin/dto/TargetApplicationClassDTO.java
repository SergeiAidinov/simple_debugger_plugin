package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.List;

public class TargetApplicationClassDTO {
    private final String className;
    private final TargetApplicationElementType elementType; // CLASS или INTERFACE
    private final List<TargetApplicationMethodDTO> methods;

    public TargetApplicationClassDTO(String className, TargetApplicationElementType elementType, List<TargetApplicationMethodDTO> methods) {
        this.className = className;
        this.elementType = elementType;
        this.methods = methods;
    }

    public String getClassName() { return className; }
    public TargetApplicationElementType getElementType() { return elementType; }
    public List<TargetApplicationMethodDTO> getMethods() { return methods; }
}
