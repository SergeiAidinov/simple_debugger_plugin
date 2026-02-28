package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;


import com.sun.jdi.ReferenceType;

public class InnerElementRepresentation extends AbstractInnerElementRepresentation {
	
	
	public InnerElementRepresentation(ReferenceType outerElementReference, String elementName, String fullQualifiedName, ElementType elementType) {
		super(outerElementReference, elementName, fullQualifiedName, elementType);
	}
}
