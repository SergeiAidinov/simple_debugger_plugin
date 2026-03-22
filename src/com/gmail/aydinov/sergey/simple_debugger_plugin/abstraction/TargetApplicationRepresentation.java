package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.ui_dto.LocalVariableShortDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.logging.SimpleDebuggerLogger;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
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

	private static final int MAX_LEVEL_RECURSION = 3;
	private final Map<AbstractElementRepresentation.Tag, UniversalElementRepresentation> topLevelElements = new ConcurrentHashMap<>();
	private final Map<AbstractElementRepresentation.Tag, AbstractElementRepresentation> subordinates = new ConcurrentHashMap<>();
//	private final Map<Long, AbstractElementRepresentation.Tag> visitedElements = new ConcurrentHashMap<>();
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
	//	visitedElements.clear();
		SimpleDebuggerLogger.info("Waiting for target classes to load...");

		// 1. Ждем загрузки всех классов
		List<ReferenceType> loadedReferenceTypes = filterTargetClasses(waitUntilClassesAreLoaded(virtualMachine));
		// System.out.println(loadedReferenceTypes);
		loadTopLevelElements(loadedReferenceTypes, breakpointEvent);

		Map<String, LocalVariableShortDTO> localsSnapshot = compileShortInfoAboutVariables(virtualMachine,
				breakpointEvent);

		for (Entry<Tag, UniversalElementRepresentation> entry : topLevelElements.entrySet()) {
			recursievlyPopulateElements(PairDTO.of(entry.getKey(), entry.getValue()), breakpointEvent, 1);
		}
		if (!localsSnapshot.isEmpty())
			addLocalVariables(virtualMachine, breakpointEvent, localsSnapshot);

