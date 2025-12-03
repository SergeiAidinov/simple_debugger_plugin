package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.jdi.Field;
import com.sun.jdi.Method;

public class TargetApplicationClassOrInterfaceRepresentation implements TargetApplicationElementRepresentation {

	private final String targetApplicationElementName;
	private final TargetApplicationElementType targetApplicationElementType;
	private final Set<TargetApplicationMethodDTO> methods;
	private final Set<com.sun.jdi.Field> fields;

	public TargetApplicationClassOrInterfaceRepresentation(String targetApplicationElementName,
			TargetApplicationElementType targetApplicationElementType, Set<TargetApplicationMethodDTO> methods,
			Set<Field> fields) {
		this.targetApplicationElementName = targetApplicationElementName;
		this.targetApplicationElementType = targetApplicationElementType;
		this.methods = methods;
		this.fields = fields;
	}

	public Set<TargetApplicationMethodDTO> getMethods() {
		return methods;
	}

	public Set<com.sun.jdi.Field> getFields() {
		return fields;
	}

	public String getTargetApplicationElementName() {
		return targetApplicationElementName;
	}

	public TargetApplicationElementType getTargetApplicationElementType() {
		return targetApplicationElementType;
	}

	public String prettyPrint() {
		String methodsPretty = methods.stream().sorted()
				.map(m -> "    " + String.format("%-30s", m.getMethodName()) + "  " + m.getReturnType())
				.collect(Collectors.joining("\n"));

		String fieldsPretty = fields.stream().sorted(Comparator.comparing(Field::name))
				.map(f -> "    " + String.format("%-30s", f.name()) + "  " + f.typeName())
				.collect(Collectors.joining("\n"));

		return """
				TargetApplicationElement {
				  name  = '%s'
				  type  = %s

				  methods:
				%s

				  fields:
				%s
				}
				""".formatted(targetApplicationElementName, targetApplicationElementType, methodsPretty, fieldsPretty);
	}
}
