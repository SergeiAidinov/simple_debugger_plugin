package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils;

import java.util.Set;

import org.eclipse.swt.graphics.Image;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

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
	
	public static Image getIcon(InnerElementRepresentationDTO dto) {

	    if (dto == null) {
	        return null;
	    }

	    ValueCategory category = dto.getValueCategory();
	    if (category == null) {
	        return null;
	    }

	    // 1. Коллекции и Map
	    if (category == ValueCategory.COLLECTION || category == ValueCategory.MAP) {
	        return SimpleDebugerWindowsManager.instance()
	                .icons.get("lens")
	                .getFirst();
	    }

	    // 2. Только пользовательские объекты
	    if (category != ValueCategory.USER_OBJECT) {
	        return null;
	    }

	    // 3. Только поля (не локальные переменные, не методы)
	    UniversalElementType type = dto.getElementType();
	    if (type != UniversalElementType.NON_STATIC_FIELD &&
	        type != UniversalElementType.STATIC_FIELD) {
	        return null;
	    }

	    // 4. Значение должно быть инициализировано
	    if (dto.getValue() == null) {
	        return null;
	    }

	    // 5. Исключаем стандартные типы Java (Integer, String и т.д.)
	    if (UiUtils.isStandartJavaType(dto.getTypeOrReturnType())) {
	        return null;
	    }

	    return SimpleDebugerWindowsManager.instance()
	            .icons.get("inspectIcon")
	            .getFirst();
	}

}
