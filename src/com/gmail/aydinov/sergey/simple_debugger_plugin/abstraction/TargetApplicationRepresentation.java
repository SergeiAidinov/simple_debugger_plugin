package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
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
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationMethodDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationMethodParameterDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.gmail.aydinov.sergey.simple_debugger_plugin.DebugConfiguration;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.CurrentRole;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StringReference;
import com.sun.jdi.Type;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequestManager;

public class TargetApplicationRepresentation {

	private final Map<UUID, UniversalElementRepresentation> targetApplicationSnapshot = new ConcurrentHashMap<>();
	private final TargetApplicationBreakpointRepresentation targetApplicationBreakepointRepresentation;
	private final VirtualMachine virtualMachine;
	private final DebugConfiguration debugConfiguration;

	public TargetApplicationRepresentation(IBreakpointManager iBreakpointManager,
			EventRequestManager eventRequestManager, VirtualMachine virtualMachine,
			BreakpointSubscriberRegistrar breakpointSubscriberRegistrar, DebugConfiguration debugConfiguration) {
		this.targetApplicationBreakepointRepresentation = new TargetApplicationBreakpointRepresentation(
				iBreakpointManager, virtualMachine);
		breakpointSubscriberRegistrar.register(targetApplicationBreakepointRepresentation);
		this.virtualMachine = virtualMachine;
		this.debugConfiguration = debugConfiguration;
	}

	public TargetApplicationBreakpointRepresentation getTargetApplicationBreakepointRepresentation() {
		return targetApplicationBreakepointRepresentation;
	}

	public Map<UUID, UniversalElementRepresentation> getTargetApplicationSnapshot() {
		return targetApplicationSnapshot;
	}

	public void refreshReferencesToClassesOfTargetApplication(VirtualMachine virtualMachine) {
		targetApplicationSnapshot.clear();
		SimpleDebuggerLogger.info("Waiting for target classes to load...");

		// 1. Ждём, пока классы загрузятся
		List<ReferenceType> loadedReferenceTypes = waitUntilClassesAreLoaded(virtualMachine);

		// 2. Фильтруем target-классы
		List<ReferenceType> targetClasses = filterTargetClasses(loadedReferenceTypes);

		SimpleDebuggerLogger.info("Loaded " + targetClasses.size() + " classes.");

		// 3. Собираем классы, определённые class loader'ами
		Set<ReferenceType> definedByLoaders = collectDefinedClasses(targetClasses);

		// 4. Обрабатываем каждый top-level элемент
		for (ReferenceType referenceType : definedByLoaders) {
			UniversalElementType elementType = determineElementType(referenceType);
			if (elementType == null)
				continue;

			// Создаём top-level элемент через фабрику
			UUID topLevelUiid = UUID.randomUUID();
			UniversalElementRepresentation topLevelElement = UniversalElementRepresentation.builder()
					.uniqueId(topLevelUiid)
					.parentUniqueId(null)
					.referenceType(referenceType)
					.elementName(DebugUtils.extractSimpleName(referenceType.name()))
					.additionalInfo(referenceType.name())
					.elementType(DebugUtils.determineUniversalElementType(referenceType))
					.currentRole(CurrentRole.OUTER)
					.fullQualifiedName(referenceType.name())
					.build();
			targetApplicationSnapshot.put(topLevelUiid, topLevelElement);
					
		}
		for (UniversalElementRepresentation topLevelElement : targetApplicationSnapshot.values()) {
			populateInnerElements(topLevelElement, topLevelElement.getReferenceType());
		}
			
		SimpleDebuggerLogger.info("LOADED TOP-LEVEL ELEMENTS: " + targetApplicationSnapshot.size());
	}

