package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
	public boolean handle(AbstractUIEvent abstractUIEvent, StackFrame currentFrame, BreakpointEvent breakpointEvent) {
		UIEvent<UserChangedFieldEventDTO> event = (UIEvent<UserChangedFieldEventDTO>) abstractUIEvent;
		UserChangedFieldEventDTO payload = event.getPayload();

		// Сначала пытаемся изменить примитив / строку
		if (!updatePrimitiveField(payload, currentFrame, breakpointEvent)) {
			// Если это не примитив, пробуем изменить поле-класс
			updateObjectField(payload, currentFrame, breakpointEvent);
		}

		return true;
	}

	// ------------------- Примитивы -------------------
	private boolean updatePrimitiveField(UserChangedFieldEventDTO fieldEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		Optional<UniversalElementRepresentation> elementOptional = TargetApplicationRepresentation.getInstance()
				.getTargetApplicationSnapshot().getSecond().values().stream()
				.filter(e -> e instanceof UniversalElementRepresentation).map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.equals(e.getTag(), fieldEvent.getTag())).findAny();

		if (elementOptional.isEmpty())
			return false;

		UniversalElementRepresentation element = elementOptional.get();
		ObjectReference targetObject = element.getObjectReference();

		// Если объекта нет, пробуем родителя
		if (targetObject == null) {
			targetObject = findParentObject(fieldEvent.getTag().getParentId());
			if (targetObject == null)
				return false;
		}

		ReferenceType type = targetObject.referenceType();
		Field field = type.fieldByName(element.getElementName());
		if (field == null)
			return false;

		try {
			// Пробуем создать JDI Value через DebugUtils
			Value newValue = DebugUtils.createJdiObjectFromString(
					TargetVirtualMachineRepresentation.getInstance().getVirtualMachine(), field.type(),
					fieldEvent.getNewValue(), breakpointEvent.thread());

			if (newValue == null)
				return false;

			targetObject.setValue(field, newValue);
			return true;

		} catch (InvalidTypeException | ClassNotLoadedException e) {
			SimpleDebuggerLogger.error("Failed to update primitive field " + field.name(), e);
			return false;
		}
	}

	// ------------------- Поля классов -------------------
	private void updateObjectField(UserChangedFieldEventDTO fieldEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		Optional<UniversalElementRepresentation> optional = TargetApplicationRepresentation.getInstance()
				.getTargetApplicationSnapshot().getSecond().values().stream()
				.filter(e -> e instanceof UniversalElementRepresentation).map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.equals(e.getTag(), fieldEvent.getTag())).findAny();
		if (optional.isEmpty())
			return;
		UniversalElementRepresentation element = optional.get();

		ReferenceType referenceType = Objects.nonNull(currentFrame.thisObject())
				? currentFrame.thisObject().referenceType()
				: currentFrame.location().declaringType();
		Field field = referenceType.fieldByName(element.getElementName());
		if (Objects.isNull(field))
			return;
		Value value = null;
		try {
			value = DebugUtils.createJdiObjectFromString(
					TargetVirtualMachineRepresentation.getInstance().getVirtualMachine(), field.type(),
					fieldEvent.getNewValue(), currentFrame.thread());
			if (Modifier.isStatic(field.modifiers()) && referenceType instanceof ClassType classType) {
				classType.setValue(field, value);
			}
			if (Modifier.isStatic(field.modifiers()) && referenceType instanceof ClassType classType) {
				classType.setValue(field, value);
			} else if (Objects.nonNull(currentFrame.thisObject())) {
				currentFrame.thisObject().setValue(field, value);
			}
		} catch (IllegalArgumentException iae) {
			System.out.println("Cannot set value of final field");
		} catch (Exception e) {
			System.out.println("ERROR: " + e);
			SimpleDebuggerLogger.error(null, e);
		}
		System.out.println("FIELD: " + field);
	}

	// ------------------- Вспомогательные методы -------------------
	private ObjectReference findParentObject(UUID uuid) {
		return TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().getSecond().values()
				.stream().filter(e -> e instanceof UniversalElementRepresentation)
				.map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.equals(e.getTag().getUniqueId(), uuid))
				.map(UniversalElementRepresentation::getObjectReference).filter(Objects::nonNull).findAny()
				.orElse(null);
	}

	private Optional<UniversalElementRepresentation> findFieldRecursively(ObjectReference targetObject,
			String fieldName) {
		return TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().getSecond().values()
				.stream().filter(e -> e instanceof UniversalElementRepresentation)
				.map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.equals(e.getObjectReference(), targetObject)).findAny();

	}
}
