package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.swt.widgets.Display;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.BreakpointSubscriberRegistrar;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.DebugEventListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.OnWorkflowReadyListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.events.SimpleDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.events.SimpleDebugEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.events.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.VarEntry;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.event.UserChangedVariable;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.event.UserPressedResumeUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.request.EventRequestManager;

public class SimpleDebuggerWorkFlow {

	private final TargetVirtualMachineRepresentation targetVirtualMachineRepresentation;
	private final TargetApplicationRepresentation targetApplicationRepresentation;
	private DebugEventListener debugEventListener;
	private boolean running = true;
	private final SimpleDebugEventCollector simpleDebugEventCollector = SimpleDebuggerEventQueue.instance();
	public TargetApplicationStatus targetApplicationStatus = TargetApplicationStatus.RUNNING;

	public SimpleDebuggerWorkFlow(TargetVirtualMachineRepresentation targetVirtualMachineRepresentation,
			IBreakpointManager iBreakpointManager, BreakpointSubscriberRegistrar breakpointListener) {
		this.targetVirtualMachineRepresentation = targetVirtualMachineRepresentation;
		EventRequestManager eventRequestManager = targetVirtualMachineRepresentation.getVirtualMachine()
				.eventRequestManager();
		this.targetApplicationRepresentation = new TargetApplicationRepresentation(iBreakpointManager,
				eventRequestManager, targetVirtualMachineRepresentation.getVirtualMachine(), breakpointListener);

	}

