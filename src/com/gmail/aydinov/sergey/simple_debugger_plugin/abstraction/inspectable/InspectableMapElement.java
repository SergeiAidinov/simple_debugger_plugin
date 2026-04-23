package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.inspectable;

import java.util.*;
import java.util.Map.Entry;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.CurrentRole;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionCollectionPage;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.MapPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;

public class InspectableMapElement extends AbstractInspectableElement
		implements PageableInspectable<AbstractInspectionCollectionPage<?>> {

	private final BreakpointEvent breakpointEvent;
	private final InnerElementRepresentationDTO anchorElement;

	private int currentPage = 0;
	private String mapType;

	// 🔥 ключ → значение
	private final List<PairDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>> entries = new ArrayList<>();

	InspectableMapElement(InnerElementRepresentationDTO anchorElement, BreakpointEvent breakpointEvent, int offset,
			int limit) {
		super(anchorElement.getTag(), anchorElement.getElementName(), UniversalElementType.MAP_ELEMENT,
				anchorElement.getValueCategory(), true);

		this.breakpointEvent = breakpointEvent;
		this.anchorElement = anchorElement;

		compileEntries(offset, limit);
	}

	// ================= GETTERS =================

	public int getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
	}

	public String getMapType() {
		return mapType;
	}

	public List<PairDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>> getEntries() {
		return entries;
	}

	public InnerElementRepresentationDTO getAnchorElement() {
		return anchorElement;
	}

	// ================= CORE =================

	private void compileEntries(int offset, int limit) {
		Optional<UniversalElementRepresentation> mapOpt = TargetApplicationRepresentation.getInstance().getAllElements()
				.stream().filter(e -> e instanceof UniversalElementRepresentation)
				.map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.equals(e.getTag(), anchorElement.getTag())).findAny();

		if (mapOpt.isEmpty())
			return;

		UniversalElementRepresentation mapRef = mapOpt.get();

		mapType = mapRef.getReferenceType() != null ? mapRef.getReferenceType().name() : "Unknown";

		List<Map.Entry<Value, Value>> rawEntries = DebugUtils.iterateThroughMap(mapRef.getObjectReference(),
				breakpointEvent, offset, limit);

		for (int i = 0; i < rawEntries.size(); i++) {
			Map.Entry<Value, Value> entry = rawEntries.get(i);
			if ((entry.getKey() instanceof ObjectReference keyRef)
					&& (entry.getValue() instanceof ObjectReference valueRef)) {
				String keyType = keyRef.referenceType().name();
				String keyText = keyType.startsWith("java.lang.") ? keyRef.toString() : keyType;

				UniversalElementRepresentation keyRepresentation = UniversalElementRepresentation.builder()
						.referenceType(keyRef.referenceType()).objectReference(keyRef).elementName(keyText)
						.elementType(UniversalElementType.MAP_ELEMENT).currentRole(CurrentRole.INNER)
						.value(DebugUtils.getObjectReferenceValueAsString(keyRef))
						.valueCategory(DebugUtils.determineValueCategory(entry.getKey())).uniqueId(UUID.randomUUID())
						.parentUniqueId(anchorElement.getTag().getUniqueId()).level(anchorElement.getLevel() + 1)
						.build();

				InnerElementRepresentationDTO keyDto = DebugUtils.createInnerElementDTO(entry.getKey(),
						keyRepresentation, i);

				String valueType = valueRef.referenceType().name();
				String valueText = valueType.startsWith("java.lang.") ? keyRef.toString() : valueType;

				UniversalElementRepresentation valueRepresentation = UniversalElementRepresentation.builder()
						.referenceType(valueRef.referenceType()).objectReference(valueRef).elementName(valueText)
						.elementType(UniversalElementType.MAP_ELEMENT)
						.additionalInfo(String.valueOf(valueRef.uniqueID())).currentRole(CurrentRole.INNER)
						.value(DebugUtils.getObjectReferenceValueAsString(valueRef))
						.valueCategory(DebugUtils.determineValueCategory(entry.getValue())).uniqueId(UUID.randomUUID())
						.parentUniqueId(anchorElement.getTag().getUniqueId()).level(anchorElement.getLevel() + 1)
						.build();

				InnerElementRepresentationDTO valueDto = InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
						.fromElement(valueRepresentation);

				entries.add(PairDTO.of(keyDto, valueDto));
			}
		}

		System.out.println(entries.size());
	}

	private List<PairDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>> getPage(int pageNumber) {
		int from = pageNumber * DebugUtils.PAGE_SIZE;
		int to = Math.min(from + DebugUtils.PAGE_SIZE, entries.size());

		if (from >= entries.size())
			return List.of();

		return entries.subList(from, to);
	}

	// ================= PAGINATION =================

	@Override
	public AbstractInspectionCollectionPage<?> inspectPage(AbstractInspectableElement inspectableElement,
			int pageNumber) {
		if (!(inspectableElement instanceof InspectableMapElement map))
			throw new IllegalArgumentException("Expected InspectableMapElement");
		Optional<UniversalElementRepresentation> mapRepresentation = TargetApplicationRepresentation.getInstance().getAllElements()
				.stream().filter(e -> Objects.equals(e.getTag(), anchorElement.getTag()))
				.filter(e -> e instanceof UniversalElementRepresentation).map(e -> (UniversalElementRepresentation) e)
				.findAny();
		int mapSize = 0;
		if (mapRepresentation.isPresent()) {
			String mapSizeString = mapRepresentation.get().getValue().substring(mapRepresentation.get().getValue().indexOf(':') + 1,
					mapRepresentation.get().getValue().indexOf(','));
			System.out.println(mapSizeString);
			try {
				mapSize = Integer.valueOf(mapSizeString);
			} catch (NumberFormatException e) {
				// TODO: handle exception
			}
		}
		int totalEntries = mapSize;
		int totalPages = (totalEntries + DebugUtils.PAGE_SIZE - 1) / DebugUtils.PAGE_SIZE;

		int fromIndex = pageNumber * DebugUtils.PAGE_SIZE;
		int toIndex = Math.min(fromIndex + DebugUtils.PAGE_SIZE - 1, totalEntries - 1);

		List<PairDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>> pageEntries = map
				.getPage(pageNumber);

		return MapPageDTO.<InnerElementRepresentationDTO, InnerElementRepresentationDTO>builder()
				.elementName(map.getAnchorElement().getElementName()).elementType(map.getMapType())
				.totalEntries(totalEntries).currentPage(pageNumber).totalPages(totalPages).fromIndex(fromIndex)
				.toIndex(toIndex).entries(pageEntries).anchorTag(map.getAnchorElement().getTag()).build();
	}
}