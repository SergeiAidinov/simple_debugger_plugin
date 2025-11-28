package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

public class TargetApplicationMethodDTO {
    private final String methodName;
    private final String returnType;

    public TargetApplicationMethodDTO(String methodName, String returnType) {
        this.methodName = methodName;
        this.returnType = returnType;
    }

    public String getMethodName() { return methodName; }
    public String getReturnType() { return returnType; }

    @Override
    public String toString() {
        return methodName + " : " + returnType;
    }
}
