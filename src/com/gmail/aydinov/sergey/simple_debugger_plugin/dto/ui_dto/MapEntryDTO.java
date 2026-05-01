package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import java.util.List;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;

public class MapEntryDTO {
	
	private final String value;
	private final long objectId;
	private final ValueCategory valueCategory;
	private final List<InnerElementRepresentationDTO> elements;

	public MapEntryDTO(String value, long objectId, ValueCategory valueCategory, List<InnerElementRepresentationDTO> elements) {
		this.value = value;
		this.objectId = objectId;
		this.valueCategory = valueCategory;
		this.elements = elements;
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

}
