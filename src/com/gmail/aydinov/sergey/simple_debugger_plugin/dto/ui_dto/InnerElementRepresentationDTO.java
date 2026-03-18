package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.ElementReference;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;

/**
 * Представление внутреннего элемента (поле, метод, локальная переменная) для UI.
 * Адаптировано под новый Tag с parentId и uniqueId.
 */
public class InnerElementRepresentationDTO implements Comparable<InnerElementRepresentationDTO> {

    private final Tag tag;               // <- объект Tag, содержит uniqueId и parentId
    private final String elementName;
    private final String additionalInfo;
    private final UniversalElementType elementType;
    private String value;
    private final boolean isStatic;
    private final ValueCategory valueCategory;
    private final String typeOrReturnType;
    private int level = 0;

    protected InnerElementRepresentationDTO(Tag tag,
                                            String elementName,
                                            String additionalInfo,
                                            UniversalElementType elementType,
                                            String value,
                                            boolean isStatic,
                                            ValueCategory valueCategory,
                                            String typeOrReturnType) {
        this.tag = tag;
        this.elementName = elementName;
        this.additionalInfo = additionalInfo;
        this.elementType = elementType;
        this.value = value;
        this.isStatic = isStatic;
        this.valueCategory = valueCategory;
        this.typeOrReturnType = typeOrReturnType;
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
    public void setLevel(int level) { this.level = level; }
    public void setValue(String value) { this.value = value; }

   

    @Override
   	public String toString() {
   		return "InnerElementRepresentationDTO [tag=" + tag + ", elementName=" + elementName + ", additionalInfo="
   				+ additionalInfo + ", elementType=" + elementType + ", value=" + value + ", isStatic=" + isStatic
   				+ ", valueCategory=" + valueCategory + ", typeOrReturnType=" + typeOrReturnType + ", level=" + level
   				+ "]";
   	}

	@Override
    public int hashCode() {
        return Objects.hash(additionalInfo, elementName, elementType, typeOrReturnType, isStatic, value,
                valueCategory);
    }

	@Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        InnerElementRepresentationDTO other = (InnerElementRepresentationDTO) obj;
        return Objects.equals(additionalInfo, other.additionalInfo)
                && Objects.equals(elementName, other.elementName)
                && elementType == other.elementType
                && Objects.equals(typeOrReturnType, other.typeOrReturnType)
                && isStatic == other.isStatic
                && Objects.equals(value, other.value)
                && valueCategory == other.valueCategory;
    }

    @Override
    public int compareTo(InnerElementRepresentationDTO other) {
        if (other == null) {
            return 1;
        }

        // 1️⃣ По приоритету типа (через ordinal)
        int typeCompare = Integer.compare(
                this.elementType.ordinal(),
                other.elementType.ordinal()
        );
        if (typeCompare != 0) return typeCompare;

        // 2️⃣ По имени (без учёта регистра)
        int nameCompare = this.elementName.compareToIgnoreCase(other.elementName);
        if (nameCompare != 0) return nameCompare;

        // 3️⃣ Стабилизируем сортировку
        return this.typeOrReturnType.compareToIgnoreCase(other.typeOrReturnType);
    }

    /**
     * Универсальная фабрика для InnerElementRepresentationDTO.
     * Позволяет создать DTO из любого AbstractElementRepresentation,
     * включая UniversalElementRepresentation и ElementReference.
     */
    public final class InnerElementRepresentationDTOFactory {

        private InnerElementRepresentationDTOFactory() {
            // private constructor to prevent instantiation
        }

        public static InnerElementRepresentationDTO fromElement(AbstractElementRepresentation element) {
            if (element == null) return null;

            if (element instanceof UniversalElementRepresentation u) {
                return fromUniversal(u);
            }

            if (element instanceof ElementReference ref) {
                return fromReference(ref);
            }

            // fallback для любых других наследников
            return new InnerElementRepresentationDTO(
                    element.getTag(),
                    element.getElementName(),
                    "",
                    UniversalElementType.UNKNOWN,
                    "",
                    false,
                    ValueCategory.UNKNOWN,
                    ""
            );
        }

        private static InnerElementRepresentationDTO fromUniversal(UniversalElementRepresentation element) {
            return new InnerElementRepresentationDTO(
                    element.getTag(),
                    element.getElementName(),
                    element.getAdditionalInfo(),
                    element.getElementType(),
                    element.getValue(),
                    element.isStatic(),
                    element.getValueCategory(),
                    element.getTypeOrReturnType()
            );
        }

        private static InnerElementRepresentationDTO fromReference(ElementReference ref) {
            return new InnerElementRepresentationDTO(
                    ref.getTag(),
                    ref.getElementName(), // "<reference to ...>"
                    "reference",
                    UniversalElementType.REFERENCE, // новый тип REFERENCE
                    "",
                    false,
                    ValueCategory.AUXILIARY,
                    ""
            );
        }
    }
}