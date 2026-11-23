
package com.gmail.aydinov.sergey.simple_debugger_plugin.core.data_provider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.InspectionHandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.provider.SimpleDataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.UserObjectPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;

import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public class UserObjectInspectionDataProvider implements SimpleDataProvider {

    private final InnerElementRepresentationDTO innerElementRepresentationDTO;
    private final DebugEventCollector debugEventCollector =
            SimpleDebuggerEventCollector.instance();
    private final InspectionHandlerContext inspectionHandlerContext;

    private UserObjectPageDTO cachedUserObjectPageDTO;
    private long dataProviderHolderId;

    public UserObjectInspectionDataProvider(
            InnerElementRepresentationDTO innerElementRepresentationDTO,
            InspectionHandlerContext inspectionHandlerContext) {

        this.innerElementRepresentationDTO = innerElementRepresentationDTO;
        this.inspectionHandlerContext = inspectionHandlerContext;
    }

    @Override
    public void handleElementRequest(
            InnerElementRepresentationDTO innerElementRepresentationDTO) {

        System.out.println("OBJECT REQUEST");

        cachedUserObjectPageDTO = cachePage();

        if (cachedUserObjectPageDTO == null) {
            System.err.println("Unable to build user object page.");
            return;
        }

        cachedUserObjectPageDTO.setBreadcrumbs(
                inspectionHandlerContext.getInspectionSeanceCache()
                        .groupBreadCrumbsintoPairs());

        System.out.println(cachedUserObjectPageDTO);

        debugEventCollector.collectDebugEvent(
                new DebugEvent<>(
                        SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_USER_OBJECT,
                        cachedUserObjectPageDTO));
    }


private UserObjectPageDTO cachePage() {

    long requestedObjectId = innerElementRepresentationDTO.getObjectId();

    System.out.println("================================");
    System.out.println("TYPE: "
            + innerElementRepresentationDTO.getTypeOrReturnType());
    System.out.println("VALUE: "
            + innerElementRepresentationDTO.getValue());
    System.out.println("OBJECT ID: " + requestedObjectId);
    System.out.println("================================");

    // First, try to retrieve the object from the inspection cache.
    ObjectReference objectReference = inspectionHandlerContext
            .getInspectionSeanceCache()
            .getLoadedPieces()
            .get(requestedObjectId);

    // If the object is not cached, search for it in the target JVM.
    if (objectReference == null) {

        VirtualMachine vm =
                TargetVirtualMachineRepresentation.getInstance()
                        .getVirtualMachine();

        if (vm == null) {
            System.err.println("Target Virtual Machine is unavailable.");
            return null;
        }

        long searchStart = System.currentTimeMillis();

        search:
        for (ReferenceType type : vm.allClasses()) {

            if (!(type instanceof ClassType classType)) {
                continue;
            }

            for (ObjectReference object : classType.instances(0)) {

                if (object.uniqueID() == requestedObjectId) {
                    objectReference = object;
                    break search;
                }
            }
        }

        System.out.println("JDI OBJECT SEARCH TIME: "
                + (System.currentTimeMillis() - searchStart) + " ms");
    }

    if (objectReference == null) {
        System.err.println(
                "Object not found in target JVM or inspection cache. ID: "
                        + requestedObjectId);
        return null;
    }

    // Register the inspected object in the inspection cache.
    registerObjectReference(objectReference);

    return build(objectReference);
}


    private UserObjectPageDTO build(ObjectReference obj) {
    	
    	long start = System.currentTimeMillis();

        ReferenceType type = obj.referenceType();

        // ===== ROOT OBJECT =====

        UUID rootId = UUID.randomUUID();

        List<UniversalElementRepresentation> entries =
                new ArrayList<>();

        // =========================================================
        // FIELDS (instance data)
        // =========================================================

        for (Field field : type.allFields()) {

            try {

                Value value = field.isStatic()
                        ? null
                        : obj.getValue(field);

                ObjectReference fieldObjectReference =
                        value instanceof ObjectReference reference
                                ? reference
                                : null;

                // Register every referenced object in the inspection cache.
                if (fieldObjectReference != null) {
                    registerObjectReference(fieldObjectReference);
                }

                UniversalElementRepresentation fieldElement =
                        UniversalElementRepresentation.builder()
                                .objectReference(fieldObjectReference)
                                .elementName(field.name())
                                .additionalInfo(field.typeName())
                                .elementType(UniversalElementType.FIELD)
                                .value(String.valueOf(value))
                                .isStatic(field.isStatic())
                                .valueCategory(
                                        DebugUtils.determineValueCategory(value))
                                .typeOrReturnType(field.typeName())
                                .parentUniqueId(rootId)
                                .level(1)
                                .build();

                entries.add(fieldElement);

            } catch (Exception exception) {
                System.err.println(
                        "Unable to inspect field "
                                + field.name() + ": " + exception);
            }
        }
        
        System.out.println(
                "FIELDS TIME: "
                        + (System.currentTimeMillis() - start) + " ms");

        // =========================================================
        // METHODS (class behavior)
        // =========================================================
        
        long methodsStart = System.currentTimeMillis();

        for (Method method : type.methods()) {

            if (DebugUtils.shouldSkipMethod(method)) {
                continue;
            }

            try {

                String returnValue = method.returnTypeName();

                String params = String.join(
                        ", ", method.argumentTypeNames());

                UniversalElementRepresentation methodElement =
                        UniversalElementRepresentation.builder()
                                .objectReference(obj)
                                .elementName(method.name() + "()")
                                .elementType(UniversalElementType.METHOD)
                                .value(
                                        returnValue + " "
                                                + method.name()
                                                + "(" + params + ")")
                                .typeOrReturnType(method.returnTypeName())
                                .isStatic(method.isStatic())
                                .parentUniqueId(rootId)
                                .level(1)
                                .build();

                entries.add(methodElement);

            } catch (Exception exception) {
                System.err.println(
                        "Unable to inspect method "
                                + method.name() + ": " + exception);
            }
        }
        
        System.out.println(
                "TOTAL BUILD TIME: "
                        + (System.currentTimeMillis() - start) + " ms");

        List<InnerElementRepresentationDTO> dtoEntries =
                entries.stream()
                        .map(InnerElementRepresentationDTO
                                .InnerElementRepresentationDTOFactory
                                ::fromElement)
                        .toList();

        return UserObjectPageDTO.builder()
                .elementName(obj.type().name())
                .elementType(obj.type().name())
                .classType(type.name())
                .entries(dtoEntries)
                .anchorTag(new Tag(rootId, null))
                .objectId(obj.uniqueID())
                .build();
    }

    /**
     * Registers an object reference in the current inspection session.
     * The key is the JDI unique object ID.
     */
    private void registerObjectReference(ObjectReference objectReference) {
    	System.out.println("REGISTER OBJECT: " + objectReference.uniqueID());
        if (objectReference == null) {
            return;
        }

        inspectionHandlerContext.getInspectionSeanceCache()
                .getLoadedPieces()
                .put(objectReference.uniqueID(), objectReference);
    }

    @Override
    public long getDataProviderHolderId() {
        return dataProviderHolderId;
    }
}
