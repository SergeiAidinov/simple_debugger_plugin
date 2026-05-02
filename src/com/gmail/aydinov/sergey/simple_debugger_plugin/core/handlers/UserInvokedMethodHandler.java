package com.gmail.aydinov.sergey.simple_debugger_plugin.core.handlers;

import java.util.List;
import java.util.Objects;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetVirtualMachineRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.data_model.TargetApplicationRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.DebugSessionImpl;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.inspection.HandlerContext;
import com.gmail.aydinov.sergey.simple_debugger_plugin.core.interfaces.UIEventHandler;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.UserInvokedMethodEventDTO;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.debug_event.DebugEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.AbstractUIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event.UIEvent;
import com.gmail.aydinov.sergey.simple_debugger_plugin.utils.DebugUtils;
import com.sun.jdi.ClassType;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;

public class UserInvokedMethodHandler implements UIEventHandler {

	@Override
	public boolean handle(HandlerContext abstractUIEventContext, AbstractUIEvent abstractSimpleDebuggerUIEvent) {
		UIEvent<UserInvokedMethodEventDTO> userInvokedMethodEventDTO = (UIEvent<UserInvokedMethodEventDTO>) abstractSimpleDebuggerUIEvent;
		return false;
	}
	
	private void invokeMethod(UserInvokedMethodEventDTO invokeEvent, BreakpointEvent breakpointEvent,
			StackFrame currentFrame) {
//		try {
//			List<Value> methodArguments = DebugUtils
//					.parseArguments(TargetVirtualMachineRepresentation.getInstance().getVirtualMachine(), invokeEvent);
//			ReferenceType referenceType = TargetApplicationRepresentation.getInstance()
//					.findReferenceTypeForClass(invokeEvent.getTargetClass());
//			Method method = referenceType.methodsByName(invokeEvent.getMethod().getMethodName()).get(0);
//			ObjectReference instance = !method.isStatic()
//					? TargetApplicationRepresentation.getInstance().createObjectInstance((ClassType) referenceType)
//					: null;
//			Value result = Objects.nonNull(instance)
//					? instance.invokeMethod(breakpointEvent.thread(),
//							method, methodArguments, ObjectReference.INVOKE_SINGLE_THREADED)
//					: ((ClassType) referenceType).invokeMethod(
//							TargetVirtualMachineRepresentation.getInstance().getVirtualMachine().allThreads().get(0), method,
//							methodArguments, ClassType.INVOKE_SINGLE_THREADED);
//			DebugSessionImpl.methodInvocationResult.set(String.valueOf(result));
//		} catch (Exception exception) {
//			exception.printStackTrace();
//		}
	}

}
