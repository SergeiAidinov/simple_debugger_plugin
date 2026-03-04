package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;

/**
 * Представление внутреннего элемента (поле, метод, локальная переменная) для UI.
 * Адаптировано под новый Tag с parentId и uniqueId.
 */
public class InnerElementRepresentationDTO {

    private final Tag tag;               // <- объект Tag, содержит uniqueId и parentId
    private final String elementName;
    private final String additionalInfo;
    private final UniversalElementType elementType;
    private final String value;
    private final boolean isStatic;
    private final ValueCategory valueCategory;
    private final String fullQualifiedName;

    protected InnerElementRepresentationDTO(Tag tag,
                                         String elementName,
                                         String additionalInfo,
                                         UniversalElementType elementType,
                                         String value,
                                         boolean isStatic,
                                         ValueCategory valueCategory,
                                         String fullQualifiedName) {
        this.tag = tag;
        this.elementName = elementName;
        this.additionalInfo = additionalInfo;
        this.elementType = elementType;
        this.value = value;
        this.isStatic = isStatic;
        this.valueCategory = valueCategory;
        this.fullQualifiedName = fullQualifiedName;
    }

    // =================== Геттеры ===================
    public Tag getTag() { return tag; }
    public String getElementName() { return elementName; }
    public String getAdditionalInfo() { return additionalInfo; }
    public UniversalElementType getElementType() { return elementType; }
    public String getValue() { return value; }
    public ValueCategory getValueCategory() { return valueCategory; }
    public boolean isStatic() { return isStatic; }
    public String getFullQualifiedName() { return fullQualifiedName; }

    @Override
    public String toString() {
        return "InnerElementRepresentationDTO [tag=" + tag + ", elementName=" + elementName
                + ", additionalInfo=" + additionalInfo + ", elementType=" + elementType
                + ", value=" + value + ", isStatic=" + isStatic
                + ", valueCategory=" + valueCategory + ", fullQualifiedName=" + fullQualifiedName + "]";
    }
    
    
    
    
    @Override
	public int hashCode() {
		return Objects.hash(additionalInfo, elementName, elementType, fullQualifiedName, isStatic, value,
				valueCategory);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InnerElementRepresentationDTO other = (InnerElementRepresentationDTO) obj;
		return Objects.equals(additionalInfo, other.additionalInfo) && Objects.equals(elementName, other.elementName)
				&& elementType == other.elementType && Objects.equals(fullQualifiedName, other.fullQualifiedName)
				&& isStatic == other.isStatic && Objects.equals(value, other.value)
				&& valueCategory == other.valueCategory;
	}

	/**
     * Фабрика для InnerElementRepresentationDTO.
     * Позволяет создать DTO из UniversalElementRepresentation.
     */
    public final class InnerElementRepresentationDTOFactory {

        private InnerElementRepresentationDTOFactory() {
            // private constructor to prevent instantiation
        }

        /**
         * Создаёт InnerElementRepresentationDTO из UniversalElementRepresentation.
         *
         * @param element исходный UniversalElementRepresentation
         * @return DTO для UI
         */
        public static InnerElementRepresentationDTO fromUniversalElement(UniversalElementRepresentation element) {
            if (element == null) {
                return null;
            }

            Tag tag = element.getTag();
            String elementName = element.getElementName();
            String additionalInfo = element.getAdditionalInfo();
            UniversalElementRepresentation.UniversalElementType elementType = element.getElementType();
            String value = element.getValue();
            boolean isStatic = element.isStatic();
            UniversalElementRepresentation.ValueCategory valueCategory = element.getValueCategory();
            String fullQualifiedName = element.gettypeOrReturnType();

            return new InnerElementRepresentationDTO(
                    tag,
                    elementName,
                    additionalInfo,
                    elementType,
                    value,
                    isStatic,
                    valueCategory,
                    fullQualifiedName
            );
        }
    }
}
