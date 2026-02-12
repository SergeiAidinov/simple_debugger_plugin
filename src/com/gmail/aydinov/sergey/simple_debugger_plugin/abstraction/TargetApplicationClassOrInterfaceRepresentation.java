package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.gmail.aydinov.sergey.simple_debugger_plugin.dto.TargetApplicationMethodDTO;
import com.sun.jdi.Field;

/**
 * Representation of a target application class or interface in the debugged JVM.
 * Contains information about its methods and fields.
 * <p>
 * Author: Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public class TargetApplicationClassOrInterfaceRepresentation implements TargetApplicationElementRepresentation {

    private final String elementName;
    private final TargetApplicationElementType elementType;
    private Set<TargetApplicationMethodDTO> methods;
    private final Set<Field> fields;

    /**
     * Constructs a new TargetApplicationClassOrInterfaceRepresentation.
     *
     * @param elementName the fully qualified name of the class or interface
     * @param elementType the type (class or interface)
     * @param methods     the set of methods in this element
     * @param fields      the set of fields in this element
     */
    public TargetApplicationClassOrInterfaceRepresentation(String elementName,
            TargetApplicationElementType elementType, Set<TargetApplicationMethodDTO> methods,
            Set<Field> fields) {
        this.elementName = elementName;
        this.elementType = elementType;
        this.methods = methods;
        this.fields = fields;
    }

    /**
     * Sets the methods of this class or interface.
     *
     * @param methods the set of methods
     */
    @Override
    public void setMethods(Set<TargetApplicationMethodDTO> methods) {
        this.methods = methods;
    }

    /**
     * Returns the methods of this class or interface.
     *
     * @return the set of methods
     */
    public Set<TargetApplicationMethodDTO> getMethods() {
        return methods;
    }

    /**
     * Returns the fields of this class or interface.
     *
     * @return the set of fields
     */
    public Set<Field> getFields() {
        return fields;
    }

    /**
     * Returns the name of this element (class or interface).
     *
     * @return the element name
     */
    public String getTargetApplicationElementName() {
        return elementName;
    }

    /**
     * Returns the type of this element (class or interface).
     *
     * @return the element type
     */
    public TargetApplicationElementType getTargetApplicationElementType() {
        return elementType;
    }

    /**
     * Returns a nicely formatted string representing this element.
     *
     * @return pretty-printed information about methods and fields
     */
    public String prettyPrint() {
        String methodsPretty = methods.stream().sorted()
                .map(m -> String.format("%-30s  %s", m.getMethodName(), m.getReturnType()))
                .collect(Collectors.joining("\n"));

        String fieldsPretty = fields.stream().sorted(Comparator.comparing(Field::name))
                .map(f -> String.format("%-30s  %s", f.name(), f.typeName()))
                .collect(Collectors.joining("\n"));

        return """
                TargetApplicationElement {
                  name  = '%s'
                  type  = %s

                  methods:
                %s

                  fields:
                %s
                }
                """.formatted(elementName, elementType, methodsPretty, fieldsPretty);
    }

    /**
     * Creates a deep copy of this object. Methods set is cloned, fields remain the same.
     *
     * @return a clone of this representation
     */
    @Override
    public TargetApplicationClassOrInterfaceRepresentation clone() {
        try {
            TargetApplicationClassOrInterfaceRepresentation copy =
                    (TargetApplicationClassOrInterfaceRepresentation) super.clone();

            // Create a new Set of methods (deep copy of the collection)
            copy.methods = new HashSet<>(this.methods);

            // Fields remain as is (JDI Field is not cloned)
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Clone not supported", e);
        }
    }
}
