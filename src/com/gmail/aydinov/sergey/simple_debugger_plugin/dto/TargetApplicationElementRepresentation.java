package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.Set;

public interface TargetApplicationElementRepresentation {

	Set<TargetApplicationMethodDTO> getMethods();

	Set<com.sun.jdi.Field> getFields();
	
	String getTargetApplicationElementName();

	TargetApplicationElementType getTargetApplicationElementType();
	
	String prettyPrint();

}
