package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.lang.reflect.Modifier;
import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedFieldEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;

public class UserChangedFieldHandler implements UIEventHandler{

	@Override
	@SuppressWarnings("unchecked")
	public boolean handle(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		UIEvent<UserChangedFieldEventDTO> userChangedFieldEvent = (UIEvent<UserChangedFieldEventDTO>) abstractSimpleDebuggerUIEvent;
		updateField(userChangedFieldEvent.getPayload(), currentFrame);
		return true;
	}
	
	private void updateField(UserChangedFieldEventDTO fieldEvent, StackFrame currentFrame) {
		ReferenceType referenceType = Objects.nonNull(currentFrame.thisObject())
				? currentFrame.thisObject().referenceType()
				: currentFrame.location().declaringType();
		Field field = referenceType.fieldByName(fieldEvent.getFieldName());
		if (Objects.isNull(field))
			return;
		Value value = null;
		try {
			value = DebugUtils.createJdiObjectFromString(TargetVirtualMachineRepresentation.getInstance().getVirtualMachine(),
					field.type(), fieldEvent.getNewValue(), currentFrame.thread());
		} catch (ClassNotLoadedException e) {
			SimpleDebuggerLogger.error(null, e);
		}
		if (Modifier.isStatic(field.modifiers()) && referenceType instanceof ClassType classType) {
			try {
				classType.setValue(field, value);
			} catch (InvalidTypeException | ClassNotLoadedException e) {
				SimpleDebuggerLogger.error(null, e);
			}
		} else if (Objects.nonNull(currentFrame.thisObject())) {
			try {
				currentFrame.thisObject().setValue(field, value);
			} catch (InvalidTypeException | ClassNotLoadedException e) {
				SimpleDebuggerLogger.error(null, e);
			}
		}
	}
}
