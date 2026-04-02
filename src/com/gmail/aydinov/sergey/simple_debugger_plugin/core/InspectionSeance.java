package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.SimpleDebuggerStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.inspectable.AbstractInspectableElement;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.InnerElementRepresentationDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.DebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.SimpleDebuggerEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.collectors.UiEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;

public class InspectionSeance {

	private final InnerElementRepresentationDTO anchorElement;
	private final StackFrame currentFrame;
	private final BreakpointEvent breakpointEvent;
	private final UiEventCollector uiEventCollector = SimpleDebuggerEventCollector.instance();
	private final DebugEventCollector debugEventCollector = SimpleDebuggerEventCollector.instance();
	private final Queue<AbstractInspectableElement> inspectableQueue = new LinkedList<>();

	private InspectionSeance(InnerElementRepresentationDTO innerElementRepresentationDTO, StackFrame currentFrame,
			BreakpointEvent breakpointEvent) {
		this.anchorElement = innerElementRepresentationDTO;
		this.currentFrame = currentFrame;
		this.breakpointEvent = breakpointEvent;
	}

	@SuppressWarnings("unchecked")
	public static boolean startInspectionSeanceForAnchorElement(AbstractUIEvent abstractSimpleDebuggerUIEvent,
			StackFrame currentFrame, BreakpointEvent breakpointEvent) {
		UIEvent<InnerElementRepresentationDTO> uiEvent = null;
		try {
			uiEvent = (UIEvent<InnerElementRepresentationDTO>) abstractSimpleDebuggerUIEvent;
		} catch (ClassCastException castException) {

		}
		if (Objects.isNull(uiEvent))
			return false;
		InspectionSeance inspectionSeance = new InspectionSeance(uiEvent.getPayload(), currentFrame, breakpointEvent);
		AbstractInspectableElement qq = AbstractInspectableElement.factory()
				.createInspectableElement(inspectionSeance.anchorElement, inspectionSeance.breakpointEvent);
		if (Objects.isNull(qq)) return false;
		inspectionSeance.inspectableQueue.offer(qq);
		InspectionProcedure inspectionProcedure = inspectionSeance.new InspectionProcedure();
		Thread inspectionThread = new Thread(inspectionProcedure);
		try {
			inspectionThread.start();
			inspectionThread.join();
		} catch (InterruptedException e) {
			SimpleDebuggerLogger.error(e.getMessage(), e);
		} finally {
			DebuggerContext.context().setStatus(SimpleDebuggerStatus.DEBUG_SESSION_RUNNING);
		}

		return true;

	}

	private class InspectionProcedure implements Runnable {

		@Override
		public void run() {
			inspectionProcedure();
		}

		private boolean inspectionProcedure() {
			for (int i = 0; i < 5; i++) {
				System.out.println("INSPECTION: " + Thread.currentThread());
				try {
					Thread.currentThread().sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return true;
		}
	}
}
