package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.EnumSet;
import java.util.Set;

import com.sun.jdi.ReferenceType;

public abstract class AbstractTargetAplicationTopLevelElement extends AbstractElementRepresentation {

	private static final EnumSet<ElementType> ALLOWED_TOP_LEVEL_TYPES = EnumSet.of(ElementType.CLASS,
			ElementType.INTERFACE, ElementType.ENUM);

	private final Set<InnerElementRepresentation> innerElements;

	protected AbstractTargetAplicationTopLevelElement(ReferenceType referenceType, String elementName,
			String fullQualifiedName, ElementType elementType, Set<InnerElementRepresentation> innerElements) {

		super(referenceType, elementName, fullQualifiedName, elementType);

		if (!ALLOWED_TOP_LEVEL_TYPES.contains(elementType)) {
			throw new IllegalArgumentException("Invalid top-level element type: " + elementType);
		}

		this.innerElements = innerElements;
	}

	public Set<InnerElementRepresentation> getInnerElements() {
		return innerElements;
	}
}