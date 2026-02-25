package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.Set;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractTargetAplicationElement;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationClassOrInterfaceRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationInnerElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractTargetAplicationElement.TargetApplicationElementType;

public final class DebugWindowDataDTO {

    private final String elementName;
    private final TargetApplicationElementType elementType;
    private final Set<DebugWindowDataDTO> innerElements;

    private DebugWindowDataDTO(String elementName,
                                        TargetApplicationElementType elementType,
                                        Set<DebugWindowDataDTO> innerElements) {
        this.elementName = elementName;
        this.elementType = elementType;
        this.innerElements = innerElements;
    }

    public String getElementName() {
        return elementName;
    }

    public TargetApplicationElementType getElementType() {
        return elementType;
    }

    public Set<DebugWindowDataDTO> getInnerElements() {
        return innerElements;
    }

    /**
     * Фабричный метод для top-level элемента
     */
    public static DebugWindowDataDTO from(
            TargetApplicationClassOrInterfaceRepresentation source) {

        return buildFromAbstract(source);
    }

    /**
     * Фабричный метод для inner элемента
     */
    public static DebugWindowDataDTO from(
            TargetApplicationInnerElementRepresentation source) {

        return buildFromAbstract(source);
    }

    private static DebugWindowDataDTO buildFromAbstract(
            AbstractTargetAplicationElement source) {

        Set<DebugWindowDataDTO> innerDtos =
                source.getInnerElements().stream()
                        .map(DebugWindowDataDTO::fromAny)
                        .collect(Collectors.toSet());

        return new DebugWindowDataDTO(
                source.getElementName(),
                source.getElementType(),
                innerDtos
        );
    }

    private static DebugWindowDataDTO fromAny(
            AbstractTargetAplicationElement element) {

        if (element instanceof TargetApplicationClassOrInterfaceRepresentation top) {
            return from(top);
        }

        if (element instanceof TargetApplicationInnerElementRepresentation inner) {
            return from(inner);
        }

        throw new IllegalArgumentException("Unknown element type: " + element.getClass());
    }
}