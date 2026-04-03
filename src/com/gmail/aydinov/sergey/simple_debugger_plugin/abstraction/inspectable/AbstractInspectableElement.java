package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.inspectable;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public abstract class AbstractInspectableElement {

    private final Tag tag;
    private final String elementName;
    private final UniversalElementType elementType;
    private final boolean inspectable;

    public AbstractInspectableElement(
            Tag tag,
            String elementName,
            UniversalElementType elementType,
            boolean inspectable
    ) {
        this.tag = tag;
        this.elementName = elementName;
        this.elementType = elementType;
        this.inspectable = inspectable;
    }

    public Tag getTag() {
        return tag;
    }

    public String getElementName() {
        return elementName;
    }

    public UniversalElementType getElementType() {
        return elementType;
    }

    public boolean isInspectable() {
        return inspectable;
    }

    // ================= Factory =================

    public static Factory factory() {
        return new Factory();
    }

    public static class Factory {

        private Factory() {}

        public AbstractInspectableElement createInspectableElement(
                AbstractUIEvent event,
                StackFrame currentFrame,
                BreakpointEvent breakpointEvent
        ) {
            return switch (event.getType()) {

                case USER_STARTED_INSPECTION_SEANCE -> {
                    if (!(event instanceof UIEvent<?> rawEvent)) {
                        throw new IllegalArgumentException("Invalid event type: " + event);
                    }

                    Object payload = rawEvent.getPayload();
                    if (!(payload instanceof InnerElementRepresentationDTO dto)) {
                        throw new IllegalArgumentException("Invalid payload: " + payload);
                    }

                    yield new InspectableIterableElement(dto, currentFrame, breakpointEvent);
                }

                default -> throw new UnsupportedOperationException(
                        "Unsupported event: " + event.getType()
                );
            };
        }
    }
}