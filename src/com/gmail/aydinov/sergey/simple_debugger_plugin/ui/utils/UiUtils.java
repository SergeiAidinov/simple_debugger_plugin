package com.gmail.aydinov.sergey.simple_debugger_plugin.ui.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserElementDetailDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.details.UserInstanceDetailsDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;

public class UiUtils {
	
	private static String GAP = "  ";
	private static String SEPARATOR = "---------------------------------------------- \n";
	
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
	    if (type != UniversalElementType.FIELD) {
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
	
	public static Image getTypeIcon(InnerElementRepresentationDTO dto) {
	    if (dto == null || dto.getElementType() == UniversalElementType.REFERENCE)
	        return null; // REFERENCE не отображаем

	    String key = switch (dto.getElementType()) {
	        case INTERFACE -> "interface";
	        case CLASS -> "class";
	        case METHOD -> dto.isStatic() ? "static_method" : "method";
	        case FIELD -> dto.isStatic() ? "static_field" : "fieldIcon";
	        case LOCAL_VARIABLE -> "variableIcon";
	        case ENUM -> "enum";
	        default -> "unknown";
	    };

	    PairDTO<Image, String> pair = SimpleDebugerWindowsManager.instance().icons.get(key);
	    return pair != null ? pair.getFirst() : null;
	}
	
	public static PairDTO<Image, String> getTypeTooltip(InnerElementRepresentationDTO dto) {
        if (dto == null) return null;

        String key = switch (dto.getElementType()) {
            case INTERFACE -> "interface";
            case CLASS -> "class";
            case METHOD -> dto.isStatic() ? "static_method" : "method";
            case FIELD -> dto.isStatic() ? "static_field" : "fieldIcon";
            case LOCAL_VARIABLE -> "variableIcon";
            case ENUM -> "enum";
            default -> "unknown";
        };

        return SimpleDebugerWindowsManager.instance()
                .icons.getOrDefault(key,
                        SimpleDebugerWindowsManager.instance().icons.get("unknown"));
    }
	
	public static Point adjustToScreen(Composite root, Point desiredLocation, Point popupSize) {
		Display display = root.getDisplay();
		Rectangle screen = display.getPrimaryMonitor().getClientArea();
		int x = desiredLocation.x;
		int y = desiredLocation.y;
		if (x + popupSize.x > screen.x + screen.width) {
			x = screen.x + screen.width - popupSize.x;
		}
		if (y + popupSize.y > screen.y + screen.height) {
			y = screen.y + screen.height - popupSize.y;
		}
		if (x < screen.x) {
			x = screen.x;
		}
		if (y < screen.y) {
			y = screen.y;
		}
		return new Point(x, y);
	}
	
	public static String buildUserObjectText(UserInstanceDetailsDTO dto) {
		StringBuilder info = new StringBuilder();

		info.append("Field name: ").append(dto.getFieldName()).append("\n");
		info.append("Field type: ").append(dto.getTypeName()).append("\n\n");

		info.append("INFO:\nFields:\n");
		addGroupOfElements(info, dto.getInnerElementsByGroups().get(1), List.of("name: ", "type: ", "value: "));

		info.append("Methods:\n");
		addGroupOfElements(info, dto.getInnerElementsByGroups().get(2), List.of("name: ", "return type: ", ""));

		info.append("Others:\n");
		addGroupOfElements(info, dto.getInnerElementsByGroups().get(3), List.of("name: ", "type: ", "value: "));

		return info.toString();
	}

	private static void addGroupOfElements(StringBuilder stringBuilder, List<UserElementDetailDTO> list,
			List<String> markers) {

		for (int outer = 0; outer < list.size(); outer++) {
			UserElementDetailDTO innerElement = list.get(outer);
			for (int i = 0; i < markers.size(); i++) {
				String announce = markers.get(i);
				if (i == 0) {
					Optional.ofNullable(announce).ifPresent(e -> stringBuilder.append(GAP).append(announce)
							.append(innerElement.getName()).append("\n"));
				} else if (i == 1) {
					Optional.ofNullable(announce).ifPresent(e -> stringBuilder.append(GAP).append(announce)
							.append(innerElement.getTypeOrReturnType()).append("\n"));
				} else if (i == 2) {
					Optional.ofNullable(announce).ifPresent(e -> stringBuilder.append(GAP).append(announce)
							.append(innerElement.getValue()).append("\n"));
				}
				if (i == 2 && (list.size() - outer != 1))
					stringBuilder.append("\n");
			}
		}
		stringBuilder.append(SEPARATOR + "\n");
	}
	
	public static int getColumnIndexAtPoint(Table table, int x) {
		int offset = 0;
		for (int i = 0; i < table.getColumnCount(); i++) {
			offset += table.getColumn(i).getWidth();
			if (x < offset)
				return i;
		}
		return table.getColumnCount() - 1;
	}


}
