package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.Set;
import java.util.UUID;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;

/**
 * Представление top-level элемента (класс, интерфейс, enum) для UI.
 */
public class TopLevelElementRepresentation {

    private final UUID uniqueId;
    private final String elementName;
    private final String fullQualifiedName;
    private final UniversalElementType elementType;
    private final Set<InnerElementRepresentation> innerElements;

    public TopLevelElementRepresentation(UUID uniqueId, String elementName, String fullQualifiedName,
                                         UniversalElementType elementType, Set<InnerElementRepresentation> innerElements) {
        this.uniqueId = uniqueId;
        this.elementName = elementName;
        this.fullQualifiedName = fullQualifiedName;
        this.elementType = elementType;
        this.innerElements = innerElements;
    }

    public UUID getUniqueId() { return uniqueId; }
    public String getElementName() { return elementName; }
    public String getFullQualifiedName() { return fullQualifiedName; }
    public UniversalElementType getElementType() { return elementType; }
    public Set<InnerElementRepresentation> getInnerElements() { return innerElements; }

    @Override
    public String toString() {
        return "TopLevelElementRepresentation [name=" + elementName +
               ", type=" + elementType + ", innerElements=" + innerElements + "]";
    }
}