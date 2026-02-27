package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractTargetAplicationElement;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationClassOrInterfaceRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationInnerElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractTargetAplicationElement.TargetApplicationElementType;
import com.sun.jdi.ReferenceType;

public class DebugWindowDataDTO {

	private ReferenceType getReferenceType;
    private String elementName;
    private TargetApplicationElementType elementType;
    private String qualifiedTypeName; // квалифицированное имя типа
    private String value;
    private Set<DebugWindowDataDTO> innerElements;
    private int lineNumber = -1;
    private List<MethodCallInStackDTO> stackCall;
    private String methodName = "[NO METHOD]";

//    public DebugWindowDataDTO(String elementName,
//                              TargetApplicationElementType elementType,
//                              Set<DebugWindowDataDTO> innerElements) {
//        this.elementName = elementName;
//        this.elementType = elementType;
//        this.innerElements = innerElements;
//    }

    // --- геттеры и сеттеры ---
    
    
    public String getElementName() { return elementName; }
    
    public ReferenceType getGetReferenceType() {
		return getReferenceType;
	}
	public void setGetReferenceType(ReferenceType getReferenceType) {
		this.getReferenceType = getReferenceType;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public void setElementName(String elementName) { this.elementName = elementName; }

    public TargetApplicationElementType getElementType() { return elementType; }
    public void setElementType(TargetApplicationElementType elementType) { this.elementType = elementType; }

    public String getQualifiedTypeName() { return qualifiedTypeName; }
    public void setQualifiedTypeName(String qualifiedTypeName) { this.qualifiedTypeName = qualifiedTypeName; }

    public String getElementValue() { return value; }
    public void setElementValue(String value) { this.value = value; }

    public Set<DebugWindowDataDTO> getInnerElements() { return innerElements; }
    public void setInnerElements(Set<DebugWindowDataDTO> innerElements) { this.innerElements = innerElements; }

    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

    public List<MethodCallInStackDTO> getStackCall() { return stackCall; }
    public void setStackCall(List<MethodCallInStackDTO> stackCall) { this.stackCall = stackCall; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    // --- фабричные методы для top-level и inner элементов ---
//    public static DebugWindowDataDTO from(TargetApplicationClassOrInterfaceRepresentation source, String qualifiedTypeName) {
//        return buildFromAbstract(source, qualifiedTypeName);
//    }

//    public static DebugWindowDataDTO from(TargetApplicationInnerElementRepresentation source, String qualifiedTypeName) {
//        return buildFromAbstract(source, qualifiedTypeName);
//    }
//
//    private static DebugWindowDataDTO buildFromAbstract(AbstractTargetAplicationElement source, String qualifiedTypeName) {
//        Set<DebugWindowDataDTO> innerDtos = source.getInnerElements().stream()
//                .map(inner -> fromAny(inner, null)) // inner FQN можно задать позже, если нужно
//                .collect(Collectors.toSet());
//
//        return new DebugWindowDataDTO(
//                source.getElementName(),
//                source.getElementType(),
//                qualifiedTypeName,
//                innerDtos
//        );
//    }

//    private static DebugWindowDataDTO fromAny(AbstractTargetAplicationElement element, String qualifiedTypeName) {
//        if (element instanceof TargetApplicationClassOrInterfaceRepresentation top) {
//            return from(top, qualifiedTypeName);
//        }
//        if (element instanceof TargetApplicationInnerElementRepresentation inner) {
//            return from(inner, qualifiedTypeName);
//        }
//        throw new IllegalArgumentException("Unknown element type: " + element.getClass());
//    }
}