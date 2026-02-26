package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

public interface DebugValueRenderable {
    /**
     * @return human-readable value for debugger UI
     */
    String renderValue();
}