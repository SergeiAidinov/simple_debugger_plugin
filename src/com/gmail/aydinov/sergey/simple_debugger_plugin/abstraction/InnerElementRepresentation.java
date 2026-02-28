package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;

public class InnerElementRepresentation extends AbstractInnerElementRepresentation {

	private String value;

	/**
	 * @param outerElementReference - ReferenceType для static поля
	 * @param objectInstance        - ObjectReference для non-static поля (может
	 *                              быть null для static)
	 */
	public InnerElementRepresentation(ReferenceType outerElementReference, ObjectReference objectInstance,
			String elementName, String fullQualifiedName, TargetApplicationElementType elementType) {

		super(outerElementReference, elementName, fullQualifiedName, elementType);

		this.value = computeValue(outerElementReference, objectInstance, elementName, elementType);
	}

	public String getValue() {
		return value;
	}

	private String computeValue(ReferenceType referenceType, ObjectReference objectInstance, String fieldName,
			TargetApplicationElementType elementType) {

		try {
			switch (elementType) {
			case STATIC_FIELD -> {
				Field field = referenceType.fieldByName(fieldName);
				if (field == null)
					return "[field not found]";
				Value val = referenceType.getValue(field); // static поля читаем через ReferenceType
				return DebugUtils.valueToString(val);
			}
			case NON_STATIC_FIELD -> {
				if (objectInstance == null)
					return "[no instance]";
				Field field = objectInstance.referenceType().fieldByName(fieldName);
				if (field == null)
					return "[field not found]";
				Value val = objectInstance.getValue(field); // non-static через ObjectReference
				return DebugUtils.valueToString(val);
			}
			case METHOD -> {
			    try {
			        com.sun.jdi.Method method = referenceType.methodsByName(getElementName())
			                                                 .stream().findFirst().orElse(null);
			        if (method == null) return "[method not found]";

			        String params = method.argumentTypes().stream()
			                              .map(com.sun.jdi.Type::name)
			                              .reduce((a, b) -> a + ", " + b)
			                              .orElse("");

			        return method.name() + "(" + params + ")"; // будет в колонке Value / Info
			    } catch (Exception e) {
			        return "[error]";
			    }
			}
			}
		} catch (Exception e) {
			return "[error]";
		}
		return "[unknown]";
	}
}