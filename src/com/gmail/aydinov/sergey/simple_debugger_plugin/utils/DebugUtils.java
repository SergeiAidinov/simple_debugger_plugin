package com.gmail.aydinov.sergey.simple_debugger_plugin.utils;

import java.util.List;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationElementRepresentation;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

/**
 * Вспомогательный класс-заглушка для локальных переменных
 */
public class DebugUtils {

    /**
     * Возвращает массив объектов для отображения локальных переменных
     * Для заглушки каждый элемент — пара {имя переменной, значение}
     */
    public static Object[][] getLocalVariables() {
        // Заглушка: можно добавить любые тестовые данные
        return new Object[][]{
            {"i", 42},
            {"count", 10},
            {"name", "Test"},
            {"flag", true}
        };
    }

	public static Object[] getLocalVariables(Location loc, StackFrame frame) {
		// TODO Auto-generated method stub
		return getLocalVariables();
	}
	
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
}
