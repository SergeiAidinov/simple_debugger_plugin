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
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TripletDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionCollectionPage;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.ArrayPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;

public class InspectableIterableElement extends AbstractInspectableElement implements PageableInspectable<AbstractInspectionCollectionPage<?>> {

    private final BreakpointEvent breakpointEvent;
    private final InnerElementRepresentationDTO anchorElement;
    private int currentPage = 0;
    private String collectionType;
   // private UniversalElementType elementType; // <- теперь enum
    private final TreeMap<Integer, InnerElementRepresentationDTO> collectionElements = new TreeMap<>();

    InspectableIterableElement(InnerElementRepresentationDTO anchorElement,
                               StackFrame currentFrame,
                               BreakpointEvent breakpointEvent) {
        super(anchorElement.getTag(),
              anchorElement.getElementName(),
              anchorElement.getElementType(),
              anchorElement.getValueCategory(),
              true);
        this.breakpointEvent = breakpointEvent;
        this.anchorElement = anchorElement;

        compileCollectionElements(anchorElement);
    }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    public String getCollectionType() { return collectionType; }
    public UniversalElementType getElementType() { return super.getElementType(); }
    public TreeMap<Integer, InnerElementRepresentationDTO> getCollectionElements() { return collectionElements; }
    public BreakpointEvent getBreakpointEvent() { return breakpointEvent; }
    public InnerElementRepresentationDTO getAnchorElement() { return anchorElement; }
    public void setCollectionType(String collectionType) { this.collectionType = collectionType; }
//    public void setElementType(UniversalElementType elementType) { this.elementType = elementType; }

    private void compileCollectionElements(InnerElementRepresentationDTO anchorElement) {
        Optional<UniversalElementRepresentation> collectionOpt = TargetApplicationRepresentation.getInstance()
                .getAllElements()
                .stream()
                .filter(e -> e instanceof UniversalElementRepresentation)
                .map(e -> (UniversalElementRepresentation) e)
                .filter(e -> Objects.equals(e.getTag(), anchorElement.getTag()))
                .findAny();

        if (collectionOpt.isEmpty()) return;

        UniversalElementRepresentation collection = collectionOpt.get();

        TripletDTO<String, String, String> ww = DebugUtils.determinCollectionType(
                collection.getObjectReference(), breakpointEvent
        );
        collectionType = ww.getFirst();

        // Преобразуем строку типа элемента в enum
//        try {
//            elementType = UniversalElementType.valueOf(ww.getSecond());
//        } catch (IllegalArgumentException e) {
//            elementType = UniversalElementType.UNKNOWN;
//        }

        List<Value> values = DebugUtils.iterateThroughCollection(collection.getObjectReference(), breakpointEvent);

        for (int i = 0; i < values.size(); i++) {
            Value v = values.get(i);
            if (v == null) continue;

            UniversalElementRepresentation uer;
            if (v instanceof ObjectReference objRef) {
                String type = objRef.referenceType().name();
                String valueText = type.startsWith("java.lang.") ? objRef.toString() : type;

                uer = UniversalElementRepresentation.builder()
                        .referenceType(objRef.referenceType())
                        .objectReference(objRef)
                        .elementName(valueText)
                        .elementType(UniversalElementType.COLLECTION_ELEMENT)
                        .currentRole(CurrentRole.INNER)
                        .value(DebugUtils.getObjectReferenceValueAsString(objRef))
                        .valueCategory(DebugUtils.determineValueCategory(v))
                        .uniqueId(UUID.randomUUID())
                        .parentUniqueId(collection.getTag().getUniqueId())
                        .level(collection.getLevel() + 1)
                        .build();
            } else {
                uer = UniversalElementRepresentation.builder()
                        .elementName("item")
                        .value(v.toString())
                        .valueCategory(ValueCategory.PRIMITIVE)
                        .uniqueId(UUID.randomUUID())
                        .parentUniqueId(collection.getTag().getUniqueId())
                        .level(collection.getLevel() + 1)
                        .build();
            }

            InnerElementRepresentationDTO dto = InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(uer);
            collectionElements.put(i, dto);
        }
    }

    private List<PairDTO<Integer, InnerElementRepresentationDTO>> getPage(int pageNumber, InspectableIterableElement collection) {
        List<PairDTO<Integer, InnerElementRepresentationDTO>> result = new ArrayList<>();
        int from = pageNumber * DebugUtils.PAGE_SIZE;
        int to = Math.min(from + DebugUtils.PAGE_SIZE, collection.getCollectionElements().size());

        List<InnerElementRepresentationDTO> pageElements = new ArrayList<>(collection.getCollectionElements().subMap(from, to).values());

        for (int i = 0; i < pageElements.size(); i++) {
            result.add(PairDTO.of(from + i, pageElements.get(i)));
        }
        return result;
    }

    @Override
    public AbstractInspectionCollectionPage<?> inspectPage(AbstractInspectableElement inspectableElement, int pageNumber) {
        if (!(inspectableElement instanceof InspectableIterableElement collection))
            throw new IllegalArgumentException("Expected InspectableIterableElement");

        int totalElements = collection.getCollectionElements().size();
        int totalPages = (totalElements + DebugUtils.PAGE_SIZE - 1) / DebugUtils.PAGE_SIZE;
        int fromIndex = pageNumber * DebugUtils.PAGE_SIZE;
        int toIndex = Math.min(fromIndex + DebugUtils.PAGE_SIZE - 1, totalElements - 1);
        Optional<UniversalElementRepresentation> c = TargetApplicationRepresentation.getInstance().getAllElements().stream()
        .filter(e -> e instanceof UniversalElementRepresentation)
        .map(e -> (UniversalElementRepresentation) e)
       // .filter(e -> Objects.nonNull(e.getObjectReference()))
        .filter(e -> Objects.equals(e.getTag(), inspectableElement.getTag())).findAny();
        if (c.isEmpty()) return null;
        
        List<Value> ww = DebugUtils.iterateThroughCollection(c.get().getObjectReference(), breakpointEvent);
        List<Long> collectionsElementsIds = new ArrayList<Long>();
        for (Value value : ww) {
        	if (value instanceof ObjectReference) {
        	    ObjectReference objectReference = (ObjectReference) value;
        	    collectionsElementsIds.add(objectReference.uniqueID());
        	}
        }

        List<PairDTO<Integer, InnerElementRepresentationDTO>> pageEntries = getPage(pageNumber, collection);

        return ArrayPageDTO.builder()
                .elementName(collection.getAnchorElement().getElementName())
                .elementType(collection.getElementType().name()) // преобразуем enum в строку для DTO
                .totalElements(totalElements)
                .currentPage(pageNumber)
                .totalPages(totalPages)
                .fromIndex(fromIndex)
                .toIndex(toIndex)
                .entries(pageEntries)
                .anchorTag(collection.getAnchorElement().getTag())
                .build();
    }
}