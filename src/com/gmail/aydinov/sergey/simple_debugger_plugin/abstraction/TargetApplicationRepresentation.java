package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
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
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.CurrentRole;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.UniversalElementType;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation.ValueCategory;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.BreakpointSubscriberRegistrar;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.PairDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TripletDTO;
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

	private final Map<AbstractElementRepresentation.Tag, UniversalElementRepresentation> topLevelElements = new ConcurrentHashMap<>();
	private final Map<AbstractElementRepresentation.Tag, AbstractElementRepresentation> subordinates = new ConcurrentHashMap<>();
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

	public PairDTO<Map<Tag, UniversalElementRepresentation>, Map<Tag, AbstractElementRepresentation>> getTargetApplicationSnapshot() {
		System.out.println("SNAPSOT BEGINNING: ");
		topLevelElements.values().stream().forEach(System.out::println);
		subordinates.values().stream().forEach(System.out::println);
		System.out.println("END OF SNAPSOT");
		return PairDTO.of(topLevelElements, subordinates);
	}

	public void takeSnapshotOfTargetApplication(VirtualMachine virtualMachine, BreakpointEvent breakpointEvent) {
		subordinates.clear();
		topLevelElements.clear();
		visitedElements.clear();
		SimpleDebuggerLogger.info("Waiting for target classes to load...");

		// 1. Ждем загрузки всех классов
		List<ReferenceType> loadedReferenceTypes = filterTargetClasses(waitUntilClassesAreLoaded(virtualMachine));
		// System.out.println(loadedReferenceTypes);
		loadTopLevelElements(loadedReferenceTypes, breakpointEvent);
		for (Entry<Tag, UniversalElementRepresentation> entry : topLevelElements.entrySet()) {
			recursievlyPopulateElements(PairDTO.of(entry.getKey(), entry.getValue()), breakpointEvent, 1);
		}
		if (Objects.nonNull(breakpointEvent))
			addLocalVariables(virtualMachine, breakpointEvent);
	}

	private void recursievlyPopulateElements(PairDTO<Tag, UniversalElementRepresentation> pairDTO,
			BreakpointEvent breakpointEvent, int level) {
		UniversalElementRepresentation parentElement = pairDTO.getSecond();
		ObjectReference objRef = parentElement.getObjectReference();

		if (objRef != null) {
			long uniqueId = objRef.uniqueID();
			if (visitedElements.containsKey(uniqueId)) {
				visitedElements.put(uniqueId, parentElement.getTag());
			}
		}

		for (Method method : parentElement.getReferenceType().allMethods()) {

			if (shouldSkipMethod(method))
				continue;
			List<String> args = method.argumentTypeNames();
			String methodArgs = String.join(", ", args);

			UniversalElementRepresentation methodElement = UniversalElementRepresentation.builder()
					.referenceType(parentElement.getReferenceType()).objectReference(parentElement.getObjectReference())
					.elementName(method.name() + "()").additionalInfo(method.returnTypeName())
					.elementType(UniversalElementRepresentation.UniversalElementType.METHOD)
					.currentRole(UniversalElementRepresentation.CurrentRole.INNER)
					.value(parentElement.getAdditionalInfo() + "." + method.name() + "(" + methodArgs + ")")
					.isStatic(method.isStatic())
					.valueCategory(UniversalElementRepresentation.ValueCategory.NOT_SPECIFIED)
					.typeOrReturnType(method.returnTypeName()).uniqueId(UUID.randomUUID())
					.parentUniqueId(parentElement.getTag().getUniqueId()).level(parentElement.getLevel() + 1).build();
			// System.out.println("METhOD FOUND: " + methodElement.toString());
			subordinates.put(methodElement.getTag(), methodElement);
		}

		for (Field field : parentElement.getReferenceType().allFields()) {
			addField(pairDTO.getSecond(), field, breakpointEvent, level);
		}
	}

	private void addField(UniversalElementRepresentation parentElement, Field field, BreakpointEvent breakpointEvent,
			int level) {

		Value value = null;
		ObjectReference instance = parentElement.getObjectReference();

		try {
			if (field.isStatic()) {
				value = field.declaringType().getValue(field);
			} else if (instance != null) {
				value = instance.getValue(field);
			}
		} catch (Exception e) {
			SimpleDebuggerLogger.warn("Failed to get value for field: " + field.name());
			return;
		}

		ObjectReference valueObj = (value instanceof ObjectReference) ? (ObjectReference) value : null;

		String valueText = (value == null) ? "<null>" : value.toString();

		UniversalElementRepresentation fieldElement = UniversalElementRepresentation.builder()
				.referenceType(valueObj != null ? valueObj.referenceType() : parentElement.getReferenceType())
				.objectReference(valueObj).elementName(field.name()).additionalInfo(field.typeName())
				.elementType(UniversalElementType.FIELD).currentRole(CurrentRole.INNER).value(valueText)
				.isStatic(field.isStatic()).valueCategory(DebugUtils.determineValueCategory(value))
				.typeOrReturnType(field.typeName()).uniqueId(UUID.randomUUID())
				.parentUniqueId(parentElement.getTag().getUniqueId()).level(level).build();

		subordinates.put(fieldElement.getTag(), fieldElement);

// 🔒 Ограничения
		if (valueObj == null || level >= 5 || !shouldExpand(valueObj)) {
			return;
		}

		long objId = valueObj.uniqueID();

// 🔥 КЛЮЧ: проверка ДО рекурсии
		if (visitedElements.containsKey(objId)) {
			return;
		}
		visitedElements.put(objId, fieldElement.getTag());

// 👉 Рекурсивно раскрываем ПОЛЯ ОБЪЕКТА
		for (Field innerField : valueObj.referenceType().allFields()) {
			addField(fieldElement, innerField, breakpointEvent, level + 1);
		}
	}

	private List<ReferenceType> waitUntilClassesAreLoaded(VirtualMachine virtualMachine) {
		List<ReferenceType> referenceTypes = new ArrayList<>();
		while (referenceTypes.isEmpty()) {
			referenceTypes.addAll(virtualMachine.allClasses());
			if (!referenceTypes.isEmpty())
				break;
			try {
				Thread.sleep(300);
			} catch (InterruptedException ignored) {
			}
		}
		return referenceTypes;
	}

	private boolean shouldExpand(ObjectReference objRef) {
		if (objRef == null)
			return false;

		String typeName = objRef.referenceType().name();

		// ❌ Не лезем в String
		if ("java.lang.String".equals(typeName))
			return false;

		// ❌ Не лезем в примитивные обёртки
		if (typeName.startsWith("java.lang.")) {
			switch (typeName) {
			case "java.lang.Integer":
			case "java.lang.Long":
			case "java.lang.Boolean":
			case "java.lang.Byte":
			case "java.lang.Short":
			case "java.lang.Character":
			case "java.lang.Double":
			case "java.lang.Float":
				return false;
			}
		}

		// ❌ Не лезем в стандартные классы JDK
		if (typeName.startsWith("java.") || typeName.startsWith("javax.") || typeName.startsWith("jdk.")
				|| typeName.startsWith("sun.")) {
			return false;
		}

		// Всё остальное — пользовательские классы, можно раскрывать
		return true;
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

	private void loadTopLevelElements(List<ReferenceType> loadedReferenceTypes, BreakpointEvent breakpointEvent) {
		for (ReferenceType refType : loadedReferenceTypes) {

			UniversalElementType elementType = DebugUtils.determineUniversalElementType(refType);
			if (elementType == null)
				continue;

			// ✅ всегда создаём класс
			ObjectReference thisObject = null;
			if (Objects.nonNull(breakpointEvent)) {
				StackFrame frame = null;
				try {
					frame = breakpointEvent.thread().frame(0);
				} catch (IncompatibleThreadStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				thisObject = frame.thisObject();
			}

			UniversalElementRepresentation topLevelElement = UniversalElementRepresentation.builder()
					.referenceType(refType).objectReference(thisObject)
					.elementName(DebugUtils.extractSimpleName(refType.name())).additionalInfo(refType.name())
					.elementType(elementType).currentRole(CurrentRole.OUTER).value(refType.name())
					.isStatic(refType.isStatic()).valueCategory(ValueCategory.USER_OBJECT)
					.typeOrReturnType(refType.name()).uniqueId(UUID.randomUUID()).parentUniqueId(null).build();

			topLevelElements.put(topLevelElement.getTag(), topLevelElement);
		}
	}

	private void populateMembersForClass(BreakpointEvent breakpointEvent) {
		for (UniversalElementRepresentation topLevelElement : topLevelElements.values()) {
			List<UniversalElementRepresentation> tops = new ArrayList();
			tops.add(topLevelElement);
			loadMembers(tops, topLevelElement.tag.getParentId(), breakpointEvent);
		}
	}

	private void loadMembers(List<UniversalElementRepresentation> universalElementRepresentations, UUID parentId,
			BreakpointEvent breakpointEvent) {

		for (UniversalElementRepresentation universalElementRepresentation : universalElementRepresentations) {
			ObjectReference objectReferenceField = universalElementRepresentation.getObjectReference();
			if (objectReferenceField == null)
				continue;

			for (Field field : objectReferenceField.referenceType().fields()) {
				Value value = objectReferenceField.getValue(field);

				// System.out.println(field.name() + " = " + value);
				// System.out.println();
			}

			long objId = getObjectUniqueId(universalElementRepresentation.getObjectReference());
			for (Method method : universalElementRepresentation.getReferenceType().allMethods()) {
				try {
					if (method.isSynthetic() || method.name().equals("<init>") || method.name().equals("<clinit>"))
						continue;
					if (method.declaringType().name().equals("java.lang.Object"))
						continue;
					if (method.declaringType().name().startsWith("java."))
						continue;

					String methodArgs = method.argumentTypes().stream().map(Type::name)
							.collect(Collectors.joining(","));

					UniversalElementRepresentation methodElement = UniversalElementRepresentation.builder()
							.referenceType(universalElementRepresentation.getReferenceType())
							.objectReference(universalElementRepresentation.getObjectReference())
							.elementName(universalElementRepresentation.getElementName() + "." + method.name())
							.additionalInfo(method.returnTypeName())
							.elementType(UniversalElementRepresentation.UniversalElementType.METHOD)
							.currentRole(UniversalElementRepresentation.CurrentRole.INNER)
							.value(universalElementRepresentation.getAdditionalInfo() + "." + method.name() + "("
									+ methodArgs + ")")
							.isStatic(method.isStatic())
							.valueCategory(UniversalElementRepresentation.ValueCategory.NOT_SPECIFIED)
							.typeOrReturnType(method.returnTypeName()).uniqueId(UUID.randomUUID())
							.parentUniqueId(universalElementRepresentation.getTag().getUniqueId()).build();

					subordinates.put(methodElement.getTag(), methodElement);

				} catch (Exception ignored) {
				}
				// targetApplicationSnapshot.put(reference.getTag(), reference);
			}
		}

		return;
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
		if (method.isSynthetic() || method.name().equals("<init>") || method.name().equals("<clinit>"))
			return true;
		if (method.declaringType().name().equals("java.lang.Object"))
			return true;
		if (method.declaringType().name().startsWith("java."))
			return true;

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

//		subordinates.values().stream().filter(e -> e instanceof UniversalElementRepresentation)
//				.map(e -> (UniversalElementRepresentation) e)
//				.filter(e -> e.getElementType().equals(UniversalElementType.METHOD))
//				.forEach(e -> System.out.println("METHOD: " + e.getValue()));
		// .filter(e -> Objects.equals(e.getElementName(), type.name())).findAny();

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
				valueText = q == -1 ? DebugUtils.getLocalVariableValueAsString(val) : "size:" + q + "; ";
				// System.out.println("INNER COLLECTIONS: " + q);
			}
		}
		return true;
	}

	private long getObjectUniqueId(Object obj) {
		if (obj instanceof ObjectReference objectRef) {
			return objectRef.uniqueID();
		}
		// Для ReferenceType (классы) можно использовать hashCode +
		// System.identityHashCode
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
		return !name.startsWith("java.") && !name.startsWith("javax.") && !name.startsWith("jdk.")
				&& !name.startsWith("sun.");
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

}