package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
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
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;

public class UserChangedFieldHandler implements UIEventHandler {

	@Override
	@SuppressWarnings("unchecked")
	public boolean handle(AbstractUIEvent abstractSimpleDebuggerUIEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		UIEvent<UserChangedFieldEventDTO> userChangedFieldEvent = (UIEvent<UserChangedFieldEventDTO>) abstractSimpleDebuggerUIEvent;
		updateField(userChangedFieldEvent.getPayload(), breakpointEvent);
		return true;
	}

	private void updateField(UserChangedFieldEventDTO fieldEvent, BreakpointEvent breakpointEvent) {

		Optional<UniversalElementRepresentation> fieldOntional = TargetApplicationRepresentation.getInstance()
				.getTargetApplicationSnapshot().getSecond().values().stream()
				.filter(e -> (e instanceof UniversalElementRepresentation)).map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.equals(e.getTag(), fieldEvent.getTag())).findAny();
		if (fieldOntional.isEmpty())
			return;
		Value value = null;
		ObjectReference targetObject = fieldOntional.get().getObjectReference();
		if (Objects.isNull(targetObject)) {
			Optional<UniversalElementRepresentation> qq = TargetApplicationRepresentation.getInstance()
					.getTargetApplicationSnapshot().getSecond().values().stream()
					.filter(e -> (e instanceof UniversalElementRepresentation))
					.map(e -> (UniversalElementRepresentation) e)
					.filter(e -> Objects.equals(e.getTag().getUniqueId(), fieldEvent.getTag().getParentId())).findAny();
			if (qq.isPresent()) {
				targetObject = qq.get().getObjectReference();
			}
		}
		ReferenceType fieldReference = fieldOntional.get().getReferenceType();
		Field field = fieldReference.fieldByName(fieldOntional.get().getElementName());

		try {
			if (Modifier.isStatic(field.modifiers()) && fieldReference instanceof ClassType classType) {
				value = fieldReference.getValue(field);
				System.out.println(value);
				classType.setValue(field, value);
			} else if (targetObject != null) {
				Value v = targetObject.getValue(field);
				System.out.println(v);
				 Map<Field, Value> ee = targetObject.getValues(List.of(field));
				 Value rr = ee.get(field); 
				 Value newValue = DebugUtils.createJdiObjectFromString(
						    TargetVirtualMachineRepresentation.getInstance().getVirtualMachine(), 
						    field.type(),  // <--- используем type(), а не declaringType()
						    fieldEvent.getNewValue(), 
						    breakpointEvent.thread()
						);
				targetObject.setValue(field, newValue);
			} else {
				// Нечего менять: объект вне текущего стека
				SimpleDebuggerLogger.warn("Cannot update field " + field.name() + ": object not available on stack");
			}
		} catch (InvalidTypeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ClassNotLoadedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
