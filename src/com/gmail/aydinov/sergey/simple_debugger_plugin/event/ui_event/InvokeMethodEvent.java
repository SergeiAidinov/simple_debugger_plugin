package com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationClassOrInterfaceRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationMethodDTO;

public class InvokeMethodEvent extends AbstractUIEvent {

	private final TargetApplicationClassOrInterfaceRepresentation clazz;
	private final TargetApplicationMethodDTO method;
	private final String argumentsText;

	public InvokeMethodEvent(TargetApplicationClassOrInterfaceRepresentation clazz, TargetApplicationMethodDTO method,
			String argumentsText) {
		this.clazz = clazz;
		this.method = method;
		this.argumentsText = argumentsText;
	}

	public TargetApplicationClassOrInterfaceRepresentation getClazz() {
		return clazz;
	}

	public TargetApplicationMethodDTO getMethod() {
		return method;
	}

	public String getArgumentsText() {
		return argumentsText;
	}

	@Override
	public String toString() {
		return "InvokeMethodEvent [clazz=" + clazz + ", method=" + method + ", argumentsText=" + argumentsText + "]";
	}
	
	
}
