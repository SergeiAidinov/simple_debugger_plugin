package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.render;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugValueRenderer;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugWindowDataDTO;

public final class EmptyValueRenderer implements DebugValueRenderer {

    @Override
    public String render(DebugWindowDataDTO dto) {
        return "";
    }
}
