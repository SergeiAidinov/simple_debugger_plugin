package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.TargetApplicationElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.InnerElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TopLevelElementRepresentation;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;

/**
 * DTO для передачи данных в UI без передачи оригинальных JDI-объектов.
 */
public class DebugWindowDataDTO {

	
    private ReferenceType referenceType; // оставляем для внутреннего использования
    private String elementName;
    private TargetApplicationElementType elementType;
    private String qualifiedTypeName;
    private Set<InnerElementRepresentationDTO> innerElements;
    private int lineNumber = -1;
    private List<MethodCallInStackDTO> stackCall;
    private String methodName = "[NO METHOD]";

    // Конструктор из anchorElement и Location
    public DebugWindowDataDTO(TopLevelElementRepresentation anchorElement, Location location) {
        if (anchorElement != null) {
            this.referenceType = anchorElement.getReferenceType();
            this.elementName = anchorElement.getElementName();
            this.elementType = anchorElement.getElementType();
            this.qualifiedTypeName = anchorElement.getFullQualifiedName();
            this.innerElements = anchorElement.getInnerElements() != null
                    ? anchorElement.getInnerElements().stream()
                        .map(InnerElementRepresentationDTO::new)
                        .collect(Collectors.toSet())
                    : Set.of();
        }
        if (location != null) {
            this.lineNumber = location.lineNumber();
            this.methodName = location.method().name() + "(..)";
        }
        this.stackCall = List.of(); // можно позже заполнить DebugUtils.compileStackInfo()
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

    public String getQualifiedTypeName() {
        return qualifiedTypeName;
    }

    public Set<InnerElementRepresentationDTO> getInnerElements() {
        return innerElements;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public List<MethodCallInStackDTO> getStackCall() {
        return stackCall;
    }

    public String getMethodName() {
        return methodName;
    }

    // Сеттеры при необходимости
    public void setStackCall(List<MethodCallInStackDTO> stackCall) {
        this.stackCall = stackCall;
    }
}