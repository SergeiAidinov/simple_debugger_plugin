
package com.gmail.aydinov.sergey.simple_debugger_plugin.core.data_provider;

import java.util.Objects;
import java.util.Optional;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.HandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.InspectionHandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.provider.DataProvider;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;

public final class TargetObjectResolver {

    public TargetObjectResolver() {
    }

    public static Optional<ObjectReference> findById(
            long objectId,
            HandlerContext context) {

        Objects.requireNonNull(context, "context");

        DataProvider.jdiAccessLock.lock();

        try {
            // 1. Search in InspectionSeanceCache.
            if (context instanceof InspectionHandlerContext inspectionContext) {
                ObjectReference reference = inspectionContext
                        .getInspectionSeanceCache()
                        .getLoadedPieces()
                        .get(objectId);

                if (isUsable(reference)) {
                    return Optional.of(reference);
                }
            }

            // 2. Search in TargetApplicationRepresentation.
            Optional<ObjectReference> snapshotReference =
                    findInTargetApplicationSnapshot(objectId);

            if (snapshotReference.isPresent()) {
                return snapshotReference;
            }

            // 3. Search in the target JVM.
            VirtualMachine vm = context.getBreakpointEvent().virtualMachine();

            return findInVirtualMachine(objectId, vm);

        } finally {
            DataProvider.jdiAccessLock.unlock();
        }
    }

    private static Optional<ObjectReference> findInTargetApplicationSnapshot(
            long objectId) {

        return TargetApplicationRepresentation.getInstance()
                .getAllElements()
                .stream()
                .filter(UniversalElementRepresentation.class::isInstance)
                .map(UniversalElementRepresentation.class::cast)
                .filter(element -> {
                    ObjectReference reference = element.getObjectReference();
                    return isUsable(reference)
                            && reference.uniqueID() == objectId;
                })
                .map(UniversalElementRepresentation::getObjectReference)
                .findFirst();
    }

    private static Optional<ObjectReference> findInVirtualMachine(
            long objectId,
            VirtualMachine vm) {

        if (vm == null || !vm.canGetInstanceInfo()) {
            return Optional.empty();
        }

        for (ReferenceType referenceType : vm.allClasses()) {
            try {
                for (ObjectReference instance : referenceType.instances(0)) {
                    if (instance.uniqueID() == objectId) {
                        return Optional.of(instance);
                    }
                }
            } catch (UnsupportedOperationException ignored) {
                // Continue with the next type.
            }
        }

        return Optional.empty();
    }

    private static boolean isUsable(ObjectReference reference) {
        if (reference == null) {
            return false;
        }

        try {
            return !reference.isCollected();
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
