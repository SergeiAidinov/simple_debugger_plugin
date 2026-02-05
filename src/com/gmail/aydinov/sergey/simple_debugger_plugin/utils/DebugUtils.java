package com.gmail.aydinov.sergey.simple_debugger_plugin.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.MethodCallInStackDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationMethodParameterDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.VariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UserInvokedMethodEvent;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

/**
 * Utility class for JDI (Java Debug Interface) operations
 */
public class DebugUtils {

	public static Value createJdiValueFromString(VirtualMachine virtualMachine, LocalVariable localVariable,
			String sourceString) {
		String type = localVariable.typeName();
		switch (type) {
		case "int":
			return virtualMachine.mirrorOf(Integer.parseInt(sourceString));
		case "long":
			return virtualMachine.mirrorOf(Long.parseLong(sourceString));
		case "short":
			return virtualMachine.mirrorOf(Short.parseShort(sourceString));
		case "byte":
			return virtualMachine.mirrorOf(Byte.parseByte(sourceString));
		case "char":
			return virtualMachine.mirrorOf(sourceString.charAt(0));
		case "boolean":
			return virtualMachine.mirrorOf(Boolean.parseBoolean(sourceString));
		case "float":
			return virtualMachine.mirrorOf(Float.parseFloat(sourceString));
		case "double":
			return virtualMachine.mirrorOf(Double.parseDouble(sourceString));
		case "java.lang.String":
			return virtualMachine.mirrorOf(sourceString);
		default:
			throw new IllegalArgumentException("Unsupported type: " + type);
		}
	}

	/**
	 * Creates a JDI Value from a string to set it to a local variable or field.
	 *
	 * @param virtualMachine        Target process VirtualMachine
	 * @param varType   Variable type (LocalVariable.type() or Field.type())
	 * @param textValue String value from UI
	 * @param threadReference    Thread where boxed objects are created
	 */
	public static Value createJdiObjectFromString(VirtualMachine virtualMachine, Type varType, String textValue,
			ThreadReference threadReference) {

		if (Objects.isNull(textValue)) {
			return nullJdiValue(virtualMachine, varType);
		}

		String trimmed = textValue.trim();

		// 1. null
		if (trimmed.equals("null"))
			return nullJdiValue(virtualMachine, varType);

		String typeName = varType.name();

		// 2. Primitives
		try {
			switch (typeName) {
			case "int":
				return virtualMachine.mirrorOf(Integer.parseInt(trimmed));
			case "long":
				return virtualMachine.mirrorOf(Long.parseLong(trimmed));
			case "float":
				return virtualMachine.mirrorOf(Float.parseFloat(trimmed));
			case "double":
				return virtualMachine.mirrorOf(Double.parseDouble(trimmed));
			case "boolean":
				return virtualMachine.mirrorOf(Boolean.parseBoolean(trimmed));
			case "char":
				if (trimmed.length() == 1)
					return virtualMachine.mirrorOf(trimmed.charAt(0));
				else if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() == 3)
					return virtualMachine.mirrorOf(trimmed.charAt(1));
				break;
			case "byte":
				return virtualMachine.mirrorOf(Byte.parseByte(trimmed));
			case "short":
				return virtualMachine.mirrorOf(Short.parseShort(trimmed));
			}
		} catch (Exception e) {
			throw new RuntimeException("Cannot parse primitive for type: " + typeName + " value: " + trimmed, e);
		}

