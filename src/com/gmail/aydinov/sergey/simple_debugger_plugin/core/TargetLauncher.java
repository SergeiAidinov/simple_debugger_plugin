package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.List;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebuggerContext.TargetApplicationStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UserPressedStartUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;
import com.sun.jdi.VirtualMachine;

public class TargetLauncher implements Runnable{

//    private final String mainClass;
//    private final List<String> options;
//    private final TargetVirtualMachineRepresentation targetVirtualMachineRepresentation;

//    public TargetVirtualMachineRepresentation getTargetVirtualMachineRepresentation() {
//		return targetVirtualMachineRepresentation;
//	}
//
//
//	public TargetLauncher(String mainClass, List<String> options, TargetVirtualMachineRepresentation targetVirtualMachineRepresentation) {
//        this.mainClass = mainClass;
//        this.options = options;
//        this.targetVirtualMachineRepresentation = targetVirtualMachineRepresentation;
//    }
//
//    public void launch() {
//        targetVirtualMachineRepresentation.getVirtualMachine().resume();
//        System.out.println("VM started");
//        DebuggerContext.context().setTargetApplicationStatus(TargetApplicationStatus.RUNNING);
//    }

	@Override
	public void run() {
//		System.out.println(Thread.currentThread() + " started!");
//		UIEvent startEvent = null;
//		try {
//			 startEvent = SimpleDebuggerEventQueue.instance().takeUiEvent();
//			 System.out.println(startEvent);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		if (startEvent instanceof UserPressedStartUiEvent) {
//			System.out.println("User pressed START");
//			launch();
//		}
//		
	}
}

