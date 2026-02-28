package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import com.sun.jdi.ReferenceType;

public abstract class AbstractElementRepresentation {
	
	private final ReferenceType referenceType;
	private final String elementName;
	private final String fullQualifiedName;
	public AbstractElementRepresentation(ReferenceType referenceType, String elementName, String fullQualifiedName) {
		this.referenceType = referenceType;
		this.elementName = elementName;
		this.fullQualifiedName = fullQualifiedName;
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
	
}
