package com.gmail.aydinov.sergey.simple_debugger_plugin.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.MethodCallInStackDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationMethodParameterDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TripletDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserInvokedMethodEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.DebugWindowDataDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.Field;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.LongValue;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;

/**
 * Utility class for JDI (Java Debug Interface) operations
 * <p>
 * Author: Sergei Aidinov <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class DebugUtils {
	
	public static final String N_A = "[N/A]";
	public static final int PAGE_SIZE = 20;

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
	 * @param virtualMachine  Target process VirtualMachine
	 * @param varType         Variable type (LocalVariable.type() or Field.type())
	 * @param textValue       String value from UI
	 * @param threadReference Thread where boxed objects are created
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
			return newBoxed(virtualMachine, (ClassType) varType, virtualMachine.mirrorOf(Integer.parseInt(trimmed)),
					threadReference);
		case "java.lang.Long":
			return newBoxed(virtualMachine, (ClassType) varType, virtualMachine.mirrorOf(Long.parseLong(trimmed)),
					threadReference);
		case "java.lang.Boolean":
			return newBoxed(virtualMachine, (ClassType) varType, virtualMachine.mirrorOf(Boolean.parseBoolean(trimmed)),
					threadReference);
		case "java.lang.Double":
			return newBoxed(virtualMachine, (ClassType) varType, virtualMachine.mirrorOf(Double.parseDouble(trimmed)),
					threadReference);
		case "java.lang.Float":
			return newBoxed(virtualMachine, (ClassType) varType, virtualMachine.mirrorOf(Float.parseFloat(trimmed)),
					threadReference);
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
						return classType.newInstance(thread, method, List.of(primitive),
								ObjectReference.INVOKE_SINGLE_THREADED);
					} catch (Exception e) {
						throw new RuntimeException("Error creating boxed " + classType.name(), e);
					}
				}
			}
		}

		throw new RuntimeException("No suitable constructor found for boxed type: " + classType.name());
	}

//	private static VariableDTO mapField(Map.Entry<Field, Value> entry) {
//		Field field = entry.getKey();
//		Value value = entry.getValue();
//		return new VariableDTO(field.name(), field.typeName(), valueToString(value));
//	}

//	private static VariableDTO mapLocal(Map.Entry<LocalVariable, Value> entry) {
//		LocalVariable localVariable = entry.getKey();
//		Value value = entry.getValue();
//		return new VariableDTO(localVariable.name(), localVariable.typeName(), valueToString(value));
//	}

	/**
	 * Converts {@code Map<Field, Value>} to {@code List<VariableDTO>}.
	 *
	 * @param fields the map of fields and their values
	 * @return list of VariableDTO representing fields
	 */
//	public static List<InnerElementDTO> mapFields(Map<Field, Value> fields) {
//		if (Objects.isNull(fields))
//			return List.of();
//		return fields.entrySet().stream()
//				.map(entry -> new InnerElementDTO(entry.getKey().name(), entry.getKey().typeName(),
//						valueToString(entry.getValue()), FieldOrVariableType.NON_STATIC_FIELD))
//				.collect(Collectors.toList());
//	}

	/**
	 * Converts {@code Map<LocalVariable, Value>} to {@code List<VariableDTO>}.
	 *
	 * @param locals the map of local variables and their values
	 * @return list of VariableDTO representing local variables
	 */
