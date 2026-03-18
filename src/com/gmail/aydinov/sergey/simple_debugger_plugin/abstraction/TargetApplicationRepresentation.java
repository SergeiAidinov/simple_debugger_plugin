package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
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
		return PairDTO.of(topLevelElements, subordinates);
	}

	public void takeSnapshotOfTargetApplication(VirtualMachine virtualMachine, BreakpointEvent breakpointEvent) {
		subordinates.clear();
		topLevelElements.clear();
		visitedElements.clear();
		SimpleDebuggerLogger.info("Waiting for target classes to load...");

		// 1. Ждем загрузки всех классов
		List<ReferenceType> loadedReferenceTypes = filterTargetClasses(waitUntilClassesAreLoaded(virtualMachine));
		System.out.println(loadedReferenceTypes);
		loadTopLevelElements(loadedReferenceTypes);
		for (Entry<Tag, UniversalElementRepresentation> entry : topLevelElements.entrySet()) {
			recursievlyPopulateElements(PairDTO.of(entry.getKey(), entry.getValue()), breakpointEvent);
		}
		// System.out.println(topLevelElements);
		populateMembersForClass(breakpointEvent);
		// System.out.println(targetApplicationSnapshot);
		// targetApplicationSnapshot.putAll(topLevelElements);
		// addLocalVariables(virtualMachine, breakpointEvent);
		// targetApplicationSnapshot.values().stream().forEach(e ->
		// System.out.println("MODEL: " + e));
		// System.out.println(targetApplicationSnapshot);
	}

	private void recursievlyPopulateElements(PairDTO<Tag, UniversalElementRepresentation> pairDTO,
			BreakpointEvent breakpointEvent) {
		for (Field field : pairDTO.getSecond().getReferenceType().allFields()) {
			boolean isStatic = field.isStatic();
			Value value = null;
			try {
				if (isStatic) {
					value = field.declaringType().getValue(field);
				} else {
					ObjectReference objectReference = pairDTO.getSecond().getObjectReference();
					if (objectReference != null) {
						value = objectReference.getValue(field);
					}
				}
			} catch (Exception e) {
				SimpleDebuggerLogger.warn("Failed to get value for field: " + field.name());
			}
			UniversalElementRepresentation universalElementRepresentation = UniversalElementRepresentation
					.buildElementForField(field, value, pairDTO.getSecond().getTag().getUniqueId(),
							pairDTO.getSecond().getObjectReference());
			Tag tag = universalElementRepresentation.getTag();
			subordinates.put(tag, universalElementRepresentation);
			System.out.println("FIELD: " + " = " + field.toString());
			System.out.println();

			if (value != null) {

				// 🔹 1. Примитив / String / boxed
				if (!(value instanceof ObjectReference)) {

					UniversalElementRepresentation valueElement = UniversalElementRepresentation.builder()
							.referenceType(null).objectReference(null).elementName("value")
							.additionalInfo(value.type() != null ? value.type().name() : "unknown")
							.elementType(DebugUtils.determineUniversalElementType(value)).currentRole(CurrentRole.INNER)
							.value(DebugUtils.getLocalVariableValueAsString(value)).isStatic(false)
							.valueCategory(DebugUtils.determineValueCategory(value))
							.typeOrReturnType(value.type() != null ? value.type().name() : "unknown")
							.uniqueId(UUID.randomUUID()).parentUniqueId(tag.getUniqueId()).build();

					subordinates.put(valueElement.getTag(), valueElement);

				} else {

					// 🔹 2. Объект
					ObjectReference childObj = (ObjectReference) value;
					long objId = getObjectUniqueId(childObj);
					ValueCategory category = DebugUtils.determineValueCategory(value);
					if (category == ValueCategory.COLLECTION || category == ValueCategory.MAP) {

					    int size = -1;
					    String collectionDescription = field.typeName(); // 👈 fallback

					    // 🔥 Runtime-инфа только если есть breakpoint
					    if (Objects.nonNull(breakpointEvent) && value instanceof ObjectReference) {

					        size = DebugUtils.getCollectionSize(childObj, breakpointEvent);

					        TripletDTO<String, String, String> triplet =
					                DebugUtils.determinCollectionType(childObj, breakpointEvent);

					        String compiled = DebugUtils.compileCollectionDescription(triplet);

					        if (compiled != null && !compiled.contains(DebugUtils.N_A)) {
					            collectionDescription = compiled;
					        }

					        visitedElements.put(getObjectUniqueId(childObj), tag);
					    }

					    UniversalElementRepresentation collectionElement =
					            UniversalElementRepresentation.builder()
					                    .referenceType(value instanceof ObjectReference obj ? obj.referenceType() : null)
					                    .objectReference(value instanceof ObjectReference obj ? obj : null)

					                    .elementName(field.name())

					                    // 🔥 size даже без breakpoint
					                    .additionalInfo(size >= 0 ? "size=" + size : "size=unknown")

					                    .elementType(UniversalElementType.CLASS)
					                    .currentRole(CurrentRole.INNER)

					                    .value(value != null ? value.toString() : "null")
					                    .isStatic(isStatic)

					                    .valueCategory(ValueCategory.COLLECTION)

					                    // 🔥 тип ВСЕГДА есть
					                    .typeOrReturnType(collectionDescription)

					                    .uniqueId(UUID.randomUUID())
					                    .parentUniqueId(tag.getUniqueId())
					                    .build();

					    subordinates.put(collectionElement.getTag(), collectionElement);

					    continue;
					
					}

					// 🔁 уже был → ссылка
					if (visitedElements.containsKey(objId)) {

						AbstractElementRepresentation.Tag existingTag = visitedElements.get(objId);

						AbstractElementRepresentation.Tag refTag = new AbstractElementRepresentation.Tag(
								UUID.randomUUID(), tag.getUniqueId());

						ElementReference reference = new ElementReference(refTag, existingTag, childObj);

						subordinates.put(refTag, reference);

					} else {

						// 🆕 создаём элемент объекта
						UniversalElementRepresentation childElement = UniversalElementRepresentation.builder()
								.referenceType(childObj.referenceType()).objectReference(childObj)
								.elementName(DebugUtils.extractSimpleName(childObj.referenceType().name()))
								.additionalInfo(childObj.referenceType().name())
								.elementType(UniversalElementRepresentation.UniversalElementType.CLASS)
								.currentRole(CurrentRole.INNER).value(childObj.toString()).isStatic(false)
								.valueCategory(DebugUtils.determineValueCategory(value))
								.typeOrReturnType(childObj.referenceType().name()).uniqueId(UUID.randomUUID())
								.parentUniqueId(tag.getUniqueId()).build();

						subordinates.put(childElement.getTag(), childElement);

						// 🔥 регистрируем ДО рекурсии
						visitedElements.put(objId, childElement.getTag());

						// 🔁 рекурсивно продолжаем ТЕМ ЖЕ методом
						if (shouldExpand(childObj)) {
							recursievlyPopulateElements(PairDTO.of(childElement.getTag(), childElement),
									breakpointEvent);
						}
					}
				}
			}

		}

		// 🔽 после обхода полей
		for (Method method : pairDTO.getSecond().getReferenceType().allMethods()) {
			if (shouldSkipMethod(method)) {
				continue;
			}
			UniversalElementRepresentation methodElement = UniversalElementRepresentation.buildElementForMethod(method,
					pairDTO.getSecond().getTag().getUniqueId(), pairDTO.getSecond().getObjectReference());
			subordinates.put(methodElement.getTag(), methodElement);
			System.out.println("METHOD: " + method.name());
		}

	}

	private boolean shouldExpand(ObjectReference obj) {
		if (obj == null)
			return false;

		String typeName = obj.referenceType().name();

		// ❌ не лезем в String
		if (typeName.equals("java.lang.String"))
			return false;

		// ❌ не лезем в boxed
		if (typeName.startsWith("java.lang.") && (typeName.contains("Integer") || typeName.contains("Long")
				|| typeName.contains("Boolean") || typeName.contains("Byte") || typeName.contains("Short")
				|| typeName.contains("Character") || typeName.contains("Double") || typeName.contains("Float"))) {
			return false;
		}

		// ❌ не лезем в JDK (можно ослабить при желании)
		if (typeName.startsWith("java.") || typeName.startsWith("jdk.")) {
			return false;
		}

		return true;
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

			UniversalElementType elementType = DebugUtils.determineUniversalElementType(refType);
			if (elementType == null)
				continue;

			// ✅ всегда создаём класс
			UniversalElementRepresentation topLevelElement = UniversalElementRepresentation.builder()
					.referenceType(refType).objectReference(null)
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

				System.out.println(field.name() + " = " + value);
				System.out.println();
			}

			long objId = getObjectUniqueId(universalElementRepresentation.getObjectReference());
			for (Method method : universalElementRepresentation.getReferenceType().allMethods()) {
				// Если уже посещали — создаём ссылку
				if (visitedElements.containsKey(objId)) {
//						AbstractElementRepresentation.Tag ownTag = new AbstractElementRepresentation.Tag(UUID.randomUUID(),
//								parentId);
//						AbstractElementRepresentation.Tag existingTag = visitedElements.get(objId);
//						ObjectReference objectReference = targetApplicationSnapshot.get(existingTag).getObjectReference();
//						ElementReference reference = new ElementReference(ownTag, existingTag, objectReference);
//						targetApplicationSnapshot.put(existingTag, universalElementRepresentation);
					continue;
				} else {

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
								.elementName(method.name() + "()").additionalInfo(method.returnTypeName())
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
		}

		return;
	}

	// Новый объект — регистрируем