	private void populateInnerElements(UniversalElementRepresentation parentElement, ReferenceType refType) {
	    if (parentElement == null || refType == null) return;

	    UUID parentId = parentElement.getTag().getUniqueId();

	    // 🔹 Получаем экземпляр (если есть)
	    ObjectReference instance = null;

	    if (refType instanceof ClassType classType) {
	        try {
	            List<ObjectReference> instances = classType.instances(1);
	            if (!instances.isEmpty()) {
	                instance = instances.get(0);
	            }
	        } catch (Exception ignored) {}
	    }

	    // ---------------- Fields ----------------
	    for (Field field : refType.allFields()) {
	        try {
	            if (field.isSynthetic()) continue;

	            boolean isStatic = field.isStatic();

	            UniversalElementRepresentation.UniversalElementType elementType =
	                    isStatic
	                            ? UniversalElementRepresentation.UniversalElementType.STATIC_FIELD
	                            : UniversalElementRepresentation.UniversalElementType.NON_STATIC_FIELD;

	            UniversalElementRepresentation.CurrentRole role =
	                    UniversalElementRepresentation.CurrentRole.INNER;

	            UniversalElementRepresentation.ValueCategory category =
	                    determineValueCategory(field.typeName());

	            // 🔹 Получаем значение (только примитивы и String)
	            
	            String value = field.name();

	            if (category == ValueCategory.PRIMITIVE
	                    || category == ValueCategory.STRING) {
	                value = extractPrimitiveOrStringAsText(field, instance);
	            }
	            
	            //String value = extractPrimitiveOrStringAsText(field, instance);

	            UniversalElementRepresentation fieldElement =
	                    UniversalElementRepresentation.builder()
	                            .uniqueId(UUID.randomUUID())
	                            .parentUniqueId(parentId)
	                            .referenceType(refType)
	                            .elementName(field.name())
	                            .additionalInfo(DebugUtils.extractSimpleName(field.typeName()))
	                            .elementType(elementType)
	                            .currentRole(role)
	                            .isStatic(isStatic)
	                            .valueCategory(category)
	                            .value(value) // ← теперь передаём реальное значение
	                            .fullQualifiedName(field.typeName())
	                            .build();

	            parentElement.getInnerElements().add(fieldElement);

	        } catch (Exception ignored) {}
	    }

	    // ---------------- Methods ----------------
	    for (Method method : refType.allMethods()) {
	        try {
	            if (method.isSynthetic()) continue;

	            String methodName = method.name();
	            if (methodName.equals("<init>") || methodName.equals("<clinit>")) continue;
	            if (isObjectMethodUnoverridden(refType, method)) continue;

	            String methodArgs = method.argumentTypes().stream()
	                    .map(Type::name)
	                    .collect(Collectors.joining(", "));

	            UniversalElementRepresentation methodElement =
	                    UniversalElementRepresentation.builder()
	                            .uniqueId(UUID.randomUUID())
	                            .parentUniqueId(parentId)
	                            .referenceType(refType)
	                            .elementName(methodName + "()")
	                            .additionalInfo(method.returnTypeName())
	                            .elementType(UniversalElementRepresentation.UniversalElementType.METHOD)
	                            .currentRole(UniversalElementRepresentation.CurrentRole.INNER)
	                            .isStatic(method.isStatic())
	                            .valueCategory(UniversalElementRepresentation.ValueCategory.UNKNOWN)
	                            .value(parentElement.getAdditionalInfo() + "." + methodName + "(" + methodArgs + ")")
	                            .fullQualifiedName(method.name())
	                            .build();

	            parentElement.getInnerElements().add(methodElement);

	        } catch (Exception ignored) {}
	    }
	}

	
	
	private String extractPrimitiveOrStringAsText(Field field, ObjectReference instance) {
	    if (field == null) {
	        return null;
	    }

	    Value value;

	    if (field.isStatic()) {
	        value = field.declaringType().getValue(field);
	    } else {
	        if (instance == null) {
	            return null;
	        }
	        value = instance.getValue(field);
	    }

	    if (value == null) {
	        return "null"; // можно вернуть null, если тебе так логически удобнее
	    }

	    // String
	    if (value instanceof StringReference stringRef) {
	        return stringRef.value();
	    }

	    // Любой примитив
	    if (value instanceof PrimitiveValue primitiveValue) {
	        return ((StringReference) primitiveValue).value().toString();
	    }

	    return "<null>";
	}

	// ---------------- Вспомогательный метод ----------------
	private UniversalElementRepresentation.ValueCategory determineValueCategory(String typeName) {
	    if (typeName == null) return UniversalElementRepresentation.ValueCategory.NULL;
	    if (typeName.equals("void")) return UniversalElementRepresentation.ValueCategory.NULL;
	    if (Set.of("byte", "short", "int", "long", "float", "double", "boolean", "char")
	            .contains(typeName)) return UniversalElementRepresentation.ValueCategory.PRIMITIVE;
	    if (Set.of("java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long",
	               "java.lang.Float", "java.lang.Double", "java.lang.Boolean", "java.lang.Character")
	            .contains(typeName)) return UniversalElementRepresentation.ValueCategory.WRAPPER;
	    if (typeName.equals("java.lang.String")) return UniversalElementRepresentation.ValueCategory.STRING;
	    if (typeName.startsWith("java.util.List") || typeName.startsWith("java.util.Set"))
	        return UniversalElementRepresentation.ValueCategory.COLLECTION;
	    if (typeName.endsWith("[]")) return UniversalElementRepresentation.ValueCategory.ARRAY;
	    if (typeName.startsWith("java.util.Map")) return UniversalElementRepresentation.ValueCategory.MAP;
	    return UniversalElementRepresentation.ValueCategory.USER_OBJECT;
	}
	
