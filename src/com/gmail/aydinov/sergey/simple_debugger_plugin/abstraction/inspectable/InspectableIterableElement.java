package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.inspectable;

import java.util.ArrayList;
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
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TripletDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.ArrayPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;


public class InspectableIterableElement extends AbstractInspectableElement {

	private final BreakpointEvent breakpointEvent;
	private final InnerElementRepresentationDTO anchorElement;
	private int currentPage = 0;
	private String collectionType;
	private String elementType;
	private final TreeMap<Integer, InnerElementRepresentationDTO> collectionElements = new TreeMap<Integer, InnerElementRepresentationDTO>();
//	private final UIEventHandler handler = new IterableInspectionSeanceHandler();

	InspectableIterableElement(InnerElementRepresentationDTO anchorElement,
			StackFrame currentFrame, BreakpointEvent breakpointEvent) {
		super(anchorElement.getTag());
		this.breakpointEvent = breakpointEvent;
		this.anchorElement = anchorElement;
		this.currentPage = currentPage;
		this.collectionType = collectionType;
		this.elementType = elementType;
		compileCollectionElements(anchorElement);
	}

	public int getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
	}

	public String getCollectionType() {
		return collectionType;
	}

	public String getElementType() {
		return elementType;
	}

	public TreeMap<Integer, InnerElementRepresentationDTO> getCollectionElements() {
		return collectionElements;
	}
	
	public BreakpointEvent getBreakpointEvent() {
		return breakpointEvent;
	}

	public InnerElementRepresentationDTO getAnchorElement() {
		return anchorElement;
	}

//	public UIEventHandler getHandler() {
//		return handler;
//	}

	public void setCollectionType(String collectionType) {
		this.collectionType = collectionType;
	}

	public void setElementType(String elementType) {
		this.elementType = elementType;
	}

	public ArrayPageDTO createPage(int pageNumber, InspectableIterableElement inspectableCollection) {
		return ArrayPageDTO.builder().collectionName(inspectableCollection.getAnchorElement().getElementName())
				.collectionType(inspectableCollection.getCollectionType()).elementType(inspectableCollection.getElementType()).totalElements(inspectableCollection.getCollectionElements().size())
				.currentPage(pageNumber).totalPages((inspectableCollection.getCollectionElements().size() / DebugUtils.PAGE_SIZE) + 1)
				.fromIndex(pageNumber * DebugUtils.PAGE_SIZE)
				.toIndex(pageNumber * DebugUtils.PAGE_SIZE + DebugUtils.PAGE_SIZE - 1).entries(getPage(pageNumber, inspectableCollection))
				.anchorTag(anchorElement.getTag()).build();
	}

	private void compileCollectionElements(InnerElementRepresentationDTO anchorElement) {
		// Получаем объект коллекции по тегу
		Optional<UniversalElementRepresentation> collectionOpt = TargetApplicationRepresentation.getInstance()
				.getAllElements() // предполагаем метод, который объединяет first и
				.stream().filter(e -> e instanceof UniversalElementRepresentation)
				.map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.equals(e.getTag(), anchorElement.getTag())).findAny();

		if (collectionOpt.isEmpty())
			return;

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
	}

	private List<PairDTO<Integer, InnerElementRepresentationDTO>> getPage(int pageNumber, InspectableIterableElement inspectableCollection) {
		List<PairDTO<Integer, InnerElementRepresentationDTO>> result = new ArrayList<PairDTO<Integer, InnerElementRepresentationDTO>>();
		List<InnerElementRepresentationDTO> entries = List.copyOf(inspectableCollection.getCollectionElements()
				.subMap((DebugUtils.PAGE_SIZE * pageNumber), true,
						(DebugUtils.PAGE_SIZE * pageNumber + DebugUtils.PAGE_SIZE), false)
				.values().stream().toList());
		for (int i = 0; i < entries.size(); i++) {
			result.add(PairDTO.of((i + DebugUtils.PAGE_SIZE * pageNumber), entries.get(i)));
		}
		return result;
	}
	
}