//	public static List<InnerElementDTO> mapLocals(Map<LocalVariable, Value> locals) {
//		if (Objects.isNull(locals))
//			return List.of();
//
//		return locals.entrySet().stream().map(entry -> new InnerElementDTO(entry.getKey().name(),
//				entry.getKey().typeName(), valueToString(entry.getValue()), FieldOrVariableType.VARIABLE))
//				.collect(Collectors.toList());
//	}

	/**
	 * Converts Value to string safely handling null
	 */
	public static String valueToString(Value value) {
		return Objects.isNull(value) ? "null" : value.toString();
	}

	/**
	 * Converts argumentsText to a list of JDI values for method invocation
	 */
	public static List<Value> parseArguments(VirtualMachine virtualMachine,
			UserInvokedMethodEventDTO userInvokedMethodEventDTO) {
		List<Value> values = new ArrayList<>();
		String argsText = userInvokedMethodEventDTO.getArgumentsText().trim();
		// Remove parentheses if method specified as method(arg1, arg2)
		int start = argsText.indexOf('(');
		int end = argsText.lastIndexOf(')');
		if (start >= 0 && end > start) {
			argsText = argsText.substring(start + 1, end).trim();
		}
		if (argsText.isEmpty())
			return values;
		String[] argStrings = argsText.split("\\s*,\\s*");
		List<TargetApplicationMethodParameterDTO> params = userInvokedMethodEventDTO.getMethod().getParameters();
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

	private static Value convertStringToValue(String argument, String typeName, VirtualMachine virtualMachine)
			throws Exception {
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
					String className = Objects.nonNull(location.declaringType()) ? location.declaringType().name()
							: "Unknown";
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

	/**
	 * Determines if the DTO can be inspected (non-primitive, non-String, non-null)
	 */
//	public static boolean isInspectable(DebugWindowDataDTO dto) {
//		if (dto == null || dto.getElementType() == null /* || dto.getValue() == null */)
//			return false;
//
//		switch (dto.getElementType().toString()) {
//		case "int", "long", "double", "float", "boolean", "byte", "short", "char":
//			return false;
//		}
//		if ("java.lang.String".equals(dto.getElementType()))
//			return false;
//
//		return true;
//	}

	public static boolean isStandardJavaCollection(Object object) {
		if (Objects.isNull(object))
			return false;
		return object instanceof java.util.Collection || object instanceof java.util.Map;
	}

	public static UniversalElementRepresentation.UniversalElementType determineUniversalElementType(Object jdiElement) {

		if (jdiElement == null) {
			return null;
		}

		// ---------- Types ----------
		if (jdiElement instanceof com.sun.jdi.InterfaceType) {
			return UniversalElementRepresentation.UniversalElementType.INTERFACE;
		}

		if (jdiElement instanceof com.sun.jdi.ClassType classType) {
			if (classType.isEnum()) {
				return UniversalElementRepresentation.UniversalElementType.ENUM;
			}
			return UniversalElementRepresentation.UniversalElementType.CLASS;
		}

		// ---------- Fields ----------
		if (jdiElement instanceof com.sun.jdi.Field field) {
			return UniversalElementRepresentation.UniversalElementType.FIELD;
		}

		// ---------- Methods ----------
		if (jdiElement instanceof com.sun.jdi.Method) {
			return UniversalElementRepresentation.UniversalElementType.METHOD;
		}

		// ---------- Local variables ----------
		if (jdiElement instanceof com.sun.jdi.LocalVariable) {
			return UniversalElementRepresentation.UniversalElementType.LOCAL_VARIABLE;
		}

		return null;
	}

	public static String extractSimpleName(String fullQualifiedName) {
		if (fullQualifiedName == null || fullQualifiedName.isBlank()) {
			return "";
		}

		int lastDot = fullQualifiedName.lastIndexOf('.');
		String name = (lastDot >= 0) ? fullQualifiedName.substring(lastDot + 1) : fullQualifiedName;

		// Для inner / anonymous классов: Outer$Inner → Inner
		int lastDollar = name.lastIndexOf('$');
		if (lastDollar >= 0 && lastDollar < name.length() - 1) {
			name = name.substring(lastDollar + 1);
		}

		return name;
	}

	public static ValueCategory determineValueCategory(Value value) {

	    if (value == null) {
	        return ValueCategory.NULL;
	    }

	    // ---------- Primitive ----------
	    if (value instanceof com.sun.jdi.PrimitiveValue) {
	        return ValueCategory.PRIMITIVE;
	    }

	    // ---------- Array ----------
	    if (value instanceof com.sun.jdi.ArrayReference) {
	        return ValueCategory.ARRAY;
	    }

	    if (!(value instanceof com.sun.jdi.ObjectReference objectReference)) {
	        return ValueCategory.NOT_SPECIFIED;
	    }

	    ReferenceType referenceType = objectReference.referenceType();
	    String typeName = referenceType.name();

	    // ---------- String ----------
	    if ("java.lang.String".equals(typeName)) {
	        return ValueCategory.STRING;
	    }

	    // ---------- Wrapper (строго) ----------
	    switch (typeName) {
	        case "java.lang.Integer":
	        case "java.lang.Long":
	        case "java.lang.Double":
	        case "java.lang.Float":
	        case "java.lang.Boolean":
	        case "java.lang.Character":
	        case "java.lang.Byte":
	        case "java.lang.Short":
	            return ValueCategory.WRAPPER;
	    }

	    // ---------- Map ----------
	    if (implementsInterface(referenceType, "java.util.Map")) {
	        return ValueCategory.MAP;
	    }

	    // ---------- Collection ----------
	    if (implementsInterface(referenceType, "java.util.Collection")) {
	        return ValueCategory.COLLECTION;
	    }

	    // ---------- User object ----------
	    return ValueCategory.USER_OBJECT;
	}

//	private static boolean implementsInterface(ReferenceType referenceType, String interfaceName) {
//	    return ((ClassType) referenceType).allInterfaces()
//	            .stream()
//	            .anyMatch(i -> i.name().equals(interfaceName));
//	}

	public static String getLocalVariableValueAsString(Value value) {
		try {
			//Value value = frame.getValue(variable); // получаем Value из фрейма
			if (value == null)
				return "null";

			// ---------- Примитивы ----------
			if (value instanceof PrimitiveValue pv) {
				return pv.toString();
			}

			// ---------- String ----------
			if (value instanceof StringReference sr) {
				return sr.value();
			}

			// ---------- Object ----------
			if (value instanceof ObjectReference objRef) {
				return objRef.toString(); // по умолчанию toString() объекта
				// если нужно можно получать className: objRef.referenceType().name()
			}

			return value.toString();
		} catch (Exception e) {
			return "<error>";
		}
	}

	public static List<ObjectReference> getCollectionElements(ObjectReference objRef, BreakpointEvent breakpointEvent) {
	    List<ObjectReference> result = new ArrayList<>();

	    if (objRef == null || breakpointEvent == null) {
	        return result;
	    }

	    ReferenceType refType = objRef.referenceType();
	    if (!(refType instanceof ClassType classType)) {
	        return result;
	    }

	    boolean isIterable = classType.allInterfaces().stream()
	            .anyMatch(iface -> "java.lang.Iterable".equals(iface.name()));

	    if (!isIterable) {
	        return result;
	    }

	    ThreadReference thread = breakpointEvent.thread();

	    try {
	        Method iteratorMethod = classType.concreteMethodByName("iterator", "()Ljava/util/Iterator;");
	        if (iteratorMethod == null) {
	            return result;
	        }

	        Value iteratorValue = objRef.invokeMethod(
	                thread,
	                iteratorMethod,
	                Collections.emptyList(),
	                ObjectReference.INVOKE_SINGLE_THREADED
	        );

	        if (!(iteratorValue instanceof ObjectReference iterator)) {
	            return result;
	        }

	        if (!(iterator.referenceType() instanceof ClassType iteratorType)) {
	            return result;
	        }

	        Method hasNextMethod = iteratorType.concreteMethodByName("hasNext", "()Z");
	        Method nextMethod = iteratorType.concreteMethodByName("next", "()Ljava/lang/Object;");

	        if (hasNextMethod == null || nextMethod == null) {
	            return result;
	        }

	        while (true) {
	            Value hasNextValue = iterator.invokeMethod(
	                    thread,
	                    hasNextMethod,
	                    Collections.emptyList(),
	                    ObjectReference.INVOKE_SINGLE_THREADED
	            );

	            if (!(hasNextValue instanceof BooleanValue booleanValue)) {
	                break;
	            }

	            if (!booleanValue.value()) {
	                break;
	            }

	            Value elementValue = iterator.invokeMethod(
	                    thread,
	                    nextMethod,
	                    Collections.emptyList(),
	                    ObjectReference.INVOKE_SINGLE_THREADED
	            );

	            if (elementValue instanceof ObjectReference elementObject) {
	                result.add(elementObject);
	            }
	        }

	    } catch (Exception ignored) {
	    }

	    return result;
	}

	/**
	 * Проверка, реализует ли объект интерфейс (с учетом суперклассов)
	 */
	private static boolean implementsInterface(ReferenceType refType, String interfaceName) {
		try {
			for (Field iType : refType.allFields()) {
				if (iType.name().equals(interfaceName)) {
					return true;
				}
			}
			ClassType superClass = refType instanceof ClassType c ? c.superclass() : null;
			while (superClass != null) {
				for (InterfaceType iType : superClass.allInterfaces()) {
					if (iType.name().equals(interfaceName))
						return true;
				}
				superClass = superClass.superclass();
			}
		} catch (Exception ignored) {
		}
		return false;
	}

	/**
	 * Ищет метод в ReferenceType по имени и сигнатуре
	 */
	private static Method findMethod(ReferenceType refType, String name, String signature) {
		for (Method m : refType.methodsByName(name, signature)) {
			return m;
		}
		return null;
	}

	public static ObjectReference wrapValueAsObjectReference(Value val) {
		if (val == null) {
			return null;
		}

		// 1. Если уже ObjectReference — просто вернуть
		if (val instanceof ObjectReference objRef) {
			return objRef;
		}

		// 2. Если массив — ArrayReference наследует ObjectReference
		if (val instanceof ArrayReference arrayRef) {
			return arrayRef;
		}

		// 3. Если это объект коллекции, упакованный как внутренний wrapper
		Type type = val.type();
		if (type instanceof ReferenceType refType) {
			try {
				// Попытка привести к ClassType и создать ObjectReference
				if (refType instanceof ClassType classType) {
					// Берем первый поток виртуальной машины для создания экземпляра
					VirtualMachine vm = classType.virtualMachine();
					return classType.newInstance(vm.allThreads().get(0),
							classType.concreteMethodByName("<init>", "()V"), java.util.Collections.emptyList(),
							ClassType.INVOKE_SINGLE_THREADED);
				}
			} catch (Exception ignored) {
				// Если не удалось создать, просто возвращаем null
				return null;
			}
		}

		return null;
	}

	public static int getCollectionSize(ObjectReference instance, BreakpointEvent breakpointEvent) {
		if (instance == null)
			return -1;

		// ===== Если массив =====
		if (instance instanceof ArrayReference arrayRef) {
			return arrayRef.length();
		}

		ReferenceType refType = instance.referenceType();
		if (!(refType instanceof ClassType classType))
			return -1;

		// ===== Попытка получить через поле "size" =====
		// Для стандартных mutable коллекций
		Field sizeField = refType.fieldByName("size");
		if (sizeField != null) {
			Value sizeValue = instance.getValue(sizeField);
			if (sizeValue instanceof IntegerValue intVal) {
				return intVal.value();
			}
		}

		// ===== Попытка через метод size() =====
		Method sizeMethod = classType.concreteMethodByName("size", "()I");
		if (sizeMethod != null && breakpointEvent.thread() != null) {
			try {
				// Оборачиваем вызов для защиты от ошибок JDI
				Value result = instance.invokeMethod(breakpointEvent.thread(), sizeMethod, Collections.emptyList(),
						ObjectReference.INVOKE_SINGLE_THREADED);
				if (result instanceof IntegerValue intVal) {
					return intVal.value();
				}
			} catch (Exception e) {

			}
		}

		// ===== Проверка известных immutable коллекций (Java 9+) через внутренние поля
		// =====
		List<String> knownFields = Arrays.asList("a", "table", "elements"); // возможные внутренние поля массивов
		for (String fieldName : knownFields) {
			Field field = refType.fieldByName(fieldName);
			if (field != null) {
				Value value = instance.getValue(field);
				if (value instanceof ArrayReference innerArray) {
					return innerArray.length();
				}
			}
		}

		// Если не удалось определить размер
		return -1;
	}

	public static  TripletDTO<String, String, String> determinCollectionType(ObjectReference instance,
			BreakpointEvent breakpointEvent) {
		if (Objects.isNull(instance) || Objects.isNull(breakpointEvent))
			return TripletDTO.of(N_A, N_A, N_A);
		String collectionType = N_A;
		String collectionFirstParameter = N_A;
		String collectionSecondParameter = null;
		// Проверяем, что это класс (а не интерфейс)
		ReferenceType refType = instance.referenceType();
		if (refType instanceof ClassType classType) {
			// Берём все интерфейсы, которые реализует этот класс (включая унаследованные)
			List<InterfaceType> interfaces = classType.allInterfaces();
			for (InterfaceType iface : interfaces) {
				if (iface.name().equals("java.util.Map")) {
					collectionType = refType.name();
					collectionSecondParameter = N_A;
					ThreadReference thread = breakpointEvent.thread();
					ClassType mapType = (ClassType) instance.referenceType();

					// entrySet()
					Method entrySetMethod = mapType.concreteMethodByName("entrySet", "()Ljava/util/Set;");
					ObjectReference entrySet;
					try {
						entrySet = (ObjectReference) instance.invokeMethod(breakpointEvent.thread(), entrySetMethod,
								Collections.emptyList(), ObjectReference.INVOKE_SINGLE_THREADED);
						ClassType setType = (ClassType) entrySet.referenceType();
						Method iteratorMethod = setType.concreteMethodByName("iterator", "()Ljava/util/Iterator;");
						ObjectReference iterator = (ObjectReference) entrySet.invokeMethod(thread, iteratorMethod,
								Collections.emptyList(), ObjectReference.INVOKE_SINGLE_THREADED);

						// next()
						ClassType iteratorType = (ClassType) iterator.referenceType();
						Method nextMethod = iteratorType.concreteMethodByName("next", "()Ljava/lang/Object;");
						ObjectReference entry = (ObjectReference) iterator.invokeMethod(thread, nextMethod,
								Collections.emptyList(), ObjectReference.INVOKE_SINGLE_THREADED);

						// getKey / getValue
						ClassType entryType = (ClassType) entry.referenceType();

						Method getKeyMethod = entryType.concreteMethodByName("getKey", "()Ljava/lang/Object;");
						Method getValueMethod = entryType.concreteMethodByName("getValue", "()Ljava/lang/Object;");

						Value key = entry.invokeMethod(thread, getKeyMethod, Collections.emptyList(),
								ObjectReference.INVOKE_SINGLE_THREADED);

						Value value = entry.invokeMethod(thread, getValueMethod, Collections.emptyList(),
								ObjectReference.INVOKE_SINGLE_THREADED);
						key.type();
						collectionFirstParameter = key.type().name();
						collectionSecondParameter = value.type().name();
						System.out.println("key = " + key.type());
						System.out.println("value = " + value);
						System.out.println(collectionType);
						return TripletDTO.of(collectionType, collectionFirstParameter, collectionSecondParameter);
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} else if (iface.name().equals("java.lang.Iterable")) {
					ClassType mapType = (ClassType) instance.referenceType();
					Method iteratorMethod = mapType.concreteMethodByName("iterator", "()Ljava/util/Iterator;");
					try {
						ObjectReference iterator = (ObjectReference) instance.invokeMethod(breakpointEvent.thread(),
								iteratorMethod, Collections.emptyList(), ObjectReference.INVOKE_SINGLE_THREADED);
						ClassType iteratorType = (ClassType) iterator.referenceType();
						collectionType = refType.name();
						String[] parts = instance.toString().split(" ");
						if (parts.length > 2) collectionType = parts[2];
						Method nextMethod = iteratorType.concreteMethodByName("next", "()Ljava/lang/Object;");
						Method hasNextMethod = iteratorType.concreteMethodByName("hasNext", "()Z");
							Value resultHasNext = iterator.invokeMethod(breakpointEvent.thread(), hasNextMethod,
									Collections.emptyList(), ObjectReference.INVOKE_SINGLE_THREADED);
							if ((resultHasNext instanceof BooleanValue bool) && (bool.value())) {
								Value element = iterator.invokeMethod(breakpointEvent.thread(), nextMethod,
										Collections.emptyList(), ObjectReference.INVOKE_SINGLE_THREADED);
								collectionFirstParameter = element.type().name();
							}
							
							
							return TripletDTO.of(collectionType, collectionFirstParameter, collectionSecondParameter);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

			}
		}
		return TripletDTO.empty();
	}

	// ---------------------- Вспомогательные методы ----------------------

	private static ObjectReference getFirstElementFromIterator(ObjectReference collectionOrIterator, ThreadReference thread)
	        throws Exception {
	    ClassType iteratorType;
	    ObjectReference iterator;

	    if (collectionOrIterator.referenceType().name().equals("java.util.Iterator")) {
	        iterator = collectionOrIterator;
	        iteratorType = (ClassType) iterator.referenceType();
	    } else {
	        // Получаем iterator() для коллекции
	        Method iteratorMethod = ((ClassType) collectionOrIterator.referenceType())
	                .concreteMethodByName("iterator", "()Ljava/util/Iterator;");
	        iterator = (ObjectReference) collectionOrIterator.invokeMethod(thread,
	                iteratorMethod, Collections.emptyList(), ObjectReference.INVOKE_SINGLE_THREADED);
	        iteratorType = (ClassType) iterator.referenceType();
	    }

	    Method hasNext = iteratorType.concreteMethodByName("hasNext", "()Z");
	    Value hasNextVal = iterator.invokeMethod(thread, hasNext, Collections.emptyList(), ObjectReference.INVOKE_SINGLE_THREADED);
	    if (hasNextVal instanceof BooleanValue bool && bool.value()) {
	        Method next = iteratorType.concreteMethodByName("next", "()Ljava/lang/Object;");
	        return (ObjectReference) iterator.invokeMethod(thread, next, Collections.emptyList(),
	                ObjectReference.INVOKE_SINGLE_THREADED);
	    }
	    return null;
	}

	private static boolean isLocalVariable(ReferenceType refType) {
	    // Если имя пакета начинается с "java." или "jdk." – скорее всего поле, иначе локальная переменная
	    return !refType.name().startsWith("java.") && !refType.name().startsWith("jdk.");
	}

	private static Value invokeMethod(ObjectReference obj, String methodName, ThreadReference thread) throws Exception {
	    ClassType type = (ClassType) obj.referenceType();
	    Method method = type.concreteMethodByName(methodName, "()Ljava/lang/Object;");
	    return obj.invokeMethod(thread, method, Collections.emptyList(), ObjectReference.INVOKE_SINGLE_THREADED);
	}

	public static List<Value> iterateThroughCollection(ObjectReference instance, BreakpointEvent breakpointEvent) {
	    List<Value> result = new ArrayList<>();
	    if (instance == null || breakpointEvent == null) {
	        return result;
	    }
	    ReferenceType refType = instance.referenceType();
	    if (!(refType instanceof ClassType classType)) {
	        return result;
	    }
	    boolean isIterable = classType.allInterfaces().stream()
	            .anyMatch(iface -> "java.lang.Iterable".equals(iface.name()));

	    if (!isIterable) {
	        return result;
	    }
	    ThreadReference thread = breakpointEvent.thread();
	    try {
	        Method iteratorMethod = classType.concreteMethodByName("iterator", "()Ljava/util/Iterator;");
	        if (iteratorMethod == null) {
	            return result;
	        }
	        Value iteratorValue = instance.invokeMethod(
	                thread,
	                iteratorMethod,
	                Collections.emptyList(),
	                ObjectReference.INVOKE_SINGLE_THREADED
	        );
	        if (!(iteratorValue instanceof ObjectReference iterator)) {
	            return result;
	        }
	        if (!(iterator.referenceType() instanceof ClassType iteratorType)) {
	            return result;
	        }
	        Method hasNextMethod = iteratorType.concreteMethodByName("hasNext", "()Z");
	        Method nextMethod = iteratorType.concreteMethodByName("next", "()Ljava/lang/Object;");
	        if (hasNextMethod == null || nextMethod == null) {
	            return result;
	        }
	        while (true) {
	            Value hasNextValue = iterator.invokeMethod(
	                    thread,
	                    hasNextMethod,
	                    Collections.emptyList(),
	                    ObjectReference.INVOKE_SINGLE_THREADED
	            );
	            if (!(hasNextValue instanceof BooleanValue booleanValue)) {
	                break;
	            }
	            if (!booleanValue.value()) {
	                break;
	            }
	            Value element = iterator.invokeMethod(
	                    thread,
	                    nextMethod,
	                    Collections.emptyList(),
	                    ObjectReference.INVOKE_SINGLE_THREADED
	            );
	            result.add(element);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return result;
	}
	
	public static String compileCollectionDescription(TripletDTO<String, String, String> tripletDTO) {
		if (Objects.isNull(tripletDTO.getFirst())) return "";
		if (Objects.isNull(tripletDTO.getThird())) {
			return tripletDTO.getFirst() + "<" + tripletDTO.getSecond() + ">";
		} else {
			return tripletDTO.getFirst() + "<" + tripletDTO.getSecond() + ", " + tripletDTO.getThird() + ">";
		}
	}
	
    /**
     * Преобразует ObjectReference в читаемое строковое значение.
     * Для примитивов, обёрток и String возвращает значение.
     * Для коллекций и массивов возвращает краткое описание.
     */
    public static String getObjectReferenceValueAsString(ObjectReference objRef) {
        if (objRef == null) return "null";

        try {
            ReferenceType refType = objRef.referenceType();
            String typeName = refType.name();

            // String
            if ("java.lang.String".equals(typeName) && objRef instanceof StringReference sRef) {
                return sRef.value();
            }

            // Обёртки примитивов
            if (typeName.startsWith("java.lang.") &&
                List.of("Integer","Long","Short","Byte","Double","Float","Boolean","Character").contains(typeName.substring(10))) {
                Field valueField = refType.fieldByName("value");
                if (valueField != null) {
                    Value val = objRef.getValue(valueField);
                    return val != null ? val.toString() : "null";
                }
            }

            // Массивы
            if (objRef instanceof ArrayReference arrayRef) {
                return "Array[" + arrayRef.length() + "]";
            }

            // Коллекции
            if (implementsInterface(refType, "java.util.Collection")) {
                return "Collection[size=" + getCollectionSize(objRef) + "]";
            }

            // Map
            if (implementsInterface(refType, "java.util.Map")) {
                return "Map[size=" + getCollectionSize(objRef) + "]";
            }

            // Любой другой объект
            return typeName;

        } catch (Exception e) {
            return "<error>";
        }
    }

    

    /**
     * Получает размер коллекции или карты через invokeMethod.
     */
    private static int getCollectionSize(ObjectReference objRef) {
        try {
            Method sizeMethod = objRef.referenceType().methodsByName("size").stream().findFirst().orElse(null);
            if (sizeMethod != null) {
                Value sizeValue = objRef.invokeMethod(
                        objRef.virtualMachine().allThreads().get(0),
                        sizeMethod,
                        List.of(),
                        ObjectReference.INVOKE_SINGLE_THREADED
                );
                if (sizeValue instanceof PrimitiveValue pVal) {
                    return Integer.parseInt(pVal.toString());
                }
            }
        } catch (Exception ignored) { }
        return -1;
    }

    public static Comparable<?> getComparableValue(ObjectReference ref) {
        if (ref == null) return null;

        ReferenceType refType = ref.referenceType();
        String typeName = refType.name();

        // ---------- String ----------
        if ("java.lang.String".equals(typeName)) {
            StringReference sRef = (StringReference) ref;
            return sRef.value();
        }

        // ---------- Wrapper ----------
        if (typeName.equals("java.lang.Integer")) {
            return ((IntegerValue) ref.getValue(refType.fieldByName("value"))).value();
        }
        if (typeName.equals("java.lang.Long")) {
            return ((LongValue) ref.getValue(refType.fieldByName("value"))).value();
        }
        if (typeName.equals("java.lang.Double")) {
            return ((DoubleValue) ref.getValue(refType.fieldByName("value"))).value();
        }
        if (typeName.equals("java.lang.Float")) {
            return ((FloatValue) ref.getValue(refType.fieldByName("value"))).value();
        }
        if (typeName.equals("java.lang.Boolean")) {
            return ((BooleanValue) ref.getValue(refType.fieldByName("value"))).value();
        }
        if (typeName.equals("java.lang.Character")) {
            return (char) ((CharValue) ref.getValue(refType.fieldByName("value"))).value();
        }
        if (typeName.equals("java.lang.Byte")) {
            return ((ByteValue) ref.getValue(refType.fieldByName("value"))).value();
        }
        if (typeName.equals("java.lang.Short")) {
            return ((ShortValue) ref.getValue(refType.fieldByName("value"))).value();
        }

        // ---------- Примитивные поля (если ObjectReference ссылается на объект с одним полем value) ----------
        // можно расширить при необходимости

        // ---------- Неизвестный объект ----------
        return null;
    }
    
    public static String toReadableSignature(com.sun.jdi.Method method) {
        StringBuilder sb = new StringBuilder();

        sb.append(method.declaringType().name());
        sb.append(".").append(method.name());
        sb.append("(");

        var args = method.argumentTypeNames();
        for (int i = 0; i < args.size(); i++) {
            sb.append(args.get(i));
            if (i < args.size() - 1) {
                sb.append(",");
            }
        }

        sb.append(")");
        return sb.toString();
    }
	
}
