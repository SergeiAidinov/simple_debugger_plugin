package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TargetApplicationMethodDTO implements Comparable<TargetApplicationMethodDTO> {

    private final String methodName;
    private final String returnType;
    private final List<TargetApplicationMethodParameterDTO> parameters; 

    public TargetApplicationMethodDTO(
            String methodName,
            String returnType,
            List<TargetApplicationMethodParameterDTO> parameters
    ) {
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameters = parameters;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<TargetApplicationMethodParameterDTO> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        String params = parameters.stream()
            .map(p -> {
                String typeName;
                try {
                    typeName = p.getType().name();
                } catch (Exception e) {
                    typeName = e.getMessage();
                }

                // Убираем только фразу no class loader
                if (typeName != null && typeName.contains("no class loader")) {
                    typeName = "";
                }

                return typeName.isEmpty()
                        ? p.getName()
                        : p.getName() + ": " + typeName;
            })
            .collect(Collectors.joining(", "));

        return methodName + "(" + params + ")";
    }


    @Override
    public int compareTo(TargetApplicationMethodDTO o) {
        if (o == null) return 1;
        int c = methodName.compareTo(o.methodName);
        if (c != 0) return c;
        return returnType.compareTo(o.returnType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TargetApplicationMethodDTO other)) return false;
        return Objects.equals(methodName, other.methodName)
                && Objects.equals(returnType, other.returnType)
                && Objects.equals(parameters, other.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodName, returnType, parameters);
    }
}
