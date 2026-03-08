package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;

/**
 * UniversalElementRepresentation — представление элемента (класс, метод, поле, объект)
 * с уникальным UUID и parentUUID.
 */
public class UniversalElementRepresentation implements Comparable<UniversalElementRepresentation>{

    // =================== Энумы ===================
    public enum UniversalElementType {
        INTERFACE, CLASS, ENUM, STATIC_FIELD, NON_STATIC_FIELD, METHOD, METHOD_PARAMETER, LOCAL_VARIABLE, OBJECT_INSTANCE, UNKNOWN
    }

    public enum CurrentRole { OUTER, INNER, LOCAL }

    public enum ValueCategory {
        PRIMITIVE, WRAPPER, STRING, COLLECTION, ARRAY, MAP, USER_OBJECT, NULL, NOT_SPECIFIED
    }

    // =================== Поля ===================
    private final Tag tag;
    private final ReferenceType referenceType;       // класс объекта (ReferenceType)
    private final ObjectReference objectReference;   // конкретный объект (ObjectReference)
    private final String elementName;
    private final String additionalInfo;
    private final UniversalElementType elementType;
    private CurrentRole currentRole;
    private final String value;
    private final boolean isStatic;
    private final ValueCategory valueCategory;
    private final String typeOrReturnType;

    // =================== Конструктор ===================
    private UniversalElementRepresentation(Tag tag,
                                           ReferenceType referenceType,
                                           ObjectReference objectReference,
                                           String elementName,
                                           String additionalInfo,
                                           UniversalElementType elementType,
                                           CurrentRole currentRole,
                                           String value,
                                           boolean isStatic,
                                           ValueCategory valueCategory,
                                           String typeOrReturnType) {
        this.tag = tag;
        this.referenceType = referenceType;
        this.objectReference = objectReference;
        this.elementName = elementName;
        this.additionalInfo = additionalInfo;
        this.elementType = elementType;
        this.currentRole = currentRole;
        this.value = value;
        this.isStatic = isStatic;
        this.valueCategory = valueCategory;
        this.typeOrReturnType = typeOrReturnType;
    }

    // =================== Геттеры ===================
    public Tag getTag() { return tag; }
    public ReferenceType getReferenceType() { return referenceType; }
    public ObjectReference getObjectReference() { return objectReference; }
    public String getElementName() { return elementName; }
    public String getAdditionalInfo() { return additionalInfo; }
    public UniversalElementType getElementType() { return elementType; }
    public CurrentRole getCurrentRole() { return currentRole; }
    public void setCurrentRole(CurrentRole currentRole) { this.currentRole = currentRole; }
    public String getValue() { return value; }
    public boolean isStatic() { return isStatic; }
    public ValueCategory getValueCategory() { return valueCategory; }
    public String gettypeOrReturnType() { return typeOrReturnType; }
    
    

    @Override
	public String toString() {
		return "UniversalElementRepresentation [tag=" + tag + ", referenceType=" + referenceType + ", objectReference="
				+ objectReference + ", elementName=" + elementName + ", additionalInfo=" + additionalInfo
				+ ", elementType=" + elementType + ", currentRole=" + currentRole + ", value=" + value + ", isStatic="
				+ isStatic + ", valueCategory=" + valueCategory + ", typeOrReturnType=" + typeOrReturnType + "]";
	}

	
    
    

	@Override
	public int hashCode() {
		return Objects.hash(additionalInfo, currentRole, elementName, elementType, isStatic, objectReference,
				referenceType, typeOrReturnType, value, valueCategory);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UniversalElementRepresentation other = (UniversalElementRepresentation) obj;
		return Objects.equals(additionalInfo, other.additionalInfo) && currentRole == other.currentRole
				&& Objects.equals(elementName, other.elementName) && elementType == other.elementType
				&& isStatic == other.isStatic && Objects.equals(objectReference, other.objectReference)
				&& Objects.equals(referenceType, other.referenceType)
				&& Objects.equals(typeOrReturnType, other.typeOrReturnType) && Objects.equals(value, other.value)
				&& valueCategory == other.valueCategory;
	}

