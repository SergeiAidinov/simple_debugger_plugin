package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.ArrayList;
import java.util.Collections;
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

import com.gmail.aydinov.sergey.simple_debugger_plugin.DebugConfiguration;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.CurrentRole;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.BreakpointSubscriberRegistrar;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TripletDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
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

public class TargetApplicationRepresentation {

	private final Map<AbstractElementRepresentation.Tag, AbstractElementRepresentation> targetApplicationSnapshot = new ConcurrentHashMap<>();
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
			String fqName = referenceType.name();

			// Пропускаем JDK-классы
			if (fqName.startsWith("java.") || fqName.startsWith("javax."))
				continue;

			UniversalElementRepresentation.UniversalElementType elementType = determineElementType(referenceType);
			if (elementType == null)
				continue;

			ObjectReference instance = null;
			if (referenceType instanceof ClassType classType) {
				try {
					List<ObjectReference> instances = classType.instances(1);
					if (!instances.isEmpty())
						instance = instances.get(0);
				} catch (Exception ignored) {
				}
			}

			UniversalElementRepresentation topLevelElement = UniversalElementRepresentation.builder()
					.referenceType(referenceType).objectReference(instance)
					.elementName(DebugUtils.extractSimpleName(fqName)).additionalInfo(fqName).elementType(elementType)
					.currentRole(UniversalElementRepresentation.CurrentRole.OUTER).value(fqName)
					.isStatic(referenceType.isStatic())
					.valueCategory(UniversalElementRepresentation.ValueCategory.NOT_SPECIFIED).typeOrReturnType(fqName)
					.uniqueId(UUID.randomUUID()).parentUniqueId(null).build();

