package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.UUID;

import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.Field;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;

public class UniversalElementRepresentation extends AbstractElementRepresentation implements Comparable<UniversalElementRepresentation> {

    public enum UniversalElementType { INTERFACE, CLASS, ENUM, FIELD, METHOD, METHOD_PARAMETER, LOCAL_VARIABLE, OBJECT_INSTANCE, UNKNOWN, REFERENCE, COLLECTION, MAP }
    public enum CurrentRole { OUTER, INNER, LOCAL }
    public enum ValueCategory { PRIMITIVE, WRAPPER, STRING, COLLECTION, ARRAY, MAP, USER_OBJECT, NULL, NOT_SPECIFIED, AUXILIARY, UNKNOWN }

    private final ReferenceType referenceType;
    private final String elementName;
    private final String additionalInfo;
    private final UniversalElementType elementType;
    private CurrentRole currentRole;
    private final String value;
    private final boolean isStatic;
    private final ValueCategory valueCategory;
    private final String typeOrReturnType;
   // private final int level; // уровень вложенности

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
                                           String typeOrReturnType,
                                           int level) {
        super(tag, objectReference, level);
        this.referenceType = referenceType;
        this.elementName = elementName;
        this.additionalInfo = additionalInfo;
        this.elementType = elementType;
        this.currentRole = currentRole;
        this.value = value;
        this.isStatic = isStatic;
        this.valueCategory = valueCategory;
        this.typeOrReturnType = typeOrReturnType;
       // this.level = level;
    }

    // ===================== Геттеры =====================
    public ReferenceType getReferenceType() { return referenceType; }
    public String getElementName() { return elementName; }
    public UniversalElementType getElementType() { return elementType; }
    public CurrentRole getCurrentRole() { return currentRole; }
    public void setCurrentRole(CurrentRole currentRole) { this.currentRole = currentRole; }
    public String getAdditionalInfo() { return additionalInfo; }
    public String getValue() { return value; }
    public boolean isStatic() { return isStatic; }
    public ValueCategory getValueCategory() { return valueCategory; }
    public String getTypeOrReturnType() { return typeOrReturnType; }
    public int getLevel() { return super.getLevel(); }

    // ===================== compareTo =====================
    @Override
    public int compareTo(UniversalElementRepresentation other) {
        if (other == null) return 1;
        int cmp = Integer.compare(this.elementType.ordinal(), other.elementType.ordinal());
        if (cmp != 0) return cmp;
        return this.elementName.compareToIgnoreCase(other.elementName);
    }

    // ===================== Builder =====================
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
        private int level;

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
        public Builder level(int level) { this.level = level; return this; }

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
                    typeOrReturnType,
                    level
            );
        }
    }

    public static Builder builder() { return new Builder(); }

    // ===================== Utility Builders =====================
    public static UniversalElementRepresentation buildElementForField(Field field, Value value, UUID parentId, ObjectReference objectReference, int level) {
        return UniversalElementRepresentation.builder()
                .referenceType(field.declaringType())
                .objectReference(objectReference)
                .elementName(field.name())
                .additionalInfo(field.typeName())
                .elementType(UniversalElementType.FIELD)
                .currentRole(CurrentRole.INNER)
                .value(value != null ? value.toString() : "null")
                .isStatic(field.isStatic())
                .valueCategory(DebugUtils.determineValueCategory(value))
                .typeOrReturnType(DebugUtils.valueToString(value))
                .uniqueId(UUID.randomUUID())
                .parentUniqueId(parentId)
                .level(level) // по умолчанию 0, потом можно увеличивать рекурсивно
                .build();
    }

    public static UniversalElementRepresentation buildElementForMethod(Method method, UUID parentId, ObjectReference objectReference, int level) {
        return UniversalElementRepresentation.builder()
                .referenceType(method.declaringType())
                .objectReference(objectReference)
                .elementName(method.name() + "()")
                .additionalInfo(String.join(",", method.argumentTypeNames()))
                .elementType(UniversalElementType.METHOD)
                .currentRole(CurrentRole.INNER)
                .value(method.name() + "(" + String.join(",", method.argumentTypeNames()) + ")")
                .isStatic(method.isStatic())
                .valueCategory(ValueCategory.NOT_SPECIFIED)
                .typeOrReturnType(method.returnTypeName())
                .uniqueId(UUID.randomUUID())
                .parentUniqueId(parentId)
                .level(level)
                .build();
    }

    // ===================== toString =====================
    @Override
    public String toString() {
        String indent = "  ".repeat(super.getLevel());
        return indent + "↳ " + elementName
                + " [" + elementType + "]"
                + ", value=" + value
                + ", type=" + typeOrReturnType
                + ", isStatic=" + isStatic
                + ", valueCategory=" + valueCategory;
    }
}