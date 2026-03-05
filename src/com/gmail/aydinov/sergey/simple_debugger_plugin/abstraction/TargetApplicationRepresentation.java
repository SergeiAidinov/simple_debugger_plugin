package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.gmail.aydinov.sergey.simple_debugger_plugin.DebugConfiguration;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.CurrentRole;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.Type;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.request.EventRequestManager;

public class TargetApplicationRepresentation {

    private final Map<UniversalElementRepresentation.Tag, UniversalElementRepresentation> targetApplicationSnapshot = new ConcurrentHashMap<>();
    private final TargetApplicationBreakpointRepresentation targetApplicationBreakepointRepresentation;
    private final VirtualMachine virtualMachine;
    private final DebugConfiguration debugConfiguration;

    public TargetApplicationRepresentation(IBreakpointManager iBreakpointManager,
                                           EventRequestManager eventRequestManager,
                                           VirtualMachine virtualMachine,
                                           BreakpointSubscriberRegistrar breakpointSubscriberRegistrar,
                                           DebugConfiguration debugConfiguration) {
        this.targetApplicationBreakepointRepresentation = new TargetApplicationBreakpointRepresentation(iBreakpointManager, virtualMachine);
        breakpointSubscriberRegistrar.register(targetApplicationBreakepointRepresentation);
        this.virtualMachine = virtualMachine;
        this.debugConfiguration = debugConfiguration;
    }

    public TargetApplicationBreakpointRepresentation getTargetApplicationBreakepointRepresentation() {
        return targetApplicationBreakepointRepresentation;
    }

    public Map<UniversalElementRepresentation.Tag, UniversalElementRepresentation> getTargetApplicationSnapshot() {
        return targetApplicationSnapshot;
    }

    public void takeSnapshotOfTargetApplication(VirtualMachine virtualMachine) {
        targetApplicationSnapshot.clear();
        SimpleDebuggerLogger.info("Waiting for target classes to load...");

        // 1. Ждем загрузки всех классов
        List<ReferenceType> loadedReferenceTypes = waitUntilClassesAreLoaded(virtualMachine);

        // 2. Фильтруем только нужные классы таргета
        List<ReferenceType> targetClasses = filterTargetClasses(loadedReferenceTypes);
        SimpleDebuggerLogger.info("Loaded " + targetClasses.size() + " classes.");

        // 3. Собираем только определенные загрузчиками
        Set<ReferenceType> definedByLoaders = collectDefinedClasses(targetClasses);

        Map<UniversalElementRepresentation.Tag, UniversalElementRepresentation> topLevelElements = new HashMap<>();

        // 4. Создаем top-level элементы (классы)
        for (ReferenceType referenceType : definedByLoaders) {
            UniversalElementRepresentation.UniversalElementType elementType = determineElementType(referenceType);
            if (elementType == null) continue;

            String fqName = referenceType.name();

            UniversalElementRepresentation topLevelElement = UniversalElementRepresentation.builder()
                    .referenceType(referenceType)       // сам ReferenceType
                    .objectReference(null)              // у класса пока нет ObjectReference
                    .elementName(DebugUtils.extractSimpleName(fqName))
                    .additionalInfo(fqName)
                    .elementType(elementType)
                    .currentRole(UniversalElementRepresentation.CurrentRole.OUTER)
                    .value(fqName)
                    .isStatic(referenceType.isStatic())
                    .valueCategory(UniversalElementRepresentation.ValueCategory.NOT_SPECIFIED)
                    .typeOrReturnType(fqName)
                    .uniqueId(UUID.randomUUID())
                    .parentUniqueId(null)
                    .build();

            topLevelElements.put(topLevelElement.getTag(), topLevelElement);
        }

        // 5. Добавляем в общий snapshot
        targetApplicationSnapshot.putAll(topLevelElements);

        // 6. Заполняем внутренние элементы (методы, поля)
        for (UniversalElementRepresentation topLevelElement : topLevelElements.values()) {
            populateInnerElements(topLevelElement, topLevelElement.getReferenceType());
        }

        SimpleDebuggerLogger.info("LOADED TOP-LEVEL ELEMENTS: " + targetApplicationSnapshot.size());
    }

