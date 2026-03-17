package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

import com.gmail.aydinov.sergey.simple_debugger_plugin.DebugConfiguration;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.CurrentRole;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.BreakpointSubscriberRegistrar;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;

public class TargetApplicationRepresentation {

	private final Map<AbstractElementRepresentation.Tag, UniversalElementRepresentation> topLevelElements = new ConcurrentHashMap<>();
	private final Map<AbstractElementRepresentation.Tag, AbstractElementRepresentation> targetApplicationSnapshot = new ConcurrentHashMap<>();
	private final Map<Long, AbstractElementRepresentation.Tag> visitedElements = new ConcurrentHashMap<>();
	private final DebugConfiguration debugConfiguration;
	private static TargetApplicationRepresentation INSTANCE;

	private TargetApplicationRepresentation(IBreakpointManager iBreakpointManager,
			BreakpointSubscriberRegistrar breakpointSubscriberRegistrar, DebugConfiguration debugConfiguration) {

		// Регистрируем синглтон брейкпоинтов
		breakpointSubscriberRegistrar.register(TargetApplicationBreakpointRepresentation.getInstance());
		this.debugConfiguration = debugConfiguration;
	}

	/** Создаём синглтон с параметрами */
	public static synchronized TargetApplicationRepresentation getInstanceFor(IBreakpointManager iBreakpointManager,
			BreakpointSubscriberRegistrar breakpointSubscriberRegistrar, DebugConfiguration debugConfiguration) {

		if (INSTANCE != null) {
			throw new IllegalStateException("TargetApplicationRepresentation уже создан");
		}
		INSTANCE = new TargetApplicationRepresentation(iBreakpointManager, breakpointSubscriberRegistrar,
				debugConfiguration);
		return INSTANCE;
	}

	/** Получаем уже созданный синглтон */
	public static TargetApplicationRepresentation getInstance() {
		if (INSTANCE == null) {
			throw new IllegalStateException("TargetApplicationRepresentation ещё не создан");
		}
		return INSTANCE;
	}

	public Map<AbstractElementRepresentation.Tag, AbstractElementRepresentation> getTargetApplicationSnapshot() {
		return targetApplicationSnapshot;
	}

	public void takeSnapshotOfTargetApplication(VirtualMachine virtualMachine, BreakpointEvent breakpointEvent) {
		targetApplicationSnapshot.clear();
		topLevelElements.clear();
		visitedElements.clear();
		SimpleDebuggerLogger.info("Waiting for target classes to load...");

		// 1. Ждем загрузки всех классов
		List<ReferenceType> loadedReferenceTypes = filterTargetClasses(waitUntilClassesAreLoaded(virtualMachine));
		System.out.println(loadedReferenceTypes);
		loadTopLevelElements(loadedReferenceTypes);
		System.out.println(targetApplicationSnapshot);
		populateMembersForTopLevelElements();
		System.out.println(targetApplicationSnapshot);
		targetApplicationSnapshot.putAll(topLevelElements);
		addLocalVariables(virtualMachine, breakpointEvent);
		System.out.println(targetApplicationSnapshot);
	}

