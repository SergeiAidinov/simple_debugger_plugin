package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.HandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.DebugHandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedFieldEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.window.SimpleDebugerWindowsManager;
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
	public boolean handle(HandlerContext abstractUIEventContext, AbstractUIEvent abstractUIEvent) {
		DebugHandlerContext debugSessionUIEventContext = (DebugHandlerContext) abstractUIEventContext;
		UIEvent<UserChangedFieldEventDTO> event = (UIEvent<UserChangedFieldEventDTO>) abstractUIEvent;
		UserChangedFieldEventDTO payload = event.getPayload();
		boolean shouldUpdateUi = false;
		// Сначала пытаемся изменить примитив / строку
		shouldUpdateUi = updatePrimitiveField(payload, debugSessionUIEventContext.getFrame(), debugSessionUIEventContext.getBreakpointEvent());
		if (shouldUpdateUi)
			return shouldUpdateUi;
		else
			return updateObjectField(payload, debugSessionUIEventContext.getFrame(), debugSessionUIEventContext.getBreakpointEvent());
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
	private boolean updateObjectField(UserChangedFieldEventDTO fieldEvent, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		List<AbstractElementRepresentation> allElements = new ArrayList<AbstractElementRepresentation>(
				TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().getFirst().values());
		allElements.addAll(
				TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().getSecond().values());
		Optional<UniversalElementRepresentation> fieldToBeChangedOptional = allElements.stream()
				.filter(e -> e instanceof UniversalElementRepresentation).map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.equals(e.getTag(), fieldEvent.getTag())).findAny();
		if (fieldToBeChangedOptional.isEmpty())
			return false;
		UniversalElementRepresentation fieldToBeChanged = fieldToBeChangedOptional.get();
		Optional<ObjectReference> parentInstanceOptional = allElements.stream()
				.filter(e -> e instanceof UniversalElementRepresentation).map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.equals(fieldToBeChanged.getTag().getParentId(), e.getTag().getUniqueId()))
				.map(e -> e.getObjectReference()).findAny();
		Optional<ReferenceType> parentTypeOpt = allElements.stream()
				.filter(e -> e instanceof UniversalElementRepresentation).map(e -> (UniversalElementRepresentation) e)
				.filter(e -> Objects.equals(fieldToBeChanged.getTag().getParentId(), e.getTag().getUniqueId()))
				.map(e -> e.getReferenceType()).findAny();
		if (parentInstanceOptional.isEmpty() || parentTypeOpt.isEmpty())
			return false;
		ObjectReference parentInstance = parentInstanceOptional.get();
		ReferenceType parentType = parentTypeOpt.get();
		Field field = parentType.fieldByName(fieldToBeChanged.getElementName());
		if (Objects.isNull(field))
			return false;
		Value value = null;
		try {
			value = DebugUtils.createJdiObjectFromString(
					TargetVirtualMachineRepresentation.getInstance().getVirtualMachine(), field.type(),
					fieldEvent.getNewValue(), currentFrame.thread());
			if (Modifier.isFinal(field.modifiers())) {
				SimpleDebugerWindowsManager.instance().getOrCreateMainWindow().showError("Field Modification Error",
						"Cannot modify a final field.");
				return false;
			} else if (Modifier.isStatic(field.modifiers()) && parentType instanceof ClassType classType) {
				classType.setValue(field, value);
			} else {
				parentInstance.setValue(field, value);
			}
		} catch (Exception e) {
			SimpleDebugerWindowsManager.instance().getOrCreateMainWindow().showError("Field Modification Error",
					"Cannot modify a field.");
			return false;
		}
		System.out.println("FIELD: " + field);
		return true;
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
}
