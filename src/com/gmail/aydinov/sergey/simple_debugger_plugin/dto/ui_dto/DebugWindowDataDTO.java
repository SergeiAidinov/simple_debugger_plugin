package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.MethodCallInStackDTO;

/**
 * DTO для передачи данных в UI без прямой передачи JDI-объектов.
 * Создаётся на основе UniversalElementRepresentation.
 */
public class DebugWindowDataDTO extends InnerElementRepresentationDTO { 
	
	private final int lineNumber;
	private final String methodName;
	private final List<MethodCallInStackDTO> compileStackInfo;
	private final Set<InnerElementRepresentationDTO> innerElements;

	// =================== Приватный конструктор ===================
	private DebugWindowDataDTO(Tag tag, String elementName, String additionalInfo,
			UniversalElementType elementType, String value, boolean isStatic,
			ValueCategory valueCategory, String fullQualifiedName,
			int lineNumber, String methodName, List<MethodCallInStackDTO> compileStackInfo,
			Set<InnerElementRepresentationDTO> innerElements) {
		
		super(tag, elementName, additionalInfo, elementType, value, isStatic, valueCategory, fullQualifiedName);
		this.lineNumber = lineNumber;
		this.methodName = methodName;
		this.compileStackInfo = Objects.isNull(compileStackInfo) ? List.copyOf(compileStackInfo) : List.of();
		this.innerElements = innerElements != null ? new HashSet<>(innerElements) : new HashSet<>();
	}

	// =================== Геттеры ===================
	public int getLineNumber() { return lineNumber; }
	public String getMethodName() { return methodName; }
	public Set<InnerElementRepresentationDTO> getInnerElements() {
		return Collections.unmodifiableSet(innerElements);
	}
	public List<MethodCallInStackDTO> getCompileStackInfo() {
		return compileStackInfo;
	}

	@Override
	public String toString() {
		return "DebugWindowDataDTO [lineNumber=" + lineNumber + ", methodName=" + methodName
				+ ", innerElements=" + innerElements + ", " + super.toString() + "]";
	}

	// =================== Вложенная фабрика ===================
	public static final class Factory {

	    private Factory() { }

	    /**
	     * Создаёт DebugWindowDataDTO из UniversalElementRepresentation с указанием номера строки,
	     * имени метода и внутренних элементов.
	     */
	    public static DebugWindowDataDTO fromUniversalElement(UniversalElementRepresentation element,
	                                                          int lineNumber,
	                                                          String methodName,
	                                                          Set<InnerElementRepresentationDTO> innerElements,
	                                                          List<MethodCallInStackDTO> compileStackInfo) {
	        if (element == null) return null;

	        return new DebugWindowDataDTO(
	                element.getTag(),
	                element.getElementName(),
	                element.getAdditionalInfo(),
	                element.getElementType(),
	                element.getValue(),
	                element.isStatic(),
	                element.getValueCategory(),
	                element.getFullQualifiedName(),
	                lineNumber,
	                methodName,
	                 compileStackInfo,
	                innerElements
	               
	        );
	    }

	    /**
	     * Упрощённый вариант без внутренних элементов.
	     */
//	    public static DebugWindowDataDTO fromUniversalElement(UniversalElementRepresentation element,
//	                                                          int lineNumber,
//	                                                          String methodName) {
//	        return fromUniversalElement(element, lineNumber, methodName, null);
//	    }
	}
}