package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details;

import java.util.ArrayList;
import java.util.Map;

public class UserInstanceDetailsDTO {
	
	private final  String fieldName;
	private final String typeName;
	private final Map<Integer, ArrayList<UserElementDetailDTO>> innerElementsByGroups;
	
	public UserInstanceDetailsDTO(String fieldName, String typeName,
			Map<Integer, ArrayList<UserElementDetailDTO>> innerElementsByGroups) {
		this.fieldName = fieldName;
		this.typeName = typeName;
		this.innerElementsByGroups = innerElementsByGroups;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getTypeName() {
		return typeName;
	}

	public Map<Integer, ArrayList<UserElementDetailDTO>> getInnerElementsByGroups() {
		return innerElementsByGroups;
	}
}