	public void updateVariables(UserChangedVariable userChangedVariable, StackFrame farme) {
		VarEntry varEntry = userChangedVariable.getVarEntry();
		LocalVariable localVariable = varEntry.getLocalVar();
		String value = (String) varEntry.getNewValue();
		Value jdiValue = DebugUtils.createJdiValueFromString(targetVirtualMachineRepresentation.getVirtualMachine(),
				localVariable, value);
		try {
			farme.setValue(localVariable, jdiValue);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setDebugEventListener(DebugEventListener debugEventListener) {
		this.debugEventListener = debugEventListener;
	}

	public List<ReferenceType> getClassesOfTargetApplication() {
		return targetVirtualMachineRepresentation.getVirtualMachine().allClasses();
	}

	public void debug(){
		System.out.println("DEBUG");
		// Открываем окно отладчика в UI thread
		Display.getDefault().asyncExec(() -> {
			DebugWindow window = DebugWindowManager.instance().getOrCreateWindow();
			if (window == null || !window.isOpen()) {
				window.open();
			}
		});

		// Обновляем breakpoints
		targetApplicationRepresentation.getTargetApplicationBreakepointRepresentation().refreshBreakePoints();
		System.out.println("Waiting for events...");

		EventQueue queue;

		while (running) {
			System.out.println("Start iteration...");
			queue = targetVirtualMachineRepresentation.getVirtualMachine().eventQueue();
			EventSet eventSet = null;
			try {
				eventSet = queue.remove(); // блокирующий вызов
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (eventSet == null)
				continue;
			System.out.println("eventSet.size() " + eventSet.size());
			outer: for (Event event : eventSet) {
				if (event instanceof BreakpointEvent breakpointEvent) {
					refreshUserInterface(breakpointEvent);
					StackFrame frame = null;
					try {
						frame = breakpointEvent.thread().frame(0);
					} catch (IncompatibleThreadStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					while (running) {
						UIEvent uiEvent = null;
						try {
							uiEvent = SimpleDebuggerEventQueue.instance().takeUiEvent();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						System.out.println("handling: " + uiEvent);

						if (uiEvent instanceof UserPressedResumeUiEvent) {
							break outer;
						}
						if (uiEvent instanceof UserChangedVariable) {
							 
							applyPendingChanges((UserChangedVariable) uiEvent, frame);
							refreshUserInterface(breakpointEvent);
							continue;
						}
						StackFrame farme = null;
						try {
							farme = breakpointEvent.thread().frame(0);
						} catch (IncompatibleThreadStateException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						handleEvent(uiEvent, farme);
					}
				} else if (event instanceof VMDisconnectEvent || event instanceof VMDeathEvent) {
					System.out.println("Target VM stopped");
					return;
				}
			}
			// Продолжаем выполнение target VM
			eventSet.resume();

			// И обновляем интерфейс после выхода из breakpoint
			//refreshUserInterface();
			System.out.println("End iteration. DebugEventListener: " + debugEventListener + "\n");
		}
	}

	private void applyPendingChanges(UserChangedVariable event, StackFrame frame) {
		VarEntry varEntry = event.getVarEntry();
		LocalVariable localVar = varEntry.getLocalVar();
		String strValue = (String) varEntry.getNewValue();
		Value jdiValue = DebugUtils.createJdiValueFromString(targetVirtualMachineRepresentation.getVirtualMachine(),
				localVar, strValue);
		try {
			frame.setValue(localVar, jdiValue);
			System.out.println("Variable updated: " + localVar.name() + " = " + jdiValue);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void handleEvent(UIEvent uIevent, StackFrame farme) {
		if (uIevent instanceof UserChangedVariable) {
			UserChangedVariable userChangedVariable = (UserChangedVariable) uIevent;
			updateVariables(userChangedVariable, farme);
			System.out.println("PROCESS: " + uIevent);
			return;
		}
	}

	private boolean refreshUserInterface(BreakpointEvent breakpointEvent) {
		if (breakpointEvent == null)
			return false;
		ThreadReference threadReference = breakpointEvent.thread();
		Location location = breakpointEvent.location();
		StackFrame frame;
		try {
			frame = threadReference.frame(0);
		} catch (IncompatibleThreadStateException | IndexOutOfBoundsException e) {
			// Поток не в состоянии остановки или нет фрейма
			System.err.println("Cannot read stack frame: " + e.getMessage());
			return false;
		} catch (com.sun.jdi.InvalidStackFrameException e) {
			System.err.println("Invalid stack frame: " + e.getMessage());
			return false;
		}
		if (frame == null)
			return false;
		System.out.println("Breakpoint hit at " + location.declaringType().name() + "."
				+ frame.location().method().name() + " line " + location.lineNumber());

		// ===== Локальные переменные =====
		Map<LocalVariable, Value> localVariables = new HashMap<>();
		try {
			// frame.visibleVariables() может бросать InvalidStackFrameException
			for (LocalVariable localVariable : frame.visibleVariables()) {
				try {
					Value value = frame.getValue(localVariable);
					localVariables.put(localVariable, value);
					System.out.println(localVariable.name() + " = " + value);
				} catch (com.sun.jdi.InvalidStackFrameException ex) {
					System.err.println("Frame invalid while reading value: " + ex.getMessage());
					break; // дальше читать смысла нет
				}
			}
		} catch (AbsentInformationException e) {
			System.err.println("No debug info: " + e.getMessage());
		} catch (com.sun.jdi.InvalidStackFrameException e) {
			System.err.println("Cannot read variables: " + e.getMessage());
			return false;
		}

		// ===== Поля объекта this =====
		Map<Field, Value> fields = Collections.emptyMap();
		ObjectReference thisObject;
		try {
			thisObject = frame.thisObject();
		} catch (com.sun.jdi.InvalidStackFrameException e) {
			System.err.println("Cannot read thisObject: " + e.getMessage());
			thisObject = null;
		}

		if (thisObject != null) {
			try {
				fields = thisObject.getValues(thisObject.referenceType().fields());
			} catch (com.sun.jdi.InvalidStackFrameException e) {
				System.err.println("Cannot read fields: " + e.getMessage());
			}
		}

		// ===== Прочая информация =====
		String className = location.declaringType().name();
		String methodName = location.method().name();
		int lineNumber = location.lineNumber();
		String stackDescription = compileStackInfo(threadReference);
		SimpleDebugEvent debugEvent = new SimpleDebugEvent(SimpleDebugEventType.REFRESH_DATA, className, methodName,
				lineNumber, fields, localVariables, stackDescription);
		simpleDebugEventCollector.collectDebugEvent(debugEvent);
		return true;
	}

	private String compileStackInfo(ThreadReference threadReference) {
		String classAndMethod = "Unknown";
		String sourceAndLine = "Unknown";
		List<StackFrame> frames = Collections.EMPTY_LIST;
		try {
			frames = threadReference.frames();
		} catch (IncompatibleThreadStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (StackFrame frame : frames) {
			if (frame == null)
				continue;
			try {
				Location loc = frame.location();
				if (loc != null) {
					String className = loc.declaringType() != null ? loc.declaringType().name() : "Unknown";
					String method = loc.method() != null ? loc.method().name() : "unknown";
					int line = loc.lineNumber();
					classAndMethod = className + "." + method + "()";
					try {
						String src = loc.sourceName();
						sourceAndLine = src + ":" + line;
					} catch (AbsentInformationException aie) {
						sourceAndLine = "Unknown:" + line;
					}
				}
			} catch (Exception e) {
				// защищаемся от возможных исключений JDI
				e.printStackTrace();
			}

		}
		return classAndMethod + " " + sourceAndLine;
	}

	public static class Factory {

		public static void create(String host, int port, OnWorkflowReadyListener listener) {

			// 1️⃣ Подключение к JVM асинхронно
			CompletableFuture<VirtualMachine> vmFuture = CompletableFuture
					.supplyAsync(() -> configureVirtualMachine(host, port));

			// 2️⃣ Асинхронное ожидание DebugPlugin и BreakpointManager
			CompletableFuture<IBreakpointManager> bpmFuture = getDebugPluginAndBreakpointManager();

			// 3️⃣ Когда оба готовы — создаём workflow с listener
			vmFuture.thenCombine(bpmFuture, (vm, bpManager) -> {

				// DebugPlugin plugin = DebugPlugin.getDefault();

				// 🔹 создаём и регистрируем listener
				BreakePointListener breakpointListener = new BreakePointListener();
				bpManager.setEnabled(true);
				bpManager.addBreakpointListener(breakpointListener);
				System.out.println("[Factory] Breakpoint listener registered!");
				return new SimpleDebuggerWorkFlow(new TargetVirtualMachineRepresentation(host, port, vm),
						bpManager /* , */
				/* plugin, */ , breakpointListener);

			}).thenAccept(workflow -> {
				if (Objects.nonNull(listener))
					listener.onReady(workflow);
			});
		}

		// -------------------
		private static CompletableFuture<IBreakpointManager> getDebugPluginAndBreakpointManager() {
			CompletableFuture<IBreakpointManager> future = new CompletableFuture<>();

			Runnable check = new Runnable() {
				@Override
				public void run() {
					DebugPlugin plugin = DebugPlugin.getDefault();
					if (Objects.nonNull(plugin) && Objects.nonNull(plugin.getBreakpointManager())) {
						future.complete(plugin.getBreakpointManager());
					} else {
						Display.getDefault().timerExec(500, this);
					}
				}
			};

			Display.getDefault().asyncExec(check);
			return future;
		}

		// -------------------
		private static VirtualMachine configureVirtualMachine(String host, int port) {
			VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
			AttachingConnector connector = vmm.attachingConnectors().stream()
					.filter(c -> c.name().equals("com.sun.jdi.SocketAttach")).findAny().orElseThrow();

			Map<String, Connector.Argument> args = connector.defaultArguments();
			args.get("hostname").setValue(host);
			args.get("port").setValue(String.valueOf(port));

			while (true) {
				try {
					System.out.println("Connecting to " + host + ":" + port + "...");
					VirtualMachine vm = connector.attach(args);
					System.out.println("Successfully connected to VM.");
					return vm;
				} catch (Exception ignored) {
					try {
						TimeUnit.SECONDS.sleep(1);
					} catch (InterruptedException ignored2) {
					}
				}
			}
		}
	}

}
