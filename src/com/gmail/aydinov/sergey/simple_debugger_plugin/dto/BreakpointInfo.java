package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.Objects;

/**
 * Represents information about a breakpoint: class name and line number.
 */
public class BreakpointInfo {

    private final String className;
    private final int lineNumber;

    /**
     * Constructs a BreakpointInfo instance.
     *
     * @param className  fully qualified class name where the breakpoint is set
     * @param lineNumber line number of the breakpoint
     */
    public BreakpointInfo(String className, int lineNumber) {
        this.className = className;
        this.lineNumber = lineNumber;
    }

    /**
     * Returns the fully qualified class name of the breakpoint.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns the line number of the breakpoint.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BreakpointInfo other)) return false;
        return lineNumber == other.lineNumber && Objects.equals(className, other.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, lineNumber);
    }

    @Override
    public String toString() {
        return className + ":" + lineNumber;
    }
}
