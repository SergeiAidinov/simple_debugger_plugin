package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowManager;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
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
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.EventRequestManager;

public class SimpleDebuggerWorkFlow
		implements UiEventListener, DebugEventProvider, TargetApplicationResumer, DebuggerTerminator {

	private final TargetVirtualMachineRepresentation targetVirtualMachineRepresentation;
	private final TargetApplicationRepresentation targetApplicationRepresentation;
	// private final IBreakpointManager manager;
	private final DebugPlugin debugPlugin; // новое поле
	// private final BreakepintViewController breakepintViewController =
	// BreakepintViewController.instance();
	private CountDownLatch countDownLatch = null;
	private DebugEventListener debugEventListener;
	private boolean running = true;
	private final UiEventQueue uiEventQueue = UiEventQueue.instance();
	public TargetApplicationStatus targetApplicationStatus = TargetApplicationStatus.RUNNING;
	private final AutoBreakpointHighlighter autoBreakpointHighlighter = new AutoBreakpointHighlighter();

	public SimpleDebuggerWorkFlow(TargetVirtualMachineRepresentation targetVirtualMachineRepresentation,
			IBreakpointManager iBreakpointManager, DebugPlugin debugPlugin,
			BreakpointSubscriberRegistrar breakpointListener) {
		this.targetVirtualMachineRepresentation = targetVirtualMachineRepresentation;
		EventRequestManager eventRequestManager = targetVirtualMachineRepresentation.getVirtualMachine()
				.eventRequestManager();
		this.targetApplicationRepresentation = new TargetApplicationRepresentation(iBreakpointManager,
				eventRequestManager, targetVirtualMachineRepresentation.getVirtualMachine(), breakpointListener);
		// this.manager = manager;
		this.debugPlugin = debugPlugin;
		DebugWindowManager.instance().setDebugEventProvider(this);
		// debugEventListener = DebugWindowManager.instance().getOrCreateWindow();
		UiEventProcessor uiEventProcessor = new UiEventProcessor(uiEventQueue, this, targetVirtualMachineRepresentation,
				this);
		Thread uiEventProcessorThread = new Thread(uiEventProcessor);
		uiEventProcessorThread.setDaemon(true);
		uiEventProcessorThread.start();

	}

	@Override
	public void terminate() {
		System.out.println("TERMINATE");
		running = false;
		targetVirtualMachineRepresentation.getVirtualMachine().resume();
		countDownLatch.countDown();

	}

	@Override
	public void resumeTargetApplication() {
		countDownLatch.countDown();

	}

	public void setDebugEventListener(DebugEventListener debugEventListener) {
		this.debugEventListener = debugEventListener;
	}

	public List<ReferenceType> getClassesOfTargetApplication() {
		return targetVirtualMachineRepresentation.getVirtualMachine().allClasses();
	}

	public void debug() throws IOException, AbsentInformationException {
		System.out.println("DEBUG");

		Display.getDefault().asyncExec(() -> {
			DebugWindow window = DebugWindowManager.instance().getOrCreateWindow();
			setDebugEventListener(window);
			window.setDebugEventProvider(this);
			window.setUiEventListener(this);
			if (window == null || !window.isOpen()) {
				// window = DebugWindowManager.instance().getOrCreateWindow(); // создаём окно
				window.open(); // обязательно открываем shell
			}
		});

		// Обновляем данные о target приложении
		targetApplicationRepresentation
				.refreshReferencesToClassesOfTargetApplication(targetVirtualMachineRepresentation.getVirtualMachine());
		targetApplicationRepresentation.getTargetApplicationBreakepointRepresentation().refreshBreakePoints();

		// Получаем JDI EventRequestManager
		EventRequestManager eventRequestManager = targetVirtualMachineRepresentation.getVirtualMachine()
				.eventRequestManager();

		System.out.println("Waiting for events...");
		EventQueue queue;

		while (running) {
			System.out.println("Start iteration...");
			queue = targetVirtualMachineRepresentation.getVirtualMachine().eventQueue();

			try {
				EventSet eventSet = queue.remove(); // блокирует до события
				System.out.println("eventSet.size() " + eventSet.size());
				for (Event event : eventSet) {
					if (event instanceof BreakpointEvent bpEvent) {
						ThreadReference thread = bpEvent.thread();
						org.eclipse.debug.core.DebugEvent debugEvent = convertJdiToDebugEvent(thread, event);
						org.eclipse.debug.core.DebugEvent[] events = { debugEvent };
						ITextEditor activeEditor = getActiveTextEditor();
						autoBreakpointHighlighter.highlightLine(activeEditor, 16);
						targetApplicationStatus = TargetApplicationStatus.STOPPED_AT_BREAKPOINT;
						stoppedAtBreakpoint(bpEvent);
						eventSet.resume();
					} else if (event instanceof VMDisconnectEvent || event instanceof VMDeathEvent) {
						System.out.println("Target VM stopped");
						return;
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("End iteration. DebugEventListener: " + debugEventListener + "\n");
		}
	}
	
	public ITextEditor getActiveTextEditor() {
		 final ITextEditor[] result = new ITextEditor[1];

		    PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
		        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		        if (window == null) return;

		        IWorkbenchPage page = window.getActivePage();
		        if (page == null) return;

		        IEditorPart part = page.getActiveEditor();
		        if (part instanceof ITextEditor) {
		            result[0] = (ITextEditor) part;
		        }
		    });

		    return result[0];
    }

	// Отдельный метод для обработки события
	private void stoppedAtBreakpoint(BreakpointEvent bpEvent) {

		ThreadReference thread = bpEvent.thread();
		Location location = bpEvent.location();
		// DebugWindowManager.instance().updateLocation(loc, thread);

		try {

			StackFrame frame = thread.frame(0);
			System.out.println("Breakpoint hit at " + location.declaringType().name() + "."
					+ frame.location().method().name() + " line " + location.lineNumber());

			// Локальные переменные
			// Локальные переменные
			Map<LocalVariable, Value> localVariables = new HashMap<LocalVariable, Value>();
			for (LocalVariable localVariable : frame.visibleVariables()) {
				Value value = frame.getValue(localVariable);
				localVariables.put(localVariable, value);
				System.out.println(localVariable.name() + " = " + value);
			}
			ObjectReference thisObject = frame.thisObject();
			Map<Field, Value> fields = Collections.EMPTY_MAP;
			if (thisObject != null) {
				fields = thisObject.getValues(thisObject.referenceType().fields());

			}
			String className = location.declaringType().name();
			String methodName = location.method().name();
			int lineNumber = location.lineNumber();
			List<StackFrame> frames = thread.frames();
			String stackDescription = compileStackInfo(thread.frames());
			DebugEvent debugEvent = new DebugEvent(className, methodName, lineNumber, fields, localVariables, frames,
					stackDescription);
			countDownLatch = new CountDownLatch(1);
			debugEventListener.handleDebugEvent(debugEvent);
			countDownLatch.await();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String compileStackInfo(List<StackFrame> frames) {
		String classAndMethod = "Unknown";
		String sourceAndLine = "Unknown";
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

	private Optional<Location> findLocation(Method method, int sourceLine) {
		try {
			for (Location location : method.allLineLocations()) {
				if (location.lineNumber() == sourceLine) {
					return Optional.of(location);
				}
			}
		} catch (AbsentInformationException e) {
			// в этом случае исходники не доступны: метод скомпилирован без -g
			return Optional.empty();
		}
		return Optional.empty();
	}

	@Override
	public void handleUiEvent(UIEvent uIevent) {
		uiEventQueue.addEvent(uIevent);
//		if (uIevent instanceof UIEventResumeButtonPressed) {
//			System.out.println("Button pressed");
//			countDownLatch.countDown();
//			return;
//		}
//
//		if (uIevent instanceof UIEventWindowClosed) {
//			System.out.println("Window closed — stopping debug loop");
//			countDownLatch.countDown();
//			try {
//				targetVirtualMachineRepresentation.getVirtualMachine().resume();
//				targetVirtualMachineRepresentation.getVirtualMachine().dispose();
//			} catch (Exception ignored) {
//			}
//			running = false;
//			return;
//		}
//
//		if (uIevent instanceof UIEventUpdateVariable) {
//			UIEventUpdateVariable uiEventUpdateVariable = (UIEventUpdateVariable) uIevent;
//			Value jdiValue = createJdiValueFromString(targetVirtualMachineRepresentation.getVirtualMachine(),
//					uiEventUpdateVariable.getLocalVariable(), uiEventUpdateVariable.getNewValue());
//			try {
//				uiEventUpdateVariable.getStackFrame().setValue(uiEventUpdateVariable.getLocalVariable(), jdiValue);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
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

	private org.eclipse.debug.core.DebugEvent convertJdiToDebugEvent(ThreadReference thread, Event event) {
		int kind = org.eclipse.debug.core.DebugEvent.SUSPEND; // для примера — остановка на брейкпоинте
		int detail = org.eclipse.debug.core.DebugEvent.BREAKPOINT; // деталь события
		DummyDebugElement source = new DummyDebugElement();
		if (event instanceof BreakpointEvent) {
			return new org.eclipse.debug.core.DebugEvent(source, org.eclipse.debug.core.DebugEvent.SUSPEND,
					org.eclipse.debug.core.DebugEvent.BREAKPOINT);
		} else if (event instanceof StepEvent) {
			return new org.eclipse.debug.core.DebugEvent(source, org.eclipse.debug.core.DebugEvent.SUSPEND,
					org.eclipse.debug.core.DebugEvent.STEP_END);
		} else if (event instanceof ExceptionEvent) {
			// return new org.eclipse.debug.core.DebugEvent(debugTarget,
			// org.eclipse.debug.core.DebugEvent.SUSPEND,
			// org.eclipse.debug.core.DebugEvent.EXCEPTION);
		} else if (event instanceof VMStartEvent) {
			return new org.eclipse.debug.core.DebugEvent(source, org.eclipse.debug.core.DebugEvent.CREATE);
		} else if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
			return new org.eclipse.debug.core.DebugEvent(source, org.eclipse.debug.core.DebugEvent.TERMINATE);
		}
		// другие события можно добавить по мере необходимости
		return null; // если событие не обрабатываем
	}

	@Override
	public void sendDebugEvent(DebugEvent debugEvent) {
		// TODO Auto-generated method stub

	}

	public static class Factory {

		// private static SimpleDebuggerWorkFlow DEBUGGER_INSTANCE;

//		public static SimpleDebuggerWorkFlow getInstanceOfSimpleDebuggerWorkFlow() {
//			return DEBUGGER_INSTANCE;
//		}

		public static void create(String host, int port, OnWorkflowReadyListener listener) {

			// 1️⃣ Подключение к JVM асинхронно
			CompletableFuture<VirtualMachine> vmFuture = CompletableFuture
					.supplyAsync(() -> configureVirtualMachine(host, port));

			// 2️⃣ Асинхронное ожидание DebugPlugin и BreakpointManager
			CompletableFuture<IBreakpointManager> bpmFuture = getDebugPluginAndBreakpointManager();

			// 3️⃣ Когда оба готовы — создаём workflow с listener
			vmFuture.thenCombine(bpmFuture, (vm, bpManager) -> {

				DebugPlugin plugin = DebugPlugin.getDefault();

				// 🔹 создаём и регистрируем listener
				BreakePointListener breakpointListener = new BreakePointListener();
				bpManager.setEnabled(true);
				bpManager.addBreakpointListener(breakpointListener);
				System.out.println("[Factory] Breakpoint listener registered!");

				// 🔹 создаём workflow с listener

//				DEBUGGER_INSTANCE = new SimpleDebuggerWorkFlow(new TargetVirtualMachineRepresentation(host, port, vm),
//						bpManager, plugin, breakpointListener);
//				return DEBUGGER_INSTANCE;
				return new SimpleDebuggerWorkFlow(new TargetVirtualMachineRepresentation(host, port, vm), bpManager,
						plugin, breakpointListener);

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