	private boolean isObjectMethodUnoverridden(ReferenceType refType, Method method) {
	    try {
	        // Если это сам Object, то ничего не пропускаем
	        if (refType.name().equals("java.lang.Object")) return false;

	        // Получаем классы Object в VM
	        List<ReferenceType> objectClasses = method.virtualMachine().classesByName("java.lang.Object");
	        if (objectClasses.isEmpty()) return false;

	        ReferenceType objectRef = objectClasses.get(0);

	        // Проверяем, есть ли у Object метод с такой же сигнатурой
	        for (Method objMethod : objectRef.allMethods()) {
	            if (objMethod.name().equals(method.name()) &&
	                objMethod.signature().equals(method.signature())) {
	                // Метод есть в Object, значит проверяем, переопределён ли он
	                // Если класс refType НЕ содержит свой метод с такой сигнатурой — значит не переопределён
	                for (Method classMethod : refType.allMethods()) {
	                    if (classMethod.name().equals(method.name()) &&
	                        classMethod.signature().equals(method.signature()) &&
	                        classMethod.declaringType().equals(refType)) {
	                        // Нашли метод именно в этом классе — значит переопределён
	                        return false;
	                    }
	                }
	                // Метод есть в Object, но не переопределён
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
			if (!referenceTypes.isEmpty()) {
				break;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException interruptedException) {
				SimpleDebuggerLogger.error(interruptedException.getMessage(), interruptedException);
			}
		}
		return referenceTypes;
	}

	private List<ReferenceType> filterTargetClasses(List<ReferenceType> referenceTypes) {
		// return referenceTypes;
		// Determine the target root package
		String targetPackage = debugConfiguration.getTargetRootPackage();
		if (targetPackage == null || targetPackage.isBlank()) {
			// Fallback to the package of the main class
			String mainClass = debugConfiguration.getMainClassName();
			int lastDot = mainClass.lastIndexOf('.');
			targetPackage = lastDot > 0 ? mainClass.substring(0, lastDot) : "";
		}
		String finalTargetPackage = targetPackage;
		// Filter classes belonging to the target package
		return referenceTypes.stream().filter(rt -> rt.name().startsWith(finalTargetPackage + ".")).toList();
	}

	private Set<ReferenceType> collectDefinedClasses(List<ReferenceType> referenceTypes) {
		Set<ReferenceType> result = new HashSet<>();

		for (ReferenceType referenceType : referenceTypes) {
			ClassLoaderReference classLoaderReference = referenceType.classLoader();

			if (Objects.isNull(classLoaderReference)) {
				continue;
			}

			for (ReferenceType definedReferenceType : classLoaderReference.definedClasses()) {

				if (Objects.nonNull(definedReferenceType)) {
					result.add(definedReferenceType);
				}
			}
		}
		return result;
	}

	private UniversalElementType determineElementType(ReferenceType referenceType) {
		if (referenceType instanceof ClassType) {
			return UniversalElementType.CLASS;
		}
		if (referenceType instanceof InterfaceType) {
			return UniversalElementType.INTERFACE;
		}
		return null;
	}

	private Set<TargetApplicationMethodDTO> buildMethodDTOs(ReferenceType referenceType) {
		Set<TargetApplicationMethodDTO> result = new TreeSet<>();

		for (Method method : referenceType.allMethods()) {

			if (method.isNative()) {
				continue;
			}
			if ("<init>".equals(method.name())) {
				continue;
			}

			TargetApplicationMethodDTO targetApplicationMethodDTO = createMethodDTO(method);

			if (Objects.nonNull(targetApplicationMethodDTO)) {
				result.add(targetApplicationMethodDTO);
			}
		}
		return result;
	}

	private TargetApplicationMethodDTO createMethodDTO(Method method) {
		try {
			List<com.sun.jdi.Type> argumentTypes = method.argumentTypes();
			List<com.sun.jdi.LocalVariable> argumentVariables = loadArgVars(method);

			List<TargetApplicationMethodParameterDTO> parameters = compileParameters(argumentTypes, argumentVariables);

			return new TargetApplicationMethodDTO(method.name(), method.returnType().toString(), parameters);

		} catch (ClassNotLoadedException classNotLoadedException) {
			classNotLoadedException.printStackTrace();
			return null;
		}
	}

	private List<com.sun.jdi.LocalVariable> loadArgVars(Method method) {
		try {
			return method.arguments();
		} catch (Exception exception) {
			return List.of();
		}
	}

	private List<TargetApplicationMethodParameterDTO> compileParameters(List<com.sun.jdi.Type> argumentTypes,
			List<com.sun.jdi.LocalVariable> argumentVariables) {
		List<TargetApplicationMethodParameterDTO> parameters = new ArrayList<>();

		for (int index = 0; index < argumentTypes.size(); index++) {
			com.sun.jdi.Type type = argumentTypes.get(index);

			String name = (index < argumentVariables.size()) ? argumentVariables.get(index).name() : "arg" + index;

			String typeName;
			try {
				typeName = type.name();
			} catch (Exception exception) {
				typeName = "";
			}

			if (Objects.nonNull(typeName) && typeName.contains("no class loader")) {
				typeName = "";
			}

			parameters.add(new TargetApplicationMethodParameterDTO(name, typeName));
		}
		return parameters;
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

	public ReferenceType findReferenceTypeForClass(
			UniversalElementRepresentation universalElementRepresentation) {
		if (Objects.isNull(universalElementRepresentation)) {
			return null;
		}

		String className = universalElementRepresentation.getElementName();

		for (Entry<UUID, UniversalElementRepresentation> entry : targetApplicationSnapshot.entrySet()) {

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
}