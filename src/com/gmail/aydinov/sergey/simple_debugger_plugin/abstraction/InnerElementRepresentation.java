package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.UUID;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;

/**
 * Представление внутреннего элемента (поле, метод, локальная переменная) для UI.
 */
public class InnerElementRepresentation {

    private final UUID uniqueId;
    private final UUID parentUniqueId;
    private final String elementName;
    private final String fullQualifiedName;
    private final UniversalElementType elementType;
    private final String value;

    public InnerElementRepresentation(UUID uniqueId, UUID parentUniqueId, String elementName, String fullQualifiedName,
                                      UniversalElementType elementType, String value) {
        this.uniqueId = uniqueId;
        this.parentUniqueId = parentUniqueId;
        this.elementName = elementName;
        this.fullQualifiedName = fullQualifiedName;
        this.elementType = elementType;
        this.value = value;
    }

    public UUID getUniqueId() { return uniqueId; }
    public UUID getParentUniqueId() {return parentUniqueId;}
    public String getElementName() { return elementName; }
    public String getFullQualifiedName() { return fullQualifiedName; }
    public UniversalElementType getElementType() { return elementType; }
    public String getValue() { return value; }

    @Override
    public String toString() {
        return "InnerElementRepresentation [name=" + elementName +
               ", type=" + elementType + ", value=" + value + "]";
    }
}