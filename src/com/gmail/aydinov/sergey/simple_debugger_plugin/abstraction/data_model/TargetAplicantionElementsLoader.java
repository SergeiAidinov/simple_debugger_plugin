package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.CurrentRole;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TripletDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.Field;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;

public class TargetAplicantionElementsLoader {
	
	private final int CURRENT_LEVEL_RECURSION;
	
	

	public TargetAplicantionElementsLoader(int additionalDepthOfRecursion) {
		
		CURRENT_LEVEL_RECURSION = TargetApplicationRepresentation.DEAFAULT_LEVEL_RECURSION + additionalDepthOfRecursion;
	}

	public Map<AbstractElementRepresentation.Tag, AbstractElementRepresentation> recursievlyPopulateSubordinatesElements(
			PairDTO<Tag, UniversalElementRepresentation> pairDTO, BreakpointEvent breakpointEvent,
			Map<AbstractElementRepresentation.Tag, AbstractElementRepresentation> subordinates, int level, boolean shoulExpandCollections) {
		// final Map<AbstractElementRepresentation.Tag, AbstractElementRepresentation>
		// subordinates = new ConcurrentHashMap<>();
		UniversalElementRepresentation parentElement = pairDTO.getSecond();
		ObjectReference objRef = parentElement.getObjectReference();

		if (objRef != null) {
			long uniqueId = objRef.uniqueID();
//			if (!visitedElements.containsKey(uniqueId)) {
//				visitedElements.put(uniqueId, parentElement.getTag());
//			}
		}

		for (Method method : parentElement.getReferenceType().allMethods()) {

			if (DebugUtils.shouldSkipMethod(method))
				continue;
			if (!method.declaringType().equals(parentElement.getReferenceType()))
				continue;

			List<String> args = method.argumentTypeNames();
			String methodArgs = String.join(", ", args);

			UniversalElementRepresentation methodElement = UniversalElementRepresentation.builder()
					.referenceType(parentElement.getReferenceType()).objectReference(parentElement.getObjectReference())
					.elementName(method.name() + "()").additionalInfo(method.returnTypeName())
					.elementType(UniversalElementRepresentation.UniversalElementType.METHOD)
					.currentRole(UniversalElementRepresentation.CurrentRole.INNER)
					.value(parentElement.getAdditionalInfo() + "." + method.name() + "(" + methodArgs + ")")
					.isStatic(method.isStatic())
					.valueCategory(UniversalElementRepresentation.ValueCategory.NOT_SPECIFIED)
					.typeOrReturnType(method.returnTypeName()).uniqueId(UUID.randomUUID())
					.parentUniqueId(parentElement.getTag().getUniqueId()).level(parentElement.getLevel() + 1).build();
			// System.out.println("METhOD FOUND: " + methodElement.toString());
			subordinates.put(methodElement.getTag(), methodElement);
		}

		for (Field field : parentElement.getReferenceType().allFields()) {
			addField(pairDTO.getSecond(), field, subordinates, breakpointEvent, level, shoulExpandCollections);
		}

		return subordinates;
	}

