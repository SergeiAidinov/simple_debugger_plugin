package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction.TopLevelElementRepresentation;

public class UserInvokedMethodEventDTO {

    /** Target class or interface where the method will be invoked */
    private final TopLevelElementRepresentation targetClass;

    /** Method to invoke */
    private final TargetApplicationMethodDTO method;

    /** Arguments for the method invocation, serialized as a string */
    private final String argumentsText;

    public UserInvokedMethodEventDTO(
            TopLevelElementRepresentation targetClass,
            TargetApplicationMethodDTO method,
            String argumentsText
    ) {
        this.targetClass = targetClass;
        this.method = method;
        this.argumentsText = argumentsText;
    }

    public TopLevelElementRepresentation getTargetClass() {
        return targetClass;
    }

    public TargetApplicationMethodDTO getMethod() {
        return method;
    }

    public String getArgumentsText() {
        return argumentsText;
    }

    @Override
    public String toString() {
        return "InvokeMethodEvent [targetClass=" + targetClass +
                ", method=" + method +
                ", argumentsText=" + argumentsText + "]";
    }
}
