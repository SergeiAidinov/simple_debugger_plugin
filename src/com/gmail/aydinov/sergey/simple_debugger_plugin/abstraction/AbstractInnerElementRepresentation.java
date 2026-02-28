package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.EnumSet;

import com.sun.jdi.ReferenceType;

public abstract class AbstractInnerElementRepresentation
        extends AbstractElementRepresentation {

    private static final EnumSet<TargetApplicationElementType> ALLOWED_ELEMENT_TYPES =
            EnumSet.of(
                    TargetApplicationElementType.STATIC_FIELD,
                    TargetApplicationElementType.NON_STATIC_FIELD,
                    TargetApplicationElementType.METHOD
            );

    private final TargetApplicationElementType elementType;

    protected AbstractInnerElementRepresentation(
            ReferenceType outerElementReference,
            String elementName,
            String fullQualifiedName,
            TargetApplicationElementType elementType) {

        super(outerElementReference, elementName, fullQualifiedName, elementType);

        if (!ALLOWED_ELEMENT_TYPES.contains(elementType)) {
            throw new IllegalArgumentException(
                    "ElementType " + elementType +
                    " is not allowed for inner elements"
            );
        }

        this.elementType = elementType;
    }

    public TargetApplicationElementType getElementType() {
        return elementType;
    }
}