package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.ElementReference;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserInstanceDetailsDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.ArrayPageDTO;

/**
 * Представление внутреннего элемента (поле, метод, локальная переменная) для UI.
 */
public class InnerElementRepresentationDTO implements Comparable<InnerElementRepresentationDTO> {

    private final Tag tag;
    private final String elementName;
    private final String additionalInfo;
    private final UniversalElementType elementType;
    private String value;
    private final boolean isStatic;
    private final ValueCategory valueCategory;
    private final String typeOrReturnType;
    private final int level;

    // ✅ НОВОЕ ПОЛЕ
    private final Long objectId;

    protected InnerElementRepresentationDTO(
            Tag tag,
            String elementName,
            String additionalInfo,
            UniversalElementType elementType,
            String value,
            boolean isStatic,
            ValueCategory valueCategory,
            String typeOrReturnType,
            int level,
            Long objectId
    ) {
        this.tag = tag;
        this.elementName = elementName;
        this.additionalInfo = additionalInfo;
        this.elementType = elementType;
        this.value = value;
        this.isStatic = isStatic;
        this.valueCategory = valueCategory;
        this.typeOrReturnType = typeOrReturnType;
        this.level = level;
        this.objectId = objectId;
    }

    // =================== Геттеры ===================
    public Tag getTag() { return tag; }
    public String getElementName() { return elementName; }
    public String getAdditionalInfo() { return additionalInfo; }
    public UniversalElementType getElementType() { return elementType; }
    public String getValue() { return value; }
    public ValueCategory getValueCategory() { return valueCategory; }
    public boolean isStatic() { return isStatic; }
    public String getTypeOrReturnType() { return typeOrReturnType; }
    public int getLevel() { return level; }
    public Long getObjectId() { return objectId; }

    public void setValue(String value) { this.value = value; }

    @Override
    public String toString() {
        return "InnerElementRepresentationDTO {" +
                "tag=" + tag +
                ", elementName='" + elementName + '\'' +
                ", additionalInfo='" + additionalInfo + '\'' +
                ", elementType=" + elementType +
                ", value='" + value + '\'' +
                ", isStatic=" + isStatic +
                ", valueCategory=" + valueCategory +
                ", typeOrReturnType='" + typeOrReturnType + '\'' +
                ", level=" + level +
                ", objectId=" + objectId +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                additionalInfo,
                elementName,
                elementType,
                typeOrReturnType,
                isStatic,
                value,
                valueCategory,
                objectId // ✅ добавили
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        InnerElementRepresentationDTO other = (InnerElementRepresentationDTO) obj;

        return Objects.equals(additionalInfo, other.additionalInfo)
                && Objects.equals(elementName, other.elementName)
                && elementType == other.elementType
                && Objects.equals(typeOrReturnType, other.typeOrReturnType)
                && isStatic == other.isStatic
                && Objects.equals(value, other.value)
                && valueCategory == other.valueCategory
                && Objects.equals(objectId, other.objectId); // ✅ добавили
    }

    @Override
    public int compareTo(InnerElementRepresentationDTO other) {
        if (other == null) return 1;

        int typeCompare = Integer.compare(
                this.elementType.ordinal(),
                other.elementType.ordinal()
        );
        if (typeCompare != 0) return typeCompare;

        int nameCompare = this.elementName.compareToIgnoreCase(other.elementName);
        if (nameCompare != 0) return nameCompare;

        return this.typeOrReturnType.compareToIgnoreCase(other.typeOrReturnType);
    }

    // =========================================================
    // Factory
    // =========================================================

    public static final class InnerElementRepresentationDTOFactory {

        private InnerElementRepresentationDTOFactory() {}

        public static InnerElementRepresentationDTO fromElement(AbstractElementRepresentation element) {
            if (element == null) return null;

            if (element instanceof UniversalElementRepresentation u) {
                return fromUniversal(u);
            }

            if (element instanceof ElementReference ref) {
                return fromReference(ref);
            }

            return new InnerElementRepresentationDTO(
                    element.getTag(),
                    element.getElementName(),
                    "",
                    UniversalElementType.UNKNOWN,
                    "",
                    false,
                    ValueCategory.UNKNOWN,
                    "",
                    0,
                    null
            );
        }
        
        public static InnerElementRepresentationDTO fromUserInstanceDetails(UserInstanceDetailsDTO dto) {
            if (dto == null) return null;

            Long objectId = dto.getObjectId();

            String additionalInfo = objectId == null ? "<null>" : String.valueOf(objectId);

            return new InnerElementRepresentationDTO(
                    dto.getTag(),
                    dto.getFieldName(),
                    additionalInfo,
                    UniversalElementType.FIELD,                 // это поле-инстанс
                    "instance of " + dto.getTypeName() + "(id=" + objectId + ")",
                    false,                                      // обычно не static
                    ValueCategory.USER_OBJECT,
                    dto.getTypeName(),
                    0,                                          // уровень — можно потом передавать
                    objectId
            );
        }
        
        public static InnerElementRepresentationDTO fromArrayPage(ArrayPageDTO page) {
            if (page == null) return null;

            String name = page.getElementName() != null ? page.getElementName() : "Collection";
            String type = page.getElementType() != null ? page.getElementType() : "java.util.Collection";

            return new InnerElementRepresentationDTO(
                    null,                          // tag
                    name,                          // elementName
                    String.valueOf(page.getObjectId()), // additionalInfo
                    UniversalElementType.COLLECTION_ELEMENT, // elementType
                    name + " (size=" + page.getTotalElements() + ")", // value
                    false,                         // isStatic
                    ValueCategory.COLLECTION,      // valueCategory
                    type,                          // typeOrReturnType
                    0,                             // level
                    page.getObjectId()             // objectId
            );
        }

        private static InnerElementRepresentationDTO fromUniversal(UniversalElementRepresentation element) {

            Long objectId = null;
            if (element.getObjectReference() != null) {
                objectId = element.getObjectReference().uniqueID();
            }

            String additionalInfo = objectId == null ? "<null>" : String.valueOf(objectId);

            return new InnerElementRepresentationDTO(
                    element.getTag(),
                    element.getElementName(),
                    additionalInfo,
                    element.getElementType(),
                    element.getValue(),
                    element.isStatic(),
                    element.getValueCategory(),
                    element.getTypeOrReturnType(),
                    element.getLevel(),
                    objectId
            );
        }

        private static InnerElementRepresentationDTO fromReference(ElementReference ref) {
            return new InnerElementRepresentationDTO(
                    ref.getTag(),
                    ref.getElementName(),
                    "reference",
                    UniversalElementType.REFERENCE,
                    "",
                    false,
                    ValueCategory.AUXILIARY,
                    "",
                    ref.getLevel(),
                    null
            );
        }

        public static InnerElementRepresentationDTO fromMapEntry(MapEntryDTO dto) {
            if (dto == null) return null;

            Long objectId = dto.getObjectId();

            return new InnerElementRepresentationDTO(
                    null, // tag — у map entry обычно нет
                    null, // или dto.getValue() если это value-side
                    objectId == null ? "<null>" : String.valueOf(objectId),
                    UniversalElementType.FIELD,
                    dto.getValue(),
                    false,
                    dto.getValueCategory(),
                    null, // если есть
                    0,
                    objectId
            );
        }
    }
}