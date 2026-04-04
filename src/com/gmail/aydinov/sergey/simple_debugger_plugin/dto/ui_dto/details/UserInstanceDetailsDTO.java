package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details;

import java.util.ArrayList;
import java.util.Map;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;

public class UserInstanceDetailsDTO {
	
	private final Tag tag;
	private final  String fieldName;
	private final String typeName;
	private final Map<Integer, ArrayList<UserElementDetailDTO>> innerElementsByGroups;
	
	public UserInstanceDetailsDTO(Tag tag, String fieldName, String typeName,
			Map<Integer, ArrayList<UserElementDetailDTO>> innerElementsByGroups) {
		this.tag = tag;
		this.fieldName = fieldName;
		this.typeName = typeName;
		this.innerElementsByGroups = innerElementsByGroups;
	}
	
	public Tag getTag() {
		return tag;
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
