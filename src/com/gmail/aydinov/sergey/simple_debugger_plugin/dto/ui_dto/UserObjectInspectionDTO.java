package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import java.util.List;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;

public class UserObjectInspectionDTO {

	private final Tag tag;

	private final String className;
	private final String stringValue; // результат toString()

	private final List<InnerElementRepresentationDTO> instanceFields;
	private final List<InnerElementRepresentationDTO> staticFields;

	private final List<InnerElementRepresentationDTO> instanceMethods;
	private final List<InnerElementRepresentationDTO> staticMethods;

	public UserObjectInspectionDTO(Tag tag, String className, String stringValue,
			List<InnerElementRepresentationDTO> instanceFields, List<InnerElementRepresentationDTO> staticFields,
			List<InnerElementRepresentationDTO> instanceMethods, List<InnerElementRepresentationDTO> staticMethods) {
		this.tag = tag;
		this.className = className;
		this.stringValue = stringValue;
		this.instanceFields = instanceFields;
		this.staticFields = staticFields;
		this.instanceMethods = instanceMethods;
		this.staticMethods = staticMethods;
	}

	public Tag getTag() {
		return tag;
	}

	public String getClassName() {
		return className;
	}

	public String getStringValue() {
		return stringValue;
	}

	public List<InnerElementRepresentationDTO> getInstanceFields() {
		return instanceFields;
	}

	public List<InnerElementRepresentationDTO> getStaticFields() {
		return staticFields;
	}

	public List<InnerElementRepresentationDTO> getInstanceMethods() {
		return instanceMethods;
	}

	public List<InnerElementRepresentationDTO> getStaticMethods() {
		return staticMethods;
	}

	public static class Builder {

		private Builder() {
		}

		private Tag tag;

		private String className;
		private String stringValue;

		private List<InnerElementRepresentationDTO> instanceFields;
		private List<InnerElementRepresentationDTO> staticFields;

		private List<InnerElementRepresentationDTO> instanceMethods;
		private List<InnerElementRepresentationDTO> staticMethods;

		public Builder tag(Tag tag) {
			this.tag = tag;
			return this;
		}

		public Builder className(String className) {
			this.className = className;
			return this;
		}

		public Builder stringValue(String stringValue) {
			this.stringValue = stringValue;
			return this;
		}

		public Builder instanceFields(List<InnerElementRepresentationDTO> instanceFields) {
			this.instanceFields = instanceFields;
			return this;
		}

		public Builder staticFields(List<InnerElementRepresentationDTO> staticFields) {
			this.staticFields = staticFields;
			return this;
		}

		public Builder instanceMethods(List<InnerElementRepresentationDTO> instanceMethods) {
			this.instanceMethods = instanceMethods;
			return this;
		}

		public Builder staticMethods(List<InnerElementRepresentationDTO> staticMethods) {
			this.staticMethods = staticMethods;
			return this;
		}

		public UserObjectInspectionDTO build() {
			return new UserObjectInspectionDTO(tag, className, stringValue, instanceFields, staticFields,
					instanceMethods, staticMethods);
		}
	}

	public static Builder builder() {
		return new Builder();
	}
}
