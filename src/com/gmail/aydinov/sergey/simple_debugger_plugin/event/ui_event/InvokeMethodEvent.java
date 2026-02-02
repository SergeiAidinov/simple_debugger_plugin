package com.gmail.aydinov.sergey.simple_debugger_plugin.event.ui_event;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationClassOrInterfaceRepresentation;
import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationMethodDTO;

/**
 * Event representing a request to invoke a method on a target class or interface
 * from the UI.
 */
public class InvokeMethodEvent extends AbstractUIEvent {

    /** Target class or interface where the method will be invoked */
    private final TargetApplicationClassOrInterfaceRepresentation targetClass;

    /** Method to invoke */
    private final TargetApplicationMethodDTO method;

    /** Arguments for the method invocation, serialized as a string */
    private final String argumentsText;

    public InvokeMethodEvent(
            TargetApplicationClassOrInterfaceRepresentation targetClass,
            TargetApplicationMethodDTO method,
            String argumentsText
    ) {
        this.targetClass = targetClass;
        this.method = method;
        this.argumentsText = argumentsText;
    }

    public TargetApplicationClassOrInterfaceRepresentation getTargetClass() {
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