    public boolean addLocalVariables(VirtualMachine virtualMachine, BreakpointEvent breakpointEvent) {
        StackFrame frame;
        try {
            frame = breakpointEvent.thread().frame(0);
        } catch (IncompatibleThreadStateException e) {
            SimpleDebuggerLogger.error(e.getMessage(), e);
            return false;
        }

        Location location = breakpointEvent.location();
        Method method = location.method();
        if (method == null) return false;

        // Получаем представление метода
        UniversalElementRepresentation methodRepresentation = targetApplicationSnapshot.values().stream()
                .filter(c -> c.getReferenceType() != null &&
                        c.getReferenceType().equals(method.declaringType()))
                .findFirst().orElse(null);
        if (methodRepresentation == null) return false;

        Map<LocalVariable, Value> locals = new HashMap<>();

        // -----------------------------
        // 1️⃣ Добавляем параметры метода
        try {
            for (LocalVariable param : method.arguments()) {
                if (!locals.containsKey(param)) {
                    locals.put(param, frame.getValue(param));
                }
            }
        } catch (AbsentInformationException ignored) { }

        // -----------------------------
        // 2️⃣ Добавляем видимые локальные переменные
        try {
            for (LocalVariable local : frame.visibleVariables()) {
                if (!locals.containsKey(local)) {
                    locals.put(local, frame.getValue(local));
                }
            }
        } catch (AbsentInformationException ignored) { }

        // -----------------------------
        // 3️⃣ Формируем элементы для UI
        List<UniversalElementRepresentation> localVariables = new ArrayList<>();
        for (Map.Entry<LocalVariable, Value> entry : locals.entrySet()) {
            LocalVariable localVar = entry.getKey();
            Value value = entry.getValue();

            String valueText = value == null ? "<null>" : value.toString();
            UniversalElementRepresentation variable = UniversalElementRepresentation.builder()
                    .referenceType(null)
                    .elementName(localVar.name())
                    .additionalInfo(localVar.typeName()) // используем тип переменной
                    .elementType(UniversalElementType.VARIABLE)
                    .currentRole(CurrentRole.INNER)
                    .value(valueText)
                    .isStatic(false)
                    .valueCategory(DebugUtils.determineValueCategory(value))
                    .typeOrReturnType(localVar.typeName())
                    .uniqueId(UUID.randomUUID())
                    .parentUniqueId(methodRepresentation.getTag().getUniqueId())
                    .build();

            targetApplicationSnapshot.put(variable.getTag(), variable);
            localVariables.add(variable);
        }

        return true;
    }

    private void populateInnerElements(UniversalElementRepresentation parentElement, ReferenceType refType) {
        if (parentElement == null || refType == null) return;

        // Получаем объект-экземпляр, если класс — ClassType
        ObjectReference instance = null;
        if (refType instanceof ClassType classType) {
            try {
                List<ObjectReference> instances = classType.instances(1);
                if (!instances.isEmpty()) instance = instances.get(0);
            } catch (Exception ignored) {}
        }

        // ---------------- Поля ----------------
        for (Field field : refType.allFields()) {
            try {
                if (field.isSynthetic()) continue;

                boolean isStatic = field.isStatic();
                UniversalElementRepresentation.UniversalElementType elementType =
                        isStatic ? UniversalElementRepresentation.UniversalElementType.STATIC_FIELD
                                 : UniversalElementRepresentation.UniversalElementType.NON_STATIC_FIELD;

                UniversalElementRepresentation.ValueCategory category = determineValueCategory(field.typeName());
                String valueText = field.name();

                if (category == UniversalElementRepresentation.ValueCategory.PRIMITIVE ||
                    category == UniversalElementRepresentation.ValueCategory.STRING) {
                    valueText = extractPrimitiveOrStringAsText(field, instance);
                }

                UniversalElementRepresentation fieldElement = UniversalElementRepresentation.builder()
                        .referenceType(refType)
                        .objectReference(isStatic ? null : instance) // для нестатических полей назначаем экземпляр
                        .elementName(field.name())
                        .additionalInfo(DebugUtils.extractSimpleName(field.typeName()))
                        .elementType(elementType)
                        .currentRole(UniversalElementRepresentation.CurrentRole.INNER)
                        .value(valueText)
                        .isStatic(isStatic)
                        .valueCategory(category)
                        .typeOrReturnType(field.typeName())
                        .uniqueId(UUID.randomUUID())
                        .parentUniqueId(parentElement.getTag().getUniqueId())
                        .build();

                targetApplicationSnapshot.put(fieldElement.getTag(), fieldElement);
            } catch (Exception ignored) {}
        }

        // ---------------- Методы ----------------
        for (Method method : refType.allMethods()) {
            try {
                if (method.isSynthetic() || method.name().equals("<init>") || method.name().equals("<clinit>"))
                    continue;

                if (isObjectMethodUnoverridden(refType, method)) continue;

                String methodArgs = method.argumentTypes().stream()
                        .map(Type::name)
                        .collect(Collectors.joining(", "));

                UniversalElementRepresentation methodElement = UniversalElementRepresentation.builder()
                        .referenceType(refType)
                        .objectReference(instance) // для методов можем назначить экземпляр (null для статических)
                        .elementName(method.name() + "()")
                        .additionalInfo(method.returnTypeName())
                        .elementType(UniversalElementRepresentation.UniversalElementType.METHOD)
                        .currentRole(UniversalElementRepresentation.CurrentRole.INNER)
                        .value(parentElement.getAdditionalInfo() + "." + method.name() + "(" + methodArgs + ")")
                        .isStatic(method.isStatic())
                        .valueCategory(UniversalElementRepresentation.ValueCategory.NOT_SPECIFIED)
                        .typeOrReturnType(method.returnTypeName())
                        .uniqueId(UUID.randomUUID())
                        .parentUniqueId(parentElement.getTag().getUniqueId())
                        .build();

                targetApplicationSnapshot.put(methodElement.getTag(), methodElement);
            } catch (Exception ignored) {}
        }
    }