			topLevelElements.put(topLevelElement.getTag(), topLevelElement);
		}

		// 5. Добавляем в общий snapshot
		targetApplicationSnapshot.putAll(topLevelElements);

		// 6. Заполняем внутренние элементы (методы, поля)
		for (UniversalElementRepresentation topLevelElement : topLevelElements.values()) {
			populateInnerElements(topLevelElement, topLevelElement.getReferenceType(), breakpointEvent);
		}

		addLocalVariables(virtualMachine, breakpointEvent);
		SimpleDebuggerLogger.info("LOADED TOP-LEVEL ELEMENTS: " + targetApplicationSnapshot.size());
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

		UniversalElementRepresentation methodRepresentation = targetApplicationSnapshot.values().stream()
			    .map(e -> {
			        if (e instanceof ElementReference ref) {
			            // достаём реальный элемент из snapshot по referenceTag
			            AbstractElementRepresentation real = targetApplicationSnapshot.get(ref.getReferenceTag());
			            if (real instanceof UniversalElementRepresentation ue) return ue;
			            return null; // если не UE — игнорируем
			        }
			        if (e instanceof UniversalElementRepresentation ue) return ue; // уже реальный объект
			        return null; // остальные типы игнорируем
			    })
			    .filter(Objects::nonNull) // убираем null
			    .filter(e -> e.getReferenceType() != null
			                 && e.getReferenceType().equals(type)
			                 && e.getElementType() == UniversalElementRepresentation.UniversalElementType.METHOD
			                 && e.getElementName().startsWith(method.name()))
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

	private void populateInnerElements(UniversalElementRepresentation parentElement, ReferenceType refType,
			BreakpointEvent breakpointEvent) {
		if (parentElement == null || refType == null)
			return;

		String className = refType.name();
		if (className.startsWith("java.") || className.startsWith("javax."))
			return;

		// ---------------- Получаем все экземпляры класса ----------------
		List<ObjectReference> instances = new ArrayList<>();
		if (refType instanceof ClassType classType) {
			try {
				instances = classType.instances(Integer.MAX_VALUE); 
			} catch (Exception ignored) {
			}
		}

		// ---------------- Проходим по всем экземплярам ----------------
		for (ObjectReference instance : instances) {
			// ---------------- Поля ----------------
			for (Field field : refType.allFields()) {
				try {
					if (field.isSynthetic())
						continue;

					boolean isStatic = field.isStatic();
					UniversalElementRepresentation.UniversalElementType elementType = isStatic
							? UniversalElementRepresentation.UniversalElementType.STATIC_FIELD
							: UniversalElementRepresentation.UniversalElementType.NON_STATIC_FIELD;

					UniversalElementRepresentation.ValueCategory category = determineValueCategory(field.typeName());

					String valueText = field.name();

					if (category == UniversalElementRepresentation.ValueCategory.PRIMITIVE
							|| category == UniversalElementRepresentation.ValueCategory.STRING) {
						valueText = extractPrimitiveOrStringAsText(field, instance);
					}
					if (Objects.equals(category, UniversalElementRepresentation.ValueCategory.COLLECTION)
							|| Objects.equals(category, UniversalElementRepresentation.ValueCategory.MAP)
							|| Objects.equals(category, UniversalElementRepresentation.ValueCategory.ARRAY)) {
						System.out.println("COLLECTION FOUND: " + field.name());
						Value value = null;
						if (field.isStatic()) {
							value = field.declaringType().getValue(field);
						} else {
							value = instance.getValue(field);
						}

						ObjectReference objRef = (value instanceof ObjectReference) ? (ObjectReference) value : null;
					TripletDTO<String, String, String> triplet = DebugUtils.determinCollectionType(objRef, breakpointEvent);
					String description = DebugUtils.compileCollectionDescription(triplet);
						int q = DebugUtils.getCollectionSize(objRef, breakpointEvent);
						// int q = -1;
						if (q != -1)
							valueText = "size: " + q + "; " + description;
						System.out.println("SIZE_COLLECTION: " + field.name() + " " + q);
					}

					UniversalElementRepresentation fieldElement = UniversalElementRepresentation.builder()
							.referenceType(refType).objectReference(isStatic ? null : instance)
							.elementName(field.name()).additionalInfo(DebugUtils.extractSimpleName(field.typeName()))
							.elementType(elementType).currentRole(UniversalElementRepresentation.CurrentRole.INNER)
							.value(valueText).isStatic(isStatic).valueCategory(category)
							.typeOrReturnType(field.typeName()).uniqueId(UUID.randomUUID())
							.parentUniqueId(parentElement.getTag().getUniqueId()).build();
					targetApplicationSnapshot.put(fieldElement.getTag(), fieldElement);

					// ---------------- Рекурсивно собираем объекты ----------------
					if (instance != null && !isStatic
						//	&& category == UniversalElementRepresentation.ValueCategory.USER_OBJECT
							) {
						Value fieldValue = instance.getValue(field);
						if (fieldValue instanceof ObjectReference childRef) {
							populateObjectReference(fieldElement, childRef, breakpointEvent);
						}
					}

				} catch (Exception ignored) {
				}
			}

			// ---------------- Методы ----------------
			for (Method method : refType.allMethods()) {
				try {
					if (method.isSynthetic() || method.name().equals("<init>") || method.name().equals("<clinit>"))
						continue;
					if (method.declaringType().name().equals("java.lang.Object"))
						continue;
					if (method.declaringType().name().startsWith("java.") && method.isStatic())
						continue;

					String methodArgs = method.argumentTypes().stream().map(Type::name)
							.collect(Collectors.joining(","));

					UniversalElementRepresentation methodElement = UniversalElementRepresentation.builder()
							.referenceType(refType).objectReference(instance).elementName(method.name() + "()")
							.additionalInfo(method.returnTypeName())
							.elementType(UniversalElementRepresentation.UniversalElementType.METHOD)
							.currentRole(UniversalElementRepresentation.CurrentRole.INNER)
							.value(parentElement.getAdditionalInfo() + "." + method.name() + "(" + methodArgs + ")")
							.isStatic(method.isStatic())
							.valueCategory(UniversalElementRepresentation.ValueCategory.NOT_SPECIFIED)
							.typeOrReturnType(method.returnTypeName()).uniqueId(UUID.randomUUID())
							.parentUniqueId(parentElement.getTag().getUniqueId()).build();

					targetApplicationSnapshot.put(methodElement.getTag(), methodElement);

				} catch (Exception ignored) {
				}
			}
		}
	}

	/**
	 * Рекурсивно добавляет объект, на который ссылается поле
	 */
	private void populateObjectReference(UniversalElementRepresentation parentFieldElement, ObjectReference objRef,
			BreakpointEvent breakpointEvent) {
		if (objRef == null)
			return;

		ReferenceType refType = objRef.referenceType();
		ValueCategory valueCategory = DebugUtils.determineValueCategory(objRef);
//		if (refType.name().startsWith("java.") || refType.name().startsWith("javax."))
//			return;

		boolean isIterable = false;
		boolean isMap = false;

		if (refType instanceof ClassType classType) {
			isIterable = classType.allInterfaces().stream().anyMatch(i -> i.name().equals("java.lang.Iterable"));

			isMap = classType.allInterfaces().stream().anyMatch(i -> i.name().equals("java.util.Map"));
		}

		if (isIterable) {
			System.out.println("ITERABLE FOUND");
		} else if (isMap) {
			System.out.println("MAP FOUND");
		}

		UniversalElementRepresentation objElement = UniversalElementRepresentation.builder().referenceType(refType)
				.objectReference(objRef).elementName(refType.name()) // имя класса как elementName
				.additionalInfo(refType.name()).elementType(UniversalElementRepresentation.UniversalElementType.CLASS)
				.currentRole(UniversalElementRepresentation.CurrentRole.INNER).value(refType.name()).isStatic(false)
				.valueCategory(valueCategory)
				.typeOrReturnType(refType.name()).uniqueId(UUID.randomUUID())
				.parentUniqueId(parentFieldElement.getTag().getUniqueId()).build();

		targetApplicationSnapshot.put(objElement.getTag(), objElement);

		// Собираем внутренние поля объекта
		populateInnerElements(objElement, refType, breakpointEvent);

		List<ObjectReference> children = DebugUtils.getCollectionElements(objRef, breakpointEvent);
		for (ObjectReference child : children) {
			populateObjectReference(objElement, child, breakpointEvent);
		}
	}


	private String extractPrimitiveOrStringAsText(Field field, ObjectReference instance) {
		if (field == null)
			return null;
		Value value = field.isStatic() ? field.declaringType().getValue(field) : instance.getValue(field);
		if (value == null)
			return "null";
		if (value instanceof StringReference sRef)
			return sRef.value();
		if (value instanceof PrimitiveValue pVal)
			return pVal.toString();
		return "<null>";
	}

	private ValueCategory determineValueCategory(String typeName) {
		if (typeName == null || typeName.equals("void"))
			return ValueCategory.NULL;
		if (Set.of("byte", "short", "int", "long", "float", "double", "boolean", "char").contains(typeName))
			return ValueCategory.PRIMITIVE;
		if (Set.of("java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Float",
				"java.lang.Double", "java.lang.Boolean", "java.lang.Character").contains(typeName))
			return ValueCategory.WRAPPER;
		if ("java.lang.String".equals(typeName))
			return ValueCategory.STRING;
		if (typeName.startsWith("java.util.List") || typeName.startsWith("java.util.Set"))
			return ValueCategory.COLLECTION;
		if (typeName.endsWith("[]"))
			return ValueCategory.ARRAY;
		if (typeName.startsWith("java.util.Map"))
			return ValueCategory.MAP;
		return ValueCategory.USER_OBJECT;
	}

	private List<ReferenceType> waitUntilClassesAreLoaded(VirtualMachine virtualMachine) {
		List<ReferenceType> referenceTypes = new ArrayList<>();
		while (referenceTypes.isEmpty()) {
			referenceTypes.addAll(virtualMachine.allClasses());
			if (!referenceTypes.isEmpty())
				break;
			try {
				Thread.sleep(1000);
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
		return referenceTypes.stream().filter(rt -> rt.name().startsWith(finalTargetPackage + ".")).toList();
	}

	private Set<ReferenceType> collectDefinedClasses(List<ReferenceType> referenceTypes) {
		Set<ReferenceType> result = new HashSet<>();
		for (ReferenceType referenceType : referenceTypes) {
			ClassLoaderReference classLoaderReference = referenceType.classLoader();
			if (Objects.isNull(classLoaderReference))
				continue;
			for (ReferenceType definedReferenceType : classLoaderReference.definedClasses()) {
				if (Objects.nonNull(definedReferenceType))
					result.add(definedReferenceType);
			}
		}
		return result;
	}

	private UniversalElementType determineElementType(ReferenceType referenceType) {
		if (referenceType instanceof ClassType)
			return UniversalElementType.CLASS;
		if (referenceType instanceof InterfaceType)
			return UniversalElementType.INTERFACE;
		return null;
	}

	public void detachDebugger() {
		if (Objects.isNull(TargetVirtualMachineRepresentation.getInstance().getVirtualMachine())) {
			return;
		}
		try {
			TargetVirtualMachineRepresentation.getInstance().getVirtualMachine().eventRequestManager()
					.deleteAllBreakpoints();

			TargetVirtualMachineRepresentation.getInstance().getVirtualMachine().allThreads()
					.forEach(threadReference -> {
						try {
							if (threadReference.suspendCount() > 0) {
								threadReference.resume();
							}
						} catch (Exception ignored) {
						}
					});
			TargetVirtualMachineRepresentation.getInstance().getVirtualMachine().dispose();

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
		for (Map.Entry<AbstractElementRepresentation.Tag, AbstractElementRepresentation> entry : targetApplicationSnapshot.entrySet()) {
		    AbstractElementRepresentation element = entry.getValue();

		    if (element instanceof ElementReference ref) {
		        AbstractElementRepresentation real = targetApplicationSnapshot.get(ref.getReferenceTag());
		        if (real != null) {
		            element = real; // теперь element — реальный объект
		        } else {
		            continue; // если в snapshot нет объекта, пропускаем
		        }
		    }

		    // проверяем, что это именно UniversalElementRepresentation
		    if (element instanceof UniversalElementRepresentation ue) {
		        ReferenceType referenceType = ue.getReferenceType();
		        if (referenceType != null && className.equals(referenceType.name())) {
		            return referenceType;
		        }
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
			return classType.newInstance(
					TargetVirtualMachineRepresentation.getInstance().getVirtualMachine().allThreads().get(0),
					constructor, List.of(), ClassType.INVOKE_SINGLE_THREADED);
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