	private List<ReferenceType> waitUntilClassesAreLoaded(VirtualMachine virtualMachine) {
		List<ReferenceType> referenceTypes = new ArrayList<>();
		while (referenceTypes.isEmpty()) {
			referenceTypes.addAll(virtualMachine.allClasses());
			if (!referenceTypes.isEmpty()) break;
			try {
				Thread.sleep(300);
			} catch (InterruptedException ignored) {
			}
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

		return referenceTypes.stream()
				// исключаем стандартные библиотеки
				.filter(rt -> {
				    String name = rt.name();

				    // убираем lambda
				    if (name.contains("$$Lambda") || name.contains("Lambda/")) {
				        return false;
				    }

				    // убираем анонимные классы (часто $1, $2)
				    if (name.matches(".*\\$\\d+.*")) {
				        return false;
				    }

				    return true;
				})
				// оставляем только классы нашего пакета
				.filter(rt -> rt.name().startsWith(finalTargetPackage + ".")).toList();

	}

	private void loadTopLevelElements(List<ReferenceType> loadedReferenceTypes) {
	    for (ReferenceType refType : loadedReferenceTypes) {
	        UniversalElementRepresentation.UniversalElementType elementType = DebugUtils.determineUniversalElementType(refType);
	        if (elementType == null) continue;

	        ObjectReference instance = null;
	        if (refType instanceof ClassType classType) {
	            try {
	                List<ObjectReference> instances = classType.instances(1); // можно Integer.MAX_VALUE, но осторожно
	                if (!instances.isEmpty()) instance = instances.get(0);
	            } catch (Exception ignored) {}
	        }

	        UniversalElementRepresentation topLevelElement = UniversalElementRepresentation.builder()
	                .referenceType(refType)
	                .objectReference(instance)
	                .elementName(DebugUtils.extractSimpleName(refType.name()))
	                .additionalInfo(refType.name())
	                .elementType(elementType)
	                .currentRole(CurrentRole.OUTER)
	                .value(refType.name())
	                .isStatic(refType.isStatic())
	                .valueCategory(ValueCategory.NOT_SPECIFIED)
	                .typeOrReturnType(refType.name())
	                .uniqueId(UUID.randomUUID())
	                .parentUniqueId(null)
	                .build();

	        // Сохраняем в поле класса topLevelElements
	        this.topLevelElements.put(topLevelElement.getTag(), topLevelElement);
	        // И сразу в snapshot, если хотим, чтобы top-level элементы были видны
	       // this.targetApplicationSnapshot.put(topLevelElement.getTag(), topLevelElement);
	    }
	}
	
	private void populateMembersForTopLevelElements() {
	    for (UniversalElementRepresentation topLevel : topLevelElements.values()) {
	        if (topLevel.getReferenceType() instanceof ClassType classType) {
	            loadMembers(classType, topLevel.getTag().getUniqueId());
	        } else if (topLevel.getObjectReference() != null) {
	            loadMembers(topLevel.getObjectReference(), topLevel.getTag().getUniqueId());
	        }
	    }
	}

	private void loadMembers(Object obj, UUID parentId) {
	    if (obj == null) return;

	    long objId = getObjectUniqueId(obj);

	    // Если уже посещали — создаём ссылку
	    if (visitedElements.containsKey(objId)) {
	    	AbstractElementRepresentation.Tag ownTag = new AbstractElementRepresentation.Tag(UUID.randomUUID(), parentId);
	        AbstractElementRepresentation.Tag existingTag = visitedElements.get(objId);
	        ObjectReference objectReference = targetApplicationSnapshot.get(existingTag).getObjectReference();
	        ElementReference reference = new ElementReference(ownTag, existingTag, objectReference);

	        targetApplicationSnapshot.put(reference.getTag(), reference);
	        return;
	    }

	    // Новый объект — регистрируем
	    UniversalElementRepresentation.Tag tag =
	            new UniversalElementRepresentation.Tag(UUID.randomUUID(), parentId);
	    visitedElements.put(objId, tag);

	    if (obj instanceof ObjectReference objectRef) {
	        for (Field field : objectRef.referenceType().fields()) {
	            Value value = objectRef.getValue(field);

	            UniversalElementRepresentation member =
	                    buildElementForField(field, value, parentId);

	            targetApplicationSnapshot.put(member.getTag(), member);

	            if (value instanceof ObjectReference childObj && isUserClassOrCollection(childObj)) {
	                loadMembers(childObj, member.getTag().getUniqueId());
	            }
	        }

	    } else if (obj instanceof ClassType classType) {

	        // статические поля
	        for (Field field : classType.fields()) {
	            if (!field.isStatic()) continue;

	            Value value = classType.getValue(field);
	            UniversalElementRepresentation member =
	                    buildElementForField(field, value, parentId);

	            targetApplicationSnapshot.put(member.getTag(), member);

	            if (value instanceof ObjectReference childObj && isUserClassOrCollection(childObj)) {
	                loadMembers(childObj, member.getTag().getUniqueId());
	            }
	        }

	        // методы
	        for (Method method : classType.methods()) {

	            if (shouldSkipMethod(method)) {
	                continue;
	            }

	            UniversalElementRepresentation methodElement =
	            		UniversalElementRepresentation.buildElementForMethod(method, parentId);

	            targetApplicationSnapshot.put(methodElement.getTag(), methodElement);
	        }
	    }
	}

	private boolean shouldSkipMethod(Method method) {
	    String name = method.name();

	    // конструкторы и статика
	    if ("<init>".equals(name) || "<clinit>".equals(name)) {
	        return true;
	    }

	    // lambda методы
	    if (name.startsWith("lambda$")) {
	        return true;
	    }

	    // synthetic / bridge
	    if (method.isSynthetic() || method.isBridge()) {
	        return true;
	    }

	    return false;
	}
	
	private boolean addLocalVariables(VirtualMachine virtualMachine, BreakpointEvent breakpointEvent) {
		if (Objects.isNull(breakpointEvent))
			return false;
		StackFrame frame;
		try {
			frame = breakpointEvent.thread().frame(0);
		} catch (IncompatibleThreadStateException e) {
			SimpleDebuggerLogger.error(e.getMessage(), e);
			return false;
		}
		Location location = breakpointEvent.location();
		Method method = location.method();
		if (method == null)
			return false;

		// Получаем представление метода
		// Method method = frame.location().method();
		ReferenceType type = method.declaringType();

		UniversalElementRepresentation methodRepresentation =
		        targetApplicationSnapshot.values().stream()
		                .map(e -> {
		                    if (e instanceof ElementReference ref) {
		                        return (UniversalElementRepresentation)
		                                targetApplicationSnapshot.get(ref.getReferenceTag());
		                    }
		                    return (e instanceof UniversalElementRepresentation ue) ? ue : null;
		                })
		                .filter(Objects::nonNull)
		                .filter(e -> e.getElementType() == UniversalElementType.METHOD)
		                .filter(e -> {
		                    if (e.getReferenceType() == null) return false;

		                    return e.getReferenceType().equals(type)
		                            && e.getElementName().equals(method.name())
		                            && method.signature().equals(e.getAdditionalInfo()); // 🔥 ключ
		                })
		                .findFirst()
		                .orElse(null);
		if (methodRepresentation == null)
			return false;
		List<LocalVariable> locals = Collections.emptyList();
		try {
			// arguments = method.arguments();
			locals = frame.visibleVariables();
		} catch (AbsentInformationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String valueText = null;
		List<UniversalElementRepresentation> localVariables = new ArrayList<>();
		for (LocalVariable local : locals) {

		    Value val;

		    try {
		        val = frame.getValue(local);
		    } catch (Exception e) {
		        // StackFrame мог устареть
		        SimpleDebuggerLogger.warn("Failed to read local variable: " + local.name());
		        continue;
		    }

		    valueText = "";
		    ObjectReference objRef = val instanceof ObjectReference ? (ObjectReference) val : null;

		    if (Objects.isNull(objRef)) {

		        valueText = DebugUtils.getLocalVariableValueAsString(val);

		    } else {

		        int q = DebugUtils.getCollectionSize(objRef, breakpointEvent);

		        valueText = q == -1
		                ? DebugUtils.getLocalVariableValueAsString(val)
		                : "size:" + q + "; ";

		        System.out.println("INNER COLLECTIONS: " + q);
		    }

		    UniversalElementRepresentation variable =
		            UniversalElementRepresentation.builder()
		                    .referenceType(null)
		                    .objectReference(objRef)
		                    .elementName(local.name())
		                    .additionalInfo(local.typeName())
		                    .elementType(UniversalElementType.LOCAL_VARIABLE)
		                    .currentRole(CurrentRole.INNER)
		                    .value(valueText)
		                    .isStatic(false)
		                    .valueCategory(DebugUtils.determineValueCategory(val))
		                    .typeOrReturnType(local.typeName())
		                    .uniqueId(UUID.randomUUID())
		                    .parentUniqueId(methodRepresentation.getTag().getUniqueId())
		                    .build();

		    targetApplicationSnapshot.put(variable.getTag(), variable);
		    localVariables.add(variable);
		}

		return true;
	}

	private long getObjectUniqueId(Object obj) {
	    if (obj instanceof ObjectReference objectRef) {
	        return objectRef.uniqueID();
	    }
	    // Для ReferenceType (классы) можно использовать hashCode + System.identityHashCode
	    else if (obj instanceof ReferenceType refType) {
	        // Для классов можно брать hashCode типа или уникальный tag
	        return refType.hashCode();
	    }
	    // Для всего остального — fallback
	    else {
	        return System.identityHashCode(obj);
	    }
	}

	// Метод определяет, нужно ли рекурсивно обрабатывать объект
	private boolean isUserClassOrCollection(ObjectReference objRef) {
	    String name = objRef.referenceType().name();
	    return !name.startsWith("java.") && !name.startsWith("javax.") &&
	           !name.startsWith("jdk.") && !name.startsWith("sun.");
	}

	// Создание элемента для поля
	private UniversalElementRepresentation buildElementForField(Field field, Value value, UUID parentId) {
	    return UniversalElementRepresentation.builder()
	        .elementName(field.name())
	        .additionalInfo(field.typeName())
	        .elementType(UniversalElementType.NON_STATIC_FIELD)
	        .currentRole(CurrentRole.INNER)
	        .value(value != null ? value.toString() : "null")
	        .valueCategory(ValueCategory.NOT_SPECIFIED)
	        .uniqueId(UUID.randomUUID())
	        .parentUniqueId(parentId)
	        .build();
	}

	// Создание элемента для метода
//	private UniversalElementRepresentation buildElementForMethod(Method method, UUID parentId) {
//	    return UniversalElementRepresentation.builder()
//	        .elementName(method.name())
//	        .additionalInfo(method.signature())
//	        .elementType(UniversalElementType.METHOD)
//	        .currentRole(CurrentRole.INNER)
//	        .valueCategory(ValueCategory.NOT_SPECIFIED)
//	        .uniqueId(UUID.randomUUID())
//	        .parentUniqueId(parentId)
//	        .build();
//	}

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