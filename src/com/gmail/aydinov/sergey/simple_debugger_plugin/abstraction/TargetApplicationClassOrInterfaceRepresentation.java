package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.Set;

import com.sun.jdi.ReferenceType;

/**
 * Representation of a target application class or interface in the debugged
 * JVM. Contains information about its methods and fields.
 * <p>
 * Author: Sergei Aidinov <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class TargetApplicationClassOrInterfaceRepresentation extends AbstractTargetAplicationElement {

	private TargetApplicationClassOrInterfaceRepresentation(ReferenceType referenceType, String elementName,
			TargetApplicationElementType elementType, Set<TargetApplicationInnerElementRepresentation> innerElements) {
		super(referenceType, elementName, elementType, innerElements);
	}

	public static TargetApplicationClassOrInterfaceRepresentation createTopLevelElement(ReferenceType referenceType,
			String elementName, TargetApplicationElementType elementType,
			Set<TargetApplicationInnerElementRepresentation> innerElements) {
		if (!AbstractTargetAplicationElement.OUTER_TYPES.contains(elementType)) {
		    throw new IllegalArgumentException("Top-level element must be CLASS or INTERFACE");
		} else {
			return new TargetApplicationClassOrInterfaceRepresentation(referenceType, elementName, elementType,
					innerElements);
		}
	}

}
