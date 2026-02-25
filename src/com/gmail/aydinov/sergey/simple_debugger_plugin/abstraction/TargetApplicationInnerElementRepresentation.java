package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.Set;

import com.sun.jdi.ReferenceType;

public class TargetApplicationInnerElementRepresentation extends AbstractTargetAplicationElement {
	
	private TargetApplicationInnerElementRepresentation(ReferenceType referenceType, String elementName,
			TargetApplicationElementType elementType, Set<TargetApplicationInnerElementRepresentation> innerElements) {
		super(referenceType, elementName, elementType, innerElements);
	}

	public static TargetApplicationInnerElementRepresentation createInnerElement(ReferenceType referenceType,
			String elementName, TargetApplicationElementType elementType,
			Set<TargetApplicationInnerElementRepresentation> innerElements) {

		return new TargetApplicationInnerElementRepresentation(referenceType, elementName, elementType, innerElements);
	}
}