//		System.out.println("SNAPSOT BEGINNING: ");
//		topLevelElements.values().stream().forEach(System.out::println);
//		subordinates.values().stream().forEach(System.out::println);
//		System.out.println("END OF SNAPSOT");
	}

	private Map<String, LocalVariableShortDTO> compileShortInfoAboutVariables(VirtualMachine virtualMachine,
			BreakpointEvent breakpointEvent) {
		if (Objects.isNull(breakpointEvent))
			return Collections.emptyMap();
		Map<String, LocalVariableShortDTO> shortInfo = new HashMap<String, LocalVariableShortDTO>();
		StackFrame frame;
		List<LocalVariable> locals;
		try {
			frame = breakpointEvent.thread().frame(0);
			locals = frame.visibleVariables();
		} catch (IncompatibleThreadStateException | AbsentInformationException e) {
			SimpleDebuggerLogger.error(e.getMessage(), e);
			return shortInfo;
		}
		Map<LocalVariable, Value> values = frame.getValues(locals);
		for (LocalVariable local : locals) {
			Value value = values.get(local);
			String name = local.name();
			String valueText;
			TripletDTO<String, String, String> data = null;
			String typeOrReturnType;

			// ---------- Для ObjectReference (коллекции, объекты) ----------
			if (value instanceof ObjectReference objRef) {
				// размер коллекции
				int collectionSize = DebugUtils.getCollectionSize(objRef, breakpointEvent);
				data = DebugUtils.determinCollectionType(objRef, breakpointEvent);

				valueText = collectionSize >= 0 ? "size:" + collectionSize
						: DebugUtils.getLocalVariableValueAsString(value);
				typeOrReturnType = DebugUtils.determineValueCategory(value) == ValueCategory.COLLECTION
						|| DebugUtils.determineValueCategory(value) == ValueCategory.MAP
								? DebugUtils.compileCollectionDescription(data)
								: objRef.referenceType().name();

			} else {
				// ---------- Для примитивов или null ----------
				valueText = DebugUtils.getLocalVariableValueAsString(value);

				// Если null, используем тип локальной переменной
				if (value == null) {
					typeOrReturnType = local.typeName();
				} else {
					// Для примитивов берем type() у Value
					typeOrReturnType = value.type().name();
				}
			}

			shortInfo.put(name, new LocalVariableShortDTO(name, local.typeName(), valueText,
					DebugUtils.determineValueCategory(value), typeOrReturnType, data));
		}
		return shortInfo;
	}

	private void recursievlyPopulateElements(PairDTO<Tag, UniversalElementRepresentation> pairDTO,
			BreakpointEvent breakpointEvent, int level) {
		UniversalElementRepresentation parentElement = pairDTO.getSecond();
		ObjectReference objRef = parentElement.getObjectReference();

		if (objRef != null) {
			long uniqueId = objRef.uniqueID();
//			if (!visitedElements.containsKey(uniqueId)) {
//				visitedElements.put(uniqueId, parentElement.getTag());
//			}
		}

		for (Method method : parentElement.getReferenceType().allMethods()) {

			if (shouldSkipMethod(method))
				continue;
			if (!method.declaringType().equals(parentElement.getReferenceType())) 
		        continue; // пропускаем методы из интерфейсов/суперклассов
		    
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
		if (level >= MAX_LEVEL_RECURSION) {
			return;
		}
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
		ValueCategory category = DebugUtils.determineValueCategory(value);
		if (List.of(ValueCategory.COLLECTION, ValueCategory.ARRAY, ValueCategory.MAP).contains(category)
				&& Objects.nonNull(breakpointEvent)) {
			int size = DebugUtils.getCollectionSize(valueObj, breakpointEvent);
			TripletDTO<String, String, String> data = DebugUtils.determinCollectionType(valueObj, breakpointEvent);
			String parameters = Objects.isNull(data.getThird()) ? "params.:<" + data.getSecond() + ">"
					: "params.:<" + data.getSecond() + ", " + data.getThird() + ">";
			valueText = parameters + ", " + data.getFirst() + ", size:" + size;
		}

		if (List.of(ValueCategory.COLLECTION, ValueCategory.MAP).contains(category)) {
			int size = (valueObj != null) ? DebugUtils.getCollectionSize(valueObj) : -1;
			String valueTextWithSize = "size: " + (size != -1 ? size : "?") + "; " + valueText;
			String typeOrReturnType = field.name();
			if (Objects.nonNull(breakpointEvent)) {
				TripletDTO<String, String, String> data = DebugUtils.determinCollectionType(valueObj, breakpointEvent);
				int collectionSize = DebugUtils.getCollectionSize(valueObj, breakpointEvent);
				String parameters = Objects.isNull(data.getThird()) ? "params.:<" + data.getSecond() + ">"
						: "params.:<" + data.getSecond() + ", " + data.getThird() + ">";
				valueTextWithSize = "size:" + collectionSize + ", " + parameters + ", " + data.getFirst();
			}
			UniversalElementRepresentation collectionElement = UniversalElementRepresentation.builder()
					.referenceType(valueObj != null ? valueObj.referenceType() : parentElement.getReferenceType())
					.objectReference(valueObj).elementName(field.name()).additionalInfo(field.typeName())
					.elementType(UniversalElementType.FIELD).currentRole(CurrentRole.INNER).value(valueTextWithSize)
					.isStatic(field.isStatic()).valueCategory(category).typeOrReturnType(typeOrReturnType)
					.uniqueId(UUID.randomUUID()).parentUniqueId(parentElement.getTag().getUniqueId()).level(level)
					.build();

			subordinates.put(collectionElement.getTag(), collectionElement);

		} else if (ValueCategory.ARRAY == category && (valueObj instanceof ArrayReference arrayRef)) {
			int size = arrayRef.length(); 
		    ArrayType arrayType = (ArrayType) valueObj.referenceType();
		    String elementType = arrayType.componentTypeName();
		    valueText = "size:" + size + ", elementType:" + elementType;
		    UniversalElementRepresentation arrayElement = UniversalElementRepresentation.builder()
		            .referenceType(valueObj.referenceType())
		            .objectReference(valueObj)
		            .elementName(field.name())
		            .additionalInfo(field.typeName())
		            .elementType(UniversalElementType.FIELD)
		            .currentRole(CurrentRole.INNER)
		            .value(valueText)
		            .isStatic(field.isStatic())
		            .valueCategory(ValueCategory.ARRAY)
		            .typeOrReturnType(field.typeName())
		            .uniqueId(UUID.randomUUID())
		            .parentUniqueId(parentElement.getTag().getUniqueId())
		            .level(level)
		            .build();
		    subordinates.put(arrayElement.getTag(), arrayElement);
		} else {
			// ---------------- Создаем элемент поля
			UniversalElementRepresentation fieldElement = UniversalElementRepresentation.builder()
					.referenceType(valueObj != null ? valueObj.referenceType() : parentElement.getReferenceType())
					.objectReference(valueObj).elementName(field.name()).additionalInfo(field.typeName())
					.elementType(UniversalElementType.FIELD).currentRole(CurrentRole.INNER).value(valueText)
					.isStatic(field.isStatic()).valueCategory(category).typeOrReturnType(field.typeName())
					.uniqueId(UUID.randomUUID()).parentUniqueId(parentElement.getTag().getUniqueId()).level(level)
					.build();

			subordinates.put(fieldElement.getTag(), fieldElement);

			if (Objects.nonNull(valueObj)) {
				long objId = valueObj.uniqueID();
//				if (visitedElements.containsKey(objId)) {
//					return;
//				}
			//	visitedElements.put(objId, parentElement.getTag());
				for (Field innerField : valueObj.referenceType().allFields()) {
					if (shouldExpand(valueObj)) {
						addField(fieldElement, innerField, breakpointEvent, level + 1);
					}
				}
			}
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
		ObjectReference thisObject = null;
		StackFrame frame = null;
		if (breakpointEvent != null) {
			try {
				frame = breakpointEvent.thread().frame(0);
				thisObject = frame.thisObject(); // объект текущего экземпляра
			} catch (IncompatibleThreadStateException e) {
				e.printStackTrace();
			}
		}

		for (ReferenceType refType : loadedReferenceTypes) {

			UniversalElementType elementType = DebugUtils.determineUniversalElementType(refType);
			if (elementType == null)
				continue;

			// Если текущий refType совпадает с объектом на стеке, используем его
			// ObjectReference
			ObjectReference refObject = thisObject;
			if (refObject != null && !Objects.equals(refObject.referenceType(), refType)) {
				refObject = null; // если объект не того типа, оставляем null
			}

			UniversalElementRepresentation topLevelElement = UniversalElementRepresentation.builder()
					.referenceType(refType).objectReference(refObject) // вот сюда кладем реальный объект
					.elementName(DebugUtils.extractSimpleName(refType.name())).additionalInfo(refType.name())
					.elementType(elementType).currentRole(CurrentRole.OUTER).value(refType.name())
					.isStatic(refType.isStatic()).valueCategory(ValueCategory.USER_OBJECT)
					.typeOrReturnType(refType.name()).uniqueId(UUID.randomUUID()).parentUniqueId(null).level(0).build();

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

	private boolean addLocalVariables(VirtualMachine virtualMachine, BreakpointEvent breakpointEvent,
			Map<String, LocalVariableShortDTO> localsSnapshot) {
		if (Objects.isNull(breakpointEvent))
			return false;
		Location location = breakpointEvent.location();
		Method method = location.method();
		if (method == null)
			return false;
		Optional<UniversalElementRepresentation> methodRepresentationOptional = subordinates.values().stream()
				.filter(e -> e instanceof UniversalElementRepresentation).map(e -> (UniversalElementRepresentation) e)
				.filter(e -> e.getElementType().equals(UniversalElementType.METHOD))
				.filter(e -> Objects.equals(e.getValue(), DebugUtils.toReadableSignature(method))).findAny();
		if (methodRepresentationOptional.isEmpty())
			return false;
		for (Entry<String, LocalVariableShortDTO> entry : localsSnapshot.entrySet()) {

			if (List.of(ValueCategory.COLLECTION, ValueCategory.ARRAY, ValueCategory.MAP)
					.contains(entry.getValue().getValueCategory())) {
				String parameters = Objects.isNull(entry.getValue().getData().getThird())
						? "params.:<" + entry.getValue().getData().getSecond() + ">"
						: "params.:<" + entry.getValue().getData().getSecond() + ", "
								+ entry.getValue().getData().getThird() + ">";
				String valueText = entry.getValue().getValue() + ", " + parameters + ", "
						+ entry.getValue().getData().getFirst();
				UniversalElementRepresentation localStructure = UniversalElementRepresentation.builder()
						.referenceType(methodRepresentationOptional.get().getReferenceType())
						.elementName(entry.getValue().getElementName())
						.additionalInfo(entry.getValue().getAdditionalInfo())
						.elementType(UniversalElementType.LOCAL_VARIABLE).currentRole(CurrentRole.LOCAL)
						.value(valueText).isStatic(false).valueCategory(entry.getValue().getValueCategory())
						.typeOrReturnType(entry.getValue().getTypeOrReturnType()).uniqueId(UUID.randomUUID())
						.parentUniqueId(methodRepresentationOptional.get().getTag().getUniqueId())
						.level(methodRepresentationOptional.get().getLevel() + 1).build();

				subordinates.put(localStructure.getTag(), localStructure);

			} else {
				UniversalElementRepresentation localPrimitive = UniversalElementRepresentation.builder()
						.referenceType(methodRepresentationOptional.get().getReferenceType())
						.elementName(entry.getValue().getElementName())
						.additionalInfo(entry.getValue().getAdditionalInfo())
						.elementType(UniversalElementType.LOCAL_VARIABLE).currentRole(CurrentRole.LOCAL)
						.value(entry.getValue().getValue()).isStatic(false)
						.valueCategory(entry.getValue().getValueCategory())
						.typeOrReturnType(entry.getValue().getTypeOrReturnType()).uniqueId(UUID.randomUUID())
						.parentUniqueId(methodRepresentationOptional.get().getTag().getUniqueId())
						.level(methodRepresentationOptional.get().getLevel() + 1).build();

				subordinates.put(localPrimitive.getTag(), localPrimitive);
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