    private String extractPrimitiveOrStringAsText(Field field, ObjectReference instance) {
        if (field == null) return null;
        Value value = field.isStatic() ? field.declaringType().getValue(field) : instance.getValue(field);
        if (value == null) return "null";
        if (value instanceof StringReference sRef) return sRef.value();
        if (value instanceof PrimitiveValue pVal) return pVal.toString();
        return "<null>";
    }

    private ValueCategory determineValueCategory(String typeName) {
        if (typeName == null || typeName.equals("void")) return ValueCategory.NULL;
        if (Set.of("byte", "short", "int", "long", "float", "double", "boolean", "char").contains(typeName))
            return ValueCategory.PRIMITIVE;
        if (Set.of("java.lang.Byte","java.lang.Short","java.lang.Integer","java.lang.Long","java.lang.Float",
                "java.lang.Double","java.lang.Boolean","java.lang.Character").contains(typeName))
            return ValueCategory.WRAPPER;
        if ("java.lang.String".equals(typeName)) return ValueCategory.STRING;
        if (typeName.startsWith("java.util.List") || typeName.startsWith("java.util.Set")) return ValueCategory.COLLECTION;
        if (typeName.endsWith("[]")) return ValueCategory.ARRAY;
        if (typeName.startsWith("java.util.Map")) return ValueCategory.MAP;
        return ValueCategory.USER_OBJECT;
    }

