package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractTargetAplicationElement;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationClassOrInterfaceRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationInnerElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractTargetAplicationElement.TargetApplicationElementType;

public final class DebugWindowDataDTO {

	private final String elementName;
	private final TargetApplicationElementType elementType;
	private String value;
	private final Set<DebugWindowDataDTO> innerElements;
	private int lineNumber = -1;
	private List<MethodCallInStackDTO> stackCall;
	private String methodName = "[NO METHOD]";
	

	private DebugWindowDataDTO(String elementName, TargetApplicationElementType elementType,
			Set<DebugWindowDataDTO> innerElements) {
		this.elementName = elementName;
		this.elementType = elementType;
		this.innerElements = innerElements;
	}

	public String getElementName() {
		return elementName;
	}

	public TargetApplicationElementType getElementType() {
		return elementType;
	}

	public Set<DebugWindowDataDTO> getInnerElements() {
		return innerElements;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public List<MethodCallInStackDTO> getStackCall() {
		return stackCall;
	}

	public void setStackCall(List<MethodCallInStackDTO> stackCall) {
		this.stackCall = stackCall;
	}
	
	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	
	public String getElementValue() {
		return value;
	}
	
	public void setElementValue(String value) {
		this.value = value;
	}

	/**
	 * Фабричный метод для top-level элемента
	 */
	public static DebugWindowDataDTO from(TargetApplicationClassOrInterfaceRepresentation source) {

		return buildFromAbstract(source);
	}

	/**
	 * Фабричный метод для inner элемента
	 */
	public static DebugWindowDataDTO from(TargetApplicationInnerElementRepresentation source) {

		return buildFromAbstract(source);
	}

	private static DebugWindowDataDTO buildFromAbstract(AbstractTargetAplicationElement source) {

		Set<DebugWindowDataDTO> innerDtos = source.getInnerElements().stream().map(DebugWindowDataDTO::fromAny)
				.collect(Collectors.toSet());

		return new DebugWindowDataDTO(source.getElementName(), source.getElementType(), innerDtos);
	}

	private static DebugWindowDataDTO fromAny(AbstractTargetAplicationElement element) {

		if (element instanceof TargetApplicationClassOrInterfaceRepresentation top) {
			return from(top);
		}

		if (element instanceof TargetApplicationInnerElementRepresentation inner) {
			return from(inner);
		}

		throw new IllegalArgumentException("Unknown element type: " + element.getClass());
	}
}