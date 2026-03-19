package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.ElementReference;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationBreakpointRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DebugSession;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.InspectionSeance;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.DebugWindowDataDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes.SimpleDebuggerEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
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

	public static final AtomicReference<String> methodInvocationResult = new AtomicReference<>("");
	private final EventSet eventSet;
	private final CurrentLineHighlighterImpl currentLineHighlighter;
	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private final DebugEventCollector simpleDebugEventCollector = SimpleDebuggerEventCollector.instance();
	public AtomicBoolean shouldRefreshSnapsotAndUi = new AtomicBoolean(true);

	public DebugSessionImpl(EventSet eventSet) {
		this.eventSet = eventSet;
		this.currentLineHighlighter = new CurrentLineHighlighterImpl();
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
		TargetApplicationBreakpointRepresentation.getInstance().refreshBreakpoints();
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
			TargetApplicationRepresentation.getInstance().detachDebugger();
			eventSet.resume();
		} catch (Exception ignored) {
		}
	}

	private void doWorkAtBreakpoint(BreakpointEvent breakpointEvent) {
		if (breakpointEvent.thread().isSuspended()) {
			TargetApplicationRepresentation.getInstance().takeSnapshotOfTargetApplication(
					TargetVirtualMachineRepresentation.getInstance().getVirtualMachine(), breakpointEvent);
			updateUI(breakpointEvent);
		}
		Display display = Display.getDefault();
		if (Objects.nonNull(display) && !display.isDisposed()) {
			while (DebuggerContext.context().isDebugSessionActive()) {
				AbstractUIEvent uiEvent = null;
				;
				try {
					// if (DebuggerContext.context().isDebugSessionActive())
					uiEvent = uiEventCollector.takeUiEvent();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("EVENT IN DEGUG SESSION: " + uiEvent);
				if (Objects.isNull(uiEvent))
					continue;
				try {
					shouldRefreshSnapsotAndUi.set(true);
					handleSingleUiEvent(uiEvent, breakpointEvent);
					if (shouldRefreshSnapsotAndUi.get()) {
//						TargetApplicationRepresentation.getInstance().takeSnapshotOfTargetApplication(
//								TargetVirtualMachineRepresentation.getInstance().getVirtualMachine(), breakpointEvent);
						updateUI(breakpointEvent);
					}
				} catch (Throwable exception) {
					logError("Breakpoint handler error", exception);
				}

			}
		}
		// eventSet.resume();
	}

	private void handleSingleUiEvent(AbstractUIEvent abstractSimpleDebuggerUIEvent, BreakpointEvent breakpointEvent) {
		StackFrame currentFrame = getTopFrame(breakpointEvent.thread());
		if (currentFrame == null)
			return;
		try {
			UIEventHandler qq = abstractSimpleDebuggerUIEvent.getType().getUiEventHandler();
			System.out.println(abstractSimpleDebuggerUIEvent.getType() + " handler: " + qq);
			shouldRefreshSnapsotAndUi.set(abstractSimpleDebuggerUIEvent.getType().getUiEventHandler()
					.handle(abstractSimpleDebuggerUIEvent, currentFrame, breakpointEvent));
			System.out.println(shouldRefreshSnapsotAndUi);
		} catch (Exception exception) {
			SimpleDebuggerLogger.error(exception.getMessage(), exception);
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

		PairDTO<Map<Tag, UniversalElementRepresentation>, Map<Tag, AbstractElementRepresentation>> qq = TargetApplicationRepresentation
				.getInstance().getTargetApplicationSnapshot();
		Map<Tag, UniversalElementRepresentation> topLevelElements = qq.getFirst();
		Map<Tag, AbstractElementRepresentation> subordinates = qq.getSecond();
//		topLevelElements.values().stream().forEach(e -> System.out.println("TL: " + e));
//		subordinates.values().stream().forEach(e -> System.out.println("SO: " + e));
		Location location = breakpointEvent.location();
		String methodName = location.declaringType().name() + "." + location.method().name() + "()";
		Set<InnerElementRepresentationDTO> innerElementDTOs = topLevelElements.values().stream()
				.map(e -> InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(e))
				.collect(Collectors.toSet());
		
		
		subordinates.values().stream().filter(e -> e instanceof UniversalElementRepresentation)
				.map(e -> InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
						.fromElement((UniversalElementRepresentation) e))
				.forEach(innerElementDTOs::add);
		Map<InnerElementRepresentationDTO, Set<InnerElementRepresentationDTO>> topElementsWithSubordinates = new HashMap<>();

		for (UniversalElementRepresentation topLevelElement : topLevelElements.values()) {
		    InnerElementRepresentationDTO topDTO = InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
		            .fromElement(topLevelElement);
		    Set<InnerElementRepresentationDTO> subordinatesSet = new HashSet<InnerElementRepresentationDTO>();
		    for (AbstractElementRepresentation subordinate : subordinates.values()) {
		    	if (subordinate.getTag().getParentId().equals(topLevelElement.getTag().getUniqueId())) {
		    		subordinatesSet.add(InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
				            .fromElement(subordinate));
		    	}
		    	topElementsWithSubordinates.put(topDTO, subordinatesSet);
		    }

		    
		}

		System.out.println(topElementsWithSubordinates);
		System.out.println("<====");

		DebugWindowDataDTO debugWindowDataDTO = new DebugWindowDataDTO(location.lineNumber(), methodName,
				DebugUtils.compileStackInfo(breakpointEvent.thread()), topElementsWithSubordinates);

		simpleDebugEventCollector.collectDebugEvent(new DebugEvent<DebugWindowDataDTO>(
				SimpleDebuggerEventTypes.SimpleDebuggerEventType.STOPPED_AT_BREAKPOINT, debugWindowDataDTO));
		simpleDebugEventCollector
				.collectDebugEvent(new DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
		currentLineHighlighter.highlight(breakpointEvent.location());

		/*
		 * for (UniversalElementRepresentation toplevelElement :
		 * topLevelElements.values()) {
		 * 
		 * }
		 * 
		 * ObjectReference thisObj = null; try { thisObj =
		 * breakpointEvent.thread().frame(0).thisObject(); } catch
		 * (IncompatibleThreadStateException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 * 
		 * if (thisObj != null) { String runtimeClass = thisObj.referenceType().name();
		 * }
		 * 
		 * if (Objects.isNull(breakpointEvent)) return false; Location location =
		 * breakpointEvent.location(); AtomicReference<ObjectReference> thisObjectRef =
		 * new AtomicReference<>(); try {
		 * thisObjectRef.set(breakpointEvent.thread().frame(0).thisObject()); } catch
		 * (IncompatibleThreadStateException e) { e.printStackTrace(); return false; }
		 * ThreadReference thread = breakpointEvent.thread(); StackFrame frame = null;
		 * try { frame = thread.frame(0); } catch (IncompatibleThreadStateException e) {
		 * // TODO Auto-generated catch block e.printStackTrace(); } if
		 * (Objects.isNull(frame)) return false; ObjectReference inctance =
		 * frame.thisObject(); //
		 * TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().
		 * getFirst().values().stream().forEach(e -> System.out.println("MODEL: " + e));
		 * Optional<UniversalElementRepresentation> anchorInstanceOptional =
		 * TargetApplicationRepresentation.getInstance()
		 * .getTargetApplicationSnapshot().getFirst().values().stream() .filter(e ->
		 * Objects.equals(e.getObjectReference(), inctance)) // .filter(e ->
		 * Objects.isNull(e.getTag().getParentId())
		 * 
		 * .findAny();
		 * 
		 * if (anchorInstanceOptional.isEmpty()) return false;
		 * UniversalElementRepresentation anchorElement = null; if
		 * (anchorInstanceOptional.get() instanceof UniversalElementRepresentation) {
		 * anchorElement = (UniversalElementRepresentation)
		 * anchorInstanceOptional.get(); } else { // ElementReference elementReference =
		 * (ElementReference) anchorInstanceOptional.get(); // anchorElement =
		 * (UniversalElementRepresentation)
		 * TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().
		 * get(elementReference.getReferenceTag()); } System.out.println(anchorElement);
		 * Set<UniversalElementRepresentation> relevantElements =
		 * selectFieldsAndMethods(anchorElement); List<UniversalElementRepresentation>
		 * locals = TargetApplicationRepresentation.getInstance()
		 * .getTargetApplicationSnapshot().getSecond().values().stream() // берём только
		 * реальные UniversalElementRepresentation .filter(e -> e instanceof
		 * UniversalElementRepresentation) .map(e -> (UniversalElementRepresentation) e)
		 * // безопасное приведение // фильтруем только локальные переменные .filter(w
		 * -> w.getElementType() ==
		 * UniversalElementRepresentation.UniversalElementType.LOCAL_VARIABLE)
		 * .toList(); relevantElements.stream().forEach(e -> System.out.println("RL:" +
		 * e)); relevantElements.addAll(locals); Set<InnerElementRepresentationDTO>
		 * innerElementDTOs = new HashSet(); for (UniversalElementRepresentation element
		 * : relevantElements) { InnerElementRepresentationDTO elementRepresentation =
		 * InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
		 * .fromUniversalElement(element); innerElementDTOs.add(elementRepresentation);
		 * } // System.out.println("MODEL SIZE: " +
		 * TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().
		 * size()); //
		 * TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot().
		 * values().stream() // .forEach(e -> System.out.println("MD:" + e)); //
		 * System.out.println("INNER_ELS:" + innerElementDTOs); String methodName =
		 * location.declaringType().name() + "." + location.method().name() + "()";
		 * DebugWindowDataDTO debugWindowDataDTO = new
		 * DebugWindowDataDTO(location.lineNumber(), methodName,
		 * DebugUtils.compileStackInfo(thread), innerElementDTOs);
		 * simpleDebugEventCollector.collectDebugEvent(new
		 * DebugEvent<DebugWindowDataDTO>(
		 * SimpleDebuggerEventTypes.SimpleDebuggerEventType.STOPPED_AT_BREAKPOINT,
		 * debugWindowDataDTO)); simpleDebugEventCollector .collectDebugEvent(new
		 * DebugEvent<Boolean>(SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));
		 * currentLineHighlighter.highlight(breakpointEvent.location());
		 * System.out.println("relevantElements: " + relevantElements.size());
		 */
		return true;
	}
	
	// Метод для построения дерева дочерних элементов
	private void buildSubTreeMap(
	        InnerElementRepresentationDTO parent,
	        Collection<AbstractElementRepresentation> allElements,
	        Map<InnerElementRepresentationDTO, Set<InnerElementRepresentationDTO>> map) {

	    Set<InnerElementRepresentationDTO> children = new HashSet<>();

	    for (AbstractElementRepresentation element : allElements) {
	        if (parent.getTag().getUniqueId().equals(element.getTag().getParentId())) {
	            InnerElementRepresentationDTO child = InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
	                    .fromElement(element);

	            children.add(child);

	            // рекурсивно строим поддерево
	            buildSubTreeMap(child, allElements, map);
	        }
	    }

	    map.put(parent, children);
	}


	private Set<UniversalElementRepresentation> selectFieldsAndMethods(UniversalElementRepresentation initElement) {
		if (initElement == null) {
			return Collections.emptySet();
		}
		Set<UniversalElementRepresentation> foundElements = new HashSet<>();
		foundElements.add(initElement);
		// Элементы текущего уровня обхода
		Set<UniversalElementRepresentation> currentLevel = new HashSet<>();
		currentLevel.add(initElement);
		while (!currentLevel.isEmpty()) {
			Set<UniversalElementRepresentation> nextLevel = new HashSet<>();
			for (UniversalElementRepresentation parentElement : currentLevel) {
				// Ищем всех детей текущего элемента
				List<UniversalElementRepresentation> children = TargetApplicationRepresentation.getInstance()
						.getTargetApplicationSnapshot().getSecond().values().stream()
						.filter(e -> Objects.equals(e.getTag().getParentId(), parentElement.getTag().getUniqueId()))
						.map(e -> (UniversalElementRepresentation) e).toList();

				nextLevel.addAll(children);
			}
			// Добавляем найденных детей в общий Set
			foundElements.addAll(nextLevel);
			// Переходим на следующий уровень
			currentLevel = nextLevel;
		}
		return foundElements;
	}

	private void logError(String message, Throwable exception) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, "simple_debugger_plugin", message, exception),
				StatusManager.LOG);
	}
}
