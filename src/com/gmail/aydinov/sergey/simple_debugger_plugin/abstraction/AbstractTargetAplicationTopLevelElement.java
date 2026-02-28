package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.Set;

import com.sun.jdi.ReferenceType;

public abstract class AbstractTargetAplicationTopLevelElement extends AbstractElementRepresentation {

	/**
	 * Type of a target application element. Can be either a class or an interface.
	 * <p>
	 * Author: Sergei Aidinov <br>
	 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
	 * </p>
	 */
	public enum TargetApplicationTopLevelElementType {

		/** Represents an interface in the target application */
		INTERFACE,

		/** Represents a class in the target application */
		CLASS,
		
		ENUM
	}

	private final TargetApplicationTopLevelElementType elementType;
	private final Set<InnerElementRepresentation> innerElements;

	public AbstractTargetAplicationTopLevelElement(ReferenceType referenceType, String elementName,
			String fullQualifiedName, 
			TargetApplicationTopLevelElementType elementType, Set<InnerElementRepresentation> innerElements) {
		super(referenceType, elementName, fullQualifiedName);
		this.elementType = elementType;
		this.innerElements = innerElements;
	}

	public TargetApplicationTopLevelElementType getElementType() {
		return elementType;
	}

	public Set<InnerElementRepresentation> getInnerElements() {
		return innerElements;
	}
}
