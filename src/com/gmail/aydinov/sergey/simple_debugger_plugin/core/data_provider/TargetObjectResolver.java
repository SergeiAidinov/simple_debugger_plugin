
package com.gmail.aydinov.sergey.simple_debugger_plugin.core.data_provider;

import java.util.Optional;

import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;

public final class TargetObjectResolver {
	
    public TargetObjectResolver() {
		super();
	}

	public Optional<ObjectReference> findById(
            long objectId,
            ObjectReference inspectedObject) {

        long startTime = System.nanoTime();

        try {
            if (inspectedObject == null || inspectedObject.isCollected()) {
                return Optional.empty();
            }

            for (Field field : inspectedObject.referenceType().allFields()) {

                // Статические поля не относятся к конкретному экземпляру.
                if (field.isStatic()) {
                    continue;
                }

                Value value = inspectedObject.getValue(field);

                if (value instanceof ObjectReference reference
                        && reference.uniqueID() == objectId) {

                    return Optional.of(reference);
                }
            }

            return Optional.empty();

        } finally {
            long elapsedMillis =
                    (System.nanoTime() - startTime) / 1_000_000;

            System.out.println("Search time: " + elapsedMillis + " ms.");
        }
    }
}


