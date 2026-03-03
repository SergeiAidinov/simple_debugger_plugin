package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
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
    private String additionalInfo;
    private Set<InnerElementRepresentationDTO> innerElements;
    private final boolean isStatic;
    private int lineNumber = -1;
    private List<MethodCallInStackDTO> stackCall = List.of();
    private String methodName = "[NO METHOD]";
    private final ValueCategory valueCategory;

    private final String fullQualifiedName; // <- новое поле

    // ===== Конструктор =====
    public DebugWindowDataDTO(UniversalElementRepresentation element, Location location) {
        if (element != null) {
            this.referenceType = element.getReferenceType();
            this.uniqueId = element.getTag().getUniqueId();
            this.elementName = element.getElementName();
            this.elementType = element.getElementType();
            this.additionalInfo = element.getAdditionalInfo();
            this.fullQualifiedName = element.getFullQualifiedName(); // <- переносим значение

            this.innerElements = element.getInnerElements() == null
                    ? Set.of()
                    : element.getInnerElements().stream()
                        .filter(DebugWindowDataDTO::isSupportedInnerElement)
                        .map(DebugWindowDataDTO::toInnerRepresentation)
                        .collect(Collectors.toSet());
        } else {
            this.innerElements = Set.of();
            this.fullQualifiedName = "";
        }

        if (location != null) {
            this.lineNumber = location.lineNumber();
            this.methodName = location.method().name() + "(..)";
        }
        this.isStatic = element != null && element.isStatic();
        this.valueCategory = element != null ? element.getValueCategory() : ValueCategory.UNKNOWN;
       // addInnerElements(location);
       
    }

	// ===== Фильтрация допустимых элементов =====
    private static boolean isSupportedInnerElement(UniversalElementRepresentation e) {
        return e.getElementType() == UniversalElementType.METHOD
            || e.getElementType() == UniversalElementType.STATIC_FIELD
            || e.getElementType() == UniversalElementType.NON_STATIC_FIELD
            || e.getElementType() == UniversalElementType.VARIABLE;
    }

    // ===== Маппинг UniversalElementRepresentation → InnerElementRepresentationDTO =====
    public static InnerElementRepresentationDTO toInnerRepresentation(UniversalElementRepresentation e) {
        return new InnerElementRepresentationDTO(
                e.getTag().getUniqueId(),          // UUID текущего элемента
                e.getTag().getParentUniqueId(),    // UUID родителя
                e.getElementName(),
                e.getAdditionalInfo(),
                e.getElementType(),
                e.getValue(),
                e.isStatic(),
                e.getValueCategory(),
                e.getFullQualifiedName()           // полное имя класса/элемента
        );
    }

    // ===== Getters =====
    public ReferenceType getReferenceType() { return referenceType; }
    public UUID getUniqueId() { return uniqueId; }
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
    public String getFullQualifiedName() { return fullQualifiedName; } // <- геттер для нового поля
}