		// 3. String
		if ("java.lang.String".equals(typeName)) {
			if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2)
				trimmed = trimmed.substring(1, trimmed.length() - 1);
			return virtualMachine.mirrorOf(trimmed);
		}

		// 4. Boxing types
		switch (typeName) {
		case "java.lang.Integer":
			return newBoxed(virtualMachine, (ClassType) varType, virtualMachine.mirrorOf(Integer.parseInt(trimmed)), threadReference);
		case "java.lang.Long":
			return newBoxed(virtualMachine, (ClassType) varType, virtualMachine.mirrorOf(Long.parseLong(trimmed)), threadReference);
		case "java.lang.Boolean":
			return newBoxed(virtualMachine, (ClassType) varType, virtualMachine.mirrorOf(Boolean.parseBoolean(trimmed)), threadReference);
		case "java.lang.Double":
			return newBoxed(virtualMachine, (ClassType) varType, virtualMachine.mirrorOf(Double.parseDouble(trimmed)), threadReference);
		case "java.lang.Float":
			return newBoxed(virtualMachine, (ClassType) varType, virtualMachine.mirrorOf(Float.parseFloat(trimmed)), threadReference);
		case "java.lang.Character":
			char c = trimmed.length() == 1 ? trimmed.charAt(0) : trimmed.charAt(1);
			return newBoxed(virtualMachine, (ClassType) varType, virtualMachine.mirrorOf(c), threadReference);
		}

		return null;
	}

	// ====================================================
	// Helper methods
	// ====================================================

	private static Value nullJdiValue(VirtualMachine virtualMachine, Type type) {
		if (type instanceof PrimitiveType)
			throw new RuntimeException("Cannot assign null to primitive " + type.name());

		return virtualMachine.mirrorOf(null);
	}

	/**
	 * Creates a boxed value using ClassType.newInstance(...), e.g., new
	 * Integer(intValue)
	 */
	private static ObjectReference newBoxed(VirtualMachine virtualMachine, ClassType classType, Value primitive,
			ThreadReference thread) {
		List<Method> methods = classType.methods();
		for (Method method : methods) {
			if (method.isConstructor()) {
				List<Type> args = null;
				try {
					args = method.argumentTypes();
				} catch (ClassNotLoadedException e) {
					e.printStackTrace();
				}
				if (Objects.nonNull(args) && args.size() == 1) {
					try {
						return classType.newInstance(thread, method, List.of(primitive), ObjectReference.INVOKE_SINGLE_THREADED);
					} catch (Exception e) {
						throw new RuntimeException("Error creating boxed " + classType.name(), e);
					}
				}
			}
		}

		throw new RuntimeException("No suitable constructor found for boxed type: " + classType.name());
	}

	private static VariableDTO mapField(Map.Entry<Field, Value> entry) {
		Field field = entry.getKey();
		Value value = entry.getValue();
		return new VariableDTO(field.name(), field.typeName(), valueToString(value));
	}

	private static VariableDTO mapLocal(Map.Entry<LocalVariable, Value> entry) {
		LocalVariable localVariable = entry.getKey();
		Value value = entry.getValue();
		return new VariableDTO(localVariable.name(), localVariable.typeName(), valueToString(value));
	}

	/**
	 * Converts Map<Field, Value> to List<VariableDTO>
	 */
	public static List<VariableDTO> mapFields(Map<Field, Value> fields) {
		if (Objects.isNull(fields))
			return List.of();

		return fields.entrySet().stream().map(entry -> new VariableDTO(entry.getKey().name(), entry.getKey().typeName(),
				valueToString(entry.getValue()))).collect(Collectors.toList());
	}

	/**
	 * Converts Map<LocalVariable, Value> to List<VariableDTO>
	 */
	public static List<VariableDTO> mapLocals(Map<LocalVariable, Value> locals) {
		if (Objects.isNull(locals))
			return List.of();

		return locals.entrySet().stream().map(entry -> new VariableDTO(entry.getKey().name(), entry.getKey().typeName(),
				valueToString(entry.getValue()))).collect(Collectors.toList());
	}

	/**
	 * Converts Value to string safely handling null
	 */
	private static String valueToString(Value value) {
		return Objects.isNull(value) ? "null" : value.toString();
	}

	/**
	 * Converts argumentsText to a list of JDI values for method invocation
	 */
	public static List<Value> parseArguments(VirtualMachine virtualMachine, UserInvokedMethodEvent userInvokedMethodEvent) {
		List<Value> values = new ArrayList<>();
		String argsText = userInvokedMethodEvent.getArgumentsText().trim();
		// Remove parentheses if method specified as method(arg1, arg2)
		int start = argsText.indexOf('(');
		int end = argsText.lastIndexOf(')');
		if (start >= 0 && end > start) {
			argsText = argsText.substring(start + 1, end).trim();
		}
		if (argsText.isEmpty())
			return values;
		String[] argStrings = argsText.split("\\s*,\\s*");
		List<TargetApplicationMethodParameterDTO> params = userInvokedMethodEvent.getMethod().getParameters();
		if (argStrings.length != params.size()) {
			throw new IllegalArgumentException("Argument count does not match method parameter count");
		}
		for (int i = 0; i < params.size(); i++) {
			TargetApplicationMethodParameterDTO param = params.get(i);
			String argStr = argStrings[i].trim();
			String typeName = param.getTypeName();

			// Remove quotes for strings
			if ((argStr.startsWith("\"") && argStr.endsWith("\""))
					|| (argStr.startsWith("'") && argStr.endsWith("'"))) {
				argStr = argStr.substring(1, argStr.length() - 1);
			}

			Value value;
			try {
				value = convertStringToValue(argStr, typeName, virtualMachine);
			} catch (Exception e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Error converting argument '" + argStr + "' to type " + typeName, e);
			}

			values.add(value);
		}

		return values;
	}

	private static Value convertStringToValue(String argument, String typeName, VirtualMachine virtualMachine) throws Exception {
		switch (typeName) {
		case "int":
			return virtualMachine.mirrorOf(Integer.parseInt(argument));
		case "boolean":
			return virtualMachine.mirrorOf(Boolean.parseBoolean(argument));
		case "long":
			return virtualMachine.mirrorOf(Long.parseLong(argument));
		case "double":
			return virtualMachine.mirrorOf(Double.parseDouble(argument));
		case "float":
			return virtualMachine.mirrorOf(Float.parseFloat(argument));
		case "short":
			return virtualMachine.mirrorOf(Short.parseShort(argument));
		case "byte":
			return virtualMachine.mirrorOf(Byte.parseByte(argument));
		case "char":
			if (argument.length() != 1)
				throw new IllegalArgumentException("Invalid char argument: " + argument);
			return virtualMachine.mirrorOf(argument.charAt(0));
		case "java.lang.String":
			return virtualMachine.mirrorOf(argument);
		default:
			List<ReferenceType> classes = virtualMachine.classesByName(typeName);
			if (classes.isEmpty())
				throw new ClassNotLoadedException(typeName, "Class not found in target application");

			ReferenceType refType = classes.get(0);
			if (!(refType instanceof ClassType))
				throw new IllegalArgumentException("Type is not a class: " + typeName);

			ClassType classType = (ClassType) refType;
			Method constructor = classType.concreteMethodByName("<init>", "()V");
			if (Objects.isNull(constructor))
				throw new IllegalArgumentException("No no-args constructor for " + typeName);

			return classType.newInstance(virtualMachine.allThreads().get(0), constructor, List.of(),
					ClassType.INVOKE_SINGLE_THREADED);
		}
	}

	public static List<MethodCallInStackDTO> compileStackInfo(ThreadReference threadReference) {
		List<StackFrame> frames = Collections.emptyList();
		List<MethodCallInStackDTO> calls = new ArrayList<>();

		try {
			frames = threadReference.frames();
		} catch (IncompatibleThreadStateException e) {
			e.printStackTrace();
			return List.of(new MethodCallInStackDTO("Cannot get frames: " + e.getMessage(), "", ""));
		}

		for (int i = 0; i < frames.size(); i++) {
			StackFrame frame = frames.get(i);
			if (Objects.isNull(frame))
				continue;

			try {
				Location location = frame.location();
				if (Objects.nonNull(location)) {
					String className = Objects.nonNull(location.declaringType()) ? location.declaringType().name() : "Unknown";
					String methodName = Objects.nonNull(location.method()) ? location.method().name() : "unknown";
					int line = location.lineNumber();

					String sourceInfo;
					try {
						sourceInfo = location.sourceName() + ":" + line;
					} catch (AbsentInformationException aie) {
						sourceInfo = "Unknown:" + line;
					}

					calls.add(new MethodCallInStackDTO(className, methodName, sourceInfo));
				}
			} catch (Exception e) {
				e.printStackTrace();
				calls.add(new MethodCallInStackDTO("<error retrieving frame>", "", ""));
			}
		}

		Collections.reverse(calls);
		return calls;
	}

	public static Map<Field, Value> compileFields(StackFrame frame) {
		Map<Field, Value> fields = Collections.EMPTY_MAP;
		try {
			if (Objects.nonNull(frame.thisObject())) {
				fields = frame.thisObject().getValues(frame.thisObject().referenceType().fields());
			}
		} catch (Exception ignored) {
		}
		return fields;
	}

	public static Map<LocalVariable, Value> compileLocalVariables(StackFrame frame) {
		Map<LocalVariable, Value> localVariables = Collections.emptyMap();
		try {
			localVariables = frame.getValues(frame.visibleVariables()).entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		} catch (AbsentInformationException e) {
			System.err.println("No debug info: " + e.getMessage());
		} catch (com.sun.jdi.InvalidStackFrameException e) {
			System.err.println("Cannot read variables: " + e.getMessage());
		}
		return localVariables;
	}

}
