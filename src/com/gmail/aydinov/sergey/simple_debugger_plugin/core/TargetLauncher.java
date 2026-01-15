package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.List;

import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UserPressedStartUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;
import com.sun.jdi.VirtualMachine;

public class TargetLauncher implements Runnable{

    private final String mainClass;
    private final List<String> options;

    public TargetLauncher(String mainClass, List<String> options) {
        this.mainClass = mainClass;
        this.options = options;
    }

    public void launch() {
        // Используем suspend=y, чтобы VM стартовала, но сразу не выполняла main
        VirtualMachine vm = VMLauncher.launch(mainClass, options); 
        // Можно сразу создать TargetVirtualMachineRepresentation / SimpleDebuggerWorkFlow
        // или послать событие в очередь, что VM готова
       // SimpleDebuggerEventQueue.instance().collectUiEvent(new VmStartedUiEvent(vm));
    }

	@Override
	public void run() {
		System.out.println(Thread.currentThread() + " started!");
		UIEvent startEvent = null;
		try {
			 startEvent = SimpleDebuggerEventQueue.instance().takeUiEvent();
			 System.out.println(startEvent);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (startEvent instanceof UserPressedStartUiEvent) {
			System.out.println("User pressed START");
		}
		
	}
}

