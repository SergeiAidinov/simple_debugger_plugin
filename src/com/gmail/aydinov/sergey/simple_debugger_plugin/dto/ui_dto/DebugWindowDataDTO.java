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
	private final Map<InnerElementRepresentationDTO, Set<InnerElementRepresentationDTO>> topElementsWithSubordinates;
	public DebugWindowDataDTO(int lineNumber, String methodName, List<MethodCallInStackDTO> compileStackInfo,
			Map<InnerElementRepresentationDTO, Set<InnerElementRepresentationDTO>> innerElementDTOs) {
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
	public Map<InnerElementRepresentationDTO, Set<InnerElementRepresentationDTO>> getTopElementsWithSubordinates() {
		return topElementsWithSubordinates;
	}
	@Override
	public String toString() {
		return "DebugWindowDataDTO [lineNumber=" + lineNumber + ", methodName=" + methodName + ", compileStackInfo="
				+ compileStackInfo + ", topElementsWithSubordinates=" + topElementsWithSubordinates + "]";
	}
	
}