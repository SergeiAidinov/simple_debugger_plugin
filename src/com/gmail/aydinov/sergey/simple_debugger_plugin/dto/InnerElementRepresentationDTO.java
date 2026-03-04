package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;

/**
 * Представление внутреннего элемента (поле, метод, локальная переменная) для UI.
 * Адаптировано под новый Tag с fullQualifiedName.
 */
public class InnerElementRepresentationDTO {

    private final String uniqueId;       // <- теперь строка
    private final String parentUniqueId; // <- тоже строка
    private final String elementName;
    private final String additionalInfo;
    private final UniversalElementType elementType;
    private final String value;
    private final boolean isStatic;
    private final ValueCategory valueCategory;

    private final String fullQualifiedName;

    public InnerElementRepresentationDTO(String uniqueId,
                                        String parentUniqueId,
                                        String elementName,
                                        String additionalInfo,
                                        UniversalElementType elementType,
                                        String value,
                                        boolean isStatic,
                                        ValueCategory valueCategory,
                                        String fullQualifiedName) {
        this.uniqueId = uniqueId;
        this.parentUniqueId = parentUniqueId;
        this.elementName = elementName;
        this.additionalInfo = additionalInfo;
        this.elementType = elementType;
        this.value = value;
        this.isStatic = isStatic;
        this.valueCategory = valueCategory;
        this.fullQualifiedName = fullQualifiedName;
    }

    // =================== Геттеры ===================
    public String getUniqueId() { return uniqueId; }
    public String getParentUniqueId() { return parentUniqueId; }
    public String getElementName() { return elementName; }
    public String getAdditionalInfo() { return additionalInfo; }
    public UniversalElementType getElementType() { return elementType; }
    public String getValue() { return value; }
    public ValueCategory getValueCategory() { return valueCategory; }
    public boolean isStatic() { return isStatic; }
    public String getFullQualifiedName() { return fullQualifiedName; }

    @Override
    public String toString() {
        return "InnerElementRepresentationDTO [uniqueId=" + uniqueId + ", parentUniqueId=" + parentUniqueId
                + ", elementName=" + elementName + ", additionalInfo=" + additionalInfo
                + ", elementType=" + elementType + ", value=" + value + ", isStatic=" + isStatic
                + ", valueCategory=" + valueCategory + ", fullQualifiedName=" + fullQualifiedName + "]";
    }
}