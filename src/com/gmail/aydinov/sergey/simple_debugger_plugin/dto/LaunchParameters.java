package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the parameters required to launch a Java application in debug mode.
 */
public class LaunchParameters {

    private final String mainClass;
    private final List<String> options;
    private final int debugPort;

    /**
     * Constructs LaunchParameters.
     *
     * @param mainClass the fully qualified main class name
     * @param options   VM options or program arguments
     * @param debugPort the port for JDWP debugging
     * @throws IllegalArgumentException if mainClass is null or blank
     */
    public LaunchParameters(String mainClass, List<String> options, int debugPort) {
        if (mainClass == null || mainClass.isBlank()) {
            throw new IllegalArgumentException("Main class cannot be null or blank");
        }
        this.mainClass = mainClass;
        this.options = Objects.requireNonNullElse(options, List.of());
        this.debugPort = debugPort;
    }

    /**
     * Returns the fully qualified main class name.
     */
    public String getMainClass() {
        return mainClass;
    }

    /**
     * Returns the list of VM options or program arguments.
     */
    public List<String> getOptions() {
        return options;
    }

    /**
     * Returns the debug port for JDWP connection.
     */
    public int getDebugPort() {
        return debugPort;
    }

    @Override
    public String toString() {
        return "LaunchParameters{" +
                "mainClass='" + mainClass + '\'' +
                ", options=" + options +
                ", debugPort=" + debugPort +
                '}';
    }
}
