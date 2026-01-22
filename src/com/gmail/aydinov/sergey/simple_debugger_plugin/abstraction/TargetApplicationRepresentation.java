package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

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
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
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
				iBreakpointManager, virtualMachine);
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
	        //System.out.println(ci.prettyPrint());
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
	        	System.out.println(ignored);
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

	        TargetApplicationMethodDTO dto = createMethodDTO(m);
	        if (dto != null) {
	            result.add(dto);
	        }
	    }

	    return result;
	}

	private TargetApplicationMethodDTO createMethodDTO(Method m) {
	    try {
	        List<com.sun.jdi.Type> argTypes = m.argumentTypes();
	        List<com.sun.jdi.LocalVariable> argVars = loadArgVars(m);

	        List<TargetApplicationMethodParameterDTO> params =
	                compileParameters(argTypes, argVars);

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

	private List<TargetApplicationMethodParameterDTO> compileParameters(
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
	
	public IFile findIFileForLocation(Location location) {
		ReferenceType refType = location.declaringType();
		if (refType == null)
			return null;

		// Имя типа в формате JVM => преобразуем в Java вид
		// было: Lcom/example/MyClass; => com.example.MyClass
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
			try {
				if (!project.isOpen() || !project.hasNature(JavaCore.NATURE_ID))
					continue;
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			IJavaProject javaProject = JavaCore.create(project);
			IType type = null;
			try {
				type = javaProject.findType(className);
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (type != null) {
				ICompilationUnit unit = type.getCompilationUnit();
				if (unit != null) {
					IResource resource = null;
					try {
						resource = unit.getUnderlyingResource();
					} catch (JavaModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (resource instanceof IFile) {
						return (IFile) resource;
					}
				}
			}
		}
		return null;
	}
	

	public ReferenceType findReferenceTypeForClass(TargetApplicationClassOrInterfaceRepresentation clazz) {
	    if (clazz == null) return null;

	    String className = clazz.getTargetApplicationElementName();

	    for (Map.Entry<ReferenceType, TargetApplicationElementRepresentation> entry : referencesAtClassesAndInterfaces.entrySet()) {
	        ReferenceType ref = entry.getKey();
	        if (ref != null && className.equals(ref.name())) {
	            return ref;
	        }
	    }

	    return null; // не найден
	}
	
	public ObjectReference createObjectInstance(ClassType classType) {
	    try {
	        Method constructor = classType.concreteMethodByName("<init>", "()V");
	        if (constructor == null) 
	            throw new RuntimeException("No default constructor for " + classType.name());
	        return classType.newInstance(
	                virtualMachine.allThreads().get(0),
	                constructor,
	                List.of(),
	                ClassType.INVOKE_SINGLE_THREADED
	        );
	    } catch (Exception e) {
	        throw new RuntimeException("Cannot create instance of " + classType.name(), e);
	    }
	}

}
