package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.debug.core.IBreakpointManager;

import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.BreakpointSubscriberRegistrar;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationClassOrInterfaceRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationMethodDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationMethodParameterDTO;
import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequestManager;

public class TargetApplicationRepresentation {

	private final Map<ReferenceType, TargetApplicationElementRepresentation> referencesAtClassesAndInterfaces = new ConcurrentHashMap<ReferenceType, TargetApplicationElementRepresentation>();
	private final TargetApplicationBreakepointRepresentation targetApplicationBreakepointRepresentation;
	private final VirtualMachine virtualMachine;

	public TargetApplicationRepresentation(IBreakpointManager iBreakpointManager,
			EventRequestManager eventRequestManager, VirtualMachine virtualMachine,
			BreakpointSubscriberRegistrar breakpointHitListener) {
		this.targetApplicationBreakepointRepresentation = new TargetApplicationBreakepointRepresentation(
				iBreakpointManager, eventRequestManager, virtualMachine);
		breakpointHitListener.register(targetApplicationBreakepointRepresentation);
		this.virtualMachine = virtualMachine;
	}

	public TargetApplicationBreakepointRepresentation getTargetApplicationBreakepointRepresentation() {
		return targetApplicationBreakepointRepresentation;
	}

	public Map<ReferenceType, TargetApplicationElementRepresentation> getReferencesAtClassesAndInterfaces() {
		return referencesAtClassesAndInterfaces;
	}

	public List<TargetApplicationElementRepresentation> getTargetApplicationElements() {
		return referencesAtClassesAndInterfaces.values().stream().collect(Collectors.toList());
	}

	public void refreshReferencesToClassesOfTargetApplication(VirtualMachine virtualMachine) {
	    referencesAtClassesAndInterfaces.clear();
	    System.out.println("Target class not loaded yet. Waiting...");

	    // 1. Ждём загрузки классов
	    List<ReferenceType> loaded = waitUntilClassesAreLoaded(virtualMachine);

	    // 2. Фильтруем классы содержащие слово "target"
	    List<ReferenceType> targetClasses = filterTargetClasses(loaded);
	    System.out.println("Loaded " + targetClasses.size() + " classes.");

	    // 3. Находим классы, определённые загрузчиками
	    Set<ReferenceType> definedByLoaders = collectDefinedClasses(targetClasses);

	    // 4. Обрабатываем каждый класс/интерфейс
	    for (ReferenceType referenceType : definedByLoaders) {

	        TargetApplicationElementType type = determineElementType(referenceType);
	        if (type == null) {
	            continue;
	        }

	        Set<TargetApplicationMethodDTO> methods =
	                buildMethodDTOs(referenceType);

	        Set<Field> fields = new HashSet<>(referenceType.allFields());

	        referencesAtClassesAndInterfaces.put(
	                referenceType,
	                new TargetApplicationClassOrInterfaceRepresentation(
	                        referenceType.name(),
	                        type,
	                        methods,
	                        fields
	                )
	        );
	    }

	    System.out.println("LOADED CLASSES: " + referencesAtClassesAndInterfaces.size());
	    for (var ci : referencesAtClassesAndInterfaces.values()) {
	        System.out.println(ci.prettyPrint());
	    }
	}

	private List<ReferenceType> waitUntilClassesAreLoaded(VirtualMachine vm) {
	    List<ReferenceType> list = new ArrayList<>();

	    while (list.isEmpty()) {
	        list.addAll(vm.allClasses());
	        if (!list.isEmpty()) {
	            break;
	        }
	        try {
	            Thread.sleep(1000);
	        } catch (InterruptedException ignored) {
	        }
	    }
	    return list;
	}

	private List<ReferenceType> filterTargetClasses(List<ReferenceType> list) {
	    List<ReferenceType> result = new ArrayList<>();
	    for (ReferenceType rt : list) {
	        if (rt.name().contains("target")) {
	            result.add(rt);
	        }
	    }
	    return result;
	}

	private Set<ReferenceType> collectDefinedClasses(List<ReferenceType> classes) {
	    Set<ReferenceType> result = new HashSet<>();

	    for (ReferenceType ref : classes) {
	        ClassLoaderReference loader = ref.classLoader();
	        if (loader == null) {
	            continue;
	        }

	        List<ReferenceType> defined = loader.definedClasses();
	        for (ReferenceType d : defined) {
	            if (d != null) {
	                result.add(d);
	            }
	        }
	    }
	    return result;
	}

	private TargetApplicationElementType determineElementType(ReferenceType ref) {
	    if (ref instanceof ClassType) {
	        return TargetApplicationElementType.CLASS;
	    } else if (ref instanceof InterfaceType) {
	        return TargetApplicationElementType.INTERFACE;
	    }
	    return null;
	}

	private Set<TargetApplicationMethodDTO> buildMethodDTOs(ReferenceType referenceType) {
	    Set<TargetApplicationMethodDTO> result = new TreeSet<>();

	    for (Method m : referenceType.allMethods()) {

	        if (m.isNative()) {
	            continue;
	        }
	        if ("<init>".equals(m.name())) {
	            continue;
	        }

	        TargetApplicationMethodDTO dto = buildMethodDTO(m);
	        if (dto != null) {
	            result.add(dto);
	        }
	    }

	    return result;
	}

	private TargetApplicationMethodDTO buildMethodDTO(Method m) {
	    try {
	        List<com.sun.jdi.Type> argTypes = m.argumentTypes();
	        List<com.sun.jdi.LocalVariable> argVars = loadArgVars(m);

	        List<TargetApplicationMethodParameterDTO> params =
	                buildParameters(argTypes, argVars);

	        return new TargetApplicationMethodDTO(
	                m.name(),
	                m.returnType().toString(),
	                params
	        );

	    } catch (ClassNotLoadedException e) {
	        e.printStackTrace();
	        return null;
	    }
	}

	private List<com.sun.jdi.LocalVariable> loadArgVars(Method m) {
	    try {
	        return m.arguments();
	    } catch (Exception e) {
	        return List.of();
	    }
	}

	private List<TargetApplicationMethodParameterDTO> buildParameters(
	        List<com.sun.jdi.Type> argTypes,
	        List<com.sun.jdi.LocalVariable> argVars
	) {
	    List<TargetApplicationMethodParameterDTO> params = new ArrayList<>();

	    for (int i = 0; i < argTypes.size(); i++) {
	        com.sun.jdi.Type type = argTypes.get(i);

	        String name;
	        if (i < argVars.size()) {
	            name = argVars.get(i).name();
	        } else {
	            name = "arg" + i;
	        }

	        params.add(new TargetApplicationMethodParameterDTO(name, type));
	    }

	    return params;
	}

	
	public void detachDebugger() {
        if (virtualMachine == null) return;

        try {
            // Отключаем все брейкпоинты
            virtualMachine.eventRequestManager().deleteAllBreakpoints();

            // Продолжаем все приостановленные потоки
            virtualMachine.allThreads().forEach(thread -> {
                try {
                    if (thread.suspendCount() > 0) {
                        thread.resume();
                    }
                } catch (Exception ignored) {}
            });

            // Отсоединяемся от VM, оставляя процесс работать
           virtualMachine.dispose();

        } catch (VMDisconnectedException e) {
            // VM уже отключена — игнорируем
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
