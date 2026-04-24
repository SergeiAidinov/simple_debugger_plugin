package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;

public class UserInstanceDetailsDTO {
	
	private final Tag tag;
	private final  String fieldName;
	private final String typeName;
	private final Map<Integer, ArrayList<UserElementDetailDTO>> innerElementsByGroups;
	private final Long objectId;
	
	public UserInstanceDetailsDTO(Tag tag, String fieldName, String typeName,
			Map<Integer, ArrayList<UserElementDetailDTO>> innerElementsByGroups, Long objectId) {
		this.tag = tag;
		this.fieldName = fieldName;
		this.typeName = typeName;
		this.innerElementsByGroups = innerElementsByGroups;
		this.objectId = objectId;
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

	public Long getObjectId() {
		return objectId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fieldName, innerElementsByGroups, objectId, tag, typeName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserInstanceDetailsDTO other = (UserInstanceDetailsDTO) obj;
		return Objects.equals(fieldName, other.fieldName)
				&& Objects.equals(innerElementsByGroups, other.innerElementsByGroups)
				&& Objects.equals(objectId, other.objectId) && Objects.equals(tag, other.tag)
				&& Objects.equals(typeName, other.typeName);
	}
	
	
}
