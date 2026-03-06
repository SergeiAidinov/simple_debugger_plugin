package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import java.util.List;

public class UserInstanceInspectionDTO {
	
	private final  String instanceName;
	private  final List<FieldInspectionDTO> instanceElements;
	public UserInstanceInspectionDTO(String instanceName, List<FieldInspectionDTO> instanceElements) {
		super();
		this.instanceName = instanceName;
		this.instanceElements = instanceElements;
	}
	public String getInstanceName() {
		return instanceName;
	}
	public List<FieldInspectionDTO> getInstanceElements() {
		return instanceElements;
	}
	
}
