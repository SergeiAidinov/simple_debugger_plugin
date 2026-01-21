package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.Objects;

/**
 * Информация о брейкпойнте: класс и номер строки
 */
public class BreakpointInfo {

    private final String className;
    private final int lineNumber;

    public BreakpointInfo(String className, int lineNumber) {
        this.className = className;
        this.lineNumber = lineNumber;
    }

    public String getClassName() {
        return className;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BreakpointInfo that)) return false;
        return lineNumber == that.lineNumber && Objects.equals(className, that.className);
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
