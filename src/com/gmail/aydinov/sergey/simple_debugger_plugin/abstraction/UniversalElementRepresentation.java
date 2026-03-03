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

    public enum ValueCategory {
        PRIMITIVE, WRAPPER, STRING, COLLECTION, ARRAY, MAP, USER_OBJECT, NULL, UNKNOWN
    }

    // =================== Поля ===================
    private final Tag tag;
    private final ReferenceType referenceType;
    private final String elementName;
    private final String additionalInfo;
    private final UniversalElementType elementType;
    private CurrentRole currentRole;
    private final String value;
    private final Set<UniversalElementRepresentation> innerElements = new HashSet<>();
    private final boolean isStatic;
    private final ValueCategory valueCategory;

    // =================== Конструктор ===================
    private UniversalElementRepresentation(Tag tag, ReferenceType referenceType,
                                           String elementName, String additionalInfo,
                                           UniversalElementType elementType, CurrentRole currentRole,
                                           String value, boolean isStatic, ValueCategory valueCategory) {
        this.tag = tag;
        this.referenceType = referenceType;
        this.elementName = elementName;
        this.additionalInfo = additionalInfo;
        this.elementType = elementType;
        this.currentRole = currentRole;
        this.value = value;
        this.isStatic = isStatic;
        this.valueCategory = valueCategory;
    }

    // =================== Геттеры ===================
    public Tag getTag() { return tag; }
    public ReferenceType getReferenceType() { return referenceType; }
    public String getElementName() { return elementName; }
    public String getAdditionalInfo() { return additionalInfo; }
    public UniversalElementType getElementType() { return elementType; }
    public CurrentRole getCurrentRole() { return currentRole; }
    public void setCurrentRole(CurrentRole currentRole) { this.currentRole = currentRole; }
    public String getValue() { return value; }
    public Set<UniversalElementRepresentation> getInnerElements() { return innerElements; }
    public boolean isStatic() { return isStatic; }
    public ValueCategory getValueCategory() { return valueCategory; }

    // =================== Equals и hashCode по бизнес-логике ===================
    @Override
    public int hashCode() {
        return Objects.hash(elementName, additionalInfo, elementType, currentRole, value, innerElements, isStatic, valueCategory);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UniversalElementRepresentation other = (UniversalElementRepresentation) obj;
        return Objects.equals(elementName, other.elementName) &&
               Objects.equals(additionalInfo, other.additionalInfo) &&
               elementType == other.elementType &&
               currentRole == other.currentRole &&
               Objects.equals(value, other.value) &&
               Objects.equals(innerElements, other.innerElements) &&
               isStatic == other.isStatic &&
               valueCategory == other.valueCategory;
    }

    // =================== Builder ===================
    public static class Builder {
        private Tag tag;
        private ReferenceType referenceType = null;
        private String elementName = "";
        private String additionalInfo = "";
        private UniversalElementType elementType = UniversalElementType.UNKNOWN;
        private CurrentRole currentRole = CurrentRole.OUTER;
        private String value = null;
        private Set<UniversalElementRepresentation> innerElements = new HashSet<>();
        private boolean isStatic = false;
        private ValueCategory valueCategory = ValueCategory.UNKNOWN;

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
        public Builder additionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; return this; }
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
        public Builder isStatic(boolean isStatic) { this.isStatic = isStatic; return this; }
        public Builder valueCategory(ValueCategory valueCategory) { this.valueCategory = valueCategory; return this; }

        public UniversalElementRepresentation build() {
            if (tag == null) tag = new Tag(UUID.randomUUID(), null);
            UniversalElementRepresentation element = new UniversalElementRepresentation(
                    tag, referenceType, elementName, additionalInfo,
                    elementType, currentRole, value, isStatic, valueCategory
            );
            element.getInnerElements().addAll(innerElements);
            return element;
        }
    }

    public static Builder builder() { return new Builder(); }
}