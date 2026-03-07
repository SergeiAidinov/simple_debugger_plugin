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
public class DebugWindowDataDTO { 
	
	private final int lineNumber;
	private final String methodName;
	private final List<MethodCallInStackDTO> compileStackInfo;
	private final Set<InnerElementRepresentationDTO> innerElements;
	public DebugWindowDataDTO(int lineNumber, String methodName, List<MethodCallInStackDTO> compileStackInfo,
			Set<InnerElementRepresentationDTO> innerElements) {
		super();
		this.lineNumber = lineNumber;
		this.methodName = methodName;
		this.compileStackInfo = compileStackInfo;
		this.innerElements = innerElements;
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
	public Set<InnerElementRepresentationDTO> getInnerElements() {
		return innerElements;
	}

	
}