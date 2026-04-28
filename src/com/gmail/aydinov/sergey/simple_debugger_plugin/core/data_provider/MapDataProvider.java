package com.gmail.aydinov.sergey.simple_debugger_plugin.core.data_provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DataProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.AbstractInspectionDTO;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.BreakpointEvent;

public final class MapDataProvider implements DataProvider{

    // ===== CACHE SNAPSHOT =====
    private static final Map<Long, List<Map.Entry<Value, Value>>> SNAPSHOT_CACHE = new HashMap<>();

    // ===== FIELD CACHE (🔥 NEW) =====
    private static final Map<String, Field> FIELD_CACHE = new HashMap<>();

    public static List<Map.Entry<Value, Value>> iterateThroughMap(
            ObjectReference instance,
            BreakpointEvent breakpointEvent,
            int offset,
            int limit) {

        if (instance == null || breakpointEvent == null || limit <= 0)
            return List.of();

        ThreadReference thread = breakpointEvent.thread();

        try {
            long mapId = instance.uniqueID();

            // =========================================================
            // 1. SNAPSHOT CACHE
            // =========================================================
            List<Map.Entry<Value, Value>> cached = SNAPSHOT_CACHE.get(mapId);

            if (cached == null) {
                cached = loadSnapshot(instance, thread);
                SNAPSHOT_CACHE.put(mapId, cached);
            }

            if (cached.isEmpty())
                return List.of();

            // =========================================================
            // 2. PAGING (LOCAL FAST)
            // =========================================================
            int start = Math.min(offset, cached.size());
            int end = Math.min(start + limit, cached.size());

            return cached.subList(start, end);

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    // =============================================================
    // SNAPSHOT LOADING (1–2 JDI CALLS TOTAL)
    // =============================================================
    private static List<Map.Entry<Value, Value>> loadSnapshot(
            ObjectReference instance,
            ThreadReference thread) throws Exception {

        ReferenceType refType = instance.referenceType();
        if (!(refType instanceof ClassType classType))
            return List.of();

        boolean isMap = classType.allInterfaces().stream()
                .anyMatch(i -> "java.util.Map".equals(i.name()));

        if (!isMap)
            return List.of();

        // 1️⃣ entrySet()
        Value entrySetValue = instance.invokeMethod(
                thread,
                classType.concreteMethodByName("entrySet", "()Ljava/util/Set;"),
                Collections.emptyList(),
                ObjectReference.INVOKE_SINGLE_THREADED
        );

        if (!(entrySetValue instanceof ObjectReference entrySetRef))
            return List.of();

        // 2️⃣ toArray()
        ClassType setType = (ClassType) entrySetRef.referenceType();

        ArrayReference array = (ArrayReference) entrySetRef.invokeMethod(
                thread,
                setType.concreteMethodByName("toArray", "()[Ljava/lang/Object;"),
                Collections.emptyList(),
                ObjectReference.INVOKE_SINGLE_THREADED
        );

        List<Value> values = array.getValues();

        if (values == null || values.isEmpty())
            return List.of();

        // =========================================================
        // 3. BUILD LOCAL STRUCTURE (NO invokeMethod HERE)
        // =========================================================
        List<Map.Entry<Value, Value>> result = new ArrayList<>(values.size());

        for (Value v : values) {

            ObjectReference entryRef = (ObjectReference) v;
            ClassType entryType = (ClassType) entryRef.referenceType();

            Value key = getField(entryRef, entryType, "key");
            Value value = getField(entryRef, entryType, "value");

            result.add(Map.entry(key, value));
        }

        return result;
    }

    // =============================================================
    // FIELD ACCESS (FAST PATH)
    // =============================================================
    private static Value getField(
            ObjectReference obj,
            ClassType type,
            String fieldName) {

        try {
            String key = type.name() + ":" + fieldName;

            Field field = FIELD_CACHE.get(key);

            if (field == null) {
                field = type.fieldByName(fieldName);
                FIELD_CACHE.put(key, field);
            }

            if (field == null)
                return null;

            return obj.getValue(field);

        } catch (Exception e) {
            return null;
        }
    }

    // =============================================================
    // CACHE CLEAR
    // =============================================================
    public static void clearCache() {
        SNAPSHOT_CACHE.clear();
        FIELD_CACHE.clear();
    }

	@Override
	public AbstractInspectionDTO getData(Integer pageNumber) {
		// TODO Auto-generated method stub
		return null;
	}
}