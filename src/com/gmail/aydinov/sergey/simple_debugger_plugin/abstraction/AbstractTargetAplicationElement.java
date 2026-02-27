package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.Set;

import com.sun.jdi.ReferenceType;

public abstract class AbstractTargetAplicationElement {

	/**
	 * Type of a target application element. Can be either a class or an interface.
	 * <p>
	 * Author: Sergei Aidinov <br>
	 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
	 * </p>
	 */
	public enum TargetApplicationElementType {

		/** Represents an interface in the target application */
		INTERFACE,

		/** Represents a class in the target application */
		CLASS,

		NON_STATIC_FIELD,

		METHOD,

		VARIABLE, FIELD
	}

	public static final Set<TargetApplicationElementType> OUTER_TYPES =
	        Set.of(TargetApplicationElementType.INTERFACE, TargetApplicationElementType.CLASS);

	private final ReferenceType referenceType;
	private final String elementName;
	private final TargetApplicationElementType elementType;
	private final Set<TargetApplicationInnerElementRepresentation> innerElements;

	protected AbstractTargetAplicationElement(ReferenceType referenceType, String elementName,
			TargetApplicationElementType elementType, Set<TargetApplicationInnerElementRepresentation> innerElements) {
		this.referenceType = referenceType;
		this.elementName = elementName;
		this.elementType = elementType;
		this.innerElements = innerElements;
	}

	public ReferenceType getReferenceType() {
		return referenceType;
	}

	public String getElementName() {
		return elementName;
	}

	public TargetApplicationElementType getElementType() {
		return elementType;
	}

	public Set<TargetApplicationInnerElementRepresentation> getInnerElements() {
		return innerElements;
	}
}
