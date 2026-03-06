package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import java.util.ArrayList;
import java.util.Map;

public class UserInstanceInspectionDTO {
	
	private final  String fieldName;
	private final String typeName;
	private final Map<Integer, ArrayList<UserInstanceInnerElementInspectionDTO>> innerElementsByGroups;
	
	public UserInstanceInspectionDTO(String fieldName, String typeName,
			Map<Integer, ArrayList<UserInstanceInnerElementInspectionDTO>> innerElementsByGroups) {
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

	public Map<Integer, ArrayList<UserInstanceInnerElementInspectionDTO>> getInnerElementsByGroups() {
		return innerElementsByGroups;
	}
}
