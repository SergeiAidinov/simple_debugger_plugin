package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import com.sun.jdi.ReferenceType;

public abstract class AbstractElementRepresentation {
	
	public enum ElementType {
		/** Represents an interface in the target application */
		INTERFACE,

		/** Represents a class in the target application */
		CLASS,
		
		ENUM,
		STATIC_FIELD,
		NON_STATIC_FIELD,
		METHOD
	}
	
	private final ReferenceType referenceType;
	private final String elementName;
	private final String fullQualifiedName;
	private final ElementType elementType;
	public AbstractElementRepresentation(ReferenceType referenceType, String elementName, String fullQualifiedName, ElementType elementType) {
		this.referenceType = referenceType;
		this.elementName = elementName;
		this.fullQualifiedName = fullQualifiedName;
		this.elementType = elementType;
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
	public ElementType getElementType() {
		return elementType;
	}
	
}
