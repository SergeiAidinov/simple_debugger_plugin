package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationMethodDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationMethodParameterDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.DebugConfiguration;
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

	private final Map<ReferenceType, TargetApplicationElementRepresentation> referencesAtClassesAndInterfaces = new ConcurrentHashMap<>();
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

	public Map<ReferenceType, TargetApplicationElementRepresentation> getReferencesAtClassesAndInterfaces() {
		return referencesAtClassesAndInterfaces;
	}

	public List<TargetApplicationElementRepresentation> getTargetApplicationElements() {
		return referencesAtClassesAndInterfaces.values().stream().collect(Collectors.toList());
	}

	public void refreshReferencesToClassesOfTargetApplication(VirtualMachine virtualMachine) {
		referencesAtClassesAndInterfaces.clear();
		SimpleDebuggerLogger.info("Target classes are not loaded yet. Waiting...");

		// 1. Wait until classes are loaded
		List<ReferenceType> loadedReferenceTypes = waitUntilClassesAreLoaded(virtualMachine);

		// 2. Filter classes containing the word "target"
		List<ReferenceType> targetClasses = filterTargetClasses(loadedReferenceTypes);

		SimpleDebuggerLogger.info("Loaded " + targetClasses.size() + " classes.");

		// 3. Collect classes defined by class loaders
		Set<ReferenceType> definedByLoaders = collectDefinedClasses(targetClasses);

		// 4. Process each class or interface
		for (ReferenceType referenceType : definedByLoaders) {

			TargetApplicationElementType type = determineElementType(referenceType);

			if (Objects.isNull(type)) {
				continue;
			}

			Set<TargetApplicationMethodDTO> methods = buildMethodDTOs(referenceType);

			Set<Field> fields = new HashSet<>(referenceType.allFields());

			referencesAtClassesAndInterfaces.put(referenceType,
					new TargetApplicationClassOrInterfaceRepresentation(referenceType.name(), type, methods, fields));
		}

		SimpleDebuggerLogger.info("LOADED CLASSES: " + referencesAtClassesAndInterfaces.size());
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
	    return referenceTypes.stream()
	            .filter(rt -> rt.name().startsWith(finalTargetPackage + "."))
	            .toList();
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

	private TargetApplicationElementType determineElementType(ReferenceType referenceType) {
		if (referenceType instanceof ClassType) {
			return TargetApplicationElementType.CLASS;
		}
		if (referenceType instanceof InterfaceType) {
			return TargetApplicationElementType.INTERFACE;
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
			TargetApplicationClassOrInterfaceRepresentation targetApplicationClassOrInterfaceRepresentation) {
		if (Objects.isNull(targetApplicationClassOrInterfaceRepresentation)) {
			return null;
		}

		String className = targetApplicationClassOrInterfaceRepresentation.getTargetApplicationElementName();

		for (Map.Entry<ReferenceType, TargetApplicationElementRepresentation> entry : referencesAtClassesAndInterfaces
				.entrySet()) {

			ReferenceType referenceType = entry.getKey();

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
