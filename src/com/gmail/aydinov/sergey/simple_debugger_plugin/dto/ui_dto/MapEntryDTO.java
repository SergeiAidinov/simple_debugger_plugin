package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import java.util.List;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;

public class MapEntryDTO {

	private final String elementName;
    private final String value;
    private final long objectId;
    private final ValueCategory valueCategory;
    private final List<InnerElementRepresentationDTO> elements;
    private final String typeName;

    public MapEntryDTO(String elementName,
            String value,
            long objectId,
            ValueCategory valueCategory,
            List<InnerElementRepresentationDTO> elements,
            String typeName) {
    	
    	this.elementName = elementName;
        this.value = value;
        this.objectId = objectId;
        this.valueCategory = valueCategory;
        this.elements = elements;
        this.typeName = typeName;
    }

    public String getElementName() {
		return elementName;
	}

	public String getValue() {
        return value;
    }

    public long getObjectId() {
        return objectId;
    }

    public ValueCategory getValueCategory() {
        return valueCategory;
    }

    public List<InnerElementRepresentationDTO> getElements() {
        return elements;
    }

    public String getTypeName() {
        return typeName;
    }
}