    private boolean isObjectMethodUnoverridden(ReferenceType refType, Method method) {
        try {
            if ("java.lang.Object".equals(refType.name())) return false;
            List<ReferenceType> objectClasses = method.virtualMachine().classesByName("java.lang.Object");
            if (objectClasses.isEmpty()) return false;
            ReferenceType objectRef = objectClasses.get(0);
            for (Method objMethod : objectRef.allMethods()) {
                if (objMethod.name().equals(method.name()) && objMethod.signature().equals(method.signature())) {
                    for (Method classMethod : refType.allMethods()) {
                        if (classMethod.name().equals(method.name()) &&
                            classMethod.signature().equals(method.signature()) &&
                            classMethod.declaringType().equals(refType)) return false;
                    }
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private List<ReferenceType> waitUntilClassesAreLoaded(VirtualMachine virtualMachine) {
        List<ReferenceType> referenceTypes = new ArrayList<>();
        while (referenceTypes.isEmpty()) {
            referenceTypes.addAll(virtualMachine.allClasses());
            if (!referenceTypes.isEmpty()) break;
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
        return referenceTypes;
    }

    private List<ReferenceType> filterTargetClasses(List<ReferenceType> referenceTypes) {
        String targetPackage = debugConfiguration.getTargetRootPackage();
        if (targetPackage == null || targetPackage.isBlank()) {
            String mainClass = debugConfiguration.getMainClassName();
            int lastDot = mainClass.lastIndexOf('.');
            targetPackage = lastDot > 0 ? mainClass.substring(0, lastDot) : "";
        }
        String finalTargetPackage = targetPackage;
        return referenceTypes.stream().filter(rt -> rt.name().startsWith(finalTargetPackage + ".")).toList();
    }

    private Set<ReferenceType> collectDefinedClasses(List<ReferenceType> referenceTypes) {
        Set<ReferenceType> result = new HashSet<>();
        for (ReferenceType referenceType : referenceTypes) {
            ClassLoaderReference classLoaderReference = referenceType.classLoader();
            if (Objects.isNull(classLoaderReference)) continue;
            for (ReferenceType definedReferenceType : classLoaderReference.definedClasses()) {
                if (Objects.nonNull(definedReferenceType)) result.add(definedReferenceType);
            }
        }
        return result;
    }

    private UniversalElementType determineElementType(ReferenceType referenceType) {
        if (referenceType instanceof ClassType) return UniversalElementType.CLASS;
        if (referenceType instanceof InterfaceType) return UniversalElementType.INTERFACE;
        return null;
    }

    public void detachDebugger() {
		if (Objects.isNull(virtualMachine)) {
			return;
		}
		try {
			virtualMachine.eventRequestManager().deleteAllBreakpoints();

			virtualMachine.allThreads().forEach(threadReference -> {
				try {
					if (threadReference.suspendCount() > 0) {
						threadReference.resume();
					}
				} catch (Exception ignored) {
				}
			});
			virtualMachine.dispose();

		} catch (VMDisconnectedException ignored) {
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

    public ReferenceType findReferenceTypeForClass(UniversalElementRepresentation universalElementRepresentation) {
		if (Objects.isNull(universalElementRepresentation)) {
			return null;
		}
		String className = universalElementRepresentation.getElementName();
		for (Entry<UniversalElementRepresentation.Tag, UniversalElementRepresentation> entry : targetApplicationSnapshot.entrySet()) {
			ReferenceType referenceType = entry.getValue().getReferenceType();
			if (Objects.nonNull(referenceType) && className.equals(referenceType.name())) {
				return referenceType;
			}
		}
		return null;
	}

    public ObjectReference createObjectInstance(ClassType classType) {
		try {
			Method constructor = classType.concreteMethodByName("<init>", "()V");
			if (Objects.isNull(constructor)) {
				throw new RuntimeException("No default constructor for " + classType.name());
			}
			return classType.newInstance(virtualMachine.allThreads().get(0), constructor, List.of(),
					ClassType.INVOKE_SINGLE_THREADED);
		} catch (Exception exception) {
			throw new RuntimeException("Cannot create instance of " + classType.name(), exception);
		}
	}

    public IFile findIFileForLocation(Location location) {
		ReferenceType referenceType = location.declaringType();
		if (Objects.isNull(referenceType)) {
			return null;
		}
		String jvmName = referenceType.name();
		String className = jvmName.replace('/', '.');
		if (className.startsWith("L") && className.endsWith(";")) {
			className = className.substring(1, className.length() - 1);
		}
		IWorkspace iWorkspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot iWorkspaceRoot = iWorkspace.getRoot();
		for (IProject iProject : iWorkspaceRoot.getProjects()) {
			try {
				if (!iProject.isOpen() || !iProject.hasNature(JavaCore.NATURE_ID)) {
					continue;
				}
			} catch (CoreException coreException) {
				coreException.printStackTrace();
			}
			IJavaProject iJavaProject = JavaCore.create(iProject);
			IType iType;
			try {
				iType = iJavaProject.findType(className);
			} catch (JavaModelException javaModelException) {
				javaModelException.printStackTrace();
				continue;
			}
			if (Objects.nonNull(iType)) {
				ICompilationUnit iCompilationUnit = iType.getCompilationUnit();
				if (Objects.nonNull(iCompilationUnit)) {
					try {
						IResource iResource = iCompilationUnit.getUnderlyingResource();
						if (iResource instanceof IFile) {
							return (IFile) iResource;
						}
					} catch (JavaModelException javaModelException) {
						javaModelException.printStackTrace();
					}
				}
			}
		}
		return null;
	}
}