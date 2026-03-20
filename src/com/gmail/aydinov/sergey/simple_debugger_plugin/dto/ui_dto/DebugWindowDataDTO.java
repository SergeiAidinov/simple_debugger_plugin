package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.MethodCallInStackDTO;

/**
 * DTO для передачи данных в UI без прямой передачи JDI-объектов.
 * Создаётся на основе UniversalElementRepresentation.
 */
public class DebugWindowDataDTO { 
	
	private final int lineNumber;
	private final String methodName;
	private final List<MethodCallInStackDTO> compileStackInfo;
	private final Map<InnerElementRepresentationDTO, List<InnerElementRepresentationDTO>> topElementsWithSubordinates;
	public DebugWindowDataDTO(int lineNumber, String methodName, List<MethodCallInStackDTO> compileStackInfo,
			Map<InnerElementRepresentationDTO, List<InnerElementRepresentationDTO>> innerElementDTOs) {
		super();
		this.lineNumber = lineNumber;
		this.methodName = methodName;
		this.compileStackInfo = compileStackInfo;
		this.topElementsWithSubordinates = innerElementDTOs;
	}
	public int getLineNumber() {
		return lineNumber;
	}
	public String getMethodName() {
		return methodName;
	}
	public List<MethodCallInStackDTO> getCompileStackInfo() {
		return compileStackInfo;
	}
	public Map<InnerElementRepresentationDTO, List<InnerElementRepresentationDTO>> getTopElementsWithSubordinates() {
		return topElementsWithSubordinates;
	}
	
	@Override
	public String toString() {
	    StringBuilder sb = new StringBuilder();
	    sb.append("DebugWindowDataDTO {\n");
	    sb.append("  lineNumber: ").append(lineNumber).append(",\n");
	    sb.append("  methodName: ").append(methodName).append(",\n");

	    sb.append("  compileStackInfo:\n");
	    if (compileStackInfo != null && !compileStackInfo.isEmpty()) {
	        for (MethodCallInStackDTO call : compileStackInfo) {
	            sb.append("    - ").append(call).append("\n");
	        }
	    } else {
	        sb.append("    (empty)\n");
	    }

	    sb.append("  topElementsWithSubordinates:\n");
	    if (topElementsWithSubordinates != null && !topElementsWithSubordinates.isEmpty()) {
	        for (Map.Entry<InnerElementRepresentationDTO, List<InnerElementRepresentationDTO>> entry : topElementsWithSubordinates.entrySet()) {
	            InnerElementRepresentationDTO top = entry.getKey();
	            List<InnerElementRepresentationDTO> subs = entry.getValue();

	            sb.append(formatElement(top));

	            if (subs != null && !subs.isEmpty()) {
	                for (InnerElementRepresentationDTO sub : subs) {
	                    sb.append(formatElement(sub));
	                }
	            }
	        }
	    } else {
	        sb.append("    (empty)\n");
	    }

	    sb.append("}");
	    return sb.toString();
	}

	// Форматирование одного элемента с отступом по level
	private String formatElement(InnerElementRepresentationDTO elem) {
	    String indent = "    ".repeat(Math.max(1, elem.getLevel()));
	    return String.format("%s→ %s [type=%s, valueCategory=%s, typeOrReturnType=%s, isStatic=%b, level=%d, value=%s]\n",
	            indent,
	            elem.getElementName(),
	            elem.getElementType(),
	            elem.getValueCategory(),
	            elem.getTypeOrReturnType(),
	            elem.isStatic(),
	            elem.getLevel(),
	            elem.getValue());
	}
	
}