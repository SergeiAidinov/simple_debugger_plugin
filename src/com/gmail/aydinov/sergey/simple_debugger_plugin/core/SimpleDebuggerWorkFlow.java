package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationStatus;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.BreakpointSubscriberRegistrar;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.OnWorkflowReadyListener;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebugEventType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UserChangedField;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UserChangedVariable;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.UserPressedResumeUiEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebugEventCollector;
import com.gmail.aydinov.sergey.simple_debugger_plugin.processor.SimpleDebuggerEventQueue;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindow;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.DebugWindowManager;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.VarEntry;
import com.gmail.aydinov.sergey.simple_debugger_plugin.ui.tab.FieldEntry;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
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

	public List<ReferenceType> getClassesOfTargetApplication() {
		return targetVirtualMachineRepresentation.getVirtualMachine().allClasses();
	}

	public void debug() {
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
					Location location = breakpointEvent.location();
					Display.getDefault().asyncExec(() -> {
					    try {
					        ITextEditor editor1 = openEditorForLocation(location);
					        if (editor1 != null) {
					            int line = location.lineNumber() - 1;
					            new CurrentLineHighlighter().highlight(editor1, line);
					        }
					    } catch (Exception e) {
					        e.printStackTrace();
					    }
					});
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
						StackFrame farme = null;
						try {
							farme = breakpointEvent.thread().frame(0);
						} catch (IncompatibleThreadStateException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						handleEvent(uiEvent, farme);
						refreshUserInterface(breakpointEvent);
					}
				} else if (event instanceof VMDisconnectEvent || event instanceof VMDeathEvent) {
					System.out.println("Target VM stopped");
					return;
				}
			}
			// Продолжаем выполнение target VM
			eventSet.resume();

			// И обновляем интерфейс после выхода из breakpoint
			System.out.println("End iteration. \n");
		}
	}

	public ITextEditor openEditorForLocation(Location location) {
	    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	    if (window == null)
	        throw new IllegalStateException("Workbench window not ready");

	    IWorkbenchPage page = window.getActivePage();
	    if (page == null)
	        throw new IllegalStateException("No active workbench page");

	    IFile file = null;
		try {
			file = findIFileForLocation(location);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    if (file == null || !file.exists())
	        throw new IllegalArgumentException("IFile not found for location: " + location);

	    IEditorPart part = null;
		try {
			part = IDE.openEditor(page, file, true);
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    if (!(part instanceof ITextEditor))
	        throw new IllegalStateException("The editor is not a text editor: " + part);

	    return (ITextEditor) part;
	}

	
	public IFile findIFileForLocation(Location location) {
	    try {
	        ReferenceType refType = location.declaringType();
	        if (refType == null) return null;

	        // Имя типа в формате JVM => преобразуем в Java вид
	        // было: Lcom/example/MyClass;  =>  com.example.MyClass
	        String jvmName = refType.name();
	        String className = jvmName.replace('/', '.');

	        // Удаляем ведущую 'L' и ';', если они есть
	        if (className.startsWith("L") && className.endsWith(";")) {
	            className = className.substring(1, className.length() - 1);
	        }

	        // Теперь это нормальное имя класса, ищем по нему IType
	        IWorkspace ws = ResourcesPlugin.getWorkspace();
	        IWorkspaceRoot root = ws.getRoot();

	        // Перебираем все Java-проекты
	        for (IProject project : root.getProjects()) {
	            if (!project.isOpen() || !project.hasNature(JavaCore.NATURE_ID))
	                continue;

	            IJavaProject javaProject = JavaCore.create(project);
	            IType type = javaProject.findType(className);

	            if (type != null) {
	                ICompilationUnit unit = type.getCompilationUnit();
	                if (unit != null) {
	                    IResource resource = unit.getUnderlyingResource();
	                    if (resource instanceof IFile) {
	                        return (IFile) resource;
	                    }
	                }
	            }
	        }

	        return null;

	    } catch (Exception e) {
	        e.printStackTrace();
	        return null;
	    }
	}

	private void handleEvent(UIEvent uIevent, StackFrame farme) {

		if (uIevent instanceof UserChangedVariable) {
			UserChangedVariable userChangedVariable = (UserChangedVariable) uIevent;
			updateVariables(userChangedVariable, farme);
			System.out.println("PROCESS: " + uIevent);
			return;
		}

		if (uIevent instanceof UserChangedField) {
			UserChangedField userChangedField = (UserChangedField) uIevent;
			updateField(userChangedField, farme);
			System.out.println("PROCESS: " + uIevent);
			return;
		}
	}

	private void updateField(UserChangedField userChangedField, StackFrame frame) {
		FieldEntry fieldEntry = userChangedField.getFieldEntry();
		String fieldName = fieldEntry.getField().name();
		String value = (String) fieldEntry.getNewValue();
		Type type = null;
		Field field = fieldEntry.getField();
		try {

			type = field.type();
		} catch (ClassNotLoadedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Value jdiValue = DebugUtils.createJdiObjectFromString(targetVirtualMachineRepresentation.getVirtualMachine(),
				type, value, frame.thread());

		ObjectReference thisObject = frame.thisObject();
		ReferenceType refType = thisObject.referenceType(); // или clazz для статических полей
		Field field1 = refType.fieldByName(userChangedField.getFieldEntry().getField().name());
		if (field1 == null) {
			throw new RuntimeException("Field not found: fieldName");
		}
		try {
			thisObject.setValue(field, jdiValue);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(jdiValue);
		System.out.println("FIELD CHANGED: " + userChangedField);

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
