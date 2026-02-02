package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.Set;
import com.sun.jdi.Field;

/**
 * Represents an element of the target application (class or interface) 
 * in the debugged JVM.
 * Provides access to methods, fields, name, and type.
 */
public interface TargetApplicationElementRepresentation extends Cloneable {

    /**
     * Sets the methods of this element.
     *
     * @param methods the set of methods
     */
    void setMethods(Set<TargetApplicationMethodDTO> methods);

    /**
     * Returns the methods of this element.
     *
     * @return the set of methods
     */
    Set<TargetApplicationMethodDTO> getMethods();

    /**
     * Returns the fields of this element.
     *
     * @return the set of fields
     */
    Set<Field> getFields();

    /**
     * Returns the fully qualified name of this element.
     *
     * @return the element name
     */
    String getTargetApplicationElementName();

    /**
     * Returns the type of this element (class or interface).
     *
     * @return the element type
     */
    TargetApplicationElementType getTargetApplicationElementType();

    /**
     * Returns a nicely formatted string representing this element.
     *
     * @return pretty-printed representation of this element
     */
    String prettyPrint();

    /**
     * Returns a clone of this element.
     * Implementations should create a deep copy of methods set.
     *
     * @return cloned element
     */
    TargetApplicationElementRepresentation clone();
}
