package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details;

public class UserElementDetailDTO {
	
	private final String name;
	private final String typeOrReturnType;
	private final String value;
	
	public UserElementDetailDTO(String name, String typeOrReturnType, String value) {
		super();
		this.name = name;
		this.typeOrReturnType = typeOrReturnType;
		this.value = value;
	}
	public String getName() {
		return name;
	}
	public String getTypeOrReturnType() {
		return typeOrReturnType;
	}
	public String getValue() {
		return value;
	}
}
