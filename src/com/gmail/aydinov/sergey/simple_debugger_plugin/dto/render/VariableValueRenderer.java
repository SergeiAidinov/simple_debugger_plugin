package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.render;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugValueRenderer;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugWindowDataDTO;

/**
 * Renders value of a local variable or parameter.
 */
public final class VariableValueRenderer implements DebugValueRenderer {

    public static final VariableValueRenderer INSTANCE = new VariableValueRenderer();
    
    public VariableValueRenderer() {
    }

    @Override
    public String render(DebugWindowDataDTO dto) {
       // Object value = dto.getRuntimeValue(); // когда появится
        Object value = null;
        if (value == null) {
            return "null";
        }

        // базовый safe-render
        try {
            return value.toString();
        } catch (Exception ex) {
            return "<unavailable>";
        }
    }
}