//	UniversalElementRepresentation.Tag tag = new UniversalElementRepresentation.Tag(UUID.randomUUID(),
//			parentId);visitedElements.put(objId,tag);

//	for(
//	Method method:universalElementRepresentation.getReferenceType().methods())
//	{
//		if (shouldSkipMethod(method)) {
//			continue;
//		}
//
//		UniversalElementRepresentation methodElement = UniversalElementRepresentation.buildElementForMethod(method,
//				parentId, universalElementRepresentation.getObjectReference());
//		targetApplicationSnapshot.put(methodElement.getTag(), methodElement);
//		visitedElements.put(methodElement.getObjectReference().uniqueID(), methodElement.getTag());
//	}

//		if (obj instanceof ObjectReference objectReference) {
//			for (Field field : objectReference.referenceType().fields()) {
//				Value value = objectReference.getValue(field);
//
//				UniversalElementRepresentation member = UniversalElementRepresentation.buildElementForField(field,
//						value, parentId, objectReference);
//				targetApplicationSnapshot.put(member.getTag(), member);
//				visitedElements.put(member.getObjectReference().uniqueID(), member.getTag());
//				if (member.getValueCategory().equals(ValueCategory.COLLECTION)) {
//					List<Value> qq = DebugUtils.iterateThroughCollection(member.getObjectReference(), breakpointEvent);
//					qq.stream().forEach(e -> System.out.println("==> " + e.toString()));
//				}
//			}
//
//		} 
//	}

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

		UniversalElementRepresentation methodRepresentation = subordinates.values().stream().map(e -> {
			if (e instanceof ElementReference ref) {
				return (UniversalElementRepresentation) subordinates.get(ref.getReferenceTag());
			}
			return (e instanceof UniversalElementRepresentation ue) ? ue : null;
		}).filter(Objects::nonNull).filter(e -> e.getElementType() == UniversalElementType.METHOD).filter(e -> {
			if (e.getReferenceType() == null)
				return false;

			return e.getReferenceType().equals(type) && e.getElementName().equals(method.name())
					&& method.signature().equals(e.getAdditionalInfo()); // 🔥 ключ
		}).findFirst().orElse(null);
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

				valueText = q == -1 ? DebugUtils.getLocalVariableValueAsString(val) : "size:" + q + "; ";

				System.out.println("INNER COLLECTIONS: " + q);
			}

			UniversalElementRepresentation variable = UniversalElementRepresentation.builder().referenceType(null)
					.objectReference(objRef).elementName(local.name()).additionalInfo(local.typeName())
					.elementType(UniversalElementType.LOCAL_VARIABLE).currentRole(CurrentRole.INNER).value(valueText)
					.isStatic(false).valueCategory(DebugUtils.determineValueCategory(val))
					.typeOrReturnType(local.typeName()).uniqueId(UUID.randomUUID())
					.parentUniqueId(methodRepresentation.getTag().getUniqueId()).build();

			subordinates.put(variable.getTag(), variable);
			localVariables.add(variable);
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

	// Создание элемента для поля
//	private UniversalElementRepresentation buildElementForField(Field field, Value value, UUID parentId) {
//	    return UniversalElementRepresentation.builder()
//	        .elementName(field.name())
//	        .additionalInfo(field.typeName())
//	        .elementType(UniversalElementType.NON_STATIC_FIELD)
//	        .currentRole(CurrentRole.INNER)
//	        .value(value != null ? value.toString() : "null")
//	        .valueCategory(ValueCategory.NOT_SPECIFIED)
//	        .uniqueId(UUID.randomUUID())
//	        .parentUniqueId(parentId)
//	        .build();
//	}

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