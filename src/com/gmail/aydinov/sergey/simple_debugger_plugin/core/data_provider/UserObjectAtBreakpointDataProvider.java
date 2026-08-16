package com.gmail.aydinov.sergey.simple_debugger_plugin.core.data_provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.HandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.InspectionHandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.provider.PageableDataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.sun.jdi.ObjectReference;

public class UserObjectAtBreakpointDataProvider implements PageableDataProvider {

    private final InnerElementRepresentationDTO anchor;
    private final InspectionHandlerContext inspectionHandlerContext;

    private final DebugEventCollector debugEventCollector =
            SimpleDebuggerEventCollector.instance();

    private UserObjectPageDTO cachedPage;
    private long dataProviderHolderId;

    public UserObjectAtBreakpointDataProvider(
            InnerElementRepresentationDTO anchor,
            HandlerContext handlerContext) {

        this.anchor = Objects.requireNonNull(anchor, "anchor");
        this.inspectionHandlerContext =
                (InspectionHandlerContext) Objects.requireNonNull(handlerContext, "handlerContext");
    }

    @Override
    public void handlePageRequest(Integer pageNumber) {

        System.out.println("=== OBJECT PAGE REQUEST ===");
        System.out.println("anchor name     = " + anchor.getElementName());
        System.out.println("anchor value    = " + anchor.getValue());
        System.out.println("anchor objectId = " + anchor.getObjectId());
        System.out.println("anchor tag      = " + anchor.getTag());

        if (cachedPage == null) {
            cachedPage = buildPage();
        }

        if (cachedPage == null) {
            System.out.println("Unable to build object page");
            return;
        }

        cachedPage.setBreadcrumbs(
                inspectionHandlerContext
                        .getInspectionSeanceCache()
                        .groupBreadCrumbsintoPairs()
        );

        debugEventCollector.collectDebugEvent(
                new DebugEvent<>(
                        SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_USER_OBJECT,
                        cachedPage
                )
        );
    }

    /**
     * Строит страницу объекта, на который указывает anchor.
     *
     * Основной идентификатор объекта:
     *
     *     anchor.objectId
     *
     * Tag используется только как fallback.
     */
    private UserObjectPageDTO buildPage() {

        UniversalElementRepresentation objectRepresentation =
                findObjectRepresentation(anchor);

        if (objectRepresentation == null) {
            System.out.println("OBJECT REPRESENTATION NOT FOUND");
            return null;
        }

        System.out.println("=== OBJECT REPRESENTATION FOUND ===");
        System.out.println("object representation = " + objectRepresentation);
        System.out.println("object id              = "
                + getObjectId(objectRepresentation));
        System.out.println("object tag             = "
                + objectRepresentation.getTag());

        List<InnerElementRepresentationDTO> fields =
                findObjectFields(objectRepresentation);

        List<InnerElementRepresentationDTO> methods =
                findClassMethods(objectRepresentation);

        List<InnerElementRepresentationDTO> entries =
                mergeEntries(fields, methods);

        return UserObjectPageDTO.builder()
                .elementName(objectRepresentation.getElementName())
                .elementType(objectRepresentation.getTypeOrReturnType())
                .classType(objectRepresentation.getTypeOrReturnType())
                .objectId(getObjectId(objectRepresentation))
                .entries(entries)
                .anchorTag(objectRepresentation.getTag())
                .build();
    }

    /**
     * Ищет representation именно того JDI-объекта,
     * на который указывает anchor.
     */
    private UniversalElementRepresentation findObjectRepresentation(
            InnerElementRepresentationDTO anchor) {

        List<UniversalElementRepresentation> elements =
                getUniversalElements();

        Long objectId = anchor.getObjectId();

        /*
         * 1. Основной вариант.
         *
         * Ищем по JDI ObjectReference.uniqueID().
         */
        if (objectId != null) {

            for (UniversalElementRepresentation element : elements) {

                ObjectReference reference =
                        element.getObjectReference();

                if (reference == null) {
                    continue;
                }

                if (Objects.equals(reference.uniqueID(), objectId)) {

                    System.out.println(
                            "FOUND OBJECT BY OBJECT ID: " + element
                    );

                    return element;
                }
            }
        }

        /*
         * 2. Fallback.
         *
         * Если объект уже отсутствует в текущем наборе
         * representations, пытаемся найти его по Tag.
         */
        Tag anchorTag = anchor.getTag();

        if (anchorTag != null) {

            for (UniversalElementRepresentation element : elements) {

                ObjectReference reference =
                        element.getObjectReference();

                if (reference == null) {
                    continue;
                }

                if (Objects.equals(element.getTag(), anchorTag)) {

                    System.out.println(
                            "FOUND OBJECT BY TAG: " + element
                    );

                    return element;
                }
            }
        }

        return null;
    }

