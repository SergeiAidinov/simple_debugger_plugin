package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.*;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.CurrentRole;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;

public class MapInspectionSeanceHandler implements UIEventHandler {

    private final SimpleDebuggerEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();

    @SuppressWarnings("unchecked")
    @Override
    public boolean handle(AbstractUIEvent abstractUIEvent, StackFrame currentFrame, BreakpointEvent breakpointEvent) {
        System.out.println("MAP INSPECTION STARTED");
        DebuggerContext.context().setStatus(SimpleDebuggerStatus.COLLECTION_INSPECTION_SEANCE_RUNNING);

        UIEvent<InnerElementRepresentationDTO> uiEvent = null;
        try {
            uiEvent = (UIEvent<InnerElementRepresentationDTO>) abstractUIEvent;
        } catch (ClassCastException ignored) {}

        if (uiEvent != null) {
            Thread mapInspectionThread = new Thread(new MapInspectionSeance(uiEvent.getPayload(), breakpointEvent));
            mapInspectionThread.setDaemon(true);
            mapInspectionThread.start();
            try {
                mapInspectionThread.join();
            } catch (InterruptedException e) {
                return false;
            } finally {
                DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
                SimpleDebugerWindowsManager.instance().getUniversalInspectorWindow().close();
            }
        }

        return true;
    }

    private class MapInspectionSeance implements Runnable {

        private final InnerElementRepresentationDTO anchorElement;
        private final BreakpointEvent breakpointEvent;
        private final TreeMap<Integer, InnerElementRepresentationDTO> mapElements = new TreeMap<>();

        public MapInspectionSeance(InnerElementRepresentationDTO anchorElement, BreakpointEvent breakpointEvent) {
            this.anchorElement = anchorElement;
            this.breakpointEvent = breakpointEvent;
        }

        @Override
        public void run() {
            inspectMap(anchorElement);
        }

        @SuppressWarnings("unchecked")
        private void inspectMap(InnerElementRepresentationDTO anchorElement) {
            // получаем объект Map из TargetApplicationRepresentation
            Optional<UniversalElementRepresentation> mapOpt = TargetApplicationRepresentation.getInstance()
					.getAllElements() // предполагаем метод, который объединяет first и
					.stream().filter(e -> e instanceof UniversalElementRepresentation)
					.map(e -> (UniversalElementRepresentation) e)
					.filter(e -> Objects.equals(e.getTag(), anchorElement.getTag())).findAny();;
            if (mapOpt.isEmpty()) return;

            UniversalElementRepresentation mapRef = mapOpt.get();

            List<Map.Entry<Value, Value>> map = DebugUtils.iterateThroughMap(mapRef.getObjectReference(), breakpointEvent);
        //    List<Map.Entry<Value, Value>> entries = new ArrayList<>(map.entrySet());

            // превращаем в InnerElementRepresentationDTO
            for (int i = 0; i < map.size(); i++) {
                Map.Entry<Value, Value> entry = map.get(i);

                InnerElementRepresentationDTO keyDto = DebugUtils.createInnerElementDTO(entry.getKey(), mapRef, i, "key");
                InnerElementRepresentationDTO valueDto = DebugUtils.createInnerElementDTO(entry.getValue(), mapRef, i, "value");

                // для удобства положим сначала ключ, потом значение
                mapElements.put(i * 2, keyDto);
                mapElements.put(i * 2 + 1, valueDto);
            }

            // создаём страницу (аналог CollectionPageDTO)
            int totalElements = mapElements.size();
            int pageSize = DebugUtils.PAGE_SIZE;
            int totalPages = (totalElements / pageSize) + 1;
            int currentPage = 0;

            List<PairDTO<Integer, InnerElementRepresentationDTO>> pageEntries = mapElements.entrySet().stream()
                    .skip(currentPage * pageSize)
                    .limit(pageSize)
                    .map(e -> PairDTO.of(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());

            // Отправка на UI
            SimpleDebuggerEventCollector.instance().collectDebugEvent(
                    new com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent<>(
                            SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_MAP,
                            pageEntries
                    )
            );
        }
    }
}