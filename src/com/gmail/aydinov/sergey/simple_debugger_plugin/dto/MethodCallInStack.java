package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

public class MethodCallInStack {
	
	private final String  className, methodName, sourceInfo;

	public MethodCallInStack(String className, String methodName, String sourceInfo) {
		this.className = className;
		this.methodName = methodName;
		this.sourceInfo = sourceInfo;
	}

	public String getClassName() {
		return className;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getSourceInfo() {
		return sourceInfo;
	}

}
