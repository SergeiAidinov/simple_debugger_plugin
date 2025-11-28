package com.gmail.aydinov.sergey.simple_debugger_plugin.utils;

import java.util.List;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.ThreadReference;

/**
 * Вспомогательный класс-заглушка для локальных переменных
 */
public class DebugUtils {

	public static Value createJdiValueFromString(VirtualMachine vm, LocalVariable var, String str) {
		String type = var.typeName();
		switch (type) {
		case "int":
			return vm.mirrorOf(Integer.parseInt(str));
		case "long":
			return vm.mirrorOf(Long.parseLong(str));
		case "short":
			return vm.mirrorOf(Short.parseShort(str));
		case "byte":
			return vm.mirrorOf(Byte.parseByte(str));
		case "char":
			return vm.mirrorOf(str.charAt(0));
		case "boolean":
			return vm.mirrorOf(Boolean.parseBoolean(str));
		case "float":
			return vm.mirrorOf(Float.parseFloat(str));
		case "double":
			return vm.mirrorOf(Double.parseDouble(str));
		case "java.lang.String":
			return vm.mirrorOf(str);
		default:
			throw new IllegalArgumentException("Unsupported type: " + type);
		}
	}

	/**
	 * Создаёт JDI Value из строки для установки в локальную переменную или поле.
	 *
	 * @param vm        VirtualMachine таргет-процесса
	 * @param varType   тип переменной (LocalVariable.type() или Field.type())
	 * @param textValue строковое значение из UI
	 * @param thread    поток, на котором создаём boxed объекты
	 */
	public static Value createJdiObjectFromString(VirtualMachine vm, Type varType, String textValue,
			ThreadReference thread) {

		if (textValue == null) {
			return nullJdiValue(vm, varType);
		}

		String trimmed = textValue.trim();

		// ------------------------
		// 1. null
		// ------------------------
		if (trimmed.equals("null"))
			return nullJdiValue(vm, varType);

		String typeName = varType.name();

		// ------------------------
		// 2. Примитивы
		// ------------------------
		try {
			if (typeName.equals("int"))
				return vm.mirrorOf(Integer.parseInt(trimmed));

			if (typeName.equals("long"))
				return vm.mirrorOf(Long.parseLong(trimmed));

			if (typeName.equals("float"))
				return vm.mirrorOf(Float.parseFloat(trimmed));

			if (typeName.equals("double"))
				return vm.mirrorOf(Double.parseDouble(trimmed));

			if (typeName.equals("boolean"))
				return vm.mirrorOf(Boolean.parseBoolean(trimmed));

			if (typeName.equals("char")) {
				if (trimmed.length() == 1) {
					return vm.mirrorOf(trimmed.charAt(0));
				} else if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() == 3) {
					return vm.mirrorOf(trimmed.charAt(1));
				}
			}

			if (typeName.equals("byte"))
				return vm.mirrorOf(Byte.parseByte(trimmed));

			if (typeName.equals("short"))
				return vm.mirrorOf(Short.parseShort(trimmed));
		} catch (Exception e) {
			throw new RuntimeException("Cannot parse primitive for type: " + typeName + " value: " + trimmed, e);
		}

		// ------------------------
		// 3. String
		// ------------------------
		if (typeName.equals("java.lang.String")) {
			if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2)
				trimmed = trimmed.substring(1, trimmed.length() - 1);
			return vm.mirrorOf(trimmed);
		}
		
		// ------------------------
		// 4. Boxing-types
		// ------------------------
		if (typeName.equals("java.lang.Integer")) {
		    Value prim = vm.mirrorOf(Integer.parseInt(trimmed));
		    return newBoxed(vm, (ClassType) varType, prim, thread);
		}

		if (typeName.equals("java.lang.Long")) {
		    Value prim = vm.mirrorOf(Long.parseLong(trimmed));
		    return newBoxed(vm, (ClassType) varType, prim, thread);
		}

		if (typeName.equals("java.lang.Boolean")) {
		    Value prim = vm.mirrorOf(Boolean.parseBoolean(trimmed));
		    return newBoxed(vm, (ClassType) varType, prim, thread);
		}

		if (typeName.equals("java.lang.Double")) {
		    Value prim = vm.mirrorOf(Double.parseDouble(trimmed));
		    return newBoxed(vm, (ClassType) varType, prim, thread);
		}

		if (typeName.equals("java.lang.Float")) {
		    Value prim = vm.mirrorOf(Float.parseFloat(trimmed));
		    return newBoxed(vm, (ClassType) varType, prim, thread);
		}

		if (typeName.equals("java.lang.Character")) {
		    char c = trimmed.length() == 1 ? trimmed.charAt(0) : trimmed.charAt(1);
		    Value prim = vm.mirrorOf(c);
		    return newBoxed(vm, (ClassType) varType, prim, thread);
		}
		
		return null;
	}

	// ====================================================
	// Вспомогательные методы
	// ====================================================

	private static Value nullJdiValue(VirtualMachine vm, Type type) {
		if (type instanceof PrimitiveType)
			throw new RuntimeException("Cannot assign null to primitive " + type.name());

		return vm.mirrorOf(null);
	}

	/**
	 * Создаёт boxed значение через ClassType.newInstance(...). Например: new
	 * Integer(intValue)
	 */
	private static ObjectReference newBoxed(VirtualMachine vm, ClassType clazz, Value primitive,
			ThreadReference thread) {
		// ищем конструктор с одним аргументом
		List<Method> methods = clazz.methods();
		for (Method m : methods) {
			if (m.isConstructor()) {
				List<Type> args = null;
				try {
					args = m.argumentTypes();
				} catch (ClassNotLoadedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (args != null && args.size() == 1) {
					try {
						// Вызываем конструктор через newInstance
						return clazz.newInstance(thread, m, List.of(primitive), ObjectReference.INVOKE_SINGLE_THREADED);
					} catch (Exception e) {
						throw new RuntimeException("Error creating boxed " + clazz.name(), e);
					}
				}
			}
		}

		throw new RuntimeException("No suitable constructor found for boxed type: " + clazz.name());
	}
}
