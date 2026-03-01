package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.UUID;

import com.sun.jdi.ReferenceType;

public abstract class AbstractElementRepresentation {

	public enum TargetApplicationElementType {
		/** Represents an interface in the target application */
		INTERFACE,

		/** Represents a class in the target application */
		CLASS,

		ENUM, STATIC_FIELD, NON_STATIC_FIELD, METHOD, VARIABLE
	}

	private final UUID uniqueId;
	private final ReferenceType referenceType;
	private final String elementName;
	private final String fullQualifiedName;
	private final TargetApplicationElementType elementType;

	public AbstractElementRepresentation(UUID uniqueId, ReferenceType referenceType, String elementName, String fullQualifiedName,
			TargetApplicationElementType elementType) {
		this.uniqueId = uniqueId;
		this.referenceType = referenceType;
		this.elementName = elementName;
		this.fullQualifiedName = fullQualifiedName;
		this.elementType = elementType;
	}

	
	public UUID getUniqueId() {
		return uniqueId;
	}

	public ReferenceType getReferenceType() {
		return referenceType;
	}

	public String getElementName() {
		return elementName;
	}

	public String getFullQualifiedName() {
		return fullQualifiedName;
	}

	public TargetApplicationElementType getElementType() {
		return elementType;
	}

}
