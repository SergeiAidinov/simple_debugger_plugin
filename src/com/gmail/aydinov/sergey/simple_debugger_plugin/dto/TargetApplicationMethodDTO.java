package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.Objects;

public class TargetApplicationMethodDTO implements Comparable<TargetApplicationMethodDTO> {
    

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

    @Override
    public int compareTo(TargetApplicationMethodDTO o) {
        if (o == null) return 1;
        int nameCompare = this.methodName.compareTo(o.methodName);
        if (nameCompare != 0) {
            return nameCompare;
        }
        return this.returnType.compareTo(o.returnType);
    }

	
	@Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TargetApplicationMethodDTO other)) return false;
        return Objects.equals(this.methodName, other.methodName) &&
               Objects.equals(this.returnType, other.returnType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodName, returnType);
    }
}
