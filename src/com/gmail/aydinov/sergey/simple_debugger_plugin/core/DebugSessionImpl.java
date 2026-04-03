package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationBreakpointRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DebugSession;
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
import com.sun.jdi.ReferenceType;
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
						TargetApplicationRepresentation.getInstance().takeSnapshotOfTargetApplication(
								TargetVirtualMachineRepresentation.getInstance().getVirtualMachine(), breakpointEvent);
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
			if (SimpleDebuggerEventTypes.isInspectionEvent(abstractSimpleDebuggerUIEvent.getType())
					&& DebuggerContext.context().isDebugSessionActive()) {
				DebuggerContext.context().setStatus(SimpleDebuggerStatus.INSPECTION_SEANCE_RUNNING);
				//InspectionSeance inspectionSeance = InspectionSeanceImpl.;
				InspectionSeance.startInspectionSeanceForAnchorElement(abstractSimpleDebuggerUIEvent, currentFrame, breakpointEvent);
			}
			UIEventHandler qq = abstractSimpleDebuggerUIEvent.getType().getUiEventHandler();
			System.out.println(abstractSimpleDebuggerUIEvent.getType() + " handler: " + qq);
			if (Objects.isNull(qq)) return;
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
	    PairDTO<Map<Tag, UniversalElementRepresentation>, Map<Tag, AbstractElementRepresentation>> snapShot =
	            TargetApplicationRepresentation.getInstance().getTargetApplicationSnapshot();

	    Map<Tag, InnerElementRepresentationDTO> dtoMap = snapShot.getFirst().entrySet().stream()
	            .collect(Collectors.toMap(
	                    Map.Entry::getKey,
	                    e -> InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(e.getValue())
	            ));

	    Collection<AbstractElementRepresentation> subordinates = snapShot.getSecond().values();

	    Map<InnerElementRepresentationDTO, List<InnerElementRepresentationDTO>> result = new LinkedHashMap();

	    // Для каждого топ-элемента создаём отдельный список потомков
	    Collection<InnerElementRepresentationDTO> topElementsCopy = new ArrayList<>(dtoMap.values());
	    
	    ThreadReference thread = breakpointEvent.thread();
	    StackFrame frame = null;
	    Optional<UniversalElementRepresentation> breakpointInstance = Optional.empty();
		try {
			frame = thread.frame(0);
			ObjectReference thisObject = frame.thisObject();
			List<UniversalElementRepresentation> allEntities = new ArrayList<UniversalElementRepresentation>();
			allEntities.addAll(snapShot.getFirst().values().stream().toList());
			allEntities.addAll(snapShot.getFirst().values().stream()
					.filter(e -> (e instanceof UniversalElementRepresentation))
					.toList());
			if (thisObject != null) {
			   System.out.println("Instance: " + thisObject);
			   breakpointInstance = allEntities.stream()
			    		.filter(e -> Objects.nonNull(e.getObjectReference()))
			    		.filter(e -> Objects.equals(e.getObjectReference().uniqueID(), thisObject.uniqueID())).findAny();
			} else {
			    System.out.println("Static context (no instance)");
			    ReferenceType refType = frame.location().declaringType();
			    System.out.println(refType.name());
			    breakpointInstance = allEntities.stream()
			    		.filter(e -> Objects.nonNull(e.getReferenceType()))
			    		.filter(e -> Objects.equals(e.getReferenceType(), refType)).findAny();
			}
		} catch (IncompatibleThreadStateException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		List<InnerElementRepresentationDTO> orderedTopElements = new LinkedList<InnerElementRepresentationDTO>();
		//System.out.println("========> " + breakpointInstance.get());
		if (breakpointInstance.isPresent()) {
			 InnerElementRepresentationDTO breakepointElementDTO = InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory
			.fromElement(breakpointInstance.get());
			orderedTopElements.add(breakepointElementDTO);
			topElementsCopy.remove(breakepointElementDTO);
			orderedTopElements.addAll(topElementsCopy);
		} else {
			orderedTopElements.addAll(topElementsCopy);
		}
		
	    for (InnerElementRepresentationDTO topDTO : orderedTopElements) {
	        List<InnerElementRepresentationDTO> children = collectAllChildrenDTO(topDTO, subordinates, dtoMap);
	        result.put(topDTO, children);
	    }

	 //   System.out.println(result);

	    currentLineHighlighter.highlight(breakpointEvent.location());

	    Location location = breakpointEvent.location();
	    String methodName = location.declaringType().name() + "." + location.method().name() + "()";
	    DebugWindowDataDTO debugWindowDataDTO = new DebugWindowDataDTO(
	            location.lineNumber(), methodName,
	            DebugUtils.compileStackInfo(breakpointEvent.thread()), result
	    );

	    simpleDebugEventCollector.collectDebugEvent(new DebugEvent<>(
	            SimpleDebuggerEventTypes.SimpleDebuggerEventType.STOPPED_AT_BREAKPOINT, debugWindowDataDTO));
	    simpleDebugEventCollector.collectDebugEvent(new DebugEvent<>(
	            SimpleDebuggerEventType.SET_RESUME_BUTTON_STATE, true));

	    return true;
	}

	private List<InnerElementRepresentationDTO> collectAllChildrenDTO(
	        InnerElementRepresentationDTO rootDTO,
	        Collection<AbstractElementRepresentation> allElements,
	        Map<Tag, InnerElementRepresentationDTO> dtoMap
	) {
	    List<InnerElementRepresentationDTO> result = new ArrayList<>();
	    Set<UUID> visited = new HashSet<>();
	    collectRecursiveDTO(rootDTO, allElements, dtoMap, visited, result);
	    return result;
	}

	private void collectRecursiveDTO(
	        InnerElementRepresentationDTO parentDTO,
	        Collection<AbstractElementRepresentation> allElements,
	        Map<Tag, InnerElementRepresentationDTO> dtoMap,
	        Set<UUID> visited,
	        List<InnerElementRepresentationDTO> result
	) {
	    UUID parentId = parentDTO.getTag().getUniqueId();

	    for (AbstractElementRepresentation element : allElements) {
	        if (Objects.equals(element.getTag().getParentId(), parentId)) {

	            UUID childId = element.getTag().getUniqueId();
	            if (visited.contains(childId)) continue; // защита от зацикливания
	            visited.add(childId);

	            // Получаем DTO из мапы или создаём новый
	            InnerElementRepresentationDTO childDTO = dtoMap.get(element.getTag());
	            if (childDTO == null) {
	                childDTO = InnerElementRepresentationDTO.InnerElementRepresentationDTOFactory.fromElement(element);
	                dtoMap.put(element.getTag(), childDTO);
	            }

	            result.add(childDTO);

	            // Рекурсивно собираем потомков
	            collectRecursiveDTO(childDTO, allElements, dtoMap, visited, result);
	        }
	    }
	}
	private void logError(String message, Throwable exception) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, "simple_debugger_plugin", message, exception),
				StatusManager.LOG);
	}
}
