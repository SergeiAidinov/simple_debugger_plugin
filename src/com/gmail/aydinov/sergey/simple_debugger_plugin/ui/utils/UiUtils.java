package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils;

import java.util.Set;

public class UiUtils {
	
	private static final Set<String> JAVA_STANDARD_TYPES = Set.of("int", "long", "short", "byte", "float", "double",
			"boolean", "char", "java.lang.Integer", "java.lang.Long", "java.lang.Short", "java.lang.Byte",
			"java.lang.Float", "java.lang.Double", "java.lang.Boolean", "java.lang.Character", "java.lang.String");
	
	/**
	 * Преобразование строки в нужный примитив / объект
	 */
	public static Object convertToType(String value, String type) {
		try {
			return switch (type) {
			case "int", "java.lang.Integer" -> Integer.parseInt(value);
			case "long", "java.lang.Long" -> Long.parseLong(value);
			case "short", "java.lang.Short" -> Short.parseShort(value);
			case "byte", "java.lang.Byte" -> Byte.parseByte(value);
			case "float", "java.lang.Float" -> Float.parseFloat(value);
			case "double", "java.lang.Double" -> Double.parseDouble(value);
			case "boolean", "java.lang.Boolean" -> Boolean.parseBoolean(value);
			case "char", "java.lang.Character" -> value.length() > 0 ? value.charAt(0) : '\0';
			case "java.lang.String" -> value;
			default -> value; // fallback для неизвестных типов
			};
		} catch (Exception e) {
			return value; // если не удалось преобразовать, оставляем как строку
		}
	}
	
	public static boolean isStandartJavaType(String type) {
		return JAVA_STANDARD_TYPES.contains(type);
	}

}
