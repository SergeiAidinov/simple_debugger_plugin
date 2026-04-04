package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.inspectable;

import java.util.List;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.inspection.BreadcrumbItemDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public abstract class AbstractInspectableElement {

	private final Tag tag;
	private final String elementName;
	private final UniversalElementType elementType;
	private final ValueCategory valueCategory;
	private final boolean inspectable;
	private List<BreadcrumbItemDTO> breadcrumbs;

	public AbstractInspectableElement(Tag tag, String elementName, UniversalElementType elementType,
			ValueCategory valueCategory, boolean inspectable) {
		this.tag = tag;
		this.elementName = elementName;
		this.elementType = elementType;
		this.valueCategory = valueCategory;
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

	public ValueCategory getValueCategory() {
		return valueCategory;
	}

	public List<BreadcrumbItemDTO> getBreadcrumbs() {
		return breadcrumbs;
	}

	public void setBreadcrumbs(List<BreadcrumbItemDTO> breadcrumbs) {
		this.breadcrumbs = breadcrumbs;
	}

	// ================= Factory =================
	public static Factory factory() {
		return new Factory();
	}

	public static class Factory {

		private Factory() {
		}

		public AbstractInspectableElement createInspectableElement(AbstractUIEvent event, StackFrame currentFrame,
				BreakpointEvent breakpointEvent) {
			if (!(event instanceof UIEvent<?> rawEvent)) {
				throw new IllegalArgumentException("Invalid event type: " + event);
			}
			Object payload = rawEvent.getPayload();
			if (!(payload instanceof InnerElementRepresentationDTO dto)) {
				throw new IllegalArgumentException("Invalid payload: " + payload);
			}
			if (dto.getValueCategory().equals(ValueCategory.COLLECTION))
				return new InspectableIterableElement(dto, currentFrame, breakpointEvent);
			else if (dto.getValueCategory().equals(ValueCategory.MAP))
				return new InspectableMapElement(dto, breakpointEvent);
			else
				throw new IllegalArgumentException("Invalid category: " + dto);
		}
	}
}