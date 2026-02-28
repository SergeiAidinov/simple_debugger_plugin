package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.TargetApplicationElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractTargetAplicationTopLevelElement;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TopLevelElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.InnerElementRepresentation;
import com.sun.jdi.ReferenceType;

public class DebugWindowDataDTO {

	private ReferenceType getReferenceType;
    private String elementName;
    private TargetApplicationElementType elementType;
    private String qualifiedTypeName; // квалифицированное имя типа
   // private String value;
    private Set<InnerElementRepresentation> innerElements;
    private int lineNumber = -1;
    private List<MethodCallInStackDTO> stackCall;
    private String methodName = "[NO METHOD]";
	public ReferenceType getGetReferenceType() {
		return getReferenceType;
	}
	public void setGetReferenceType(ReferenceType getReferenceType) {
		this.getReferenceType = getReferenceType;
	}
	public String getElementName() {
		return elementName;
	}
	public void setElementName(String elementName) {
		this.elementName = elementName;
	}
	public TargetApplicationElementType getElementType() {
		return elementType;
	}
	public void setElementType(TargetApplicationElementType elementType) {
		this.elementType = elementType;
	}
	public String getQualifiedTypeName() {
		return qualifiedTypeName;
	}
	public void setQualifiedTypeName(String qualifiedTypeName) {
		this.qualifiedTypeName = qualifiedTypeName;
	}
//	public String getValue() {
//		return value;
//	}
//	public void setValue(String value) {
//		this.value = value;
//	}
	public Set<InnerElementRepresentation> getInnerElements() {
		return innerElements;
	}
	public void setInnerElements(Set<InnerElementRepresentation> set) {
		this.innerElements = set;
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


}