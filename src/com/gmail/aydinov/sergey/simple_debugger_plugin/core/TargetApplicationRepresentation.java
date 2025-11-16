package com.gmail.aydinov.sergey.simple_debugger_plugin.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.BreakpointWrapper;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationClassOrInterfaceRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationElementType;
import com.sun.jdi.ClassType;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequestManager;

public class TargetApplicationRepresentation {

	
	private final Map<ReferenceType, TargetApplicationElementRepresentation> referencesAtClassesAndInterfaces = new ConcurrentHashMap<ReferenceType, TargetApplicationElementRepresentation>();
	//private final Set<BreakpointWrapper> breakpoints = ConcurrentHashMap.newKeySet();
	private final TargetApplicationBreakepointRepresentation targetApplicationBreakepointRepresentation; 

	public TargetApplicationRepresentation(IBreakpointManager iBreakpointManager, EventRequestManager eventRequestManager, VirtualMachine virtualMachine) {
		
		targetApplicationBreakepointRepresentation = new TargetApplicationBreakepointRepresentation(iBreakpointManager, eventRequestManager, virtualMachine);
	}

	public TargetApplicationBreakepointRepresentation getTargetApplicationBreakepointRepresentation() {
		return targetApplicationBreakepointRepresentation;
	}

	public Map<ReferenceType, TargetApplicationElementRepresentation> getReferencesAtClassesAndInterfaces() {
		return referencesAtClassesAndInterfaces;
	}

	public List<? extends TargetApplicationElementRepresentation> getTargetApplicationStatus() {
		return referencesAtClassesAndInterfaces.values().stream().collect(Collectors.toList());
	}

	public void refreshReferencesToClassesOfTargetApplication(VirtualMachine virtualMachine) {
		referencesAtClassesAndInterfaces.clear();
		System.out.println("Target class not loaded yet. Waiting...");
		List<ReferenceType> loadedClassesAndInterfaces = new ArrayList<ReferenceType>();
		while (loadedClassesAndInterfaces.isEmpty()) {
			loadedClassesAndInterfaces.addAll(virtualMachine.allClasses());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				continue;
			}
		}
		loadedClassesAndInterfaces = loadedClassesAndInterfaces.stream().filter(lci -> lci.name().contains("target"))
				.toList();
		System.out.println("Loaded " + loadedClassesAndInterfaces.size() + " classes.");
		Set<ReferenceType> references = loadedClassesAndInterfaces.stream().filter(clr -> Objects.nonNull(clr))
				.map(clr -> clr.classLoader()).filter(clr -> Objects.nonNull(clr))
				.flatMap(rt -> rt.definedClasses().stream()).collect(Collectors.toSet());
		Optional<TargetApplicationElementType> targetApplicationElementTypeOptional;
		for (ReferenceType referenceType : references) {
			targetApplicationElementTypeOptional = Optional.empty();
			if (referenceType instanceof ClassType) {
				targetApplicationElementTypeOptional = Optional.of(TargetApplicationElementType.CLASS);
			} else if (referenceType instanceof InterfaceType) {
				targetApplicationElementTypeOptional = Optional.of(TargetApplicationElementType.INTERFACE);
			}
			targetApplicationElementTypeOptional.ifPresent(type -> referencesAtClassesAndInterfaces.put(referenceType,
					new TargetApplicationClassOrInterfaceRepresentation(referenceType.name(), type,
							referenceType.allMethods().stream().collect(Collectors.toSet()),
							referenceType.allFields().stream().collect(Collectors.toSet()))));
		}
		System.out.println("referencesAtClasses: " + referencesAtClassesAndInterfaces.size());
	}

}
