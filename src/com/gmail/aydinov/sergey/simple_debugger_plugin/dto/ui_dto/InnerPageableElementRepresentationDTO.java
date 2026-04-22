package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;

public class InnerPageableElementRepresentationDTO extends InnerElementRepresentationDTO {
	
	private final int offset;
	private final int limit;

	public InnerPageableElementRepresentationDTO(Tag tag, String elementName, String additionalInfo,
			UniversalElementType elementType, String value, boolean isStatic, ValueCategory valueCategory,
			String typeOrReturnType, int level, int offset, int limit) {
		super(tag, elementName, additionalInfo, elementType, value, isStatic, valueCategory, typeOrReturnType, level);
		this.offset = offset;
		this.limit = limit;
	}

	public int getOffset() {
		return offset;
	}

	public int getLimit() {
		return limit;
	}
}
