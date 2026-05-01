package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import java.util.List;

public class MapEntryDTO {
	
	private final List<InnerElementRepresentationDTO> elements;

	public MapEntryDTO(List<InnerElementRepresentationDTO> elements) {
		this.elements = elements;
	}

	public List<InnerElementRepresentationDTO> getElements() {
		return elements;
	}

}
