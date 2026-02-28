package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.EnumSet;

import com.sun.jdi.ReferenceType;

public abstract class AbstractInnerElementRepresentation
        extends AbstractElementRepresentation {

    private static final EnumSet<ElementType> ALLOWED_ELEMENT_TYPES =
            EnumSet.of(
                    ElementType.STATIC_FIELD,
                    ElementType.NON_STATIC_FIELD,
                    ElementType.METHOD
            );

    private final ElementType elementType;

    protected AbstractInnerElementRepresentation(
            ReferenceType outerElementReference,
            String elementName,
            String fullQualifiedName,
            ElementType elementType) {

        super(outerElementReference, elementName, fullQualifiedName, elementType);

        if (!ALLOWED_ELEMENT_TYPES.contains(elementType)) {
            throw new IllegalArgumentException(
                    "ElementType " + elementType +
                    " is not allowed for inner elements"
            );
        }

        this.elementType = elementType;
    }

    public ElementType getElementType() {
        return elementType;
    }
}