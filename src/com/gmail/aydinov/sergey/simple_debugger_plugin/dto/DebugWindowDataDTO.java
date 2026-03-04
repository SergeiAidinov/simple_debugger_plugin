package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;

/**
 * DTO для передачи данных в UI без прямой передачи JDI-объектов.
 * Создаётся на основе UniversalElementRepresentation.
 */
public class DebugWindowDataDTO {

    private ReferenceType referenceType; 
    private String elementName;
    private UniversalElementType elementType;
    private String additionalInfo;
    private Set<InnerElementRepresentationDTO> innerElements;
    private final boolean isStatic;
    private int lineNumber = -1;
    private List<MethodCallInStackDTO> stackCall = List.of();
    private String methodName = "[NO METHOD]";
    private final ValueCategory valueCategory;

    private final String fullQualifiedName; // уникальный идентификатор

    // ===== Конструктор =====
    public DebugWindowDataDTO(UniversalElementRepresentation element, Location location) {
        if (element != null) {
            this.referenceType = element.getReferenceType();
            this.elementName = element.getElementName();
            this.elementType = element.getTag().getElementType();
            this.additionalInfo = element.getAdditionalInfo();
            this.fullQualifiedName = element.getTag().getFullQualifiedName();

            this.innerElements = element.getInnerElements() == null
                    ? Set.of()
                    : element.getInnerElements().stream()
                        .filter(DebugWindowDataDTO::isSupportedInnerElement)
                        .map(DebugWindowDataDTO::toInnerRepresentation)
                        .collect(Collectors.toSet());
        } else {
            this.innerElements = Set.of();
            this.fullQualifiedName = "";
            this.elementType = null;
        }

        if (location != null) {
            this.lineNumber = location.lineNumber();
            Method method = location.method();
            this.methodName = method != null ? method.name() + "(..)" : "[NO METHOD]";
        }

        this.isStatic = element != null && element.isStatic();
        this.valueCategory = element != null ? element.getValueCategory() : ValueCategory.UNKNOWN;
    }

    // ===== Фильтрация допустимых inner элементов =====
    private static boolean isSupportedInnerElement(UniversalElementRepresentation e) {
        return e.getElementType() == UniversalElementType.METHOD
            || e.getElementType() == UniversalElementType.STATIC_FIELD
            || e.getElementType() == UniversalElementType.NON_STATIC_FIELD
            || e.getElementType() == UniversalElementType.VARIABLE;
    }

    // ===== Маппинг UniversalElementRepresentation → InnerElementRepresentationDTO =====
    public static InnerElementRepresentationDTO toInnerRepresentation(UniversalElementRepresentation e) {
        // Используем fullQualifiedName из Tag как uniqueId, а из элемента как parentUniqueId
        return new InnerElementRepresentationDTO(
                e.getTag().getFullQualifiedName(),  // uniqueId
                e.getFullQualifiedName(),           // parentUniqueId
                e.getElementName(),
                e.getAdditionalInfo(),
                e.getTag().getElementType(),
                e.getValue(),
                e.isStatic(),
                e.getValueCategory(),
                e.getTag().getFullQualifiedName()   // полное имя
        );
    }

    // ===== Getters =====
    public ReferenceType getReferenceType() { return referenceType; }
    public String getElementName() { return elementName; }
    public UniversalElementType getElementType() { return elementType; }
    public String getAdditionalInfo() { return additionalInfo; }
    public Set<InnerElementRepresentationDTO> getInnerElements() { return innerElements; }
    public boolean isStatic() { return isStatic; }
    public int getLineNumber() { return lineNumber; }
    public List<MethodCallInStackDTO> getStackCall() { return stackCall; }
    public void setStackCall(List<MethodCallInStackDTO> stackCall) { this.stackCall = stackCall; }
    public String getMethodName() { return methodName; }
    public ValueCategory getValueCategory() { return valueCategory; }
    public String getFullQualifiedName() { return fullQualifiedName; }
}