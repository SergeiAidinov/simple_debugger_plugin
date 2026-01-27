package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.jdi.Field;

public class TargetApplicationClassOrInterfaceRepresentation implements TargetApplicationElementRepresentation {

    private final String targetApplicationElementName;
    private final TargetApplicationElementType targetApplicationElementType;
    private Set<TargetApplicationMethodDTO> methods;
    private final Set<Field> fields;

    public TargetApplicationClassOrInterfaceRepresentation(String targetApplicationElementName,
            TargetApplicationElementType targetApplicationElementType, Set<TargetApplicationMethodDTO> methods,
            Set<Field> fields) {
        this.targetApplicationElementName = targetApplicationElementName;
        this.targetApplicationElementType = targetApplicationElementType;
        this.methods = methods;
        this.fields = fields;
    }

    @Override
    public void setMethods(Set<TargetApplicationMethodDTO> methods) {
        this.methods = methods;
    }

    public Set<TargetApplicationMethodDTO> getMethods() {
        return methods;
    }

    public Set<Field> getFields() {
        return fields;
    }

    public String getTargetApplicationElementName() {
        return targetApplicationElementName;
    }

    public TargetApplicationElementType getTargetApplicationElementType() {
        return targetApplicationElementType;
    }

    public String prettyPrint() {
        String methodsPretty = methods.stream().sorted()
                .map(m -> String.format("%-30s  %s", m.getMethodName(), m.getReturnType()))
                .collect(Collectors.joining("\n"));

        String fieldsPretty = fields.stream().sorted(Comparator.comparing(Field::name))
                .map(f -> String.format("%-30s  %s", f.name(), f.typeName()))
                .collect(Collectors.joining("\n"));

        return """
                TargetApplicationElement {
                  name  = '%s'
                  type  = %s

                  methods:
                %s

                  fields:
                %s
                }
                """.formatted(targetApplicationElementName, targetApplicationElementType, methodsPretty, fieldsPretty);
    }

    @Override
    public TargetApplicationClassOrInterfaceRepresentation clone() {
        try {
            TargetApplicationClassOrInterfaceRepresentation copy = 
                (TargetApplicationClassOrInterfaceRepresentation) super.clone();

            // создаём новый Set методов (глубокая копия коллекции)
            copy.methods = new HashSet<>(this.methods);

            // поля fields оставляем как есть (JDI Field не клонируется)
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Clone not supported", e);
        }
    }
}
