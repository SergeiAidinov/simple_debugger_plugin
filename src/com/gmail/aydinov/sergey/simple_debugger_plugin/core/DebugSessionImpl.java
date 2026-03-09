package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.internal.core.InitializerElementInfo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.ITextEditor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DebugSession;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TopLevelElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedFieldEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserChangedVariableEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserInvokedMethodEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.DebugWindowDataDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.UserInstanceElementInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.UserInstanceInnerElementInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.UserInstanceInspectionDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.InspectionSeance;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;

/**
 * Implementation of a debug session that processes JDI events, handles
 * breakpoints, updates UI, and allows variable/field modification and method
 * invocation in the target application.
 * <p>
 * Author: Sergei Aidinov <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class DebugSessionImpl implements DebugSession {

	private static final AtomicReference<String> methodInvocationResult = new AtomicReference<>("");
	private final TargetVirtualMachineRepresentation targetVirtualMachineRepresentation;
	private final TargetApplicationRepresentation targetApplicationRepresentation;
	private final EventSet eventSet;
	private final CurrentLineHighlighter currentLineHighlighter;
	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private final DebugEventCollector simpleDebugEventCollector = SimpleDebuggerEventCollector.instance();
	private boolean shouldRefreshSnapsotAndUi = true;

	public DebugSessionImpl(TargetVirtualMachineRepresentation targetVirtualMachineRepresentation,
			TargetApplicationRepresentation targetApplicationRepresentation, EventSet eventSet,
			CurrentLineHighlighter currentLineHighlighter) {
		this.targetVirtualMachineRepresentation = targetVirtualMachineRepresentation;
		this.targetApplicationRepresentation = targetApplicationRepresentation;
		this.eventSet = eventSet;
		this.currentLineHighlighter = currentLineHighlighter;
	}

	@Override
	public void run() {
		try {
			DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
			SimpleDebuggerLogger.info("DEBUG SESSION STARTED");
			simpleDebugEventCollector
					.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
			processEvents();
		} catch (Throwable exception) {
			logError("Fatal error in JDI event loop", exception);
		} finally {
			SimpleDebuggerLogger.info("DEBUG SESSION FINISHED");
			DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_FINISHED);
		}
	}

	/** Processes all events in the EventSet */
	private void processEvents() {

		for (Event event : eventSet) {
			if (!DebuggerContext.context().isRunning())
				return;

			if (event instanceof BreakpointEvent breakpointEvent) {
				if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
					handleVmDisconnected();
					return;
				}

				doWorkAtBreakpoint(breakpointEvent);
				simpleDebugEventCollector.collectDebugEvent(
						new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
			}
		}
	}

	private void handleVmDisconnected() {
		try {
			targetApplicationRepresentation.detachDebugger();
			eventSet.resume();
		} catch (Exception ignored) {
		}
	}

	private void doWorkAtBreakpoint(BreakpointEvent breakpointEvent) {
		targetApplicationRepresentation
				.takeSnapshotOfTargetApplication(targetVirtualMachineRepresentation.getVirtualMachine());
		targetApplicationRepresentation.addLocalVariables(targetVirtualMachineRepresentation.getVirtualMachine(),
				breakpointEvent);
		updateUI(breakpointEvent);
		Display display = Display.getDefault();
		if (Objects.nonNull(display) && !display.isDisposed()) {
			display.asyncExec(() -> {
				try {
					ITextEditor editor = openEditorForLocation(breakpointEvent.location());
					if (Objects.nonNull(editor)) {
						int lineNumber = breakpointEvent.location().lineNumber() - 1;
						currentLineHighlighter.highlight(editor, lineNumber);
					}
				} catch (Throwable exception) {
					logError("Cannot highlight breakpoint location", exception);
				}
			});
			while (DebuggerContext.context().isDebugSessionActive()) {
				AbstractUIEvent uiEvent = uiEventCollector.pollUiEvent();
				if (Objects.isNull(uiEvent))
					continue;
				try {
					shouldRefreshSnapsotAndUi = true;
					handleSingleUiEvent(uiEvent, breakpointEvent);
					if (shouldRefreshSnapsotAndUi) {
						targetApplicationRepresentation.takeSnapshotOfTargetApplication(
								targetVirtualMachineRepresentation.getVirtualMachine());
						targetApplicationRepresentation.addLocalVariables(
								targetVirtualMachineRepresentation.getVirtualMachine(), breakpointEvent);
						updateUI(breakpointEvent);
					}
				} catch (Throwable exception) {
					logError("Breakpoint handler error", exception);
				}

			}
		}
	}

	@SuppressWarnings("unchecked")
	private void handleSingleUiEvent(AbstractUIEvent abstractSimpleDebuggerUIEvent, BreakpointEvent breakpointEvent) {
		StackFrame currentFrame = getTopFrame(breakpointEvent.thread());
		if (Objects.isNull(currentFrame))
			return;
		try {
			if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_CHANGED_VARIABLE)) {
				UIEvent<UserChangedVariableEventDTO> userChangedVariableEvent = (UIEvent<UserChangedVariableEventDTO>) abstractSimpleDebuggerUIEvent;
				updateLocalVariable(userChangedVariableEvent.getPayload(), currentFrame);
				shouldRefreshSnapsotAndUi = false;
			} else if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_CHANGED_FIELD)) {
				UIEvent<UserChangedFieldEventDTO> userChangedFieldEvent = (UIEvent<UserChangedFieldEventDTO>) abstractSimpleDebuggerUIEvent;
				updateField(userChangedFieldEvent.getPayload(), currentFrame);
				shouldRefreshSnapsotAndUi = false;
			} else if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_INVOKED_METHOD)) {
				UIEvent<UserInvokedMethodEventDTO> userInvokedMethodEvent = (UIEvent<UserInvokedMethodEventDTO>) abstractSimpleDebuggerUIEvent;
				invokeMethod(userInvokedMethodEvent.getPayload(), breakpointEvent, currentFrame);
				shouldRefreshSnapsotAndUi = false;
			} else if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_PRESSED_RESUME_BUTTON)) {
				SimpleDebuggerLogger.info("User pressed RESUME");
				DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_FINISHED);
				shouldRefreshSnapsotAndUi = false;
			} else if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_CLOSED_DEBUG_WINDOW)) {
				SimpleDebuggerLogger.info("User closed debug window → stopping debug session");
				DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUGGER_STOPPED);
				targetVirtualMachineRepresentation.getVirtualMachine().dispose();
				shouldRefreshSnapsotAndUi = false;
			} else if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_REQUESTED_ADDITIONAL_INFO_ABOUT_OBJECT)) {
				UIEvent<InnerElementRepresentationDTO> userRequestedAdditionalInfo = (UIEvent<InnerElementRepresentationDTO>) abstractSimpleDebuggerUIEvent;
				shouldRefreshSnapsotAndUi = false;
				provideAdditionalInfoAboutObject(userRequestedAdditionalInfo);
			} else if (Objects.equals(abstractSimpleDebuggerUIEvent.getType(),
					SimpleDebuggerEventType.USER_REQUESTED_ADDITIONAL_INFO_ABOUT_COLLECTION)) {
				UIEvent<InnerElementRepresentationDTO> userRequestedAdditionalInfo = (UIEvent<InnerElementRepresentationDTO>) abstractSimpleDebuggerUIEvent;
				shouldRefreshSnapsotAndUi = false;
				System.out.println(SimpleDebuggerEventType.USER_REQUESTED_ADDITIONAL_INFO_ABOUT_COLLECTION.name()
						+ userRequestedAdditionalInfo.toString());
				provideAdditionalInfoAboutCollection(userRequestedAdditionalInfo);
			} else {
				SimpleDebuggerLogger
						.info("Unhandled UI event: " + abstractSimpleDebuggerUIEvent.getClass().getSimpleName());
			}
		} catch (Exception exception) {
			SimpleDebuggerLogger.error(exception.getMessage(), exception);
		}
	}

	private void provideAdditionalInfoAboutCollection(
			UIEvent<InnerElementRepresentationDTO> userRequestedAdditionalInfo) {

		InnerElementRepresentationDTO anchorElement = userRequestedAdditionalInfo.getPayload();
		UniversalElementRepresentation parentInstance = targetApplicationRepresentation.getTargetApplicationSnapshot()
				.get(anchorElement.getTag());

		if (parentInstance == null)
			return;

		ObjectReference parentRef = parentInstance.getObjectReference();

		List<UniversalElementRepresentation> qq = targetApplicationRepresentation.getTargetApplicationSnapshot()
				.values().stream()
				.filter(e -> Objects.equals(e.getElementName(),
						userRequestedAdditionalInfo.getPayload().getElementName()))
				.filter(e -> Objects.equals(e.getTag().getParentId(),
						userRequestedAdditionalInfo.getPayload().getTag().getParentId()))
				.toList();
		
		if (qq.size() != 1) return;

		// Получаем размер через метод, который мы сделали
		int size = getCollectionSize(qq.get(0).getObjectReference());

		System.out.println("Collection/Map size = " + size);

		// Собираем DTO для UI
		UserInstanceInspectionDTO userInstanceInspectionDTO = new UserInstanceInspectionDTO(
				anchorElement.getElementName(), anchorElement.getTypeOrReturnType() + " (size = " + size + ")",
				Collections.emptyMap());

		System.out.println(userInstanceInspectionDTO);
	}

	private int getCollectionSize(ObjectReference ref) {
		if (ref == null)
			return -1;

		ReferenceType refType = ref.referenceType();
		try {
			// ===== Если это массив =====
			if (ref instanceof ArrayReference arrayRef) {
				return arrayRef.length();
			}

			// ===== Если это объект класса =====
			if (refType instanceof ClassType classType) {

				// Сначала пробуем Map
				for (InterfaceType iface : classType.allInterfaces()) {
					System.out.println("IFACE: " + iface.name());
					if ("java.util.Map".equals(iface.name())) {
						Field sizeField = refType.fieldByName("size");
						if (sizeField != null) {
							IntegerValue intValue = (IntegerValue) ref.getValue(sizeField);
							return intValue.value();
						}
					}
				}

				// Потом Collection (List/Set)
				for (InterfaceType iface : classType.allInterfaces()) {
					System.out.println("IFACE: " + iface.name());
					if ("java.util.Collection".equals(iface.name())) {
						Field sizeField = refType.fieldByName("size");
						if (sizeField != null) {
							IntegerValue intValue = (IntegerValue) ref.getValue(sizeField);
							return intValue.value();
						}
					}
				}

				// fallback: ArrayList/LinkedList (проверка поля size напрямую)
				Field sizeField = refType.fieldByName("size");
				if (sizeField != null) {
					Value val = ref.getValue(sizeField);
					if (val instanceof IntegerValue intValue)
						return intValue.value();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1; // неизвестный тип
	}

	private int getMapSize(ObjectReference mapRef) {
		if (mapRef == null)
			return -1;
		try {
			ReferenceType mapType = mapRef.referenceType();
			Field sizeField = mapType.fieldByName("size"); // у HashMap есть поле "size"
			if (sizeField != null) {
				IntegerValue intValue = (IntegerValue) mapRef.getValue(sizeField);
				return intValue.value();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	private void provideAdditionalInfoAboutObject(UIEvent<InnerElementRepresentationDTO> userRequestedAdditionalInfo) {
		InnerElementRepresentationDTO anchorElement = userRequestedAdditionalInfo.getPayload();
		UniversalElementRepresentation topLevelElement = targetApplicationRepresentation.getTargetApplicationSnapshot()
				.get(anchorElement.getTag());
		Set<UniversalElementRepresentation> relevantElements = compileAdditionalInfo(topLevelElement);
		relevantElements.remove(topLevelElement);
		List<UniversalElementRepresentation> elements = new ArrayList<UniversalElementRepresentation>(relevantElements);
		Collections.sort(elements);
		Map<Integer, ArrayList<UserInstanceInnerElementInspectionDTO>> separatedIntoGroups = Map.of(1,
				new ArrayList<UserInstanceInnerElementInspectionDTO>(), 2,
				new ArrayList<UserInstanceInnerElementInspectionDTO>(), 3,
				new ArrayList<UserInstanceInnerElementInspectionDTO>());
		for (UniversalElementRepresentation element : elements) {
			UserInstanceInnerElementInspectionDTO userInstanceInnerElementInspectionDTO = new UserInstanceInnerElementInspectionDTO(
					element.getElementName(), element.gettypeOrReturnType(), element.getValue());
			if (element.getElementType().ordinal() < 5)
				separatedIntoGroups.get(1).add(userInstanceInnerElementInspectionDTO);
			else if (element.getElementType().ordinal() == 5)
				separatedIntoGroups.get(2).add(userInstanceInnerElementInspectionDTO);
			else if (element.getElementType().ordinal() > 5)
				separatedIntoGroups.get(3).add(userInstanceInnerElementInspectionDTO);
		}
		UserInstanceInspectionDTO userInstanceInspectionDTO = new UserInstanceInspectionDTO(
				topLevelElement.getElementName(), anchorElement.getTypeOrReturnType(), separatedIntoGroups);
		simpleDebugEventCollector.collectDebugEvent(new DebugEvent<UserInstanceInspectionDTO>(
				SimpleDebuggerEventType.DISPLAY_ADDITIONAL_INFO, userInstanceInspectionDTO));

	}

	private Set<UniversalElementRepresentation> compileAdditionalInfo(UniversalElementRepresentation topLevelElement) {
		Set<UniversalElementRepresentation> selectedElements = new HashSet<UniversalElementRepresentation>();
		targetApplicationRepresentation.getTargetApplicationSnapshot().values().stream().filter(e -> Objects.nonNull(e))
				.filter(e -> Objects.equals(e.getElementName(), topLevelElement.getElementName())).findAny()
				.ifPresent(elementName -> {
					elementName.getTag().getUniqueId();
					targetApplicationRepresentation.getTargetApplicationSnapshot().values().stream()
							.filter(e -> Objects.equals(e.getElementName(), topLevelElement.getElementName())).findAny()
							.ifPresent(field -> {
								targetApplicationRepresentation.getTargetApplicationSnapshot().values().stream().filter(
										e -> Objects.equals(e.getTag().getParentId(), field.getTag().getUniqueId()))
										.findAny().ifPresent(root -> {
											boolean found = true;
											Set<UniversalElementRepresentation> iterationElements = new HashSet<UniversalElementRepresentation>();
											iterationElements.add(root);
											while (found) {
												for (UniversalElementRepresentation iterationElement : iterationElements) {
													iterationElements.addAll(targetApplicationRepresentation
															.getTargetApplicationSnapshot().values().stream()
															.filter(e -> Objects.equals(e.getObjectReference(),
																	iterationElement.getObjectReference()))
															.filter(e -> !Objects.equals(e.getAdditionalInfo(),
																	topLevelElement.gettypeOrReturnType()))
															.toList());
												}
												iterationElements.remove(root);
												if (iterationElements.isEmpty())
													found = false;
												selectedElements.addAll(iterationElements);
												iterationElements.clear();
											}
										});
							});
				});
		return selectedElements;
	}

	private void initiateInspectionSeanceIfPossible(InnerElementRepresentationDTO innerElementRepresentationDTO) {
		if (DebuggerContext.context().getStatus().equals(SimpleDebuggerStatus.INSPECTION_SEANCE_STARTING)
				|| DebuggerContext.context().isInspectionSeanceActive())
			return;
		Map<Tag, UniversalElementRepresentation> qq = targetApplicationRepresentation.getTargetApplicationSnapshot();
		System.out.println(qq);
		targetApplicationRepresentation.getTargetApplicationSnapshot().keySet().stream()
				.filter(v -> v.getUniqueId().equals(innerElementRepresentationDTO.getTag().getUniqueId())).findAny() // <-
																														// получаем
																														// Optional<UniversalElementRepresentation>
				.ifPresent(topLevelElementId -> {
					System.out.println("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq");
					try {
						UniversalElementRepresentation topLevelElement = targetApplicationRepresentation
								.getTargetApplicationSnapshot().get(topLevelElementId);
						simpleDebugEventCollector.collectDebugEvent(new DebugEvent<Boolean>(
								SimpleDebuggerEventTypes.SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, false));
						simpleDebugEventCollector.collectDebugEvent(
								new DebugEvent<Boolean>(SimpleDebuggerEventType.DISPLAY_INSPECTION_WINDOW, true));

						InspectionSeance inspectionSession = new InspectionSeanceImpl(topLevelElement,
								targetApplicationRepresentation);
						// ...
					} catch (Exception e) {
						e.printStackTrace();
					}
				});

		simpleDebugEventCollector.collectDebugEvent(new DebugEvent<Boolean>(
				SimpleDebuggerEventTypes.SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
		DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
	}

	private void updateLocalVariable(UserChangedVariableEventDTO userChangedVariableEventDTO, StackFrame currentFrame) {
		try {
			LocalVariable localVariable = currentFrame.visibleVariables().stream()
					.filter(v -> v.name().equals(userChangedVariableEventDTO.getName())).findFirst().orElse(null);
			if (Objects.isNull(localVariable))
				return;
			Value value = DebugUtils.createJdiValueFromString(targetVirtualMachineRepresentation.getVirtualMachine(),
					localVariable, userChangedVariableEventDTO.getNewValue().toString());
			currentFrame.setValue(localVariable, value);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateField(UserChangedFieldEventDTO fieldEvent, StackFrame currentFrame) throws Exception {
		ReferenceType referenceType = Objects.nonNull(currentFrame.thisObject())
				? currentFrame.thisObject().referenceType()
				: currentFrame.location().declaringType();
		Field field = referenceType.fieldByName(fieldEvent.getFieldName());
		if (Objects.isNull(field))
			return;
		Value value = DebugUtils.createJdiObjectFromString(targetVirtualMachineRepresentation.getVirtualMachine(),
				field.type(), fieldEvent.getNewValue(), currentFrame.thread());
		if (Modifier.isStatic(field.modifiers()) && referenceType instanceof ClassType classType) {
			classType.setValue(field, value);
		} else if (Objects.nonNull(currentFrame.thisObject())) {
			currentFrame.thisObject().setValue(field, value);
		}
	}

	private void invokeMethod(UserInvokedMethodEventDTO invokeEvent, BreakpointEvent breakpointEvent,
			StackFrame currentFrame) {
		try {
			List<Value> methodArguments = DebugUtils
					.parseArguments(targetVirtualMachineRepresentation.getVirtualMachine(), invokeEvent);
			ReferenceType referenceType = targetApplicationRepresentation
					.findReferenceTypeForClass(invokeEvent.getTargetClass());
			Method method = referenceType.methodsByName(invokeEvent.getMethod().getMethodName()).get(0);
			ObjectReference instance = !method.isStatic()
					? targetApplicationRepresentation.createObjectInstance((ClassType) referenceType)
					: null;
			Value result = Objects.nonNull(instance)
					? instance.invokeMethod(targetVirtualMachineRepresentation.getVirtualMachine().allThreads().get(0),
							method, methodArguments, ObjectReference.INVOKE_SINGLE_THREADED)
					: ((ClassType) referenceType).invokeMethod(
							targetVirtualMachineRepresentation.getVirtualMachine().allThreads().get(0), method,
							methodArguments, ClassType.INVOKE_SINGLE_THREADED);
			methodInvocationResult.set(String.valueOf(result));
			simpleDebugEventCollector.collectDebugEvent(new DebugEvent<String>(
					SimpleDebuggerEventTypes.SimpleDebuggerEventType.METHOD_INVOKE, methodInvocationResult.get()));
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	private StackFrame getTopFrame(ThreadReference thread) {
		try {
			return thread.frame(0);
		} catch (Exception exception) {
			return null;
		}
	}

	private boolean updateUI(BreakpointEvent breakpointEvent) {
		if (Objects.isNull(breakpointEvent))
			return false;
		Location location = breakpointEvent.location();
		AtomicReference<ObjectReference> thisObjectRef = new AtomicReference<>();
		try {
			thisObjectRef.set(breakpointEvent.thread().frame(0).thisObject());
		} catch (IncompatibleThreadStateException e) {
			e.printStackTrace();
			return false;
		}
		ThreadReference thread = breakpointEvent.thread();
		StackFrame frame = null;
		try {
			frame = thread.frame(0);
		} catch (IncompatibleThreadStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (Objects.isNull(frame))
			return false;
		ObjectReference inctance = frame.thisObject();
		Optional<UniversalElementRepresentation> anchorInstanceOptional = targetApplicationRepresentation
				.getTargetApplicationSnapshot().values().stream()
				.filter(e -> Objects.equals(e.getObjectReference(), inctance))
				.filter(e -> Objects.isNull(e.getTag().getParentId())).findAny();
		if (anchorInstanceOptional.isEmpty())
			return false;
		UniversalElementRepresentation anchorElement = anchorInstanceOptional.get();
		Set<UniversalElementRepresentation> relevantElements = selectFieldsAndMethods(anchorElement);
		List<UniversalElementRepresentation> locals = targetApplicationRepresentation.getTargetApplicationSnapshot()
				.values().stream().filter(e -> e.getElementType().equals(UniversalElementType.LOCAL_VARIABLE)).toList();
		relevantElements.stream().forEach(e -> System.out.println("RL:" + e));
		relevantElements.addAll(locals);
		Set<InnerElementRepresentationDTO> innerElementDTOs = new HashSet();
		for (UniversalElementRepresentation element : relevantElements) {
			InnerElementRepresentationDTO elementRepresentation = InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
					.fromUniversalElement(element);
			innerElementDTOs.add(elementRepresentation);
		}
		targetApplicationRepresentation.getTargetApplicationSnapshot().values().stream()
				.forEach(e -> System.out.println("MD:" + e));
		System.out.println("INNER_ELS:" + innerElementDTOs);
		String methodName = location.declaringType().name() + "." + location.method().name() + "()";
		DebugWindowDataDTO debugWindowDataDTO = new DebugWindowDataDTO(location.lineNumber(), methodName,
				DebugUtils.compileStackInfo(thread), innerElementDTOs);
		simpleDebugEventCollector.collectDebugEvent(new DebugEvent<DebugWindowDataDTO>(
				SimpleDebuggerEventTypes.SimpleDebuggerEventType.STOPPED_AT_BREAKPOINT, debugWindowDataDTO));
		simpleDebugEventCollector
				.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
		Display display = Display.getDefault();
		if (Objects.nonNull(display) && !display.isDisposed()) {
			display.asyncExec(() -> {
				try {
					ITextEditor editor = openEditorForLocation(breakpointEvent.location());
					if (Objects.nonNull(editor)) {
						int lineNumber = breakpointEvent.location().lineNumber() - 1;
						currentLineHighlighter.highlight(editor, lineNumber);
					}
				} catch (Throwable exception) {
					logError("Cannot highlight breakpoint location", exception);
				}
			});
		}
		System.out.println("relevantElements: " + relevantElements.size());
		return true;
	}

	private Set<UniversalElementRepresentation> selectFieldsAndMethods(UniversalElementRepresentation initElement) {
		if (Objects.isNull(initElement))
			return Collections.emptySet();
		Set<UniversalElementRepresentation> foundElements = new HashSet<>();
		foundElements.add(initElement);
		boolean found = true;
		while (found) {
			// Set<UniversalElementRepresentation> currentIterationFoundElements = new
			// HashSet<>();
			for (UniversalElementRepresentation earlierFoundElement : foundElements) {
				List<UniversalElementRepresentation> justFoundElements = new ArrayList<UniversalElementRepresentation>();
				justFoundElements.addAll(targetApplicationRepresentation.getTargetApplicationSnapshot().values()
						.stream().filter(e -> Objects.equals(e.getObjectReference(), initElement.getObjectReference()))
						.filter(e -> Objects.equals(e.getTag().getParentId(),
								earlierFoundElement.getTag().getUniqueId()))
						.toList());
				if (justFoundElements.isEmpty()) {
					found = false;
					break;
				} else {
					foundElements.addAll(justFoundElements);
					justFoundElements.clear();
				}
			}
		}

		return foundElements;
	}

	private ITextEditor openEditorForLocation(Location location) throws Exception {
		if (Objects.isNull(location))
			return null;
		IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (Objects.isNull(workbenchWindow))
			return null;
		IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();
		if (Objects.isNull(workbenchPage))
			return null;
		IFile file = targetApplicationRepresentation.findIFileForLocation(location);
		if (Objects.isNull(file))
			throw new IllegalStateException("Cannot map location to IFile: " + location);
		IEditorPart editorPart = IDE.openEditor(workbenchPage, file, true);
		if (editorPart instanceof ITextEditor textEditor) {
			return textEditor;
		}
		throw new IllegalStateException("Opened editor is not a text editor");
	}

	private void logError(String message, Throwable exception) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, "simple_debugger_plugin", message, exception),
				StatusManager.LOG);
	}
}
