package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractTargetAplicationElement.TargetApplicationElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugWindowDataDTO;

/**
 * Adapter DTO to display DebugWindowDataDTO in the table.
 */
public class DebugWindowDataDTOEntry {

    private final String name;
    private final TargetApplicationElementType elementType;

    public DebugWindowDataDTOEntry(DebugWindowDataDTO dto) {
        this.name = dto.getElementName();
        this.elementType = dto.getElementType();
    }

    public String getName() {
        return name;
    }

    public TargetApplicationElementType getElementType() {
        return elementType;
    }

    @Override
    public String toString() {
        return "DebugWindowDataDTOEntry [name=" + name + ", elementType=" + elementType + "]";
    }
}