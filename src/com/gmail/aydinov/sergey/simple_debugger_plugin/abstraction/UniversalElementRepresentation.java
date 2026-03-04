package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.sun.jdi.ReferenceType;

public class UniversalElementRepresentation {

    // =================== Вложенный класс Tag ===================
    public static final class Tag {
        private final String fullQualifiedName;
        private final UniversalElementType elementType;

        private Tag(String fullQualifiedName, UniversalElementType elementType) {
            this.fullQualifiedName = fullQualifiedName;
            this.elementType = elementType;
        }

        public String getFullQualifiedName() { return fullQualifiedName; }
        public UniversalElementType getElementType() { return elementType; }

        public static Tag of(String fullQualifiedName, UniversalElementType elementType) {
            return new Tag(fullQualifiedName, elementType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fullQualifiedName, elementType);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Tag other = (Tag) obj;
            return Objects.equals(fullQualifiedName, other.fullQualifiedName) &&
                   elementType == other.elementType;
        }

        @Override
        public String toString() {
            return "Tag{" + elementType + ": " + fullQualifiedName + "}";
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
    private final UUID uniqueId;
    private final UUID parentUniqueId;

    private final Tag tag;
    private final ReferenceType referenceType;
    private final String elementName;
    private final String additionalInfo;
    private final UniversalElementType elementType;
    private CurrentRole currentRole;
    private final String value;
    private final Set<UniversalElementRepresentation> innerElements = new LinkedHashSet<>();
    private final boolean isStatic;
    private final ValueCategory valueCategory;
    private final String fullQualifiedName;

    // =================== Конструктор ===================
    private UniversalElementRepresentation(UUID uniqueId,
                                           UUID parentUniqueId,
                                           Tag tag,
                                           ReferenceType referenceType,
                                           String elementName,
                                           String additionalInfo,
                                           UniversalElementType elementType,
                                           CurrentRole currentRole,
                                           String value,
                                           boolean isStatic,
                                           ValueCategory valueCategory,
                                           String fullQualifiedName) {
        this.uniqueId = uniqueId;
        this.parentUniqueId = parentUniqueId;
        this.tag = tag;
        this.referenceType = referenceType;
        this.elementName = elementName;
        this.additionalInfo = additionalInfo;
        this.elementType = elementType;
        this.currentRole = currentRole;
        this.value = value;
        this.isStatic = isStatic;
        this.valueCategory = valueCategory;
        this.fullQualifiedName = fullQualifiedName;
    }

    // =================== Геттеры ===================
    public UUID getUniqueId() { return uniqueId; }
    public UUID getParentUniqueId() { return parentUniqueId; }
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
    public String getFullQualifiedName() { return fullQualifiedName; }

    // =================== Equals / hashCode по UUID ===================
    @Override
    public int hashCode() {
        return Objects.hash(uniqueId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UniversalElementRepresentation other = (UniversalElementRepresentation) obj;
        return Objects.equals(uniqueId, other.uniqueId);
    }

    @Override
    public String toString() {
        return "UniversalElementRepresentation{" +
                "uniqueId=" + uniqueId +
                ", elementType=" + elementType +
                ", elementName='" + elementName + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    // =================== Builder ===================
    public static class Builder {
        private UUID uniqueId = UUID.randomUUID();
        private UUID parentUniqueId = null;
        private ReferenceType referenceType = null;
        private String elementName = "";
        private String additionalInfo = "";
        private UniversalElementType elementType = UniversalElementType.UNKNOWN;
        private CurrentRole currentRole = CurrentRole.OUTER;
        private String value = null;
        private Set<UniversalElementRepresentation> innerElements = new LinkedHashSet<>();
        private boolean isStatic = false;
        private ValueCategory valueCategory = ValueCategory.UNKNOWN;
        private String fullQualifiedName = "";

        public Builder uniqueId(UUID uniqueId) { this.uniqueId = uniqueId; return this; }
        public Builder parentUniqueId(UUID parentUniqueId) { this.parentUniqueId = parentUniqueId; return this; }
        public Builder referenceType(ReferenceType referenceType) { this.referenceType = referenceType; return this; }
        public Builder elementName(String elementName) { this.elementName = elementName; return this; }
        public Builder additionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; return this; }
        public Builder elementType(UniversalElementType elementType) { this.elementType = elementType; return this; }
        public Builder currentRole(CurrentRole currentRole) { this.currentRole = currentRole; return this; }
        public Builder value(String value) { this.value = value; return this; }
        public Builder isStatic(boolean isStatic) { this.isStatic = isStatic; return this; }
        public Builder valueCategory(ValueCategory valueCategory) { this.valueCategory = valueCategory; return this; }
        public Builder fullQualifiedName(String fullQualifiedName) { this.fullQualifiedName = fullQualifiedName; return this; }

        public Builder innerElements(Set<UniversalElementRepresentation> innerElements) {
            if (innerElements != null) this.innerElements = innerElements;
            return this;
        }

        public Builder addInnerElement(UniversalElementRepresentation innerElement) {
            if (innerElement != null) this.innerElements.add(innerElement);
            return this;
        }

        public UniversalElementRepresentation build() {
            Tag tag = Tag.of(fullQualifiedName, elementType);

            UniversalElementRepresentation element =
                new UniversalElementRepresentation(
                        uniqueId,
                        parentUniqueId,
                        tag,
                        referenceType,
                        elementName,
                        additionalInfo,
                        elementType,
                        currentRole,
                        value,
                        isStatic,
                        valueCategory,
                        fullQualifiedName
                );

            element.getInnerElements().addAll(innerElements);
            return element;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // =================== Пример snapshot ===================
    public static void main(String[] args) {
        Map<UUID, UniversalElementRepresentation> snapshot = new HashMap<>();

        UniversalElementRepresentation clazz = UniversalElementRepresentation.builder()
                .elementType(UniversalElementType.CLASS)
                .fullQualifiedName("com.example.MyClass")
                .elementName("MyClass")
                .build();

        UniversalElementRepresentation method = UniversalElementRepresentation.builder()
                .elementType(UniversalElementType.METHOD)
                .fullQualifiedName("com.example.MyClass.myMethod()")
                .elementName("myMethod")
                .parentUniqueId(clazz.getUniqueId())
                .build();

        snapshot.put(clazz.getUniqueId(), clazz);
        snapshot.put(method.getUniqueId(), method);

        System.out.println(snapshot);
    }
}