	@Override
    public int compareTo(UniversalElementRepresentation other) {
        if (other == null) return 1; // null всегда в конце
        int cmp = Integer.compare(this.elementType.ordinal(), other.elementType.ordinal());
        if (cmp != 0) return cmp;
        return this.elementName.compareToIgnoreCase(other.elementName);
    }

    // =================== Builder ===================
    public static class Builder {
        private ReferenceType referenceType = null;
        private ObjectReference objectReference = null;
        private String elementName = "";
        private String additionalInfo = "";
        private UniversalElementType elementType = UniversalElementType.UNKNOWN;
        private CurrentRole currentRole = CurrentRole.OUTER;
        private String value = null;
        private boolean isStatic = false;
        private ValueCategory valueCategory = ValueCategory.NOT_SPECIFIED;
        private String typeOrReturnType = "";
        private UUID uniqueId = UUID.randomUUID();
        private UUID parentUniqueId = null;

        public Builder referenceType(ReferenceType referenceType) { this.referenceType = referenceType; return this; }
        public Builder objectReference(ObjectReference objectReference) { this.objectReference = objectReference; return this; }
        public Builder elementName(String elementName) { this.elementName = elementName; return this; }
        public Builder additionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; return this; }
        public Builder elementType(UniversalElementType elementType) { this.elementType = elementType; return this; }
        public Builder currentRole(CurrentRole currentRole) { this.currentRole = currentRole; return this; }
        public Builder value(String value) { this.value = value; return this; }
        public Builder isStatic(boolean isStatic) { this.isStatic = isStatic; return this; }
        public Builder valueCategory(ValueCategory valueCategory) { this.valueCategory = valueCategory; return this; }
        public Builder typeOrReturnType(String typeOrReturnType) { this.typeOrReturnType = typeOrReturnType; return this; }
        public Builder uniqueId(UUID uniqueId) { this.uniqueId = uniqueId; return this; }
        public Builder parentUniqueId(UUID parentUniqueId) { this.parentUniqueId = parentUniqueId; return this; }

        public UniversalElementRepresentation build() {
            Tag tag = new Tag(uniqueId, parentUniqueId);
            return new UniversalElementRepresentation(
                    tag,
                    referenceType,
                    objectReference,
                    elementName,
                    additionalInfo,
                    elementType,
                    currentRole,
                    value,
                    isStatic,
                    valueCategory,
                    typeOrReturnType
            );
        }
    }

    public static Builder builder() { return new Builder(); }

    // =================== Пример использования snapshot ===================
    public static void main(String[] args) {
        Map<Tag, UniversalElementRepresentation> snapshot = new HashMap<>();

        UniversalElementRepresentation clazz = UniversalElementRepresentation.builder()
                .elementType(UniversalElementType.CLASS)
                .typeOrReturnType("com.example.MyClass")
                .elementName("MyClass")
                .build();

        UniversalElementRepresentation method = UniversalElementRepresentation.builder()
                .elementType(UniversalElementType.METHOD)
                .typeOrReturnType("com.example.MyClass.myMethod()")
                .elementName("myMethod")
                .parentUniqueId(clazz.getTag().getUniqueId())
                .build();

        snapshot.put(clazz.getTag(), clazz);
        snapshot.put(method.getTag(), method);

        System.out.println(snapshot);
    }
    
 // =================== Вложенный класс Tag ===================
    public static final class Tag {
        private final UUID uniqueId;
        private final UUID parentId;

        public Tag(UUID uniqueId, UUID parentId) {
            this.uniqueId = uniqueId;
            this.parentId = parentId;
        }

        public UUID getUniqueId() { return uniqueId; }
        public UUID getParentId() { return parentId; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Tag other = (Tag) obj;
            return Objects.equals(uniqueId, other.uniqueId) &&
                   Objects.equals(parentId, other.parentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uniqueId, parentId);
        }

        @Override
        public String toString() {
            return "Tag{uniqueId=" + uniqueId + ", parentId=" + parentId + "}";
        }
    }
}