package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import com.sun.jdi.ReferenceType;

public abstract class AbstractInnerElementRepresentation extends AbstractElementRepresentation{
	
	public enum InnerElementType {
		STATIC_FIELD,
		NON_STATIC_FIELD,
		METHOD
	}
	
	private final InnerElementType elementType;
	
	
	public AbstractInnerElementRepresentation(ReferenceType outerElementReference, String elementName, String fullQualifiedName, InnerElementType elementType) {
		super(outerElementReference, elementName, fullQualifiedName);
		this.elementType = elementType;
	}
	
	public InnerElementType getElementType() {
		return elementType;
	}
}
