package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.List;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TargetApplicationElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.event.SimpleDebuggerEventTypes;

/**
 * Event representing that the debugger has stopped at a breakpoint. Contains
 * information about the location, local variables, fields, stack trace, target
 * application elements, method calls, and any method invocation result.
 * <p>
 * Author: Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class DebugStoppedAtBreakpointDTO {

	private final String className;
	private final String methodName;
	private final int lineNumber;
	private final List<FieldOrVariableDTO> locals;
	private final List<FieldOrVariableDTO> fields;
	private final String stackTrace;
	private final List<TargetApplicationElementRepresentation> targetApplicationElements;
	private final String resultOfMethodInvocation;
	private final List<MethodCallInStackDTO> methodCallInStacks;

	/**
	 * Creates a new event representing a breakpoint stop.
	 *
	 * @param type                      the type of the debugger event
	 * @param className                 name of the class where the breakpoint
	 *                                  occurred
	 * @param methodName                name of the method where the breakpoint
	 *                                  occurred
	 * @param lineNumber                line number of the breakpoint
	 * @param fields                    list of field variables
	 * @param locals                    list of local variables
	 * @param stackTrace                the full stack trace
	 * @param targetApplicationElements list of target application elements
	 * @param methodCallInStacks        list of method calls in the stack
	 * @param resultOfMethodInvocation  result of any invoked method (if applicable)
	 */
	private DebugStoppedAtBreakpointDTO(String className, String methodName,
			int lineNumber, List<FieldOrVariableDTO> fields, List<FieldOrVariableDTO> locals, String stackTrace,
			List<TargetApplicationElementRepresentation> targetApplicationElements,
			List<MethodCallInStackDTO> methodCallInStacks, String resultOfMethodInvocation) {
		//super(type);
		this.className = className;
		this.methodName = methodName;
		this.lineNumber = lineNumber;
		this.fields = fields;
		this.locals = locals;
		this.stackTrace = stackTrace;
		this.targetApplicationElements = targetApplicationElements;
		this.methodCallInStacks = methodCallInStacks;
		this.resultOfMethodInvocation = resultOfMethodInvocation;
	}

	// ---------------- GETTERS ----------------

	public String getClassName() {
		return className;
	}

	public String getMethodName() {
		return methodName;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public List<FieldOrVariableDTO> getLocals() {
		return locals;
	}

	public List<FieldOrVariableDTO> getFields() {
		return fields;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public List<TargetApplicationElementRepresentation> getTargetApplicationElements() {
		return targetApplicationElements;
	}

	public List<MethodCallInStackDTO> getMethodCallInStacks() {
		return methodCallInStacks;
	}

	public String getResultOfMethodInvocation() {
		return resultOfMethodInvocation;
	}

	// ---------------- BUILDER ----------------

	public static class Builder {
		//private SimpleDebuggerEventTypes.EventType type;
		private String className;
		private String methodName;
		private int lineNumber;
		private List<FieldOrVariableDTO> locals;
		private List<FieldOrVariableDTO> fields;
		private String stackTrace;
		private List<TargetApplicationElementRepresentation> targetApplicationElements;
		private List<MethodCallInStackDTO> methodCallInStacks;
		private String resultOfMethodInvocation;

//		public Builder type(SimpleDebuggerEventTypes.EventType type) {
//			this.type = type;
//			return this;
//		}

		public Builder className(String className) {
			this.className = className;
			return this;
		}

		public Builder methodName(String methodName) {
			this.methodName = methodName;
			return this;
		}

		public Builder lineNumber(int lineNumber) {
			this.lineNumber = lineNumber;
			return this;
		}

		public Builder locals(List<FieldOrVariableDTO> locals) {
			this.locals = locals;
			return this;
		}

		public Builder fields(List<FieldOrVariableDTO> fields) {
			this.fields = fields;
			return this;
		}

		public Builder stackTrace(String stackTrace) {
			this.stackTrace = stackTrace;
			return this;
		}

		public Builder targetApplicationElements(List<TargetApplicationElementRepresentation> elements) {
			this.targetApplicationElements = elements;
			return this;
		}

		public Builder methodCallInStacks(List<MethodCallInStackDTO> methodCallInStacks) {
			this.methodCallInStacks = methodCallInStacks;
			return this;
		}

		public Builder resultOfMethodInvocation(String result) {
			this.resultOfMethodInvocation = result;
			return this;
		}

		public DebugStoppedAtBreakpointDTO build() {
			return new DebugStoppedAtBreakpointDTO(className, methodName, lineNumber, fields, locals,
					stackTrace, targetApplicationElements, methodCallInStacks, resultOfMethodInvocation);
		}
	}
}
