package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.sun.jdi.ReferenceType;

public class UniversalElementRepresentation {

    public enum UniversalElementType {
        INTERFACE, CLASS, ENUM, STATIC_FIELD, NON_STATIC_FIELD, METHOD, VARIABLE, UNKNOWN
    }

    public enum CurrentRole {OUTER, STATIC_FIELD, NON_STATIC_FIELD, LOCAL}

    private final UUID uniqueId;
    private final UUID parentUniqueId;
    private final ReferenceType referenceType;
    private final String elementName;
    private final String fullQualifiedName;
    private final UniversalElementType elementType;
    private CurrentRole currentRole;
    private final String value;
    private final Set<UniversalElementRepresentation> innerElements = new HashSet<>();

    private UniversalElementRepresentation(UUID uniqueId, UUID parentUniqueId, ReferenceType referenceType,
                                           String elementName, String fullQualifiedName,
                                           UniversalElementType elementType, CurrentRole currentRole, String value) {
        this.uniqueId = uniqueId;
        this.parentUniqueId = parentUniqueId;
        this.referenceType = referenceType;
        this.elementName = elementName;
        this.fullQualifiedName = fullQualifiedName;
        this.elementType = elementType;
        this.currentRole = currentRole;
        this.value = value;
    }

    public CurrentRole getCurrentRole() { return currentRole; }
    public void setCurrentRole(CurrentRole currentRole) { this.currentRole = currentRole; }
    public UUID getUniqueId() { return uniqueId; }
    public UUID getParentUniqueId() { return parentUniqueId; }
    public ReferenceType getReferenceType() { return referenceType; }
    public String getElementName() { return elementName; }
    public String getFullQualifiedName() { return fullQualifiedName; }
    public UniversalElementType getElementType() { return elementType; }
    public String getValue() {return value;}
    public Set<UniversalElementRepresentation> getInnerElements() { return innerElements; }

    // ----------- Builder -------------
    public static class Builder {
        private UUID uniqueId;
        private UUID parentUniqueId = null;
        private ReferenceType referenceType = null;
        private String elementName = "";
        private String fullQualifiedName = "";
        private UniversalElementType elementType;
        private CurrentRole currentRole;
        private String value;
        private Set<UniversalElementRepresentation> innerElements = new HashSet<>();

        public Builder uniqueId(UUID uniqueId) { this.uniqueId = uniqueId; return this; }
        public Builder parentUniqueId(UUID parentUniqueId) { this.parentUniqueId = parentUniqueId; return this; }
        public Builder referenceType(ReferenceType referenceType) { this.referenceType = referenceType; return this; }
        public Builder elementName(String elementName) { this.elementName = elementName; return this; }
        public Builder fullQualifiedName(String fullQualifiedName) { this.fullQualifiedName = fullQualifiedName; return this; }
        public Builder elementType(UniversalElementType elementType) { this.elementType = elementType; return this; }
        public Builder currentRole(CurrentRole currentRole) { this.currentRole = currentRole; return this; }
        public Builder value(String value) {this.value = value; return this;}
        public Builder innerElements(Set<UniversalElementRepresentation> innerElements) { 
            if (innerElements != null) this.innerElements = innerElements; 
            return this; 
        }
        public Builder addInnerElement(UniversalElementRepresentation innerElement) { 
            if (innerElement != null) this.innerElements.add(innerElement); 
            return this; 
        }

        public UniversalElementRepresentation build() {
            UniversalElementRepresentation element = new UniversalElementRepresentation(
                    uniqueId, parentUniqueId, referenceType, elementName, fullQualifiedName, elementType, currentRole, value
            );
            element.getInnerElements().addAll(innerElements);
            return element;
        }
    }

    // ----------- Статический метод для быстрого билда -------------
    public static Builder builder() {
        return new Builder();
    }
}