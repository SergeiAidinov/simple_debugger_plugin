package com.gmail.aydinov.sergey.simple_debugger_plugin.utils;

import java.util.List;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationElementRepresentation;
import com.sun.jdi.Location;
import com.sun.jdi.StackFrame;

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
}
