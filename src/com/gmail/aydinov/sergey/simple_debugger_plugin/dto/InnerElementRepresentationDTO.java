package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.InnerElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.TargetApplicationElementType;

/**
 * DTO для внутреннего элемента (поле / метод) для передачи в UI.
 */
public class InnerElementRepresentationDTO {

    private final String name;
    private final String typeName;
    private final TargetApplicationElementType elementType;
    private String value;

    public InnerElementRepresentationDTO(InnerElementRepresentation original) {
        this.name = original.getElementName();
        this.typeName = original.getFullQualifiedName();
        this.elementType = original.getElementType();
        this.value = original.getValue();
    }

    public InnerElementRepresentationDTO(String name, String typeName, TargetApplicationElementType elementType,
			String value) {
		this.name = name;
		this.typeName = typeName;
		this.elementType = elementType;
		this.value = value;
	}

	public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    public TargetApplicationElementType getElementType() {
        return elementType;
    }

    public String getValue() {
        return value;
    }

	public void setValue(String newValStr) {
		value = newValStr;
		
	}
}