    /**
     * Возвращает поля конкретного объекта.
     *
     * Связь:
     *
     *     field.tag.parentId
     *             ==
     *     object.tag.uniqueId
     */
    private List<InnerElementRepresentationDTO> findObjectFields(
            UniversalElementRepresentation object) {

        Tag objectTag = object.getTag();

        if (objectTag == null) {
            return List.of();
        }

        return getUniversalElements()
                .stream()
                .filter(this::isField)
                .filter(element ->
                        element.getTag() != null)
                .filter(element ->
                        Objects.equals(
                                element.getTag().getParentId(),
                                objectTag.getUniqueId()
                        ))
                .map(InnerElementRepresentationDTO
                        .InnerElementRepresentationDTOFactory::fromElement)
                .toList();
    }

    /**
     * Ищет методы класса.
     *
     * В отличие от полей, методы принадлежат не экземпляру,
     * а class representation.
     */
    private List<InnerElementRepresentationDTO> findClassMethods(
            UniversalElementRepresentation object) {

        String classAdditionalInfo =
                object.getAdditionalInfo();

        if (classAdditionalInfo == null) {
            return List.of();
        }

        /*
         * Сначала находим representation класса.
         */
        List<UniversalElementRepresentation> classRepresentations =
                getUniversalElements()
                        .stream()
                        .filter(this::isRootClassElement)
                        .filter(element ->
                                Objects.equals(
                                        element.getAdditionalInfo(),
                                        classAdditionalInfo
                                ))
                        .toList();

        if (classRepresentations.isEmpty()) {
            return List.of();
        }

        /*
         * Затем собираем методы каждого найденного класса.
         */
        List<InnerElementRepresentationDTO> result =
                new ArrayList<>();

        for (UniversalElementRepresentation classRepresentation
                : classRepresentations) {

            Tag classTag = classRepresentation.getTag();

            if (classTag == null) {
                continue;
            }

            getUniversalElements()
                    .stream()
                    .filter(this::isMethod)
                    .filter(element ->
                            element.getTag() != null)
                    .filter(element ->
                            Objects.equals(
                                    element.getTag().getParentId(),
                                    classTag.getUniqueId()
                            ))
                    .map(InnerElementRepresentationDTO
                            .InnerElementRepresentationDTOFactory
                            ::fromElement)
                    .forEach(result::add);
        }

        return result.stream()
                .distinct()
                .toList();
    }

    /**
     * Объединяет поля и методы.
     */
    private List<InnerElementRepresentationDTO> mergeEntries(
            List<InnerElementRepresentationDTO> fields,
            List<InnerElementRepresentationDTO> methods) {

        List<InnerElementRepresentationDTO> result =
                new ArrayList<>(fields.size() + methods.size());

        result.addAll(fields);
        result.addAll(methods);

        return result.stream()
                .distinct()
                .toList();
    }

    private List<UniversalElementRepresentation> getUniversalElements() {

        return TargetApplicationRepresentation
                .getInstance()
                .getAllElements()
                .stream()
                .filter(UniversalElementRepresentation.class::isInstance)
                .map(UniversalElementRepresentation.class::cast)
                .toList();
    }

    private boolean isField(
            UniversalElementRepresentation element) {

        return element.getElementType()
                == UniversalElementType.FIELD;
    }

    private boolean isMethod(
            UniversalElementRepresentation element) {

        return element.getElementType()
                == UniversalElementType.METHOD;
    }

    /**
     * Root class representation.
     */
    private boolean isRootClassElement(
            UniversalElementRepresentation element) {

        return element.getTag() != null
                && element.getTag().getParentId() == null;
    }

    private Long getObjectId(
            UniversalElementRepresentation element) {

        ObjectReference reference =
                element.getObjectReference();

        return reference != null
                ? reference.uniqueID()
                : null;
    }

    @Override
    public long getDataProviderHolderId() {
        return dataProviderHolderId;
    }
}