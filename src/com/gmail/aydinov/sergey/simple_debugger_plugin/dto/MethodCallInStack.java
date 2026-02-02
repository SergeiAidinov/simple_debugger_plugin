package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

/**
 * Represents a single method call in a thread's stack trace.
 */
public class MethodCallInStack {

    private final String className;
    private final String methodName;
    private final String sourceInfo;

    /**
     * Constructs a MethodCallInStack instance.
     *
     * @param className  fully qualified name of the class containing the method
     * @param methodName name of the method
     * @param sourceInfo source file information (e.g., file name and line number)
     */
    public MethodCallInStack(String className, String methodName, String sourceInfo) {
        this.className = className;
        this.methodName = methodName;
        this.sourceInfo = sourceInfo;
    }

    /**
     * Returns the fully qualified class name.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns the method name.
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Returns the source information for this method call.
     */
    public String getSourceInfo() {
        return sourceInfo;
    }

    @Override
    public String toString() {
        return className + "." + methodName + " (" + sourceInfo + ")";
    }
}
