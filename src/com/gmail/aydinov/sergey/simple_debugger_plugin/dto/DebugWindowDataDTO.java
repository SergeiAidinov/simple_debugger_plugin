package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;

/**
 * DTO для передачи данных в UI без передачи оригинальных JDI-объектов.
 * Создаётся на основе UniversalElementRepresentation.
 */
public class DebugWindowDataDTO {

    private ReferenceType referenceType; // внутреннее использование
    private UUID uniqueId;
    private String elementName;
    private UniversalElementType elementType;
    private String qualifiedTypeName;
    private Set<InnerElementRepresentationDTO> innerElements;

    private int lineNumber = -1;
    private List<MethodCallInStackDTO> stackCall = List.of();
    private String methodName = "[NO METHOD]";

    // ===== Конструктор =====
    public DebugWindowDataDTO(UniversalElementRepresentation element, Location location) {
        if (element != null) {
            this.referenceType = element.getReferenceType();
            this.uniqueId = element.getUniqueId();
            this.elementName = element.getElementName();
            this.elementType = element.getElementType();
            this.qualifiedTypeName = element.getFullQualifiedName();

            this.innerElements = element.getInnerElements() == null
                    ? Set.of()
                    : element.getInnerElements().stream()
                        .filter(DebugWindowDataDTO::isSupportedInnerElement)
                        .map(DebugWindowDataDTO::toInnerRepresentation)
                        .collect(Collectors.toSet());
        } else {
            this.innerElements = Set.of();
        }

        if (location != null) {
            this.lineNumber = location.lineNumber();
            this.methodName = location.method().name() + "(..)";
        }
    }

    // ===== Фильтрация допустимых элементов =====
    private static boolean isSupportedInnerElement(UniversalElementRepresentation e) {
        return e.getElementType() == UniversalElementType.METHOD
            || e.getElementType() == UniversalElementType.STATIC_FIELD
            || e.getElementType() == UniversalElementType.NON_STATIC_FIELD
            || e.getElementType() == UniversalElementType.VARIABLE;
    }

    // ===== Маппинг UniversalElementRepresentation → InnerElementRepresentation =====
    private static InnerElementRepresentationDTO toInnerRepresentation(UniversalElementRepresentation e) {
        return new InnerElementRepresentationDTO(
                e.getUniqueId(),
                e.getUniqueId(),
                e.getElementName(),
                e.getFullQualifiedName(),
                e.getElementType(),
                e.getValue()
        );
    }

    // ===== Getters =====
    public ReferenceType getReferenceType() { return referenceType; }
    public UUID getUniqueId() { return uniqueId;}
    public String getElementName() { return elementName; }
    public UniversalElementType getElementType() { return elementType; }
    public String getQualifiedTypeName() { return qualifiedTypeName; }
    public Set<InnerElementRepresentationDTO> getInnerElements() { return innerElements; }
    public int getLineNumber() { return lineNumber; }
    public List<MethodCallInStackDTO> getStackCall() { return stackCall; }
    public String getMethodName() { return methodName; }

    public void setStackCall(List<MethodCallInStackDTO> stackCall) { this.stackCall = stackCall; }
}