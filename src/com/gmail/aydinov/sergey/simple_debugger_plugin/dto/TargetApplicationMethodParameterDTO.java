package com.gmail.aydinov.sergey.simple_debugger_plugin.dto;

import java.util.Objects;

/**
 * Represents a parameter of a method in a target application.
 * Stores the parameter's name and its type.
 * <p>
 * Author: Sergei Aidinov
 * <br>
 * Email: <a href="mailto:sergey.aydinov@gmail.com">sergey.aydinov@gmail.com</a>
 * </p>
 */
public final class TargetApplicationMethodParameterDTO {

    private final String name;
    private final String typeName;

    /**
     * Constructs a parameter representation.
     *
     * @param name the name of the parameter
     * @param typeName the type of the parameter (can be null or empty)
     */
    public TargetApplicationMethodParameterDTO(String name, String typeName) {
        this.name = name;
        this.typeName = typeName;
    }

    /** @return the name of the parameter */
    public String getName() {
        return name;
    }

    /** @return the type of the parameter */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Returns a human-readable representation of the parameter.
     * Format: "name: type" if type is present, otherwise just "name".
     */
    @Override
    public String toString() {
        return Objects.isNull(typeName) || typeName.isEmpty()
                ? name
                : name + ": " + typeName;
    }
}
