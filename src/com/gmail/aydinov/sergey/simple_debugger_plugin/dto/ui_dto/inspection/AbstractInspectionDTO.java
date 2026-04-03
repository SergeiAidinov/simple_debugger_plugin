package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;

public abstract class AbstractInspectionDTO {

    private final Tag tag;
    private final String elementName;
    private final String elementType;

    protected AbstractInspectionDTO(Tag tag, String elementName, String elementType) {
        if (tag == null) throw new IllegalArgumentException("Tag must not be null");
        this.tag = tag;
        this.elementName = elementName;
        this.elementType = elementType;
    }

    public Tag getTag() {
        return tag;
    }

    public String getElementName() {
        return elementName;
    }

    public String getElementType() {
        return elementType;
    }
}