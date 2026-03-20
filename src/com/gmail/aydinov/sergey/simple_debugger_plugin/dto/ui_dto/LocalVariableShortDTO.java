package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;

public class LocalVariableShortDTO {
	
	private final String elementName;
    private final String additionalInfo;
    private final String value;
    private final ValueCategory valueCategory;
    
	public LocalVariableShortDTO(String elementName, String additionalInfo, String value, ValueCategory valueCategory) {
		this.elementName = elementName;
		this.additionalInfo = additionalInfo;
		this.value = value;
		this.valueCategory = valueCategory;
	}

	public String getElementName() {
		return elementName;
	}

	public String getAdditionalInfo() {
		return additionalInfo;
	}

	public String getValue() {
		return value;
	}

	public ValueCategory getValueCategory() {
		return valueCategory;
	}

}
