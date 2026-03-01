package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.Set;
import java.util.UUID;

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

	private final Set<InnerElementRepresentation> innerElements;

	public TopLevelElementRepresentation(UUID uniqueId, ReferenceType referenceType, String elementName, String fullQualifiedName,
			TargetApplicationElementType elementType, Set<InnerElementRepresentation> innerElements) {
		super(uniqueId, referenceType, elementName, fullQualifiedName, elementType, innerElements);
		this.innerElements = innerElements;
	}

	public Set<InnerElementRepresentation> getInnerElements() {
		return innerElements;
	}

	@Override
	public String toString() {
		return "TargetApplicationClassOrInterfaceRepresentation [getReferenceType()=" + getReferenceType()
				+ ", getElementName()=" + getElementName() + ", getElementType()=" + getElementType()
				+ ", getInnerElements()=" + getInnerElements() + ", getClass()=" + getClass() + ", hashCode()="
				+ hashCode() + ", toString()=" + super.toString() + "]";
	}
	
	

}
