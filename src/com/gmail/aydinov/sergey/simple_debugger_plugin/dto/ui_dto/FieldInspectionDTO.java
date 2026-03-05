package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;

public class FieldInspectionDTO {

	private final String fieldName;
	private final String type;
	private final String value;
	private final List<String> methods;

	public FieldInspectionDTO(String fieldName, String type, String value, List<String> methods) {
		this.fieldName = fieldName;
		this.type = type;
		this.value = value;
		this.methods = methods != null ? methods : Collections.emptyList();
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getType() {
		return type;
	}

	public String getValue() {
		return value;
	}

	public List<String> getMethods() {
		return methods;
	}
	
	

	@Override
	public String toString() {
		return "FieldInspectionDTO [fieldName=" + fieldName + ", type=" + type + ", value=" + value + ", methods="
				+ methods + "]";
	}



	public static class FieldInspectionDTOFactory {

		public static FieldInspectionDTO fromUniversalElement(UniversalElementRepresentation element) {
			if (element == null)
				return null;

			String fieldName = element.getElementName();
			String type = element.gettypeOrReturnType();
			String value = element.getValue();

			List<String> methods = Collections.emptyList();

			// Если это объект, то можно попытаться получить методы класса через
			// ReferenceType
			if (element.getValueCategory() == UniversalElementRepresentation.ValueCategory.USER_OBJECT
					&& element.getReferenceType() != null) {
				try {
					methods = element.getReferenceType().methods().stream().map(Method::name)
							.collect(Collectors.toList());
				} catch (Exception e) {
					methods = Collections.emptyList(); // на всякий случай
				}
			}

			return new FieldInspectionDTO(fieldName, type, value, methods);
		}

	}
}