	private void addField(UniversalElementRepresentation parentElement, Field field,
			Map<AbstractElementRepresentation.Tag, AbstractElementRepresentation> subordinates,
			BreakpointEvent breakpointEvent, int level, boolean shouldExpandCollections) {
		if (level >= CURRENT_LEVEL_RECURSION) {
			return;
		}
		Value value = null;
		ObjectReference instance = parentElement.getObjectReference();

		try {
			if (field.isStatic()) {
				value = field.declaringType().getValue(field);
			} else if (instance != null) {
				value = instance.getValue(field);
			}
		} catch (Exception e) {
			SimpleDebuggerLogger.warn("Failed to get value for field: " + field.name());
			return;
		}

		ObjectReference valueObj = (value instanceof ObjectReference) ? (ObjectReference) value : null;
		String valueText = (value == null) ? "<null>" : value.toString();
		ValueCategory category = DebugUtils.determineValueCategory(value);
		if (List.of(ValueCategory.ARRAY /*, ValueCategory.COLLECTION,  ValueCategory.MAP */).contains(category)
				&& Objects.nonNull(breakpointEvent)) {
			int size = DebugUtils.getCollectionSize(valueObj, breakpointEvent);
			TripletDTO<String, String, String> data = DebugUtils.determinCollectionType(valueObj, breakpointEvent);
			String parameters = Objects.isNull(data.getThird()) ? "params.:<" + data.getSecond() + ">"
					: "params.:<" + data.getSecond() + ", " + data.getThird() + ">";
			valueText = parameters + ", " + data.getFirst() + ", size:" + size;
		}

		if (List.of(ValueCategory.COLLECTION, ValueCategory.MAP).contains(category)) {
			int size = (valueObj != null) ? DebugUtils.getCollectionSize(valueObj) : -1;
			String valueTextWithSize = "size: " + (size != -1 ? size : "?") + "; " + valueText;
			String typeOrReturnType = field.name();
			if (Objects.nonNull(breakpointEvent)) {
				TripletDTO<String, String, String> data = DebugUtils.determinCollectionType(valueObj, breakpointEvent);
				int collectionSize = DebugUtils.getCollectionSize(valueObj, breakpointEvent);
				String parameters = Objects.isNull(data.getThird()) ? "params.:<" + data.getSecond() + ">"
						: "params.:<" + data.getSecond() + ", " + data.getThird() + ">";
				valueTextWithSize = "size:" + collectionSize + ", " + parameters + ", " + data.getFirst();
			}
			UniversalElementRepresentation collectionElement = UniversalElementRepresentation.builder()
					.referenceType(valueObj != null ? valueObj.referenceType() : parentElement.getReferenceType())
					.objectReference(valueObj).elementName(field.name()).additionalInfo(field.typeName())
					.elementType(UniversalElementType.FIELD).currentRole(CurrentRole.INNER).value(valueTextWithSize)
					.isStatic(field.isStatic()).valueCategory(category).typeOrReturnType(typeOrReturnType)
					.uniqueId(UUID.randomUUID()).parentUniqueId(parentElement.getTag().getUniqueId()).level(level)
					.build();

			subordinates.put(collectionElement.getTag(), collectionElement);

		} else if (ValueCategory.ARRAY == category && (valueObj instanceof ArrayReference arrayRef)) {
			int size = arrayRef.length();
			ArrayType arrayType = (ArrayType) valueObj.referenceType();
			String elementType = arrayType.componentTypeName();
			valueText = "size:" + size + ", elementType:" + elementType;
			UniversalElementRepresentation arrayElement = UniversalElementRepresentation.builder()
					.referenceType(valueObj.referenceType()).objectReference(valueObj).elementName(field.name())
					.additionalInfo(field.typeName()).elementType(UniversalElementType.FIELD)
					.currentRole(CurrentRole.INNER).value(valueText).isStatic(field.isStatic())
					.valueCategory(ValueCategory.ARRAY).typeOrReturnType(field.typeName()).uniqueId(UUID.randomUUID())
					.parentUniqueId(parentElement.getTag().getUniqueId()).level(level).build();
			subordinates.put(arrayElement.getTag(), arrayElement);
		} else {
			// ---------------- Создаем элемент поля
			UniversalElementRepresentation fieldElement = UniversalElementRepresentation.builder()
					.referenceType(valueObj != null ? valueObj.referenceType() : parentElement.getReferenceType())
					.objectReference(valueObj).elementName(field.name()).additionalInfo(field.typeName())
					.elementType(UniversalElementType.FIELD).currentRole(CurrentRole.INNER).value(valueText)
					.isStatic(field.isStatic()).valueCategory(category).typeOrReturnType(field.typeName())
					.uniqueId(UUID.randomUUID()).parentUniqueId(parentElement.getTag().getUniqueId()).level(level)
					.build();

			subordinates.put(fieldElement.getTag(), fieldElement);

			if (Objects.nonNull(valueObj)) {
				long objId = valueObj.uniqueID();
//				if (visitedElements.containsKey(objId)) {
//					return;
//				}
				// visitedElements.put(objId, parentElement.getTag());
				for (Field innerField : valueObj.referenceType().allFields()) {
					if (shouldExpand(valueObj, shouldExpandCollections)) {
						addField(fieldElement, innerField, subordinates, breakpointEvent, (level + 1), shouldExpandCollections);
					}
				}
			}
		}

	}

//	private boolean shouldSkipMethod(Method method) {
//		String name = method.name();
//		// конструкторы и статика
//		if ("<init>".equals(name) || "<clinit>".equals(name)) {
//			return true;
//		}
//		// lambda методы
//		if (name.startsWith("lambda$")) {
//			return true;
//		}
//		// synthetic / bridge
//		if (method.isSynthetic() || method.isBridge()) {
//			return true;
//		}
//		if (method.isSynthetic() || method.name().equals("<init>") || method.name().equals("<clinit>"))
//			return true;
//		if (method.declaringType().name().equals("java.lang.Object"))
//			return true;
//		if (method.declaringType().name().startsWith("java."))
//			return true;
//
//		return false;
//	}

	private boolean shouldExpand(ObjectReference objRef, boolean shoulExpandCollections) {
	    if (objRef == null)
	        return false;

	    String typeName = objRef.referenceType().name();

	    // ❌ Не лезем в String
	    if ("java.lang.String".equals(typeName))
	        return false;

	    // ❌ Не лезем в примитивные обёртки
	    if (typeName.startsWith("java.lang.")) {
	        switch (typeName) {
	            case "java.lang.Integer":
	            case "java.lang.Long":
	            case "java.lang.Boolean":
	            case "java.lang.Byte":
	            case "java.lang.Short":
	            case "java.lang.Character":
	            case "java.lang.Double":
	            case "java.lang.Float":
	                return false;
	        }
	    }

	    // ✅ Расширяем коллекции, если включено
	    if (shoulExpandCollections) {
	        try {
	            if (DebugUtils.isInstanceOf(objRef, "java.lang.Iterable") ||
	                DebugUtils.isInstanceOf(objRef, "java.util.Map")) {
	                return true;
	            }
	        } catch (Exception e) {
	            // на всякий случай, не ломаем процесс
	        }
	    }

	    // ❌ Не лезем в стандартные классы JDK
	    if (typeName.startsWith("java.") || typeName.startsWith("javax.") || typeName.startsWith("jdk.")
	            || typeName.startsWith("sun.")) {
	        return false;
	    }

	    // Всё остальное — пользовательские классы, можно раскрывать
	    return true;
	}

}
