package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.UUID;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;

/**
 * Представление внутреннего элемента (поле, метод, локальная переменная) для UI.
 */
public class InnerElementRepresentationDTO {

    private final UUID uniqueId;
    private final UUID parentUniqueId;
    private final String elementName;
    private final String additionalInfo;
    private final UniversalElementType elementType;
    private final String value;
    private final boolean isStatic;
    private final ValueCategory valueCategoty;

    private final String fullQualifiedName; // <- новое поле

    public InnerElementRepresentationDTO(UUID uniqueId, UUID parentUniqueId, String elementName, String additionalInfo,
                                        UniversalElementType elementType, String value, boolean isStatic,
                                        ValueCategory valueCategoty, String fullQualifiedName) {
        this.uniqueId = uniqueId;
        this.parentUniqueId = parentUniqueId;
        this.elementName = elementName;
        this.additionalInfo = additionalInfo;
        this.elementType = elementType;
        this.value = value;
        this.isStatic = isStatic;
        this.valueCategoty = valueCategoty;
        this.fullQualifiedName = fullQualifiedName;
    }

    // =================== Геттеры ===================
    public UUID getUniqueId() { return uniqueId; }
    public UUID getParentUniqueId() { return parentUniqueId; }
    public String getElementName() { return elementName; }
    public String getAdditionalInfo() { return additionalInfo; }
    public UniversalElementType getElementType() { return elementType; }
    public String getValue() { return value; }
    public ValueCategory getValueCategoty() { return valueCategoty; }
    public boolean isStatic() { return isStatic; }
    public String getFullQualifiedName() { return fullQualifiedName; } // <- геттер нового поля

    @Override
    public String toString() {
        return "InnerElementRepresentationDTO [uniqueId=" + uniqueId + ", parentUniqueId=" + parentUniqueId
                + ", elementName=" + elementName + ", additionalInfo=" + additionalInfo
                + ", elementType=" + elementType + ", value=" + value + ", isStatic=" + isStatic
                + ", valueCategoty=" + valueCategoty + ", fullQualifiedName=" + fullQualifiedName + "]";
    }
}