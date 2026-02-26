package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.render;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugValueRenderer;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugWindowDataDTO;

public final class FieldValueRenderer implements DebugValueRenderer {

    @Override
    public String render(DebugWindowDataDTO dto) {
        //Object value = dto.getRuntimeValue(); // когда появится
    	Object value = null;
        return value != null ? value.toString() : "null";
    }
}
