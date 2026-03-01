package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import com.sun.jdi.ReferenceType;

public class UniversalElementRepresentation {

    // =================== Вложенный класс Tag ===================
    public static final class Tag {
        private final UUID uniqueId;
        private final UUID parentUniqueId;

        public Tag(UUID uniqueId, UUID parentUniqueId) {
            this.uniqueId = uniqueId;
            this.parentUniqueId = parentUniqueId;
        }

        public UUID getUniqueId() { return uniqueId; }
        public UUID getParentUniqueId() { return parentUniqueId; }

        @Override
        public int hashCode() {
            return Objects.hash(uniqueId, parentUniqueId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Tag other = (Tag) obj;
            return Objects.equals(uniqueId, other.uniqueId) &&
                   Objects.equals(parentUniqueId, other.parentUniqueId);
        }

        @Override
        public String toString() {
            return "Tag{" + uniqueId + ", parent=" + parentUniqueId + "}";
        }
    }

    // =================== Энумы ===================
    public enum UniversalElementType {
        INTERFACE, CLASS, ENUM, STATIC_FIELD, NON_STATIC_FIELD, METHOD, VARIABLE, UNKNOWN
    }

    public enum CurrentRole { OUTER, INNER, LOCAL }

    // =================== Поля ===================
    private final Tag tag;
    private final ReferenceType referenceType;
    private final String elementName;
    private final String fullQualifiedName;
    private final UniversalElementType elementType;
    private CurrentRole currentRole;
    private final String value;
    private final Set<UniversalElementRepresentation> innerElements = new HashSet<>();

    // =================== Конструктор ===================
    private UniversalElementRepresentation(Tag tag, ReferenceType referenceType,
                                           String elementName, String fullQualifiedName,
                                           UniversalElementType elementType, CurrentRole currentRole,
                                           String value) {
        this.tag = tag;
        this.referenceType = referenceType;
        this.elementName = elementName;
        this.fullQualifiedName = fullQualifiedName;
        this.elementType = elementType;
        this.currentRole = currentRole;
        this.value = value;
    }

    // =================== Геттеры ===================
    public Tag getTag() { return tag; }
    public ReferenceType getReferenceType() { return referenceType; }
    public String getElementName() { return elementName; }
    public String getFullQualifiedName() { return fullQualifiedName; }
    public UniversalElementType getElementType() { return elementType; }
    public CurrentRole getCurrentRole() { return currentRole; }
    public void setCurrentRole(CurrentRole currentRole) { this.currentRole = currentRole; }
    public String getValue() { return value; }
    public Set<UniversalElementRepresentation> getInnerElements() { return innerElements; }

    // =================== Equals и hashCode ===================
    @Override
	public int hashCode() {
		return Objects.hash(currentRole, elementName, elementType, fullQualifiedName, innerElements, referenceType,
				value);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Builder other = (Builder) obj;
		return currentRole == other.currentRole && Objects.equals(elementName, other.elementName)
				&& elementType == other.elementType && Objects.equals(fullQualifiedName, other.fullQualifiedName)
				&& Objects.equals(innerElements, other.innerElements)
				&& Objects.equals(referenceType, other.referenceType) && Objects.equals(value, other.value);
	}

    // =================== Builder ===================
    public static class Builder {
        

		private Tag tag;
        private ReferenceType referenceType = null;
        private String elementName = "";
        private String fullQualifiedName = "";
        private UniversalElementType elementType = UniversalElementType.UNKNOWN;
        private CurrentRole currentRole = CurrentRole.OUTER;
        private String value = null;
        private Set<UniversalElementRepresentation> innerElements = new HashSet<>();

        public Builder tag(Tag tag) { this.tag = tag; return this; }
        public Builder uniqueId(UUID uniqueId) { 
            if (this.tag == null) this.tag = new Tag(uniqueId, null); 
            else this.tag = new Tag(uniqueId, this.tag.getParentUniqueId());
            return this;
        }
        public Builder parentUniqueId(UUID parentUniqueId) { 
            if (this.tag == null) this.tag = new Tag(null, parentUniqueId);
            else this.tag = new Tag(this.tag.getUniqueId(), parentUniqueId);
            return this;
        }
        public Builder referenceType(ReferenceType referenceType) { this.referenceType = referenceType; return this; }
        public Builder elementName(String elementName) { this.elementName = elementName; return this; }
        public Builder fullQualifiedName(String fullQualifiedName) { this.fullQualifiedName = fullQualifiedName; return this; }
        public Builder elementType(UniversalElementType elementType) { this.elementType = elementType; return this; }
        public Builder currentRole(CurrentRole currentRole) { this.currentRole = currentRole; return this; }
        public Builder value(String value) { this.value = value; return this; }
        public Builder innerElements(Set<UniversalElementRepresentation> innerElements) { 
            if (innerElements != null) this.innerElements = innerElements; 
            return this; 
        }
        public Builder addInnerElement(UniversalElementRepresentation innerElement) {
            if (innerElement != null) this.innerElements.add(innerElement);
            return this;
        }

        public UniversalElementRepresentation build() {
            if (tag == null) tag = new Tag(UUID.randomUUID(), null);
            UniversalElementRepresentation element = new UniversalElementRepresentation(
                    tag, referenceType, elementName, fullQualifiedName, elementType, currentRole, value
            );
            element.getInnerElements().addAll(innerElements);
            return element;
        }
    }

    public static Builder builder() { return new Builder(); }
}