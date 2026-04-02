package com.gmail.aydinov.sergey.simple_debugger_plugin.dto.inspectable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.CurrentRole;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TripletDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;

public class AbstractInspectableElement {
	
	public static Factory factory() {
		return new Factory();
	}

	public static class Factory {
		
		private Factory() {
		}

		public AbstractInspectableElement createInspectableElement(
				InnerElementRepresentationDTO innerElementRepresentationDTO, BreakpointEvent breakpointEvent) {
			return switch (innerElementRepresentationDTO.getValueCategory()) {
			case ValueCategory.COLLECTION -> compileCollectionElements(innerElementRepresentationDTO, breakpointEvent);
			default -> null;
			};
		}

		private InspectableCollection compileCollectionElements(InnerElementRepresentationDTO anchorElement,
				BreakpointEvent breakpointEvent) {
			TreeMap<Integer, InnerElementRepresentationDTO> collectionElements = new TreeMap<>();
			String collectionType = "";
			String elementType = "";
			// Получаем объект коллекции по тегу
			Optional<UniversalElementRepresentation> collectionOpt = TargetApplicationRepresentation.getInstance()
					.getAllElements() // предполагаем метод, который объединяет first и
					.stream().filter(e -> e instanceof UniversalElementRepresentation)
					.map(e -> (UniversalElementRepresentation) e)
					.filter(e -> Objects.equals(e.getTag(), anchorElement.getTag())).findAny();

			if (collectionOpt.isEmpty())
				return null;

			UniversalElementRepresentation collection = collectionOpt.get();
			TripletDTO<String, String, String> ww = DebugUtils.determinCollectionType(collection.getObjectReference(),
					breakpointEvent);
			collectionType = ww.getFirst();
			elementType = ww.getSecond();
			// Получаем все значения коллекции
			List<Value> values = DebugUtils.iterateThroughCollection(collection.getObjectReference(), breakpointEvent);

			// Преобразуем в InnerElementRepresentationDTO и кладём в colectionElements
			for (int i = 0; i < values.size(); i++) {
				Value v = values.get(i);
				if (v == null)
					continue;

				UniversalElementRepresentation uer;
				if (v instanceof ObjectReference objRef) {
					String type = objRef.referenceType().name();
					String valueText = type.startsWith("java.lang.") ? objRef.toString() : type;

					uer = UniversalElementRepresentation.builder().referenceType(objRef.referenceType())
							.objectReference(objRef).elementName(valueText)
							.elementType(UniversalElementType.COLLECTION_ELEMENT).currentRole(CurrentRole.INNER)
							.value(DebugUtils.getObjectReferenceValueAsString(objRef))
							.valueCategory(DebugUtils.determineValueCategory(v))
							// .typeOrReturnType(DebugUtils.)
							.uniqueId(UUID.randomUUID()).parentUniqueId(collection.getTag().getUniqueId())
							.level(collection.getLevel() + 1).build();
				} else {
					// Примитив
					uer = UniversalElementRepresentation.builder().elementName("item").value(v.toString())
							.valueCategory(ValueCategory.PRIMITIVE).uniqueId(UUID.randomUUID())
							.parentUniqueId(collection.getTag().getUniqueId()).level(collection.getLevel() + 1).build();
				}

				InnerElementRepresentationDTO dto = InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
						.fromElement(uer);
				collectionElements.put(i, dto);
			}

			return new InspectableCollection(0, collectionElements);
		}

	}

}
