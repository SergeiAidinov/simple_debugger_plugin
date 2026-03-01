package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import com.sun.jdi.ReferenceType;

public abstract class AbstractTargetAplicationTopLevelElement extends AbstractElementRepresentation {

	private static final EnumSet<TargetApplicationElementType> ALLOWED_TOP_LEVEL_TYPES = EnumSet.of(
			TargetApplicationElementType.CLASS, TargetApplicationElementType.INTERFACE,
			TargetApplicationElementType.ENUM);

	private final Set<InnerElementRepresentation> innerElements;

	protected AbstractTargetAplicationTopLevelElement(UUID uniqueId, ReferenceType referenceType, String elementName,
			String fullQualifiedName, TargetApplicationElementType elementType,
			Set<InnerElementRepresentation> innerElements) {

		super(uniqueId, referenceType, elementName, fullQualifiedName, elementType);

		if (!ALLOWED_TOP_LEVEL_TYPES.contains(elementType)) {
			throw new IllegalArgumentException("Invalid top-level element type: " + elementType);
		}

		this.innerElements = innerElements;
	}

	public Set<InnerElementRepresentation> getInnerElements() {
		return innerElements;
	}
}