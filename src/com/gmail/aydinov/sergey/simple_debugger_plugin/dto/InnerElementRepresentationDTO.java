package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.UUID;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;

/**
 * DTO для внутреннего элемента (поле / метод) для передачи в UI.
 */
public class InnerElementRepresentationDTO {

	private final UUID uniqueId;
    private final String name;
    private final String typeName;
    private final UniversalElementType elementType;
    private String value;

    public InnerElementRepresentationDTO(UniversalElementRepresentation original) {
    	this.uniqueId = original.getUniqueId();
        this.name = original.getElementName();
        this.typeName = original.getFullQualifiedName();
        this.elementType = original.getElementType();
        this.value = original.getValue();
    }

    public InnerElementRepresentationDTO(UUID uniqueId, String name, String typeName, UniversalElementType elementType,
			String value) {
    	this.uniqueId = uniqueId;
		this.name = name;
		this.typeName = typeName;
		this.elementType = elementType;
		this.value = value;
	}
    
	public UUID getUniqueId() {
		return uniqueId;
	}

	public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    public UniversalElementType getElementType() {
        return elementType;
    }

    public String getValue() {
        return value;
    }

	public void setValue(String newValStr) {
		value = newValStr;
		
	}
}