package com.gmail.aydinov.sergey.simple_debugger_plugin.event;

import java.util.List;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.MethodCallInStack;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationElementRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.VariableDTO;

public class SimpleDebugEventDTO extends AbstractSimpleDebugEventDTO{

    //private final SimpleDebugEventType type;
    private final String className;
    private final String methodName;
    private final int lineNumber;
    private final List<VariableDTO> locals;
    private final List<VariableDTO> fields;
    private final String stackTrace;
    private final List<TargetApplicationElementRepresentation> targetApplicationElementRepresentationList;
    private final String resultOfMethodInvocation;
    private final List<MethodCallInStack> methodCallInStacks;

    public SimpleDebugEventDTO(SimpleDebugEventType type,
                               String className,
                               String methodName,
                               int lineNumber,
                               List<VariableDTO> fields,
                               List<VariableDTO> locals,
                               String stackTrace,
                               List<TargetApplicationElementRepresentation> list,
                               List<MethodCallInStack> methodCallInStacks,
                               String resultOfMethodInvocation) {
    	super(type);
       // this.type = type;
        this.className = className;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
        this.fields = fields;
        this.locals = locals;
        this.stackTrace = stackTrace;
        this.targetApplicationElementRepresentationList = list;
        this.methodCallInStacks = methodCallInStacks;
        this.resultOfMethodInvocation = resultOfMethodInvocation;
    }

    // ---------------- GETTERS ----------------

    public SimpleDebugEventType getType() { return super.getType(); }
    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    public int getLineNumber() { return lineNumber; }
    public List<VariableDTO> getLocals() { return locals; }
    public List<VariableDTO> getFields() { return fields; }
    public String getStackTrace() { return stackTrace; }
    public List<TargetApplicationElementRepresentation> getTargetApplicationElementRepresentationList(){return targetApplicationElementRepresentationList;}
    public List<MethodCallInStack> getMethodCallInStacks() { return methodCallInStacks;}
    public String getResultOfMethodInvocation() { return resultOfMethodInvocation; }

    // ---------------- BUILDER ----------------

    public static class Builder {
        private SimpleDebugEventType type;
        private String className;
        private String methodName;
        private int lineNumber;
        private List<VariableDTO> locals;
        private List<VariableDTO> fields;
        private String stackTrace;
        private List<TargetApplicationElementRepresentation> list;
        private String resultOfMethodInvocation;
        private List<MethodCallInStack> methodCallInStacks;

        public Builder type(SimpleDebugEventType type) {
            this.type = type;
            return this;
        }

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

        public Builder fields(List<VariableDTO> fields) {
            this.fields = fields;
            return this;
        }

        public Builder locals(List<VariableDTO> locals) {
            this.locals = locals;
            return this;
        }

        public Builder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public Builder targetApplicationElementRepresentationList(
                List<TargetApplicationElementRepresentation> list) {
            this.list = list;
            return this;
        }

        public Builder methodCallInStacks(List<MethodCallInStack> methodCallInStacks) {
            this.methodCallInStacks = methodCallInStacks;
            return this;
        }

        public Builder resultOfMethodInvocation(String result) {
            this.resultOfMethodInvocation = result;
            return this;
        }

        public SimpleDebugEventDTO build() {
            return new SimpleDebugEventDTO(
                    type,
                    className,
                    methodName,
                    lineNumber,
                    fields,
                    locals,
                    stackTrace,
                    list,
                    methodCallInStacks,
                    resultOfMethodInvocation
            );
        }
    }
}
