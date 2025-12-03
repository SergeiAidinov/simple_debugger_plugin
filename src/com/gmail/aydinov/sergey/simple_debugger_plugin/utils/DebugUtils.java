package com.gmail.aydinov.sergey.simple_debugger_plugin.utils;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.MethodCallInStack;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationMethodParameterDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.VariableDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.InvokeMethodEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventDTO;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.widgets.Text;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.texteditor.ITextEditor;



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
	

    private static VariableDTO mapField(Map.Entry<Field, Value> entry) {
        Field f = entry.getKey();
        Value v = entry.getValue();
        return new VariableDTO(f.name(), f.typeName(), valueToString(v));
    }

    private static VariableDTO mapLocal(Map.Entry<LocalVariable, Value> entry) {
        LocalVariable v = entry.getKey();
        Value val = entry.getValue();
        return new VariableDTO(v.name(), v.typeName(), valueToString(val));
    }

    /**
     * Преобразует Map<Field, Value> в List<VariableDTO>
     */
    public static List<VariableDTO> mapFields(Map<Field, Value> fields) {
        if (fields == null) return List.of();

        return fields.entrySet().stream()
                .map(entry -> {
                    Field field = entry.getKey();
                    Value value = entry.getValue();
                    return new VariableDTO(
                            field.name(),
                            field.typeName(),
                            valueToString(value)
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Преобразует Map<LocalVariable, Value> в List<VariableDTO>
     */
    public static List<VariableDTO> mapLocals(Map<LocalVariable, Value> locals) {
        if (locals == null) return List.of();

        return locals.entrySet().stream()
                .map(entry -> {
                    LocalVariable local = entry.getKey();
                    Value value = entry.getValue();
                    return new VariableDTO(
                            local.name(),
                            local.typeName(),
                            valueToString(value)
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Преобразует Value в строку, безопасно обрабатывая null
     */
    private static String valueToString(Value value) {
        if (value == null) return "null";
        return value.toString();
    }
    
    /**
     * Преобразует argumentsText в список JDI-значений для вызова метода.
     */
    public static List<Value> parseArguments(VirtualMachine vm, InvokeMethodEvent invokeMethodEvent) {
        List<Value> values = new ArrayList<>();

        String argsText = invokeMethodEvent.getArgumentsText().trim();

        // Очищаем скобки, если метод указан как method(arg1, arg2)
        int start = argsText.indexOf('(');
        int end = argsText.lastIndexOf(')');
        if (start >= 0 && end > start) {
            argsText = argsText.substring(start + 1, end).trim();
        }

        if (argsText.isEmpty()) return values;

        String[] argStrings = argsText.split("\\s*,\\s*");
        List<TargetApplicationMethodParameterDTO> params = invokeMethodEvent.getMethod().getParameters();

        if (argStrings.length != params.size()) {
            throw new IllegalArgumentException("Количество аргументов не совпадает с количеством параметров метода");
        }

        for (int i = 0; i < params.size(); i++) {
            TargetApplicationMethodParameterDTO param = params.get(i);
            String argStr = argStrings[i].trim();
            String typeName = param.getType().name();

            // Убираем кавычки у строк
            if ((argStr.startsWith("\"") && argStr.endsWith("\"")) ||
                (argStr.startsWith("'") && argStr.endsWith("'"))) {
                argStr = argStr.substring(1, argStr.length() - 1);
            }

            Value value = null;
            try {
                value = convertStringToValue(argStr, typeName, vm);
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalArgumentException("Ошибка конвертации аргумента '" + argStr + "' в тип " + typeName, e);
            }

            values.add(value);
        }

        return values;
    }


    
    private static Value convertStringToValue(String arg, String typeName, VirtualMachine vm) throws Exception {
        switch (typeName) {
            case "int": return vm.mirrorOf(Integer.parseInt(arg));
            case "boolean": return vm.mirrorOf(Boolean.parseBoolean(arg));
            case "long": return vm.mirrorOf(Long.parseLong(arg));
            case "double": return vm.mirrorOf(Double.parseDouble(arg));
            case "float": return vm.mirrorOf(Float.parseFloat(arg));
            case "short": return vm.mirrorOf(Short.parseShort(arg));
            case "byte": return vm.mirrorOf(Byte.parseByte(arg));
            case "char":
                if (arg.length() != 1) throw new IllegalArgumentException("Неверный char аргумент: " + arg);
                return vm.mirrorOf(arg.charAt(0));
            case "java.lang.String": return vm.mirrorOf(arg);
            default:
                // Для объектов пытаемся создать экземпляр через конструктор без аргументов
                List<ReferenceType> classes = vm.classesByName(typeName);
                if (classes.isEmpty()) throw new ClassNotLoadedException(typeName, "Класс не найден в таргет-приложении");

                ReferenceType refType = classes.get(0);
                if (!(refType instanceof ClassType)) throw new IllegalArgumentException("Тип не является классом: " + typeName);

                ClassType classType = (ClassType) refType;
                Method constructor = classType.concreteMethodByName("<init>", "()V");
                if (constructor == null) throw new IllegalArgumentException("Нет конструктора без аргументов для " + typeName);

                ObjectReference obj = classType.newInstance(vm.allThreads().get(0), constructor, List.of(), ClassType.INVOKE_SINGLE_THREADED);
                return obj;
        }
    }
    public static List<MethodCallInStack> compileStackInfo(ThreadReference threadReference) {
		List<StackFrame> frames = Collections.emptyList();
		List<MethodCallInStack> calls = new ArrayList<>();

		try {
			frames = threadReference.frames();
		} catch (IncompatibleThreadStateException e) {
			e.printStackTrace();
			return List.of(new MethodCallInStack("Cannot get frames: " + e.getMessage(), "", ""));
		}

		for (int i = 0; i < frames.size(); i++) {
			StackFrame frame = frames.get(i);
			if (frame == null)
				continue;

			try {
				Location loc = frame.location();
				if (loc != null) {
					String className = loc.declaringType() != null ? loc.declaringType().name() : "Unknown";
					String methodName = loc.method() != null ? loc.method().name() : "unknown";
					int line = loc.lineNumber();

					String sourceInfo;
					try {
						String src = loc.sourceName();
						sourceInfo = src + ":" + line;
					} catch (AbsentInformationException aie) {
						sourceInfo = "Unknown:" + line;
					}

					// calls.add(String.format("#%d %s.%s() at %s%n", i, className, methodName,
					// sourceInfo));
					calls.add(new MethodCallInStack(className, methodName, sourceInfo));
				}
			} catch (Exception e) {
				// защищаемся от возможных исключений JDI
				e.printStackTrace();
				// calls.add(String.format("#%d <error retrieving frame>%n", i));
				calls.add(new MethodCallInStack("<error retrieving frame>", "", ""));
			}
		}
		Collections.reverse(calls);
		return calls;
	}
    
}
    

