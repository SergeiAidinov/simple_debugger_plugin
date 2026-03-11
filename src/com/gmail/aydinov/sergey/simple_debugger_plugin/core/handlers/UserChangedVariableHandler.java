package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedVariableEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;

public class UserChangedVariableHandler implements UIEventHandler<UserChangedVariableEventDTO>{

	@Override
	@SuppressWarnings("unchecked")
	public boolean handle(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		UIEvent<UserChangedVariableEventDTO> userChangedVariableEvent = (UIEvent<UserChangedVariableEventDTO>) abstractSimpleDebuggerUIEvent;
		updateLocalVariable(userChangedVariableEvent, currentFrame);
		return true;
	}
	
	private void updateLocalVariable(UIEvent<UserChangedVariableEventDTO> userChangedVariableEventDTO, StackFrame currentFrame) {
		try {
			LocalVariable localVariable = currentFrame.visibleVariables().stream()
					.filter(v -> v.name().equals(userChangedVariableEventDTO.getPayload().getName())).findFirst().orElse(null);
			if (Objects.isNull(localVariable))
				return;
			Value value = DebugUtils.createJdiValueFromString(TargetVirtualMachineRepresentation.getInstance().getVirtualMachine(),
					localVariable, userChangedVariableEventDTO.getPayload().getNewValue().toString());
			currentFrame.setValue(localVariable, value);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
