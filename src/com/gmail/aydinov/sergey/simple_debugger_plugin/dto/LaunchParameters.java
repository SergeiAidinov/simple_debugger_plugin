package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.List;

public class LaunchParameters {
    private final String mainClass;
    private final List<String> options;
    private final int debugPort;

    public LaunchParameters(String mainClass, List<String> options, int debugPort) {
        if (mainClass == null || mainClass.isBlank()) {
            throw new IllegalArgumentException("Main class cannot be null or empty");
        }
        this.mainClass = mainClass;
        this.options = options;
        this.debugPort = debugPort;
    }

    public String getMainClass() {
        return mainClass;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getDebugPort() {
        return debugPort;
    }
}

