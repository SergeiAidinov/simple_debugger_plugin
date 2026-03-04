package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final String fullQualifiedName;

    // =================== Конструктор ===================
    private UniversalElementRepresentation(Tag tag, ReferenceType referenceType,
                                           String elementName, String additionalInfo,
                                           UniversalElementType elementType, CurrentRole currentRole,
                                           String value, boolean isStatic, ValueCategory valueCategory,
                                           String fullQualifiedName) {
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

    // =================== Equals и hashCode по бизнес-логике ===================
    @Override
    public int hashCode() {
        return Objects.hash(elementName, additionalInfo, elementType, currentRole, value, innerElements,
                            isStatic, valueCategory, fullQualifiedName);
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
               valueCategory == other.valueCategory &&
               Objects.equals(fullQualifiedName, other.fullQualifiedName);
    }

    // =================== Builder ===================
    public static class Builder {
        private ReferenceType referenceType = null;
        private String elementName = "";
        private String additionalInfo = "";
        private UniversalElementType elementType = UniversalElementType.UNKNOWN;
        private CurrentRole currentRole = CurrentRole.OUTER;
        private String value = null;
        private Set<UniversalElementRepresentation> innerElements = new HashSet<>();
        private boolean isStatic = false;
        private ValueCategory valueCategory = ValueCategory.UNKNOWN;
        private String fullQualifiedName = "";

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
        public Builder fullQualifiedName(String fullQualifiedName) { this.fullQualifiedName = fullQualifiedName; return this; }

        public UniversalElementRepresentation build() {
            Tag tag = Tag.of(fullQualifiedName, elementType);
            UniversalElementRepresentation element = new UniversalElementRepresentation(
                    tag, referenceType, elementName, additionalInfo, elementType, currentRole,
                    value, isStatic, valueCategory, fullQualifiedName
            );
            element.getInnerElements().addAll(innerElements);
            return element;
        }
    }

    public static Builder builder() { return new Builder(); }

    // =================== Пример использования snapshot ===================
    public static void main(String[] args) {
        Map<Tag, UniversalElementRepresentation> snapshot = new HashMap<>();

        UniversalElementRepresentation clazz = UniversalElementRepresentation.builder()
                .elementType(UniversalElementType.CLASS)
                .fullQualifiedName("com.example.MyClass")
                .elementName("MyClass")
                .build();

        UniversalElementRepresentation method = UniversalElementRepresentation.builder()
                .elementType(UniversalElementType.METHOD)
                .fullQualifiedName("com.example.MyClass.myMethod()")
                .elementName("myMethod")
                .build();

        snapshot.put(clazz.getTag(), clazz);
        snapshot.put(method.getTag(), method);

        System.out.println(snapshot);
    }
}