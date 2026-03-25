package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TripletDTO;
import com.sun.jdi.ObjectReference;

public class LocalVariableShortDTO {
	
	private final String elementName;
    private final String additionalInfo;
    private final String value;
    private final ValueCategory valueCategory;
    private final String typeOrReturnType;
    private final TripletDTO<String, String, String> data;
    private final ObjectReference objectReference;
    
	public LocalVariableShortDTO(String elementName, String additionalInfo, String value, ValueCategory valueCategory, String typeOrReturnType, TripletDTO<String, String, String> data, ObjectReference objectReference) {
		this.elementName = elementName;
		this.additionalInfo = additionalInfo;
		this.value = value;
		this.valueCategory = valueCategory;
		this.typeOrReturnType = typeOrReturnType;
		this.data = data;
		this.objectReference = objectReference;
		
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
	
	public String getTypeOrReturnType() {
		return typeOrReturnType;
	}

	public TripletDTO<String, String, String> getData() {
		return data;
	}

	public ObjectReference getObjectReference() {
		return objectReference;
	}

}
