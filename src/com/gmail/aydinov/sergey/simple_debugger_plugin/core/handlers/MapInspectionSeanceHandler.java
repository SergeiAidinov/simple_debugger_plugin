package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.*;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.MapPageDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;

public class MapInspectionSeanceHandler implements UIEventHandler {

    private final SimpleDebuggerEventCollector eventCollector =
            SimpleDebuggerEventCollector.instance();

    @SuppressWarnings("unchecked")
    @Override
    public boolean handle(AbstractUIEvent abstractUIEvent,
                          StackFrame currentFrame,
                          BreakpointEvent breakpointEvent) {

        DebuggerContext.context().setStatus(
                SimpleDebuggerStatus.COLLECTION_INSPECTION_SEANCE_RUNNING
        );

        UIEvent<InnerElementRepresentationDTO> uiEvent;

        try {
            uiEvent = (UIEvent<InnerElementRepresentationDTO>) abstractUIEvent;
        } catch (ClassCastException e) {
            return false;
        }

        Thread worker = new Thread(
                new MapInspectionSeance(uiEvent.getPayload(), breakpointEvent)
        );
        worker.setDaemon(true);
        worker.start();

        try {
            worker.join();
        } catch (InterruptedException ignored) {
            return false;
        } finally {
            DebuggerContext.context().setStatus(
                    SimpleDebuggerStatus.DEBUG_SESSION_RUNNING
            );
        }

        return true;
    }

    // =========================================================
    // Worker
    // =========================================================

    private class MapInspectionSeance implements Runnable {

        private final InnerElementRepresentationDTO anchorElement;
        private final BreakpointEvent breakpointEvent;

        MapInspectionSeance(InnerElementRepresentationDTO anchorElement,
                            BreakpointEvent breakpointEvent) {
            this.anchorElement = anchorElement;
            this.breakpointEvent = breakpointEvent;
        }

        @Override
        public void run() {
            Optional<UniversalElementRepresentation> mapOpt = findMap(anchorElement);

            if (mapOpt.isEmpty()) {
                return;
            }

            UniversalElementRepresentation mapRef = mapOpt.get();

            List<Map.Entry<Value, Value>> rawEntries =
                    DebugUtils.iterateThroughMap(mapRef.getObjectReference(), breakpointEvent);

            List<PairDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>> entries =
                    buildEntries(rawEntries, mapRef);

            MapPageDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO> page =
                    buildPage(entries, mapRef);

            sendToUI(page);
        }

        // =========================================================
        // Steps
        // =========================================================

        private Optional<UniversalElementRepresentation> findMap(InnerElementRepresentationDTO anchor) {
            return TargetApplicationRepresentation.getInstance()
                    .getAllElements()
                    .stream()
                    .filter(e -> e instanceof UniversalElementRepresentation)
                    .map(e -> (UniversalElementRepresentation) e)
                    .filter(e -> Objects.equals(e.getTag(), anchor.getTag()))
                    .findFirst();
        }

        private List<PairDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>> buildEntries(
                List<Map.Entry<Value, Value>> rawEntries,
                UniversalElementRepresentation mapRef
        ) {
            List<PairDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>> result =
                    new ArrayList<>(rawEntries.size());

            for (int i = 0; i < rawEntries.size(); i++) {
                Map.Entry<Value, Value> entry = rawEntries.get(i);

                InnerElementRepresentationDTO keyDto =
                        DebugUtils.createInnerElementDTO(entry.getKey(), mapRef, i, "key");

                InnerElementRepresentationDTO valueDto =
                        DebugUtils.createInnerElementDTO(entry.getValue(), mapRef, i, "value");

                result.add(PairDTO.of(keyDto, valueDto));
            }

            return result;
        }

        private MapPageDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO> buildPage(
                List<PairDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>> entries,
                UniversalElementRepresentation mapRef
        ) {
            int totalEntries = entries.size();
            int pageSize = DebugUtils.PAGE_SIZE;

            int currentPage = 0;
            int totalPages = (int) Math.ceil((double) totalEntries / pageSize);

            int fromIndex = currentPage * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, totalEntries);

            List<PairDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO>> pageEntries =
                    entries.subList(fromIndex, toIndex);

            return MapPageDTO.<InnerElementRepresentationDTO, InnerElementRepresentationDTO>builder()
                    .mapName(anchorElement.getElementName())
                    .mapType(mapRef.getElementType().name()) // при желании заменить на concrete
                    .totalEntries(totalEntries)
                    .currentPage(currentPage)
                    .totalPages(totalPages)
                    .fromIndex(fromIndex)
                    .toIndex(toIndex)
                    .entries(pageEntries)
                    .anchorTag(anchorElement.getTag())
                    .build();
        }

        private void sendToUI(
                MapPageDTO<InnerElementRepresentationDTO, InnerElementRepresentationDTO> page
        ) {
            eventCollector.collectDebugEvent(
                    new DebugEvent<>(
                            SimpleDebuggerEventType.DISPLAY_PAGE_OF_INSPECTABLE_MAP,
                            page
                    )
            );
        }
    }
}