package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model;

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

import org.eclipse.debug.core.IBreakpointManager;

import com.gmail.aydinov.sergey.simple_debugger_plugin.DebugConfiguration;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.AbstractElementRepresentation.Tag;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationBreakpointRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.UniversalElementRepresentation;
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
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;


public class TargetApplicationRepresentation {

	public static final int DEAFAULT_LEVEL_RECURSION = 3;
	private final Map<AbstractElementRepresentation.Tag, UniversalElementRepresentation> topLevelElements = new ConcurrentHashMap<>();
	private final Map<AbstractElementRepresentation.Tag, AbstractElementRepresentation> subordinates = new ConcurrentHashMap<>();
//	private final Map<Long, AbstractElementRepresentation.Tag> visitedElements = new ConcurrentHashMap<>();
	private final DebugConfiguration debugConfiguration;
	private final TargetAplicantionElementsLoader elementsLoader = new TargetAplicantionElementsLoader(0);
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
//		System.out.println("SNAPSOT BEGINNING: ");
//		topLevelElements.values().stream().forEach(System.out::println);
//		subordinates.values().stream().forEach(System.out::println);
//		System.out.println("END OF SNAPSOT");
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
		Map<Tag, AbstractElementRepresentation> iterationSubordinates = new ConcurrentHashMap<>();

		for (Entry<Tag, UniversalElementRepresentation> entry : topLevelElements.entrySet()) {
		    iterationSubordinates =
		        elementsLoader.recursievlyPopulateSubordinatesElements(
		            PairDTO.of(entry.getKey(), entry.getValue()),
		            breakpointEvent,
		            iterationSubordinates,
		            1, false
		        );
		}
		subordinates.putAll(iterationSubordinates);
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