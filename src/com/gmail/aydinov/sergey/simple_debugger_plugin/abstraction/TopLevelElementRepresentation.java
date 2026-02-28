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
public class TopLevelElementRepresentation extends AbstractTargetAplicationTopLevelElement {

	Set<InnerElementRepresentation> innerElements;

	public TopLevelElementRepresentation(ReferenceType referenceType, String elementName, String fullQualifiedName,
			ElementType elementType, Set<InnerElementRepresentation> innerElements) {
		super(referenceType, elementName, fullQualifiedName, elementType, innerElements);
	}

	@Override
	public String toString() {
		return "TargetApplicationClassOrInterfaceRepresentation [getReferenceType()=" + getReferenceType()
				+ ", getElementName()=" + getElementName() + ", getElementType()=" + getElementType()
				+ ", getInnerElements()=" + getInnerElements() + ", getClass()=" + getClass() + ", hashCode()="
				+ hashCode() + ", toString()=" + super.toString() + "]";
	}

}
