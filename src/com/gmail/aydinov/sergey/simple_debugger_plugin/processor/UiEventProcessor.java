package com.gmail.aydinov.sergey.simple_debugger_plugin.processor;

import java.text.spi.BreakIteratorProvider;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.BreakpointEventProvider;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.Resumable;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.Terminable;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.Updatable;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.WorkFlow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.events.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.events.UIEventUpdateVariable;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.event.UserChangedVariable;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.event.UserClosedWindowUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.event.UserPressedResumeUiEvent;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;

public class UiEventProcessor implements Runnable {

	private volatile boolean running = true;
	private final Resumable resumable;
	private final Terminable terminable;
	private final Updatable updatable;

//	private final SimpleDebuggerEventQueue eventQueue;
//	
//	private final Resumable targetApplicationResumer;
//	private final TargetVirtualMachineRepresentation targetVirtualMachineRepresentation;
//	private final Terminable debuggerTerminator;
//
//	public UiEventProcessor(SimpleDebuggerEventQueue queue, Resumable targetApplicationResumer,
//			TargetVirtualMachineRepresentation targetVirtualMachineRepresentation,
//			Terminable debuggerTerminator) {
//		this.eventQueue = queue;
//		this.targetApplicationResumer = targetApplicationResumer;
//		this.targetVirtualMachineRepresentation = targetVirtualMachineRepresentation;
//		this.debuggerTerminator = debuggerTerminator;
//	}

	public UiEventProcessor(WorkFlow workFlow) {
		this.resumable = workFlow;
		this.terminable = workFlow;
		this.updatable = workFlow;
	}

	@Override
	public void run() {
		System.out.println("THREAD STARTED");
		while (running) {
			// if ()
			try {
				UIEvent event = SimpleDebuggerEventQueue.instance().takeUiEvent();
				System.out.println(event);
				handleEvent(event);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void handleEvent(UIEvent uIevent) {
		if (uIevent instanceof UserPressedResumeUiEvent) {
			System.out.println("Button pressed");
			resumable.resumeTargetApplication();
			return;
		}

		if (uIevent instanceof UserClosedWindowUiEvent) {
			terminable.terminate();
			running = false;
			return;
		}

		if (uIevent instanceof UserChangedVariable) {
//			UIEventUpdateVariable uiEventUpdateVariable = (UIEventUpdateVariable) uIevent;
//			updatable.updateVariables(uiEventUpdateVariable);
			System.out.println("PROCESS: " + uIevent);
		}
	}

	private Value createJdiValueFromString(VirtualMachine vm, LocalVariable var, String str) {
		String type = var.typeName();
		switch (type) {
		case "int":
			return vm.mirrorOf(Integer.parseInt(str));
		case "long":
			return vm.mirrorOf(Long.parseLong(str));
		case "short":
			return vm.mirrorOf(Short.parseShort(str));
		case "byte":
			return vm.mirrorOf(Byte.parseByte(str));
		case "char":
			return vm.mirrorOf(str.charAt(0));
		case "boolean":
			return vm.mirrorOf(Boolean.parseBoolean(str));
		case "float":
			return vm.mirrorOf(Float.parseFloat(str));
		case "double":
			return vm.mirrorOf(Double.parseDouble(str));
		case "java.lang.String":
			return vm.mirrorOf(str);
		default:
			throw new IllegalArgumentException("Unsupported type: " + type);
		}
	}

	public void stop() {
		running = false;
	}
}
